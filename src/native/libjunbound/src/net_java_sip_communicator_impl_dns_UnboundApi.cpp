/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <unbound.h>
#include "net_java_sip_communicator_impl_dns_UnboundApi.h"

void ub_async_cb(void* my_arg, int err, struct ub_result* result);
jobject createUnboundResult(JNIEnv* env, ub_result* resolveResult);

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    setDebugLevel
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_setDebugLevel
  (JNIEnv* env, jclass clazz, jlong context, jint level)
{
    int result = ub_ctx_debuglevel((ub_ctx*)context, level);
    if(result != 0)
    {
        env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
        return;
    }
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    createContext
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_createContext
  (JNIEnv* env, jclass clazz)
{
	return (jlong)ub_ctx_create();
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    deleteContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_deleteContext
  (JNIEnv* env, jclass clazz, jlong context)
{
	ub_ctx_delete((ub_ctx*)context);
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    setForwarder
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_setForwarder
  (JNIEnv* env, jclass clazz, jlong context, jstring server)
{
	char* chars = (char*)env->GetStringUTFChars(server, NULL);
	int result = ub_ctx_set_fwd((ub_ctx*)context, chars);
	env->ReleaseStringUTFChars(server, chars);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return;
	}
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    addTrustAnchor
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_addTrustAnchor
  (JNIEnv* env, jclass clazz, jlong context, jstring anchor)
{
	char* chars = (char*)env->GetStringUTFChars(anchor, NULL);
	int result = ub_ctx_add_ta((ub_ctx*)context, chars);
	env->ReleaseStringUTFChars(anchor, chars);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return;
	}
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    resolve
 * Signature: (JLjava/lang/String;II)Lnet/java/sip/communicator/impl/dns/UnboundResult;
 */
JNIEXPORT jobject JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_resolve
  (JNIEnv* env, jclass clazz, jlong context, jstring name, jint rrtype, jint rrclass)
{
	char* chars = (char*)env->GetStringUTFChars(name, NULL);
	ub_result* resolveResult;
	int result = ub_resolve((ub_ctx*)context, chars, rrtype, rrclass, &resolveResult);
	env->ReleaseStringUTFChars(name, chars);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return NULL;
	}
	return createUnboundResult(env, resolveResult);
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    resolveAsync
 * Signature: (JLjava/lang/String;IILnet/java/sip/communicator/impl/dns/UnboundApi/UnboundCallback;)J
 */
JNIEXPORT jint JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_resolveAsync
  (JNIEnv* env, jclass clazz, jlong context, jstring name, jint rrtype, jint rrclass, jobject data, jobject cb)
{
	JavaVM* jvm;
	if(env->GetJavaVM(&jvm) != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), "Unable to obtain Java VM pointer");
		return 0;
	}

	int result = ub_ctx_async((ub_ctx*)context, true);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return 0;
	}

	//ensure the objects stay alive when this method leaves
	void** cbData = new void*[3];
	cbData[0] = env->NewGlobalRef(data);
	cbData[1] = env->NewGlobalRef(cb);
	cbData[2] = jvm;

	int asyncId;
	char* chars = (char*)env->GetStringUTFChars(name, NULL);
	result = ub_resolve_async((ub_ctx*)context, chars, rrtype, rrclass, cbData, &ub_async_cb, &asyncId);
	env->ReleaseStringUTFChars(name, chars);
	if(result != 0)
	{
		delete[] cbData;
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return 0;
	}
	return asyncId;
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    cancelAsync
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_cancelAsync
  (JNIEnv* env, jclass clazz, jlong context, jint asyncId)
{
	int result = ub_cancel((ub_ctx*)context, asyncId);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return;
	}
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    errorCodeToString
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_errorCodeToString
  (JNIEnv* env, jclass clazz, jint code)
{
	return env->NewStringUTF(ub_strerror(code));
}

/*
 * Class:     net_java_sip_communicator_impl_dns_UnboundApi
 * Method:    processAsync
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_dns_UnboundApi_processAsync
  (JNIEnv* env, jclass clazz, jlong context)
{
	int result = ub_wait((ub_ctx*)context);
	if(result != 0)
	{
		env->ThrowNew(env->FindClass("net/java/sip/communicator/impl/dns/UnboundException"), ub_strerror(result));
		return;
	}
}





void ub_async_cb(void* my_arg, int err, struct ub_result* result)
{
	void** cbData = (void**)my_arg;
	JavaVM* jvm = ((JavaVM*)cbData[2]);
	JNIEnv* env;
	if(jvm->AttachCurrentThreadAsDaemon((void**)&env, NULL) == 0)
	{
		jobject data = (jobject)cbData[0];
		jobject cb = (jobject)cbData[1];
		delete[] cbData;

		jobject ubResult = NULL;
		if(err == 0)
		{
			ubResult = createUnboundResult(env, result);
		}
		env->CallVoidMethod(
			cb, env->GetMethodID(
				env->FindClass("net/java/sip/communicator/impl/dns/UnboundApi$UnboundCallback"),
				"UnboundResolveCallback",
				"(Ljava/lang/Object;ILnet/java/sip/communicator/impl/dns/UnboundResult;)V"
			), data, (jint)err, ubResult
		);

		env->DeleteGlobalRef(data);
		env->DeleteGlobalRef(cb);
		env->DeleteLocalRef(ubResult);
	}
	jvm->DetachCurrentThread();
}




jobject createUnboundResult(JNIEnv* env, ub_result* resolveResult)
{
	jclass ubResultClass = env->FindClass("net/java/sip/communicator/impl/dns/UnboundResult");
	jmethodID constructor = env->GetMethodID(ubResultClass, "<init>", "()V");
	jobject ubResult = env->NewObject(ubResultClass, constructor);

	//copy data
	//int numData = 0;
	//char** data = resolveResult->data;
	//while(*(data++) != NULL)
	//	numData++;

	//jobjectArray dataArray = env->NewObjectArray(numData, env->FindClass("[B"), NULL);
	//for(int i = 0; i < numData; i++)
	//{
	//	jbyteArray dataEntry = env->NewByteArray(resolveResult->len[i]);
	//	env->SetByteArrayRegion(dataEntry, 0, resolveResult->len[i], (jbyte*)resolveResult->data[i]);
	//	env->SetObjectArrayElement(dataArray, i, dataEntry);
	//	env->DeleteLocalRef(dataEntry);
	//}

	//copy answer packet
	jbyteArray answerPacket = env->NewByteArray(resolveResult->answer_len);
	env->SetByteArrayRegion(answerPacket, 0, resolveResult->answer_len, (jbyte*)resolveResult->answer_packet);

	//set fields
	env->SetObjectField( ubResult, env->GetFieldID(ubResultClass, "qname",        "Ljava/lang/String;"),     env->NewStringUTF(resolveResult->qname));
	env->SetIntField(    ubResult, env->GetFieldID(ubResultClass, "qtype",        "I"),                      resolveResult->qtype);
	env->SetIntField(    ubResult, env->GetFieldID(ubResultClass, "qclass",       "I"),                      resolveResult->qclass);

	//env->SetObjectField( ubResult, env->GetFieldID(ubResultClass, "data",         "[[B"),                    dataArray);
	//env->SetObjectField( ubResult, env->GetFieldID(ubResultClass, "canonname",    "Ljava/lang/String;"),     env->NewStringUTF(resolveResult->canonname));
	env->SetIntField(    ubResult, env->GetFieldID(ubResultClass, "rcode",        "I"),                      resolveResult->rcode);
	env->SetObjectField( ubResult, env->GetFieldID(ubResultClass, "answerPacket", "[B"),                     answerPacket);

	env->SetBooleanField(ubResult, env->GetFieldID(ubResultClass, "haveData",     "Z"),                      resolveResult->havedata);
	env->SetBooleanField(ubResult, env->GetFieldID(ubResultClass, "nxDomain",     "Z"),                      resolveResult->nxdomain);
	env->SetBooleanField(ubResult, env->GetFieldID(ubResultClass, "secure",       "Z"),                      resolveResult->secure);
	env->SetBooleanField(ubResult, env->GetFieldID(ubResultClass, "bogus",        "Z"),                      resolveResult->bogus);
	env->SetObjectField( ubResult, env->GetFieldID(ubResultClass, "whyBogus",     "Ljava/lang/String;"),     env->NewStringUTF(resolveResult->why_bogus));

	ub_resolve_free(resolveResult);
	return ubResult;
}
