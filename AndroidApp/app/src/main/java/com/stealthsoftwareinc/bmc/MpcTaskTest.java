/*
 * For the copyright information for this file, please search up the
 * directory tree for the first README.md file.
 */

/*

# How to run this thing (adjust as needed):

# Work from the directory that contains
# com/stealthsoftwareinc/bmc/MpcTaskTest.java

# You need a local RabbitMQ server (localhost:5672).
# You need gmp, nettle, alsz (Stealth), and bmc (Stealth) installed.

# You need amqp-client-5.7.3.jar and its dependencies:
wget https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.7.3/amqp-client-5.7.3.jar
wget https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.26/slf4j-api-1.7.26.jar
wget https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.26/slf4j-simple-1.7.26.jar

# First do this
export CLASSPATH=.:*
javac com/stealthsoftwareinc/bmc/MpcTaskTest.java

# Run this to test two_party_mpc
(
  java com.stealthsoftwareinc.bmc.MpcTaskTest \
    2 0 scrap0 \
    coffeeshop_n_2_lookahead_4.cbl \
    x0_lat=1,x0_lng=-1 \
  &
  java com.stealthsoftwareinc.bmc.MpcTaskTest \
    2 1 scrap1 \
    coffeeshop_n_2_lookahead_4.cbl \
    x1_lat=1,x1_lng=-1 \
  &
  wait
)
# It should print out "result: 2,-2" twice.

*/

package com.stealthsoftwareinc.bmc;

/* begin_imports */

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.stealthsoftwareinc.bmc.MpcTask;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/* end_imports */

public final class MpcTaskTest
{

  private final static class MyChannel
  implements
    MpcTask.Channel
  {

    private final Channel channel;
    private final String sendQueue;

    public MyChannel(
      final Channel channel,
      final String sendQueue
    ) {
      this.channel = channel;
      this.sendQueue = sendQueue;
    }

    @Override
    public final void send(
      final byte[] buf
    ) throws
      IOException
    {
      channel.basicPublish("", sendQueue, null, buf);
    }

    @Override
    public final void recv(
      final byte[] buf
    ) throws
      IOException
    {
       throw new IOException(); // we're a push channel
    }
  }

  public static void main(
    final String... args
  ) throws
    Exception
  {

    System.loadLibrary("gmp");
    System.loadLibrary("nettle");
    System.loadLibrary("alsz");
    System.loadLibrary("bmc");

    final ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");

    try (
      final Connection connection = factory.newConnection();
      final Channel channel = connection.createChannel();
    ) {

      final int partyCount = Integer.parseInt(args[0]);
      final int partyIndex = Integer.parseInt(args[1]);
      final String scrapDirectory = args[2];
      final String circuitFile = args[3];
      final String inputString = args[4];

      final ArrayList<MpcTask.Channel> channels = new ArrayList<>();
      for (int i = 0; i != partyCount; ++i) {
        if (i == partyIndex) {
          channels.add(null);
        } else {
          final String sendQueue = partyIndex + "-" + i;
          final String recvQueue = i + "-" + partyIndex;
          channel.queueDeclare(sendQueue, false, false, true, null);
          channel.queueDeclare(recvQueue, false, false, true, null);
          channels.add(new MyChannel(channel, sendQueue));
        }
      }
      final MpcTask mpc =
        new MpcTask(
          circuitFile,
          inputString,
          channels,
          true,
          scrapDirectory
        )
      ;
      for (int i = 0; i != partyCount; ++i) {
        if (i != partyIndex) {
          final String recvQueue = i + "-" + partyIndex;
          final MpcTask.Channel c = channels.get(i);
          channel.basicConsume(
            recvQueue,
            new DefaultConsumer(channel) {
              @Override
              public final void handleDelivery(
                final String consumerTag,
                final Envelope envelope,
                final AMQP.BasicProperties properties,
                final byte[] body
              ) throws
                IOException
              {
                mpc.recv(c, body);
              }
            }
          );
        }
      }
      final String result = mpc.call();
      System.out.println("result: " + result);

    }

  }

}
