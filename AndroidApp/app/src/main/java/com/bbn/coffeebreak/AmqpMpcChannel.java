package com.bbn.coffeebreak;

import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AmqpMpcChannel implements CoffeebreakChannel {

    private Channel mChannel;
    private String mSendQueue;
    private String mRecvQueue;
    private String mUsername;
    private Consumer mInviteConsumer;
    private BlockingQueue<byte[]> mBlockingQueue;

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
                    mBlockingQueue.offer(body);
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
    public byte[] receive(int buflen) throws IOException {
        byte[] response = mBlockingQueue.poll();
        if(response == null)
            throw new IOException("received NULL from polling operation");

        if(response.length != buflen)
            throw new IOException("received " + response.length + " bytes, but was expecting " + buflen);

        return response;
    }
}
