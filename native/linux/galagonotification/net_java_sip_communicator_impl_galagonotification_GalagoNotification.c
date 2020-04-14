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

#include "net_java_sip_communicator_impl_galagonotification_GalagoNotification.h"

#include <stdlib.h>
#include <dbus/dbus.h>

typedef
    struct _Image
        {
            dbus_int32_t width;
            dbus_int32_t height;
            dbus_int32_t rowstride;
            dbus_bool_t hasAlpha;
            dbus_int32_t bitsPerSample;
            dbus_int32_t channels;
            jint *data;
            int dataLength;

            jintArray jdata;
        }
    Image;

static jint
GalagoNotification_callIntGetter(
    JNIEnv *env, jclass clazz, jobject obj, const char *getter)
{
    jmethodID getterID;

    getterID = (*env)->GetMethodID(env, clazz, getter, "()I");
    return getterID ? (*env)->CallIntMethod(env, obj, getterID) : 0;
}

static dbus_bool_t
GalagoNotification_catchException(JNIEnv *env)
{
    if ((*env)->ExceptionOccurred(env))
    {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return TRUE;
    }
    return FALSE;
}

static dbus_bool_t
GalagoNotification_jobject2Image(JNIEnv *env, jobject image, Image *_image)
{
    jclass clazz;
    jint x, y;
    jmethodID getRGB;

    clazz = (*env)->GetObjectClass(env, image);
    if (!clazz)
        return FALSE;

    x = GalagoNotification_callIntGetter(env, clazz, image, "getMinX");
    if (GalagoNotification_catchException(env))
        return FALSE;
    y = GalagoNotification_callIntGetter(env, clazz, image, "getMinY");
    if (GalagoNotification_catchException(env))
        return FALSE;
    _image->width
        = GalagoNotification_callIntGetter(env, clazz, image, "getWidth");
    if (GalagoNotification_catchException(env))
        return FALSE;
    _image->height
        = GalagoNotification_callIntGetter(env, clazz, image, "getHeight");
    if (GalagoNotification_catchException(env))
        return FALSE;

    getRGB = (*env)->GetMethodID(env, clazz, "getRGB", "(IIII[III)[I");
    if (!getRGB)
    {
        GalagoNotification_catchException(env);
        return FALSE;
    }
    _image->jdata
        = (jintArray)
            (*env)
                ->CallObjectMethod(
                    env,
                    image,
                    getRGB,
                    x,
                    y,
                    _image->width,
                    _image->height,
                    NULL,
                    0,
                    _image->width);
    if (!(_image->jdata))
    {
        GalagoNotification_catchException(env);
        return FALSE;
    }
    _image->data = (*env)->GetIntArrayElements(env, _image->jdata, NULL);
    if (!(_image->data))
    {
        GalagoNotification_catchException(env);
        return FALSE;
    }

    {
        /*
         * Java is big endian, we're likely little endian. Which means that when
         * Java returns us the jint pixel values they are not the same series of
         * bytes. So we have to bring back the order to the one in which the
         * BufferedImage originally put them. Additionally, BufferedImage
         * returns them as ARGB and we have to provide them to the server as
         * RGBA. The combination of the two byte reorderings results in swapping
         * the values of the bytes at indices 0 and 2.
         */
        jint jdataLength;
        jint *data;
        jint i;

        jdataLength = (*env)->GetArrayLength(env, _image->jdata);
        if (GalagoNotification_catchException(env))
            return FALSE;
        data = _image->data;
        for (i = 0; i < jdataLength; i++)
        {
            jint *datai = data + i;
            char *pixel = (char *) datai;
            char swap;

            swap = *pixel;
            *pixel = *(pixel + 2);
            *(pixel + 2) = swap;
        }
    }

    _image->rowstride = 4 * _image->width;
    _image->hasAlpha = TRUE;
    _image->bitsPerSample = 8;
    _image->channels = 4;

    _image->dataLength
        = ((_image->height - 1) * _image->rowstride)
            + (_image->width
                * ((_image->channels * _image->bitsPerSample + 7) / 8));
    return TRUE;
}

static dbus_bool_t
GalagoNotification_messageAppendIconHint(
    DBusMessageIter *iter, const Image *image)
{
    DBusMessageIter entryIter;
    const char *str = "icon_data";
    DBusMessageIter varIter;
    DBusMessageIter structIter;
    DBusMessageIter arrayIter;

    if (!dbus_message_iter_open_container(
            iter,
            DBUS_TYPE_DICT_ENTRY,
            NULL,
            &entryIter))
        return FALSE;
    if (!dbus_message_iter_append_basic(&entryIter, DBUS_TYPE_STRING, &str))
        return FALSE;
    if (!dbus_message_iter_open_container(
            &entryIter,
            DBUS_TYPE_VARIANT,
            "(iiibiiay)",
            &varIter))
        return FALSE;
    if (!dbus_message_iter_open_container(
            &varIter,
            DBUS_TYPE_STRUCT,
            NULL,
            &structIter))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_INT32,
            &(image->width)))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_INT32,
            &(image->height)))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_INT32,
            &(image->rowstride)))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_BOOLEAN,
            &(image->hasAlpha)))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_INT32,
            &(image->bitsPerSample)))
        return FALSE;
    if (!dbus_message_iter_append_basic(
            &structIter,
            DBUS_TYPE_INT32,
            &(image->channels)))
        return FALSE;
    if (!dbus_message_iter_open_container(
            &structIter,
            DBUS_TYPE_ARRAY,
            DBUS_TYPE_BYTE_AS_STRING,
            &arrayIter))
        return FALSE;
    if (!dbus_message_iter_append_fixed_array(
            &arrayIter,
            DBUS_TYPE_BYTE,
            &(image->data),
            image->dataLength))
        return FALSE;
    if (!dbus_message_iter_close_container(&structIter, &arrayIter))
        return FALSE;
    if (!dbus_message_iter_close_container(&varIter, &structIter))
        return FALSE;
    if (!dbus_message_iter_close_container(&entryIter, &varIter))
        return FALSE;
    if (!dbus_message_iter_close_container(iter, &entryIter))
        return FALSE;
    return TRUE;
}

static dbus_bool_t
GalagoNotification_messageAppendString(
    JNIEnv *env, DBusMessageIter *iter, jstring jstr)
{
    const jbyte *str;
    dbus_bool_t success;
    const char *emptyStr = "";  /* cannot append NULL, use "" in this case */

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
    jobject icon, jstring summary, jstring body, jint expireTimeout)
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

    if (!GalagoNotification_messageAppendString(env, &iter, NULL))
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
    if (icon)
    {
        Image _icon;

        _icon.jdata = NULL;
        _icon.data = NULL;
        if (GalagoNotification_jobject2Image(env, icon, &_icon))
        {
            dbus_bool_t success;

            success
                = GalagoNotification_messageAppendIconHint(&subIter, &_icon);
            if (_icon.jdata && _icon.data)
                (*env)
                    ->ReleaseIntArrayElements(
                        env,
                        _icon.jdata,
                        _icon.data,
                        JNI_ABORT);
            if (!success)
                return FALSE;
        }
    }
    if (!dbus_message_iter_close_container(&iter, &subIter))
        return FALSE;

    _expireTimeout = expireTimeout;
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
    jlong replacesId, jobject icon, jstring summary, jstring body,
    jint expireTimeout)
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
                icon,
                summary,
                body,
                expireTimeout))
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
