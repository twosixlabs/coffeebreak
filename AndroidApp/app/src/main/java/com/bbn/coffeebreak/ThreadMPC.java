/*
 * Usage example:
 *
 * ThreadMPC mpc = new ThreadMPC(...);
 * CompletableFuture<Void> f = CompletableFuture.runAsync(mpc);
 * ...
 * f.join();
 * if (mpc.getException() != null) {
 *   throw mpc.getException();
 * }
 *
 */

package com.bbn.coffeebreak;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.bbn.coffeebreak.EncodedLatLon;
import com.stealthsoftwareinc.bmc.MpcTask;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class ThreadMPC
implements
  Runnable
{

  private static final class MyChannel
  implements
    MpcTask.Channel
  {

    private final CoffeeChannel channel;

    public MyChannel(
      final CoffeeChannel channel
    ) {
      if (channel == null) {
        throw new IllegalArgumentException(
          "channel == null"
        );
      }
      this.channel = channel;
    }

    @Override
    public final void send(
      final byte[] buf
    ) throws IOException {
      channel.send(buf);
    }

    @Override
    public final void recv(
      final byte[] buf
    ) throws IOException {
      channel.recv(buf);
    }

    @Override
    public final boolean isPush(
    ) {
      return false;
    }

  }

  private final String TAG = "[ThreadMPC]";
  private static final Object assetMutex = new Object();

  private final String meeting;
  private final ResultReceiver receiver;
  private final int partyCount;
  private final int partyIndex;
  private final AssetManager assets;
  private final String assetDir;
  private final String filesDir;
  private final MpcTask mpcTask;
  private Exception exception = null;

  public ThreadMPC(
    final Context context,
    final String meeting,
    final Iterable<CoffeeChannel> channels,
    final long location,
    final ResultReceiver receiver
  ) {
    if (context == null) {
      throw new IllegalArgumentException(
        "context == null"
      );
    }
    if (channels == null) {
      throw new IllegalArgumentException(
        "channels == null"
      );
    }
    this.meeting = meeting;
    this.receiver = receiver;
    final ArrayList<MpcTask.Channel> myChannels = new ArrayList<>();
    {
      int i = 0;
      int j = -1;
      for (final CoffeeChannel channel : channels) {
        if (channel == null) {
          if (j != -1) {
            throw new IllegalArgumentException(
              "channels contains more than one null: " +
              "channels[" + j + "] and channels[" + i + "]"
            );
          }
          j = i;
          myChannels.add(null);
        } else {
          myChannels.add(new MyChannel(channel));
        }
        ++i;
      }
      partyCount = i;
      partyIndex = j;
      if (partyCount < 2) {
        throw new IllegalArgumentException(
          "channels contains fewer than two elements"
        );
      }
      if (partyIndex == -1) {
        throw new IllegalArgumentException(
          "channels does not contain a null"
        );
      }
    }
    this.assets = context.getAssets();
    this.assetDir = "ThreadMPC";
    this.filesDir = context.getFilesDir() + "/" + this.assetDir;
    {
      final ArrayList<String> libraryNames = new ArrayList<>();
      libraryNames.add("libc++_shared.so");
      libraryNames.add("libgmp.so");
      libraryNames.add("libnettle.so.6");
      libraryNames.add("libalsz.so");
      libraryNames.add("libbmc.so");
      final List<String> abis = Arrays.asList(Build.SUPPORTED_ABIS);
      final String arch;
      if (abis.contains("arm64-v8a")) {
        arch = "aarch64";
      } else if (abis.contains("x86_64")) {
        arch = "x86_64";
      } else {
        throw new RuntimeException("unknown ABI");
      }
      for (final String libraryName : libraryNames) {
        final String libraryFile =
          loadAsset("lib/" + arch + "/" + libraryName, false)
        ;
        System.load(libraryFile);
      }
    }
    final String circuitFile =
      loadAsset("coffeeshop_n_" + partyCount + ".cbg", false)
    ;
    final String inputString =
      "x" + String.valueOf(partyIndex) + "_lat=" +
      String.valueOf((int)(location >>> 32)) +
      ",x" + String.valueOf(partyIndex) + "_lng=" +
      String.valueOf((int)location)
    ;
    mpcTask = new MpcTask(circuitFile, inputString, myChannels, true);
  }

  public final Exception getException(
  ) {
    return this.exception;
  }

  @SuppressLint("NewApi")
  private String loadAsset(
    final String asset,
    final boolean executable
  ) {
    try {
      synchronized (ThreadMPC.assetMutex) {
        final String assetV = asset + ".version";
        final String assetFile = this.assetDir + "/" + asset;
        final String assetHash = this.assetDir + "/" + assetV;
        final String filesFile = this.filesDir + "/" + asset;
        final String filesHash = this.filesDir + "/" + assetV;
        Log.d(this.TAG,
          "loading asset: " + assetFile
        );
        if (new File(filesFile).exists() && new File(filesHash).exists()) {
          try (
            final InputStream a1 = this.assets.open(assetHash);
            final BufferedInputStream a2 = new BufferedInputStream(a1);
            final FileInputStream b1 = new FileInputStream(filesHash);
            final BufferedInputStream b2 = new BufferedInputStream(b1);
          ) {
            while (true) {
              final int ax = a2.read();
              final int bx = b2.read();
              if (ax != bx) {
                break;
              }
              if (ax == -1) {
                ThreadMPC.setFilePermissions(filesFile, executable);
                ThreadMPC.setFilePermissions(filesHash, false);
                return filesFile;
              }
            }
          }
        }
        Files.deleteIfExists(Paths.get(filesFile));
        Files.deleteIfExists(Paths.get(filesHash));
        try (final InputStream src = this.assets.open(assetFile)) {
          final Path tmp = Paths.get(filesFile + ".tmp");
          final Path dst = Paths.get(filesFile);
          Files.createDirectories(tmp.getParent());
          Files.deleteIfExists(tmp);
          Files.copy(src, tmp);
          Files.move(tmp, dst);
        }
        try (final InputStream src = this.assets.open(assetHash)) {
          final Path tmp = Paths.get(filesHash + ".tmp");
          final Path dst = Paths.get(filesHash);
          Files.createDirectories(tmp.getParent());
          Files.deleteIfExists(tmp);
          Files.copy(src, tmp);
          Files.move(tmp, dst);
        }
        ThreadMPC.setFilePermissions(filesFile, executable);
        ThreadMPC.setFilePermissions(filesHash, false);
        return filesFile;
      }
    } catch (final Exception e) {
      throw new RuntimeException(
        "missing asset? " + this.assetDir + "/" + asset,
        e
      );
    }
  }

  @Override
  public final void run(
  ) {
    this.exception = null;
    try {
      final String[] r = mpcTask.call().split(",", -1);
      final long a = Long.parseLong(r[0]);
      final long b = Long.parseLong(r[1]);
      final EncodedLatLon c =
        EncodedLatLon.convertMpcResultToEncodedLatLon(
          (a << 32) | (int)b, this.partyCount
        )
      ;
      final EncodedLatLon locationAnswer =
        new EncodedLatLon(
          c.getLatitude() / this.partyCount,
          c.getLongitude() / this.partyCount
        )
      ;
      Log.d(this.TAG,
        "sending results to receiver"
      );
      if (this.receiver != null) {
        final Bundle results = new Bundle();
        results.putFloat("latitude", locationAnswer.getLatitude());
        results.putFloat("longitude", locationAnswer.getLongitude());
        results.putString("meetingID", meeting);
        this.receiver.send(0, results);
      }
    } catch (final Exception e1) {
      try {
        Log.d(TAG, "mpcTask.getLog(): " + mpcTask.getLog());
      } catch (final Exception e2) {
        e1.addSuppressed(e2);
      }
      try {
        if (this.receiver != null) {
          this.receiver.send(1, null);
        }
      } catch (final Exception e2) {
        e1.addSuppressed(e2);
      }
      this.exception = e1;
    }
  }

  @SuppressLint("NewApi")
  private static void setFilePermissions(
    final String file,
    final boolean executable
  ) {
    try {
      if (executable) {
        Files.setPosixFilePermissions(
          Paths.get(file),
          new HashSet<PosixFilePermission>(
            Arrays.asList(
              PosixFilePermission.OWNER_READ,
              PosixFilePermission.OWNER_WRITE,
              PosixFilePermission.OWNER_EXECUTE
            )
          )
        );
      } else {
        Files.setPosixFilePermissions(
          Paths.get(file),
          new HashSet<PosixFilePermission>(
            Arrays.asList(
              PosixFilePermission.OWNER_READ,
              PosixFilePermission.OWNER_WRITE
            )
          )
        );
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

}
