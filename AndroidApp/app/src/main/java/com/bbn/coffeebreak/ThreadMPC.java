package com.bbn.coffeebreak;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.bbn.coffeebreak.EncodedLatLon;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class ThreadMPC
implements
  Runnable
{

public enum Mode {
  DUMMY,
  SHARED_EXECUTABLES,
  STATIC_EXECUTABLES
}

public enum CircuitType {
  CBG,
  CBL
}

private interface DummyDeliveryHandler {
  public void handleDelivery(
    int i,
    String consumerTag,
    Envelope envelope,
    AMQP.BasicProperties properties,
    byte[] body
  ) throws
    Exception
  ;
}

private static class ExecResult {
  public StringBuilder stdout = new StringBuilder();
  public StringBuilder stderr = new StringBuilder();
  public Exception stdoutException = null;
  public Exception stderrException = null;
  public int exitStatus = 0;
}

private Exception exception = null;

private final Context context;
private final AssetManager assets;

private static final Object assetMutex = new Object();

private static final Charset utf8 = Charset.forName("UTF-8");

private Mode mode;
private CircuitType circuitType;
private int cblLookahead;

private final String meeting;
private final Channel channel;
private final String[] partyNames;

private final String defaultAmqpHost;
private final int defaultAmqpPort;
private static final String DEFAULT_AMQP_USERNAME = "guest";
private static final String DEFAULT_AMQP_PASSWORD = "guest";
private String amqpHost;
private int amqpPort;
private String amqpUsername;
private String amqpPassword;

private boolean amqpSslEnabled;
private String amqpSslCaCertFile;
private boolean amqpSslVerifyPeer;
private boolean amqpSslVerifyHostname;
private String amqpSslClientCertFile;
private String amqpSslClientKeyFile;

private final ResultReceiver receiver;

private final String queuePrefix;
private final int queuePrefixMaxLength = 50;
private final String queuePrefixHashAlgorithm = "SHA-256";

private final int partyCount;

private final String[] partySlugs;
private final int partySlugMaxLength = 50;

private final String partyName;
private final int partyIndex;
private final String partySlug;

private final long location;
private final byte[] locationRaw;

private final long[] locations;

private final String[] locationSendQueues;
private final String[] locationRecvQueues;

private final Map<String, Object> queueArguments;
private final int queueTimeout = 60;
private final TimeUnit queueTimeoutUnit = TimeUnit.MINUTES;

private final String[] consumers;
private AtomicReferenceArray<Exception> consumerExceptions;
private CountDownLatch consumerLatch;
private final long consumerTimeout = 600;
private final TimeUnit consumerTimeoutUnit = TimeUnit.SECONDS;

private final String assetDir;
private final String filesDir;

private String mpcExecutable = null;
private String ldLibraryPath = null;
private String locationCircuit = null;
private String locationExecDir = null;

private final String TAG = "[ThreadMPC]";

public ThreadMPC(
  final Context context,
  final String meeting,
  final Channel channel,
  final Iterable<String> partyNames,
  final String partyName,
  final long location,
  final ResultReceiver receiver
) {

  this.mode = ThreadMPC.Mode.DUMMY;
  this.circuitType = ThreadMPC.CircuitType.CBG;
  this.cblLookahead = 1;

  if (context == null) {
    throw new IllegalArgumentException(
      "context == null"
    );
  }
  this.context = context;

  this.assets = this.context.getAssets();

  if (meeting == null) {
    throw new IllegalArgumentException(
      "meeting == null"
    );
  }
  this.meeting = meeting;

  if (channel == null) {
    throw new IllegalArgumentException(
      "channel == null"
    );
  }
  this.channel = channel;

  if (partyNames == null) {
    throw new IllegalArgumentException(
      "partyNames == null"
    );
  }
  {
    final TreeSet<String> xs = new TreeSet<String>();
    for (final String x : partyNames) {
      if (x == null) {
        throw new IllegalArgumentException(
          "partyNames contains a null element"
        );
      }
      if (!xs.add(x)) {
        throw new IllegalArgumentException(
          "partyNames contains a repeated element: " + x
        );
      }
    }
    if (xs.isEmpty()) {
      throw new IllegalArgumentException(
        "partyNames is empty"
      );
    }
    this.partyNames = xs.toArray(new String[0]);
  }

  if (partyName == null) {
    throw new IllegalArgumentException(
      "partyName == null"
    );
  }
  this.partyIndex = Arrays.binarySearch(this.partyNames, partyName);
  if (this.partyIndex < 0) {
    throw new IllegalArgumentException(
      "partyNames does not contain partyName: " + partyName
    );
  }
  this.partyName = partyName;

  this.location = location;

  this.locationRaw = new byte[8];
  ByteBuffer.wrap(this.locationRaw).putLong(0, this.location);

  this.receiver = receiver;

  this.defaultAmqpHost = this.channel.getConnection().getAddress().getHostAddress();
  this.defaultAmqpPort = this.channel.getConnection().getPort();
  this.amqpHost = this.defaultAmqpHost;
  this.amqpPort = this.defaultAmqpPort;
  this.amqpUsername = ThreadMPC.DEFAULT_AMQP_USERNAME;
  this.amqpPassword = ThreadMPC.DEFAULT_AMQP_PASSWORD;

  this.amqpSslEnabled = false;
  this.amqpSslCaCertFile = null;
  this.amqpSslVerifyPeer = false;
  this.amqpSslVerifyHostname = false;
  this.amqpSslClientCertFile = null;
  this.amqpSslClientKeyFile = null;

  if (this.meeting.equals(this.slugify(this.meeting, this.queuePrefixMaxLength))) {
    this.queuePrefix = "ThreadMPC:" + this.meeting;
  } else {
    final MessageDigest hasher;
    try {
      hasher = MessageDigest.getInstance(this.queuePrefixHashAlgorithm);
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    for (int i = 0; i != this.meeting.length(); ++i) {
      final char c = this.meeting.charAt(i);
      hasher.update((byte)((c >>> 8) & 0xFF));
      hasher.update((byte)((c >>> 0) & 0xFF));
    }
    final byte[] b = hasher.digest();
    final StringBuilder s = new StringBuilder();
    final String hexDigits = "0123456789ABCDEF";
    for (int i = 0; i != b.length; ++i) {
      s.append(hexDigits.charAt((b[i] >>> 4) & 0xF));
      s.append(hexDigits.charAt((b[i] >>> 0) & 0xF));
    }
    if (s.length() > this.queuePrefixMaxLength) {
      s.setLength(this.queuePrefixMaxLength);
    }
    this.queuePrefix = "ThreadMPC:" + s.toString();
  }

  this.partyCount = this.partyNames.length;

  this.partySlugs = new String[this.partyCount];
  for (int i = 0; i != this.partyCount; ++i) {
    this.partySlugs[i] =
      this.slugify(this.partyNames[i], this.partySlugMaxLength)
    ;
  }

  this.partySlug = this.partySlugs[this.partyIndex];


  this.locations = new long[this.partyCount];
  this.locations[this.partyIndex] = this.location;

  this.locationSendQueues = new String[this.partyCount];
  this.locationRecvQueues = new String[this.partyCount];
  {
    final String pLoc = this.queuePrefix + ":location";
    final String a = this.partyIndex + ":" + this.partySlug;
    for (int i = 0; i != this.partyCount; ++i) {
      final String b = i + ":" + this.partySlugs[i];
      this.locationSendQueues[i] = pLoc + ":" + a + ":" + b;
      this.locationRecvQueues[i] = pLoc + ":" + b + ":" + a;
    }
  }

  this.consumers = new String[this.partyCount];
  this.consumerExceptions = null;

  {
    final Map<String, Object> m = new HashMap<String, Object>();
    final int t = (int)this.queueTimeoutUnit.toMillis(this.queueTimeout);
    m.put("x-expires", t);
    m.put("x-message-ttl", t);
    this.queueArguments = Collections.unmodifiableMap(m);
  }

  this.assetDir =
    "ThreadMPC"
  ;
  this.filesDir =
    this.context.getFilesDir().getAbsolutePath() +
    "/" + this.assetDir
  ;

}

private void cancelConsumers(
) {
  for (int i = 0; i != this.partyCount; ++i) {
    if (this.consumers[i] != null) {
      try {
        this.channel.basicCancel(this.consumers[i]);
      } catch (final Exception e) {
      }
    }
  }
}

private void declareLocationQueues(
) throws
  Exception
{
  for (int i = 0; i != this.partyCount; ++i) {
    if (i != this.partyIndex) {
      this.declareQueue(this.locationSendQueues[i]);
      this.declareQueue(this.locationRecvQueues[i]);
    }
  }
}

private void declareQueue(
  final String queue
) throws
  Exception
{
  Log.d(this.TAG,
    "declaring queue: " + queue
  );
  this.channel.queueDeclare(
    queue,
    false, // durable
    false,  // exclusive
    true,  // auto-delete
    this.queueArguments
  );
}

@SuppressLint("NewApi")
private static void deleteRecursively(
  final String path
) throws
  Exception
{
  Files.walkFileTree(
    Paths.get(path),
    new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult postVisitDirectory(
        final Path dir,
        final IOException exc
      ) throws
        IOException
      {
        if (exc != null) {
          throw exc;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
      @Override
      public FileVisitResult visitFile(
        final Path path,
        final BasicFileAttributes attrs
      ) throws
        IOException
      {
        Files.delete(path);
        return FileVisitResult.CONTINUE;
      }
    }
  );
}

private ExecResult exec(
  final String[] argv,
  final String dir
) {
  try {
    final ExecResult result = new ExecResult();
    final Process process =
      Runtime.getRuntime().exec(
        argv,
        new String[] {
          "LD_LIBRARY_PATH=" + this.ldLibraryPath
        },
        new File(dir)
      )
    ;
    try {
      process.getOutputStream().close();
      final Thread stdoutThread =
        new Thread(
          new Runnable() {
            @Override
            public void run(
            ) {
              try {
                try (
                  final InputStream x1 =
                    process.getInputStream()
                  ;
                  final InputStreamReader x2 =
                    new InputStreamReader(x1, ThreadMPC.utf8)
                  ;
                  final BufferedReader stdout =
                    new BufferedReader(x2)
                  ;
                ) {
                  while (true) {
                    final String line = stdout.readLine();
                    if (line == null) {
                      break;
                    }
                    result.stdout.append(line);
                    result.stdout.append('\n');
                  }
                }
              } catch (final Exception e) {
                result.stdoutException = e;
              }
            }
          }
        )
      ;
      final Thread stderrThread =
        new Thread(
          new Runnable() {
            @Override
            public void run(
            ) {
              try {
                try (
                  final InputStream x1 =
                    process.getErrorStream()
                  ;
                  final InputStreamReader x2 =
                    new InputStreamReader(x1, ThreadMPC.utf8)
                  ;
                  final BufferedReader stderr =
                    new BufferedReader(x2)
                  ;
                ) {
                  while (true) {
                    final String line = stderr.readLine();
                    if (line == null) {
                      break;
                    }
                    result.stderr.append(line);
                    result.stderr.append('\n');
                  }
                }
              } catch (final Exception e) {
                result.stderrException = e;
              }
            }
          }
        )
      ;
      stdoutThread.start();
      stderrThread.start();
      while (true) {
        try {
          stdoutThread.join();
          break;
        } catch (final InterruptedException e) {
        }
      }
      while (true) {
        try {
          stderrThread.join();
          break;
        } catch (final InterruptedException e) {
        }
      }
      while (true) {
        try {
          result.exitStatus = process.waitFor();
          break;
        } catch (final InterruptedException e) {
        }
      }
    } catch (final Exception e1) {
      try {
        process.destroy();
      } catch (final Exception e2) {
        e1.addSuppressed(e2);
      }
      throw e1;
    }
    return result;
  } catch (final Exception e) {
    throw new RuntimeException(e);
  }
}

public final String getAmqpHost(
) {
  return this.amqpHost;
}

public final String getAmqpPassword(
) {
  return this.amqpPassword;
}

public final int getAmqpPort(
) {
  return this.amqpPort;
}

public final String getAmqpSslCaCertFile(
) {
  return this.amqpSslCaCertFile;
}

public final String getAmqpSslClientCertFile(
) {
  return this.amqpSslClientCertFile;
}

public final String getAmqpSslClientKeyFile(
) {
  return this.amqpSslClientKeyFile;
}

public final boolean getAmqpSslEnabled(
) {
  return this.amqpSslEnabled;
}

public final boolean getAmqpSslVerifyHostname(
) {
  return this.amqpSslVerifyHostname;
}

public final boolean getAmqpSslVerifyPeer(
) {
  return this.amqpSslVerifyPeer;
}

public final String getAmqpUsername(
) {
  return this.amqpUsername;
}

public final int getCblLookahead(
) {
  return this.cblLookahead;
}

public final ThreadMPC.CircuitType getCircuitType(
) {
  return this.circuitType;
}

public final Exception getException(
) {
  return this.exception;
}

public final ThreadMPC.Mode getMode(
) {
  return this.mode;
}

private String[] listAssets(
  final String path
) {
  try {
    return this.assets.list(this.assetDir + "/" + path);
  } catch (final IOException e) {
    throw new RuntimeException(e);
  }
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

public final void prepare(
) {
  switch (this.mode) {
    case DUMMY: if (true) {
    } break;
    case SHARED_EXECUTABLES:
    case STATIC_EXECUTABLES: if (true) {
      final String programName;
      final String circuitName;
      if (this.programIsTwoPartyMpc()) {
        programName = "two_party_mpc";
        circuitName =
          "coffeeshop_n_" + this.partyCount +
          "_lookahead_" + this.cblLookahead + ".cbl"
        ;
      } else {
        programName = "n_party_mpc_by_gate";
        circuitName =
          "coffeeshop_n_" + this.partyCount + ".cbg"
        ;
      }
      final String arch;
      {
        final List<String> abis = Arrays.asList(Build.SUPPORTED_ABIS);
        if (abis.contains("arm64-v8a")) {
          arch = "aarch64";
        } else if (abis.contains("x86_64")) {
          arch = "x86_64";
        } else {
          arch = abis.get(0);
        }
      }
      final String link =
        (this.mode == ThreadMPC.Mode.SHARED_EXECUTABLES) ?
          "shared"
        :
          "static"
      ;
      final String binLinkArch = "bin/" + link + "/" + arch;
      this.mpcExecutable =
        this.loadAsset(
          binLinkArch + "/" + programName,
          true
        )
      ;
      this.ldLibraryPath =
        new File(this.mpcExecutable).getParent()
      ;
      for (final String x : this.listAssets(binLinkArch)) {
        if (x.contains(".so") && !x.endsWith(".version")) {
          this.loadAsset(binLinkArch + "/" + x, true);
        }
      }
      this.locationCircuit =
        this.loadAsset(
          circuitName,
          false
        )
      ;
      this.locationExecDir =
        this.filesDir +
        "/" + this.queuePrefix +
        "/" + new File(this.locationCircuit).getName()
      ;
    } break;
    default: if (true) {
      throw new RuntimeException();
    } break;
  }
}

private void prepareConsumers(
) {
  for (int i = 0; i != this.partyCount; ++i) {
    this.consumers[i] = null;
  }
  this.consumerExceptions = new AtomicReferenceArray<>(this.partyCount);
  this.consumerLatch = new CountDownLatch(this.partyCount - 1);
}

private boolean programIsTwoPartyMpc(
) {
  return
    this.partyCount == 2 &&
    this.circuitType == ThreadMPC.CircuitType.CBL
  ;
}

/**
 * Runs the MPC computation.
 * <p>
 * The MPC implementation is chosen as follows:
 * </p>
 * <ol>
 * <li>
 * If
 * <code>this.mode</code>
 * is
 * <code>Mode.DUMMY</code>,
 * then the dummy implementation is used.
 * </li>
 * <li>
 * Otherwise, if
 * <code>this.partyCount</code>
 * is 2 and
 * <code>this.circuitType</code>
 * is
 * <code>CircuitType.CBL</code>,
 * then the
 * <code>two_party_mpc</code>
 * program is used with the appropriate
 * <code>coffeeshop_n_2_lookahead_*.cbl</code>
 * circuit considering
 * <code>this.cblLookahead</code>.
 * </li>
 * <li>
 * Otherwise, the
 * <code>n_party_mpc_by_gate</code>
 * program is used with the appropriate
 * <code>coffeeshop_n_*.cbg</code>
 * circuit considering
 * <code>this.partyCount</code>.
 * </li>
 * </ol>
 */

@Override
public final void run(
) {
  try {
    this.prepare();
    this.exception = null;
    switch (this.mode) {
      case DUMMY: if (true) {
        this.runDummy();
      } break;
      case SHARED_EXECUTABLES:
      case STATIC_EXECUTABLES: if (true) {
        this.runExecutables();
      } break;
      default: if (true) {
        throw new RuntimeException();
      } break;
    }
  } catch (final Exception e1) {
    try {
      e1.printStackTrace();
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

private void runDummy(
) throws
  Exception
{
  try {

    //You can get the host address and port of AMQP server from the channel
    Log.d(TAG, "Communicating with AMQP Server: " + this.amqpHost + ":" +
        this.amqpPort);


    this.declareLocationQueues();
    this.prepareConsumers();
    this.startConsumers(
      this.locationRecvQueues,
      new DummyDeliveryHandler() {
        @Override
        public void handleDelivery(
          final int i,
          final String consumerTag,
          final Envelope envelope,
          final AMQP.BasicProperties properties,
          final byte[] body
        ) throws
          Exception
        {
          if (body.length != 8) {
            throw new RuntimeException(
              "received bad length from " +
              ThreadMPC.this.locationRecvQueues[i]
            );
          }
          ThreadMPC.this.locations[i] =
            ByteBuffer.wrap(body).getLong(0)
          ;
        }
      }
    );
    this.sendMessages(this.locationSendQueues, this.locationRaw);
    this.waitForConsumers();

    Log.d(this.TAG,
      "computing average location"
    );
    final EncodedLatLon locationAnswer;
    {
      long x = 0;
      for (int i = 0; i != this.partyCount; ++i) {
        x += this.locations[i];
      }
      final EncodedLatLon y =
        EncodedLatLon.convertMpcResultToEncodedLatLon(
          x, this.partyCount
        )
      ;
      locationAnswer =
        new EncodedLatLon(
          y.getLatitude() / this.partyCount,
          y.getLongitude() / this.partyCount
        )
      ;
    }

    this.sendResultsToReceiver(
      locationAnswer
    );

  } catch (final Exception e) {
    this.cancelConsumers();
    throw e;
  }
}

@SuppressLint("NewApi")
private void runExecutables(
) throws
  Exception
{
  final EncodedLatLon locationAnswer;
  {
    final ArrayList<String> argv = new ArrayList<>();
    argv.add(this.mpcExecutable);
    argv.add("--circuit");
    argv.add(this.locationCircuit);
    if (!this.programIsTwoPartyMpc()) {
      argv.add("--num_parties");
      argv.add(Integer.toString(this.partyCount));
    }
    argv.add("--self_index");
    argv.add(Integer.toString(this.partyIndex));
    argv.add("--rabbitmq_ip");
    argv.add(this.amqpHost);
    argv.add("--rabbitmq_port");
    argv.add(String.valueOf(this.amqpPort));
    if (this.amqpSslEnabled) {
      argv.add("--rabbitmq_use_ssl");
      if (this.amqpSslCaCertFile != null) {
        argv.add("--rabbitmq_ssl_ca_cert_file");
        argv.add(this.amqpSslCaCertFile);
      }
      if (this.amqpSslVerifyPeer) {
        argv.add("--rabbitmq_ssl_verify_peer");
      }
      if (this.amqpSslVerifyHostname) {
        argv.add("--rabbitmq_ssl_verify_hostname");
      }
      if (this.amqpSslClientCertFile != null) {
        argv.add("--rabbitmq_ssl_client_cert_file");
        argv.add(this.amqpSslClientCertFile);
      }
      if (this.amqpSslClientKeyFile != null) {
        argv.add("--rabbitmq_ssl_client_key_file");
        argv.add(this.amqpSslClientKeyFile);
      }
    }
    argv.add("--rabbitmq_user");
    argv.add(this.amqpUsername);
    argv.add("--rabbitmq_pwd");
    argv.add(this.amqpPassword);
    argv.add("--print_only");
    argv.add("--nodeclare_queue");
    argv.add("--input");
    argv.add(
      "x" + Integer.toString(this.partyIndex) + "_lat=" +
      Integer.toString((int)(this.location >>> 32)) +
      ",x" + Integer.toString(this.partyIndex) + "_lng=" +
      Integer.toString((int)this.location)
    );
    for (int i = 0; i != this.partyCount; ++i) {
      if (i != this.partyIndex) {
        if (this.programIsTwoPartyMpc()) {
          argv.add("--send_queuename");
          argv.add(this.locationSendQueues[i]);
          argv.add("--rec_queuename");
          argv.add(this.locationRecvQueues[i]);
        } else {
          argv.add("--send_queuename_" + i);
          argv.add(this.locationSendQueues[i]);
          argv.add("--rec_queuename_" + i);
          argv.add(this.locationRecvQueues[i]);
        }
      }
    }
    this.declareLocationQueues();
    Log.d(this.TAG,
      "creating directory: " + this.locationExecDir
    );
    Files.createDirectories(Paths.get(this.locationExecDir));
    ThreadMPC.deleteRecursively(this.locationExecDir);
    Files.createDirectories(Paths.get(this.locationExecDir));
    Log.d(this.TAG,
      "running command: " + String.join(" ", argv)
    );
    final ExecResult result;
    try {
      result =
        this.exec(
          argv.toArray(new String[0]),
          this.locationExecDir
        )
      ;
    } finally {
      try {
        ThreadMPC.deleteRecursively(this.locationExecDir);
      } catch (final Exception e) {
      }
    }
    Log.d(this.TAG,
      "command exited with exit status " + result.exitStatus
    );
    Log.d(this.TAG,
      "stdout:\n" + result.stdout.toString()
    );
    Log.d(this.TAG,
      "stderr:\n" + result.stderr.toString()
    );
    if (result.exitStatus != 0) {
      final RuntimeException e =
        new RuntimeException(
          "command failed with exit status " +
          result.exitStatus + ": " + String.join(" ", argv)
        )
      ;
      if (result.stdoutException != null) {
        e.addSuppressed(result.stdoutException);
      }
      if (result.stderrException != null) {
        e.addSuppressed(result.stderrException);
      }
      throw e;
    }
    if (result.stdoutException != null) {
      final RuntimeException e =
        new RuntimeException(
          result.stdoutException
        )
      ;
      if (result.stderrException != null) {
        e.addSuppressed(result.stderrException);
      }
      throw e;
    }
    Log.d(this.TAG,
      "computing average location"
    );
    final String[] lines = result.stdout.toString().split("\n");
    final int i;
    if (this.programIsTwoPartyMpc()) {
      i = -1;
    } else {
      int j;
      for (j = 0; j != lines.length; ++j) {
        if (lines[j].equals("Outputs (2):")) {
          break;
        }
      }
      if (j == lines.length) {
        throw new RuntimeException();
      }
      i = j;
    }
    final long a = Long.parseLong(lines[i + 1].split(":")[1]);
    final long b = Long.parseLong(lines[i + 2].split(":")[1]);
    final EncodedLatLon c =
      EncodedLatLon.convertMpcResultToEncodedLatLon(
        (a << 32) | (int)b, this.partyCount
      )
    ;
    locationAnswer =
      new EncodedLatLon(
        c.getLatitude() / this.partyCount,
        c.getLongitude() / this.partyCount
      )
    ;
  }
  this.sendResultsToReceiver(
    locationAnswer
  );
}

private String slugify(
  String s,
  final int maxLength
) {
  if (s == null || maxLength <= 0) {
    return "";
  }
  final StringBuilder t = new StringBuilder();
  for (int i = 0; i != s.length(); ++i) {
    final char c = s.charAt(i);
    if (c >= '0' && c <= '9') {
      t.append(c);
    } else if (c >= 'A' && c <= 'Z') {
      t.append(c);
    } else if (c >= 'a' && c <= 'z') {
      t.append(c);
    } else {
      t.append('-');
    }
  }
  s = t.toString();
  s = s.replaceAll("--+", "-");
  s = s.replaceFirst("^-", "");
  s = s.replaceFirst("-$", "");
  if (s.length() > maxLength) {
    s = s.substring(0, maxLength);
  }
  return s;
}

private void sendMessages(
  final String[] sendQueues,
  final byte[] body
) throws
  Exception
{
  for (int i = 0; i != this.partyCount; ++i) {
    if (i != this.partyIndex) {
      Log.d(this.TAG,
        "sending message to " + this.locationSendQueues[i]
      );
      this.channel.basicPublish(
        "",
        sendQueues[i],
        null,
        body
      );
    }
  }
}

private void sendResultsToReceiver(
  final EncodedLatLon locationAnswer
) {
  Log.d(this.TAG,
    "sending results to receiver"
  );
  if (this.receiver != null) {
    final Bundle results = new Bundle();
    results.putFloat("latitude", locationAnswer.getLatitude());
    results.putFloat("longitude", locationAnswer.getLongitude());
    results.putString("meetingID", meeting);
    List<String> attendees = Arrays.asList(partyNames);
    results.putStringArrayList("attendees", new ArrayList<>(attendees));
    this.receiver.send(0, results);
  }
}

public final ThreadMPC setAmqpHost(
  final CharSequence host
) {
  if (host == null) {
    this.amqpHost = this.defaultAmqpHost;
  } else {
    this.amqpHost = host.toString();
  }
  return this;
}

public final ThreadMPC setAmqpPassword(
  final CharSequence password
) {
  if (password == null) {
    this.amqpPassword = ThreadMPC.DEFAULT_AMQP_PASSWORD;
  } else {
    this.amqpPassword = password.toString();
  }
  return this;
}

public final ThreadMPC setAmqpPort(
  final int port
) {
  if (port < 0 || port > 65535) {
    final IllegalArgumentException e =
      new IllegalArgumentException(
        "port must be between 0 and 65535"
      )
    ;
    e.initCause(null);
    throw e;
  }
  if (port == 0) {
    this.amqpPort = this.defaultAmqpPort;
  } else {
    this.amqpPort = port;
  }
  return this;
}

public final ThreadMPC setAmqpSslCaCertFile(
  final CharSequence caCertFile
) {
  if (caCertFile == null) {
    this.amqpSslCaCertFile = null;
  } else {
    this.amqpSslCaCertFile = caCertFile.toString();
  }
  return this;
}

public final ThreadMPC setAmqpSslClientCertFile(
  final CharSequence clientCertFile,
  final CharSequence clientKeyFile
) {
  if ((clientCertFile == null) != (clientKeyFile == null)) {
    throw new IllegalArgumentException(
      "clientCertFile and clientKeyFile " +
      "must be either both null or both not null"
    );
  }
  if (clientCertFile == null) {
    this.amqpSslClientCertFile = null;
  } else {
    this.amqpSslClientCertFile = clientCertFile.toString();
  }
  if (clientKeyFile == null) {
    this.amqpSslClientKeyFile = null;
  } else {
    this.amqpSslClientKeyFile = clientKeyFile.toString();
  }
  return this;
}

public final ThreadMPC setAmqpSslEnabled(
  final boolean enabled
) {
  this.amqpSslEnabled = enabled;
  return this;
}

public final ThreadMPC setAmqpSslVerifyPeer(
  final boolean verifyPeer
) {
  this.amqpSslVerifyPeer = verifyPeer;
  return this;
}

public final ThreadMPC setAmqpSslVerifyHostname(
  final boolean verifyHostname
) {
  this.amqpSslVerifyHostname = verifyHostname;
  return this;
}

public final ThreadMPC setAmqpUsername(
  final CharSequence username
) {
  if (username == null) {
    this.amqpUsername = ThreadMPC.DEFAULT_AMQP_USERNAME;
  } else {
    this.amqpUsername = username.toString();
  }
  return this;
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

public final ThreadMPC setCblLookahead(
  final int cblLookahead
) {
  if (cblLookahead < 1 || cblLookahead > 4) {
    final IllegalArgumentException e =
      new IllegalArgumentException(
        "cblLookahead must be between 1 and 4"
      )
    ;
    e.initCause(null);
    throw e;
  }
  this.cblLookahead = cblLookahead;
  return this;
}

public final ThreadMPC setCircuitType(
  final ThreadMPC.CircuitType circuitType
) {
  this.circuitType = circuitType;
  return this;
}

public final ThreadMPC setMode(
  final ThreadMPC.Mode mode
) {
  this.mode = mode;
  return this;
}

private void startConsumers(
  final String[] recvQueues,
  final DummyDeliveryHandler handler
) throws
  Exception
{
  final CountDownLatch latch = this.consumerLatch;
  for (int ii = 0; ii != this.partyCount; ++ii) {
    final int i = ii;
    if (i != this.partyIndex) {
      Log.d(TAG, "starting consumer for " + recvQueues[i]);
      this.consumers[i] = this.channel.basicConsume(
        recvQueues[i],
        new DefaultConsumer(this.channel) {
          private boolean firstMessage = true;
          @Override
          public void handleDelivery(
            final String consumerTag,
            final Envelope envelope,
            final AMQP.BasicProperties properties,
            final byte[] body
          ) {
            try {
              if (!this.firstMessage) {
                throw new RuntimeException(
                  "received too many messages from " + recvQueues[i]
                );
              }
              Log.d(ThreadMPC.this.TAG,
                "processing message from " + recvQueues[i]
              );
              handler.handleDelivery(
                i,
                consumerTag,
                envelope,
                properties,
                body
              );
            } catch (final Exception e1) {
              try {
                Log.d(ThreadMPC.this.TAG,
                  "error processing message from " + recvQueues[i]
                );
              } catch (final Exception e2) {
              }
              ThreadMPC.this.consumerExceptions.compareAndSet(
                i, null, e1
              );
            } finally {
              if (this.firstMessage) {
                latch.countDown();
                try {
                  this.getChannel().basicCancel(consumerTag);
                } catch (final Exception e) {
                }
                this.firstMessage = false;
              }
            }
          }
        }
      );
    }
  }
}

private void waitForConsumers(
) throws
  Exception
{
  Log.d(TAG, "waiting for consumers");
  final String s = "timed out waiting for consumers";
  long n = this.consumerTimeout;
  n = this.consumerTimeoutUnit.toNanos(n);
  long t0 = System.nanoTime();
  while (true) {
    try {
      if (this.consumerLatch.await(n, TimeUnit.NANOSECONDS)) {
        break;
      }
      throw new TimeoutException(s);
    } catch (final InterruptedException e) {
      final long t1 = System.nanoTime();
      n -= t1 - t0;
      if (n <= 0) {
        throw new TimeoutException(s);
      }
      t0 = t1;
    }
  }
  for (int i = 0; i != this.partyCount; ++i) {
    final Exception ei = this.consumerExceptions.get(i);
    if (ei != null) {
      for (int j = i + 1; j != this.partyCount; ++j) {
        final Exception ej = this.consumerExceptions.get(j);
        if (ej != null) {
          ei.addSuppressed(ej);
        }
      }
      throw ei;
    }
  }
}

}
