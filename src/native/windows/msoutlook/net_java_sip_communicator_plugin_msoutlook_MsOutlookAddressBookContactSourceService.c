#include "net_java_sip_communicator_plugin_msoutlook_MsOutlookAddressBookContactSourceService.h"

#include "MsOutlookMAPI.h"
#include "MsOutlookMAPIHResultException.h"

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_msoutlook_MsOutlookAddressBookContactSourceService_MAPIInitialize
    (JNIEnv *jniEnv, jclass clazz, jlong version, jlong flags)
{
    MAPIINIT_0 mapiInit = { (ULONG) version, (ULONG) flags };
    HRESULT hResult;

    hResult = MAPIInitialize(&mapiInit);

    if (HR_FAILED(hResult))
    {
        MsOutlookMAPIHResultException_throwNew(
                jniEnv,
                hResult,
                __FILE__, __LINE__);
    }
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_plugin_msoutlook_MsOutlookAddressBookContactSourceService_MAPIUninitialize
    (JNIEnv *jniEnv, jclass clazz)
{
    MAPIUninitialize();
}
