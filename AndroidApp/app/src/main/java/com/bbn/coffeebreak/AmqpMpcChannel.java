package com.bbn.coffeebreak;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

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

    private static String consumerTag;

    private CountDownTimer mpctimer;
    private Handler mHandler;

    public AmqpMpcChannel(Channel channel, String meetingId, String dest, String username, Handler handler) throws IOException{
        mChannel = channel;
        mSendQueue = "MPC:LOCATION:" + meetingId + ":" + username + ":" + dest;
        mRecvQueue = "MPC:LOCATION:" + meetingId + ":" + dest + ":" + username;
        mUsername = username;
        mBlockingQueue = new LinkedBlockingQueue<byte[]>();
        mHandler = handler;

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x-expires", 120000);

        mChannel.queueDeclare(mSendQueue, false, false, true, args);
        mChannel.queueDeclare(mRecvQueue, false, false, true, args);

        mInviteConsumer = new DefaultConsumer(mChannel){
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                super.handleDelivery(consumerTag, envelope, properties, body);
                Log.d(TAG, "Received message from AMQP: " + envelope);

                try {
                    mBlockingQueue.put(Arrays.copyOf(body, body.length));

                    // Interrupts the MPC thread if a message has not been sent or received in 30 seconds
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "interrupting 1 - handler: " + mHandler);

                            Thread[] tarray = new Thread[Thread.activeCount()];
                            Thread.enumerate(tarray);
                            for (Thread t : tarray) {
                                if ((t.getName()).equals("MPC-Handler-Thread")) {
                                    Log.d(TAG, "t.getState(): " + t.getState());
                                    Log.d(TAG, "Thread.currentThread().getState(): " + Thread.currentThread().getState());
                                    Log.d(TAG, "handler: " + mHandler);
                                    if (t.getState().equals(Thread.State.WAITING) && !t.isInterrupted()) {
                                        Log.d(TAG, "interrupt");
                                        t.interrupt();
                                    }
                                }
                            }

                        }
                    }, null, 30000);

                } catch (InterruptedException e) {
                    Log.d(TAG, "error in mpc channel");
                    mHandler.removeCallbacksAndMessages(null);
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
        };

        consumerTag = mChannel.basicConsume(mRecvQueue, true, mInviteConsumer);
    }

    public String getmSendQueue(){
        return mSendQueue;
    }

    public String getmRecvQueue(){
        return mRecvQueue;
    }

    public String getConsumerTag(){
        return consumerTag;
    }

    @Override
    public void send(byte[] data) throws IOException {
        String message = new String(data);

        //set the message properties
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
                .correlationId(UUID.randomUUID().toString())
                .type(MESSAGE_TYPE)
                .replyTo(mUsername)
                .build();

        if(mChannel.isOpen()) {
            mChannel.basicPublish("", mSendQueue, basicProperties, data);
        } else {
            throw new IOException("Channel is closed.");
        }
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
                Log.d(TAG, "THREAD INTERRUPTED EXCEPTION: " + e);
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
