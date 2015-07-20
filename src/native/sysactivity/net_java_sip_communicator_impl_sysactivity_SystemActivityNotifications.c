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

#include "net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications.h"

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/scrnsaver.h>
#include <gdk/gdkx.h>

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    setDelegate
 * Signature: (Lnet/java/sip/communicator/impl/sysactivity/SystemActivityNotifications/NotificationsDelegate;)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_setDelegate
  (JNIEnv* jniEnv, jclass clazz, jlong ptr, jobject m_delegate)
{
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    allocAndInit
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_allocAndInit
  (JNIEnv* jniEnv, jclass clazz)
{
    return -1;
}

JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_release
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    getLastInput
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_getLastInput
  (JNIEnv* jniEnv, jclass clazz)
{
    static XScreenSaverInfo *mit_info = NULL;
    static int has_extension = -1;
    int event_base, error_base;

    if(GDK_DISPLAY())
        has_extension = XScreenSaverQueryExtension(GDK_DISPLAY(), &event_base, &error_base);

    if (has_extension != -1)
    {
        mit_info = XScreenSaverAllocInfo();

        XScreenSaverQueryInfo(GDK_DISPLAY(), GDK_ROOT_WINDOW(), mit_info);

        return (mit_info->idle);
    }
    else
        return 0;
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    start
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_start
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
}

/*
 * Class:     net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_sip_communicator_impl_sysactivity_SystemActivityNotifications_stop
  (JNIEnv* jniEnv, jclass clazz, jlong ptr)
{
}
