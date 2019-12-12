#include <Crypto/MultiPartyComputation/Demos/n_party_mpc_by_gate_cookie_socket.h>
#include <Crypto/MultiPartyComputation/Demos/two_party_mpc_cookie_socket.h>
#include <GenericUtils/init_utils.h>
#include <LoggingUtils/logging_utils.h>
#include <Networking/cookie_socket.h>
#include <Networking/socket.h>
#include <algorithm>
#include <cstring>
#include <fstream>
#include <functional>
#include <jni.h>
#include <limits>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <vector>

struct Cookie {
  JNIEnv * env;
  jobject obj;
  jmethodID jniSend;
  jmethodID jniRecv;
  jobject channel;
};

static int cookieSend(
  void * const cookie,
  void const * const buf,
  int const len
) {
  Cookie const * const c{static_cast<Cookie const *>(cookie)};
  unsigned char const * b{static_cast<unsigned char const *>(buf)};
  int n{len};
  while (n > 0) {
    using kt =
      std::common_type<
        unsigned int,
        std::make_unsigned<jsize>::type,
        std::size_t
      >::type
    ;
    jsize const k{
      static_cast<jsize>(
        std::min({
          static_cast<kt>(n),
          static_cast<kt>(std::numeric_limits<jsize>::max()),
          static_cast<kt>(std::numeric_limits<std::size_t>::max())
        })
      )
    };
    jbyteArray const x1{c->env->NewByteArray(k)};
    if (x1 == nullptr) {
      return -1;
    }
    auto const x2{
      std::unique_ptr<
        std::remove_reference<decltype(*jbyteArray{})>::type,
        std::function<void (jbyteArray)>
      >(
        x1,
        [=](jbyteArray) {
          c->env->DeleteLocalRef(static_cast<jobject>(x1));
        }
      )
    };
    void * const x3{c->env->GetPrimitiveArrayCritical(x1, nullptr)};
    if (x3 == nullptr) {
      return -1;
    }
    std::memcpy(x3, b, static_cast<std::size_t>(k));
    c->env->ReleasePrimitiveArrayCritical(x1, x3, 0);
    c->env->CallVoidMethod(
      c->obj,
      c->jniSend,
      c->channel,
      x1
    );
    if (c->env->ExceptionCheck() != JNI_FALSE) {
      return -1;
    }
    b += k;
    n -= static_cast<int>(k);
  }
  return len;
}

static int cookieRecv(
  void * const cookie,
  void * const buf,
  int const len
) {
  Cookie const * const c{static_cast<Cookie const *>(cookie)};
  unsigned char * b{static_cast<unsigned char *>(buf)};
  int n{len};
  while (n > 0) {
    using kt =
      std::common_type<
        unsigned int,
        std::make_unsigned<jsize>::type,
        std::size_t
      >::type
    ;
    jsize const k{
      static_cast<jsize>(
        std::min({
          static_cast<kt>(n),
          static_cast<kt>(std::numeric_limits<jsize>::max()),
          static_cast<kt>(std::numeric_limits<std::size_t>::max())
        })
      )
    };
    jbyteArray const x1{c->env->NewByteArray(k)};
    if (x1 == nullptr) {
      return -1;
    }
    auto const x2{
      std::unique_ptr<
        std::remove_reference<decltype(*jbyteArray{})>::type,
        std::function<void (jbyteArray)>
      >(
        x1,
        [=](jbyteArray) {
          c->env->DeleteLocalRef(static_cast<jobject>(x1));
        }
      )
    };
    c->env->CallVoidMethod(
      c->obj,
      c->jniRecv,
      c->channel,
      x1
    );
    if (c->env->ExceptionCheck() != JNI_FALSE) {
      return -1;
    }
    void * const x3{c->env->GetPrimitiveArrayCritical(x1, nullptr)};
    if (x3 == nullptr) {
      return -1;
    }
    std::memcpy(b, x3, static_cast<std::size_t>(k));
    c->env->ReleasePrimitiveArrayCritical(x1, x3, 0);
    b += k;
    n -= static_cast<int>(k);
  }
  return len;
}

