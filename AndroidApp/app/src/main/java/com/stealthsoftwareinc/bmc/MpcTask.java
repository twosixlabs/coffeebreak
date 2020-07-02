/*
 * For the copyright information for this file, please search up the
 * directory tree for the first README.md file.
 */

package com.stealthsoftwareinc.bmc;

/* begin_imports */

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

/* end_imports */

/**
 * Performs an MPC calculation.
 */

public final class MpcTask
        implements
        Callable<String> {

    public interface Channel {

        final String TAG = "[MpcTask-Channel]";

        void send(
                byte[] buf
        ) throws IOException;

        default void recv(
                byte[] buf
        ) throws IOException {
            throw new IllegalStateException("isPush");
        }

        default boolean isPush(
        ) {
            return true;
        }

    }

    /**
     * Converts a push channel into a pull channel.
     */

    private static final class PushToPull
            implements
            Channel {

        private final String TAG = "[MpcTask-PushToPull]";

        private final Channel channel;
        private final BlockingQueue<byte[]> queue;

        // slop buffer for implementing recv()
        private byte[] slop = new byte[1024]; // starting capacity
        private int slopLen = 0; // how much can be consumed in total
        private int slopIdx = 0; // how much has been consumed so far

        public PushToPull(
                final Channel channel,
                final BlockingQueue<byte[]> queue
        ) {
            if (channel == null) {
                throw new IllegalArgumentException(
                        "channel == null"
                );
            }
            if (queue == null) {
                throw new IllegalArgumentException(
                        "queue == null"
                );
            }
            this.channel = channel;
            this.queue = queue;
        }

        @Override
        public final void send(
                final byte[] buf
        ) throws
                IOException {
            if (buf == null) {
                throw new IllegalArgumentException(
                        "buf == null"
                );
            }
            channel.send(buf);
        }

        @Override
        public final void recv(
                final byte[] buf
        ) throws
                IOException {
            if (buf == null) {
                throw new IllegalArgumentException(
                        "buf == null"
                );
            }
            int bufIdx = slopLen - slopIdx;
            if (bufIdx >= buf.length) {
                // slop is enough, no need to consume any messages
                System.arraycopy(slop, slopIdx, buf, 0, buf.length);
                slopIdx += buf.length;
                return;
            }
            // otherwise, start by consuming all the slop
            System.arraycopy(slop, slopIdx, buf, 0, bufIdx);
            // then consume queue entries until we're done
            while (bufIdx < buf.length) {
                final int want = buf.length - bufIdx;
                final byte[] r;
                try {
                    r = queue.take();
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
                    if (slopLen > slop.length) {
                        slop = new byte[slopLen];
                    }
                    System.arraycopy(r, want, slop, 0, slopLen);
                }
            }
        }

        @Override
        public final boolean isPush(
        ) {
            return false;
        }

    }

    private final String TAG = "[MpcTask]";

    private final Channel[] channels;
    private final HashMap<Channel, BlockingQueue<byte[]>> channelQueues;

    private final int partyCount;
    private final int partyIndex;

    private final int func;
    private final String[] args;
    private final String[] log;

    public MpcTask(
            final String circuitFile,
            final String inputString,
            final Iterable<Channel> channels,
            final boolean logEnabled
    ) {
        if (circuitFile == null) {
            throw new IllegalArgumentException(
                    "circuitFile == null"
            );
        }
        if (inputString == null) {
            throw new IllegalArgumentException(
                    "inputString == null"
            );
        }
        if (channels == null) {
            throw new IllegalArgumentException(
                    "channels == null"
            );
        }

        final ArrayList<Channel> xs = new ArrayList<>();
        channelQueues = new HashMap<>();
        int i = 0;
        int j = -1;
        for (final Channel channel : channels) {
            if (channel == null) {
                if (j != -1) {
                    throw new IllegalArgumentException(
                            "channels contains more than one null: " +
                                    "channels[" + j + "] and channels[" + i + "]"
                    );
                }
                j = i;
                xs.add(null);
            } else if (channel.isPush()) {
                final BlockingQueue<byte[]> q =
                        new LinkedBlockingQueue<byte[]>();
                xs.add(new PushToPull(channel, q));
                channelQueues.put(channel, q);
            } else {
                xs.add(channel);
            }
            ++i;
        }
        partyCount = i;
        partyIndex = j;
        if (partyCount < 2) {
            throw new IllegalArgumentException(
                    "partyCount < 2"
            );
        }
        if (partyIndex == -1) {
            throw new IllegalArgumentException(
                    "channels does not contain a null"
            );
        }
        this.channels = xs.toArray(new Channel[0]);

        final ArrayList<String> args = new ArrayList<>();
        if (logEnabled) {
            args.add("--debug2");
        }
        args.add("--circuit");
        args.add(circuitFile);
        args.add("--input");
        args.add(inputString);
        args.add("--nowrite_ot");
        if (false && partyCount == 2) {
            func = funcTwoPartyMpc;
            if (partyIndex == 0) {
                args.add("--as_server");
            } else {
                args.add("--as_client");
            }
        } else {
            func = funcNPartyMpcByGate;
            args.add("--num_parties");
            args.add(String.valueOf(partyCount));
            args.add("--self_index");
            args.add(String.valueOf(partyIndex));
        }
        this.args = args.toArray(new String[0]);

        log = ((logEnabled) ? new String[1] : null);
    }

    /**
     * Performs a receive for a push channel.
     */

    public final void recv(
            final int channelIndex,
            final byte[] buf
    ) throws
            IOException {
        if (channelIndex < 0) {
            throw new IllegalArgumentException(
                    "channelIndex < 0"
            );
        }
        if (channelIndex >= channels.length) {
            throw new IllegalArgumentException(
                    "channelIndex >= channels.length"
            );
        }
        if (channelIndex == partyIndex) {
            throw new IllegalArgumentException(
                    "channelIndex == partyIndex"
            );
        }
        recv(channels[channelIndex], buf);
    }

    /**
     * Performs a receive for a push channel.
     */

    public final void recv(
            final Channel channel,
            final byte[] buf
    ) throws
            IOException {
        if (channel == null) {
            throw new IllegalArgumentException(
                    "channel == null"
            );
        }
        if (buf == null) {
            throw new IllegalArgumentException(
                    "buf == null"
            );
        }
        final BlockingQueue<byte[]> queue = channelQueues.get(channel);
        if (queue == null) {
            throw new IllegalArgumentException(
                    "unknown channel"
            );
        }
        try {
            queue.put(Arrays.copyOf(buf, buf.length));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private String jniError;

    private void jniSetError(
            final String m
    ) {
        jniError = m;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void jniSend(
            final Channel channel,
            final byte[] buf
    ) throws
            IOException {
        channel.send(buf);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void jniRecv(
            final Channel channel,
            final byte[] buf
    ) throws
            IOException {
        channel.recv(buf);
    }

    private static final int funcNPartyMpcByGate = 0;
    private static final int funcTwoPartyMpc = 1;

    @SuppressLint("KeepMissing")
    private native int jniCall(
            int func,
            String[] args,
            Channel[] channels,
            String[] results,
            String[] log
    );

    @Override
    public final String call(
    ) throws
            Exception {
        if (log != null) {
            log[0] = null;
        }
        jniError = null;
        final String[] results = new String[1];
        final int s = jniCall(func, args, channels, results, log);
        if (s != 0) {
            throw new RuntimeException(jniError);
        }
        return results[0];
    }

    public final String getLog(
    ) {
        return ((log != null) ? log[0] : null);
    }

}
