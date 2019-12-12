/*
 * For the copyright information for this file, please search up the
 * directory tree for the first README.md file.
 */

package com.stealthsoftwareinc.bmc;

/* begin_imports */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
  Callable<String>
{

  public interface Channel {
    void send(byte[] buf) throws IOException;
    void recv(byte[] buf) throws IOException;
  }

  /**
   * Converts a push Channel (a Channel whose recv() method always
   * throws) into a normal Channel.
   */

  private static final class PushToPull
  implements
    Channel
  {

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
      IOException
    {
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
      IOException
    {
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

  }

  private final Channel[] channels;
  private final boolean channelsArePush;
  private final ArrayList<BlockingQueue<byte[]>> recvQueues;
  private final HashMap<Channel, Integer> channelIndexes;

  private final int partyCount;
  private final int partyIndex;

  private final String scrapDirectory;
  private final String outputFile;

  private final int func;
  private final String[] args;
  private final String logFile;

  public MpcTask(
    final String circuitFile,
    final String inputString,
    final Iterable<Channel> channels,
    final boolean channelsArePush,
    final String scrapDirectory
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
    if (scrapDirectory == null) {
      throw new IllegalArgumentException(
        "scrapDirectory == null"
      );
    }
    {
      final ArrayList<Channel> xs = new ArrayList<>();
      if (channelsArePush) {
        recvQueues = new ArrayList<>();
        channelIndexes = new HashMap<>();
      } else {
        recvQueues = null;
        channelIndexes = null;
      }
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
          if (channelsArePush) {
            recvQueues.add(null);
          }
        } else if (channelsArePush) {
          final BlockingQueue<byte[]> q =
            new LinkedBlockingQueue<byte[]>()
          ;
          xs.add(new PushToPull(channel, q));
          recvQueues.add(q);
          channelIndexes.put(channel, i);
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
      this.channelsArePush = channelsArePush;
    }
    this.scrapDirectory = scrapDirectory;
    {
      final ArrayList<String> args = new ArrayList<>();
      args.add("--debug2");
      args.add("--circuit");
      args.add(circuitFile);
      args.add("--input");
      args.add(inputString);
      args.add("--outfile_path");
      args.add(scrapDirectory);
      args.add("--outfile_id");
      args.add("output");
      outputFile = Paths.get(scrapDirectory, "output").toString();
      if (partyCount == 2) {
        func = funcTwoPartyMpc;
        if (partyIndex == 0) {
          args.add("--as_server");
        } else {
          args.add("--as_client");
        }
        args.add("--ot_filepath");
        args.add(Paths.get(scrapDirectory, "ot").toString());
      } else {
        func = funcNPartyMpcByGate;
        args.add("--num_parties");
        args.add(String.valueOf(partyCount));
        args.add("--self_index");
        args.add(String.valueOf(partyIndex));
        args.add("--ot_dir");
        args.add(Paths.get(scrapDirectory, "ots").toString());
      }
      this.args = args.toArray(new String[0]);
      logFile = scrapDirectory + "/log";
    }
  }

  /**
   * Performs a push receive when channelsArePush is enabled.
   */

  public final void recv(
    final int channelIndex,
    final byte[] buf
  ) throws
    IOException
  {
    if (!channelsArePush) {
      throw new IllegalStateException(
        "channelsArePush == false"
      );
    }
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
    if (buf == null) {
      throw new IllegalArgumentException(
        "buf == null"
      );
    }
    try {
      recvQueues.get(channelIndex).put(Arrays.copyOf(buf, buf.length));
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
  }

  /**
   * Performs a push receive when channelsArePush is enabled.
   */

  public final void recv(
    final Channel channel,
    final byte[] buf
  ) throws
    IOException
  {
    if (!channelsArePush) {
      throw new IllegalStateException(
        "channelsArePush == false"
      );
    }
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
    final Integer i = channelIndexes.get(channel);
    if (i == null) {
      throw new IllegalArgumentException(
        "unknown channel"
      );
    }
    recv(i, buf);
  }

  private String jniError;

  private void jniSetError(
    final String m
  ) {
    jniError = m;
  }

  private void jniSend(
    final Channel channel,
    final byte[] buf
  ) throws
    IOException
  {
    channel.send(buf);
  }

  private void jniRecv(
    final Channel channel,
    final byte[] buf
  ) throws
    IOException
  {
    channel.recv(buf);
  }

  private static final int funcNPartyMpcByGate = 0;
  private static final int funcTwoPartyMpc = 1;

  private native int jniCall(
    int func,
    String[] args,
    Channel[] channels,
    String logFile
  );

  @Override
  public final String call(
  ) throws
    Exception
  {
    Files.createDirectories(Paths.get(scrapDirectory));
    jniError = null;
    final int s = jniCall(func, args, channels, logFile);
    if (s != 0) {
      throw new RuntimeException(jniError);
    }
    final StringBuilder result = new StringBuilder();
    try (
      final FileInputStream x1 =
        new FileInputStream(outputFile)
      ;
      final InputStreamReader x2 =
        new InputStreamReader(x1, StandardCharsets.UTF_8)
      ;
      final BufferedReader lines =
        new BufferedReader(x2)
      ;
    ) {
      int state = 0;
      while (true) {
        final String line = lines.readLine();
        if (line == null) {
          break;
        }
        switch (state) {
          case 0: if (true) {
            if (line.matches("^Outputs \\((0|[1-9][0-9]*)\\):$")) {
              state = 1;
            }
          } break;
          case 1: if (true) {
            if (line.isEmpty()) {
              state = 2;
            } else {
              final String[] fields = line.split(":", -1);
              if (fields.length != 2) {
                throw new RuntimeException("output parsing error");
              }
              if (!fields[1].matches("^(0|-?[1-9][0-9]*)$")) {
                throw new RuntimeException("output parsing error");
              }
              if (result.length() != 0) {
                result.append(',');
              }
              result.append(fields[1]);
            }
          } break;
        }
      }
      if (state == 0) {
        throw new RuntimeException("output parsing error");
      }
    }
    return result.toString();
  }

}
