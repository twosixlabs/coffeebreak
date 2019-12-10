package com.bbn.coffeebreak;

import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AmqpMpcChannel implements CoffeeChannel {

    private Channel mChannel;
    private String mSendQueue;
    private String mRecvQueue;
    private String mUsername;
    private Consumer mInviteConsumer;
    private BlockingQueue<byte[]> mBlockingQueue;

    // slop buffer for implementing byte orientation in recv()
    private byte[] slop = new byte[1024]; // starting capacity
    private int slopLen = 0; // how much can be consumed in total
    private int slopIdx = 0; // how much has been consumed so far

    private static final String TAG = "AMQP_MPC_Channel";
    private static final String MESSAGE_TYPE = "MPC_ROUND";
    private static final int BLOCKING_QUEUE_CAPACITY = 250;

    public AmqpMpcChannel(Channel channel, String meetingId, String dest, String username){
        mChannel = channel;
        mSendQueue = "MPC:LOCATION:" + meetingId + ":" + username + ":" + dest;
        mRecvQueue = "MPC:LOCATION:" + meetingId + ":" + dest + ":" + username;
        mUsername = username;
        mBlockingQueue = new ArrayBlockingQueue<byte[]>(BLOCKING_QUEUE_CAPACITY);

        try{
            mChannel.queueDeclare(mSendQueue, false, false, true, null);
            mChannel.queueDeclare(mRecvQueue, false, false, true, null);
            mInviteConsumer = new DefaultConsumer(mChannel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    super.handleDelivery(consumerTag, envelope, properties, body);
                    Log.d(TAG, "Received message from AMQP");
                    try {
                        mBlockingQueue.put(Arrays.copyOf(body, body.length));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException(e);
                    }
                }
            };
            mChannel.basicConsume(mRecvQueue, mInviteConsumer);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void send(byte[] data) throws IOException {
        //set the message properties
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .correlationId(UUID.randomUUID().toString())
                .type(MESSAGE_TYPE)
                .replyTo(mUsername)
                .build();

        mChannel.basicPublish("", mSendQueue, basicProperties, data);
    }

    @Override
    public void recv(byte[] buf) throws IOException {
        int bufIdx = slopLen - slopIdx;
        if (bufIdx >= buf.length) {
            // slop is enough, no need to consume any messages
            System.arraycopy(slop, slopIdx, buf, 0, buf.length);
            slopIdx += buf.length;
            return;
        }
        // otherwise, start by consuming all the slop
        System.arraycopy(slop, slopIdx, buf, 0, bufIdx);
        // then consume messages until we're done
        while (bufIdx < buf.length) {
            int want = buf.length - bufIdx;
            byte[] r;
            try {
                r = mBlockingQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            if (r.length < want) {
                // r isn't enough, another loop iteration will occur
                System.arraycopy(r, 0, buf, bufIdx, r.length);
                bufIdx += r.length;
            } else {
                // r is enough, this will be the last loop iteration
                System.arraycopy(r, 0, buf, bufIdx, want);
                bufIdx += want;
                // put any remaining data into the slop buffer
                slopLen = r.length - want;
                slopIdx = 0;
                if (slopLen > slop.length)
                    slop = new byte[slopLen];
                System.arraycopy(r, want, slop, 0, slopLen);
            }
        }
    }
}