// std::vector<T>(n) for any nonnegative integer n of any type.
template<class T, class U>
static std::vector<T> make_vector(
  U const n
) {
  using V = std::vector<T>;
  if (
    static_cast<typename std::make_unsigned<U>::type>(n) >
    std::numeric_limits<typename V::size_type>::max()
  ) {
    throw std::overflow_error{"size_type overflow"};
  }
  return V(static_cast<typename V::size_type>(n));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_stealthsoftwareinc_bmc_MpcTask_jniCall(
  JNIEnv * const env,
  jobject const obj,
  jint const func,
  jobjectArray const args,
  jobjectArray const channels,
  jstring const logFile
) {

  try {

    jclass const cls{env->GetObjectClass(obj)};
    if (cls == nullptr) {
      return -1;
    }

    jmethodID const jniSetError{
      env->GetMethodID(
        cls,
        "jniSetError",
        "(Ljava/lang/String;)V"
      )
    };
    if (jniSetError == nullptr) {
      return -1;
    }
    auto const setExceptionMessage{
      [=](std::string const & s) {
        env->CallVoidMethod(
          obj,
          jniSetError,
          env->NewStringUTF(s.c_str())
        );
      }
    };

    jmethodID const jniSend{
      env->GetMethodID(
        cls,
        "jniSend",
        "(Lcom/stealthsoftwareinc/bmc/MpcTask$Channel;[B)V"
      )
    };
    if (jniSend == nullptr) {
      setExceptionMessage(
        "failed to look up jniSend"
      );
      return -1;
    }

    jmethodID const jniRecv{
      env->GetMethodID(
        cls,
        "jniRecv",
        "(Lcom/stealthsoftwareinc/bmc/MpcTask$Channel;[B)V"
      )
    };
    if (jniRecv == nullptr) {
      setExceptionMessage(
        "failed to look up jniRecv"
      );
      return -1;
    }

    jsize const argsLength{env->GetArrayLength(args)};
    if (argsLength > std::numeric_limits<jsize>::max() - 1) {
      setExceptionMessage("jsize overflow");
      return -1;
    }
    auto argVec{make_vector<std::string>(argsLength + 1)};
    for (jsize i{0}; i != argsLength; ++i) {
      jobject const x1{env->GetObjectArrayElement(args, i)};
      if (x1 == nullptr) {
        setExceptionMessage(
          "args contains a null"
        );
        return -1;
      }
      jstring const x2{static_cast<jstring>(x1)};
      char const * const x3{env->GetStringUTFChars(x2, nullptr)};
      if (x3 == nullptr) {
        setExceptionMessage(
          "GetStringUTFChars failed"
        );
        return -1;
      }
      auto const x4{
        std::unique_ptr<
          char const,
          std::function<void (char const *)>
        >(
          x3,
          [=](char const *) {
            env->ReleaseStringUTFChars(x2, x3);
          }
        )
      };
      argVec[i + 1] = x3;
    }

    jsize const partyCount{env->GetArrayLength(channels)};
    if (partyCount < 2) {
      setExceptionMessage(
        "partyCount < 2"
      );
      return -1;
    }

    jsize partyIndex{partyCount};
    auto cookies{make_vector<Cookie>(partyCount)};
    auto cookieSockets{make_vector<networking::CookieSocketParams>(partyCount)};
    for (jsize i{0}; i != partyCount; ++i) {
      cookieSockets[i].type_ = networking::SocketType::COOKIE;
      jobject const channel{env->GetObjectArrayElement(channels, i)};
      if (channel == nullptr) {
        if (partyIndex != partyCount) {
          setExceptionMessage(
            "channels contains more than one null"
          );
          return -1;
        }
        partyIndex = i;
      } else {
        cookies[i].env = env;
        cookies[i].obj = obj;
        cookies[i].jniSend = jniSend;
        cookies[i].jniRecv = jniRecv;
        cookies[i].channel = channel;
        cookieSockets[i].cookie_ = &cookies[i];
        cookieSockets[i].functions_.send_ = &cookieSend;
        cookieSockets[i].functions_.recv_ = &cookieRecv;
      }
    }
    if (partyIndex == partyCount) {
      setExceptionMessage(
        "channels does not contain a null"
      );
      return -1;
    }

    static std::mutex mutex;
    std::lock_guard<std::mutex> lock{mutex};

    SetUseLogColors(false);

    std::ofstream logStream;
    if (logFile == nullptr) {
      SetLogStream(nullptr);
    } else {
      char const * const x1{env->GetStringUTFChars(logFile, nullptr)};
      if (x1 == nullptr) {
        setExceptionMessage(
          "GetStringUTFChars failed"
        );
        return -1;
      }
      auto const x2{
        std::unique_ptr<
          char const,
          std::function<void (char const *)>
        >(
          x1,
          [=](char const *) {
            env->ReleaseStringUTFChars(logFile, x1);
          }
        )
      };
      logStream.open(x1);
      if (logStream) {
        SetLogStream(&logStream);
      } else {
        SetLogStream(nullptr);
      }
    }

    int const funcNPartyMpcByGate{0};
    int const funcTwoPartyMpc{1};

    switch (func) {

      case funcNPartyMpcByGate: {
        argVec[0] = "n_party_cookie_socket_nonmain";
        int const s{
          n_party_cookie_socket_nonmain(argVec, cookieSockets)
        };
        if (s != 0) {
          setExceptionMessage(
            "n_party_cookie_socket_nonmain failed"
          );
          return -1;
        }
        return 0;
      } break;

      case funcTwoPartyMpc: {
        argVec[0] = "two_party_cookie_socket_nonmain";
        int const s{
          two_party_cookie_socket_nonmain(argVec, cookieSockets)
        };
        if (s != 0) {
          setExceptionMessage(
            "two_party_cookie_socket_nonmain failed"
          );
          return -1;
        }
        return 0;
      } break;

    }

    setExceptionMessage(
      "unknown func"
    );
    return -1;

  } catch (...) {
    return -1;
  }

}
