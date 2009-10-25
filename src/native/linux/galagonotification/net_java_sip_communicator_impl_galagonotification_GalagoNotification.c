#include "net_java_sip_communicator_impl_galagonotification_GalagoNotification.h"

#include <dbus/dbus.h>

static dbus_bool_t
GalagoNotification_messageAppendString(
    JNIEnv *env, DBusMessageIter *iter, jstring jstr)
{
    const jbyte *str;
    dbus_bool_t success;
    const char *emptyStr = "";

    if (jstr)
    {
        str = (*env)->GetStringUTFChars(env, jstr, NULL);
        if (!str)
            return FALSE;
    }
    else
        str = NULL;
    success
        = dbus_message_iter_append_basic(
            iter,
            DBUS_TYPE_STRING,
            str ? &str : &emptyStr);
    if (str)
        (*env)->ReleaseStringUTFChars(env, jstr, str);
    return success;
}

static dbus_bool_t
GalagoNotification_notifyAppendArgs(
    JNIEnv *env, DBusMessage *message, jstring appName, jlong replacesId,
    jstring appIcon, jstring summary, jstring body)
{
    DBusMessageIter iter;
    dbus_uint32_t _replacesId;
    DBusMessageIter subIter;
    dbus_int32_t _expireTimeout;

    dbus_message_iter_init_append(message, &iter);

    if (!GalagoNotification_messageAppendString(env, &iter, appName))
        return FALSE;

    _replacesId = replacesId;
    if (!dbus_message_iter_append_basic(&iter, DBUS_TYPE_UINT32, &_replacesId))
        return FALSE;

    if (!GalagoNotification_messageAppendString(env, &iter, appIcon))
        return FALSE;

    if (!GalagoNotification_messageAppendString(env, &iter, summary))
        return FALSE;

    if (!GalagoNotification_messageAppendString(env, &iter, body))
        return FALSE;

    if (!dbus_message_iter_open_container(
            &iter,
            DBUS_TYPE_ARRAY,
            DBUS_TYPE_STRING_AS_STRING,
            &subIter))
        return FALSE;
    if (!dbus_message_iter_close_container(&iter, &subIter))
        return FALSE;

    if (!dbus_message_iter_open_container(
            &iter,
            DBUS_TYPE_ARRAY,
            "{sv}",
            &subIter))
        return FALSE;
    if (!dbus_message_iter_close_container(&iter, &subIter))
        return FALSE;

    _expireTimeout = -1;
    if (!dbus_message_iter_append_basic(&iter, DBUS_TYPE_INT32, &_expireTimeout))
        return FALSE;

    return TRUE;
}

static jobjectArray
GalagoNotification_stringArray2jstringArray(
    JNIEnv *env, char **stringArray, int stringArraySize)
{
    jclass stringClass;
    jobjectArray jstringArray;

    stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass)
    {
        jstringArray
            = (*env)->NewObjectArray(env, stringArraySize, stringClass, NULL);
        if (jstringArray)
        {
            int i;

            for (i = 0; i < stringArraySize; i++)
            {
                jstring jstr = (*env)->NewStringUTF(env, *(stringArray + i));

                if (jstr)
                {
                    (*env)->SetObjectArrayElement(env, jstringArray, i, jstr);
                    if ((*env)->ExceptionCheck(env))
                    {
                        jstringArray = NULL;
                        break;
                    }
                }
                else
                {
                    jstringArray = NULL;
                    break;
                }
            }
        }
    }
    else
        jstringArray = NULL;
    return jstringArray;
}

static void
GalagoNotification_throwException(JNIEnv *env, DBusError *error)
{
    /* TODO Auto-generated method stub */
    jclass clazz
        = (*env)
            ->FindClass(
                env,
                "net/java/sip/communicator/impl/galagonotification/DBusException");

    if (clazz)
        (*env)->ThrowNew(env, clazz, error->message);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_galagonotification_GalagoNotification_dbus_1bus_1get_1session(
    JNIEnv *env, jclass clazz)
{
    DBusError error;
    DBusConnection *connection;

    dbus_error_init(&error);
    connection = dbus_bus_get(DBUS_BUS_SESSION, &error);

    if (connection)
        dbus_connection_set_exit_on_disconnect(connection, FALSE);
    else if (dbus_error_is_set(&error))
    {
        GalagoNotification_throwException(env, &error);
        dbus_error_free(&error);
    }
    return connection;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_galagonotification_GalagoNotification_dbus_1connection_1unref(
    JNIEnv *env, jclass clazz, jlong connection)
{
    dbus_connection_unref((DBusConnection *) connection);
}

JNIEXPORT jobjectArray JNICALL
Java_net_java_sip_communicator_impl_galagonotification_GalagoNotification_getCapabilities(
    JNIEnv *env, jclass clazz, jlong connection)
{
    DBusMessage *message;
    jobjectArray jcapabilities = NULL;

    message
        = dbus_message_new_method_call(
            "org.freedesktop.Notifications",
            "/org/freedesktop/Notifications",
            "org.freedesktop.Notifications",
            "GetCapabilities");
    if (message)
    {
        DBusError error;
        DBusMessage *reply;

        dbus_error_init(&error);
        reply
            = dbus_connection_send_with_reply_and_block(
                (DBusConnection *) connection,
                message,
                -1,
                &error);
        if (reply)
        {
            char **capabilities;
            int capabilityCount;

            if (dbus_message_get_args(
                    reply,
                    &error,
                    DBUS_TYPE_ARRAY,
                    DBUS_TYPE_STRING,
                    &capabilities,
                    &capabilityCount,
                    DBUS_TYPE_INVALID))
            {
                jcapabilities
                    = GalagoNotification_stringArray2jstringArray(
                        env,
                        capabilities,
                        capabilityCount);
                dbus_free_string_array(capabilities);
            }
            else
            {
                GalagoNotification_throwException(env, &error);
                dbus_error_free(&error);
            }
            dbus_message_unref(reply);
        }
        else if (dbus_error_is_set(&error))
        {
            GalagoNotification_throwException(env, &error);
            dbus_error_free(&error);
        }
        dbus_message_unref(message);
    }
    return jcapabilities;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_galagonotification_GalagoNotification_notify(
    JNIEnv *env, jclass clazz, jlong connection, jstring appName,
    jlong replacesId, jstring appIcon, jstring summary, jstring body)
{
    DBusMessage *message;
    jlong jid = 0;

    message
        = dbus_message_new_method_call(
            "org.freedesktop.Notifications",
            "/org/freedesktop/Notifications",
            "org.freedesktop.Notifications",
            "Notify");
    if (message)
    {
        if (GalagoNotification_notifyAppendArgs(
                env,
                message,
                appName,
                replacesId,
                appIcon,
                summary,
                body))
        {
            DBusError error;
            DBusMessage *reply;

            dbus_error_init(&error);
            reply
                = dbus_connection_send_with_reply_and_block(
                    (DBusConnection *) connection,
                    message,
                    -1,
                    &error);
            if (reply)
            {
                dbus_uint32_t id;

                if (dbus_message_get_args(
                        reply,
                        &error,
                        DBUS_TYPE_UINT32,
                        &id,
                        DBUS_TYPE_INVALID))
                    jid = id;
                else
                {
                    GalagoNotification_throwException(env, &error);
                    dbus_error_free(&error);
                }
                dbus_message_unref(reply);
            }
            else if (dbus_error_is_set(&error))
            {
                GalagoNotification_throwException(env, &error);
                dbus_error_free(&error);
            }
        }
        dbus_message_unref(message);
    }
    return jid;
}
