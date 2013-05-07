/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include "net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService.h"

#include "MsOutlookAddrBookContactSourceService.h"

#include "MsOutlookMAPIHResultException.h"
#include "MAPINotification.h"
#include "MAPIBitness.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_addrbook_msoutlook_MsOutlookAddrBookContactSourceService_MAPIInitialize
    (JNIEnv *jniEnv, jclass clazz, jlong version, jlong flags,
     jobject notificationsDelegate)
{
    HRESULT hr;

    MAPINotification_registerJniNotificationsDelegate(
      jniEnv,
      notificationsDelegate);

    hr = MsOutlookAddrBookContactSourceService_MAPIInitializeCOMServer();

    if (HR_FAILED(hr))
    {
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
}

