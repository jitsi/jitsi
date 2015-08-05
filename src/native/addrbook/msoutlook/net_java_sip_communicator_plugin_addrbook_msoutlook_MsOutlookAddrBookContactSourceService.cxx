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

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.h"

#include "MsOutlookAddrBookContactSourceService.h"

#include "MsOutlookMAPIHResultException.h"
#include "MAPINotification.h"
#include "MAPIBitness.h"
#include "MsOutlookUtils.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIInitialize
    (JNIEnv *jniEnv, jclass clazz, jlong version, jlong flags,
     jobject notificationsDelegate, jstring logPath, jint logLevel)
{
    HRESULT hr;
    const char* logFileString = jniEnv->GetStringUTFChars(logPath, NULL);
	MsOutlookUtils_createLogger("msoutlookaddrbook.log", logFileString, logLevel);
    MAPINotification_registerJniNotificationsDelegate(
      jniEnv,
      notificationsDelegate);

    jniEnv->ReleaseStringUTFChars(logPath, logFileString);

    hr = MsOutlookAddrBookContactSourceService_MAPIInitializeCOMServer();

    if (HR_FAILED(hr))
    {
    	MsOutlookUtils_log("Failed to init COM Server");
        // Report any possible error regardless of where it has come from.
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hr,
                __FILE__, __LINE__);
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIUninitialize
    (JNIEnv *jniEnv, jclass clazz)
{
    MAPINotification_unregisterJniNotificationsDelegate(jniEnv);

    MsOutlookAddrBookContactSourceService_MAPIUninitializeCOMServer();
    MsOutlookUtils_deleteLogger();
}

/**
 * Returns the bitness of the Outlook installation.
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
JNIEXPORT int JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_getOutlookBitnessVersion
    (JNIEnv *jniEnv, jclass clazz)
{
    return MAPIBitness_getOutlookBitnessVersion();
}

/**
 * Returns the Outlook version installed.
 *
 * @return 2013 for "Outlook 2013", 2010 for "Outlook 2010", 2007 for "Outlook
 * 2007" or 2003 for "Outlook 2003". -1 otherwise.
 */
JNIEXPORT int JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_getOutlookVersion
    (JNIEnv *jniEnv, jclass clazz)
{
    return MAPIBitness_getOutlookVersion();
}

/**
 * Returns true if Outlook is default mail client and false otherwise.
 *
 */
JNIEXPORT jboolean JNICALL Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_isOutlookDefaultMailClient
  (JNIEnv *, jclass)
{
	return (jboolean) MsOutlookUtils_isOutlookDefaultMailClient();
}
