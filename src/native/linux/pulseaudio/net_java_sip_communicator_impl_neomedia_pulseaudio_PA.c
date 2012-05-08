/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include "net_java_sip_communicator_impl_neomedia_pulseaudio_PA.h"

#include <pulse/pulseaudio.h>
#include <stdint.h>

static void PulseAudio_contextStateCallback(pa_context *c, void *userdata);
static jlongArray PulseAudio_getFormatInfos(JNIEnv *env, jclass clazz, jsize length, pa_format_info **formats);
static void PulseAudio_infoCallback(pa_context *c, jlong i, int eol, void *userdata, jmethodID methodID);
static void PulseAudio_sinkInfoCallback(pa_context *c, const pa_sink_info *i, int eol, void *userdata);
static void PulseAudio_sourceInfoCallback(pa_context *c, const pa_source_info *i, int eol, void *userdata);
static void PulseAudio_stateCallback(void *userdata);
static void PulseAudio_streamRequestCallback(pa_stream *s, size_t nbytes, void *userdata);
static void PulseAudio_streamStateCallback(pa_stream *s, void *userdata);

static jclass PulseAudio_runnableClass = NULL;
static jmethodID PulseAudio_runnableMethodID = 0;
static jclass PulseAudio_sinkInfoCbClass = NULL;
static jmethodID PulseAudio_sinkInfoCbMethodID = 0;
static jclass PulseAudio_sourceInfoCbClass = NULL;
static jmethodID PulseAudio_sourceInfoCbMethodID = 0;
static jclass PulseAudio_streamRequestCbClass = NULL;
static jmethodID PulseAudio_streamRequestCbMethodID = 0;
static JavaVM *PulseAudio_vm = NULL;

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_buffer_1attr_1free
    (JNIEnv *env, jclass clazz, jlong attr)
{
    pa_xfree((void *) (intptr_t) attr);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_buffer_1attr_1new
    (JNIEnv *env, jclass clazz, jint maxlength, jint tlength, jint prebuf, jint minreq, jint fragsize)
{
    pa_buffer_attr *attr = pa_xmalloc(sizeof(pa_buffer_attr));

    if (attr)
    {
        attr->maxlength = (uint32_t) maxlength;
        attr->tlength = (uint32_t) tlength;
        attr->prebuf = (uint32_t) prebuf;
        attr->minreq = (uint32_t) minreq;
        attr->fragsize = (uint32_t) fragsize;
    }
    return (intptr_t) attr;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1connect
    (JNIEnv *env, jclass clazz, jlong c, jstring server, jint flags, jlong api)
{
    const char *serverChars
        = server
            ? (*env)->GetStringUTFChars(env, server, NULL)
            : NULL;
    int ret;

    if ((*env)->ExceptionCheck(env))
        ret = -1;
    else
    {
        ret
            = pa_context_connect(
                    (pa_context *) (intptr_t) c,
                    serverChars,
                    (pa_context_flags_t) flags,
                    (pa_spawn_api *) (intptr_t) api);
        if (serverChars)
            (*env)->ReleaseStringUTFChars(env, server, serverChars);
    }
    return ret;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1disconnect
    (JNIEnv *env, jclass clazz, jlong c)
{
    pa_context_disconnect((pa_context *) (intptr_t) c);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1get_1sink_1info_1list
    (JNIEnv *env, jclass clazz, jlong c, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;
    pa_operation *o;

    if ((*env)->ExceptionCheck(env))
        o = NULL;
    else
    {
        o
            = pa_context_get_sink_info_list(
                    (pa_context *) (intptr_t) c,
                    weakCb ? PulseAudio_sinkInfoCallback : NULL,
                    (void *) weakCb);
    }

    return (intptr_t) o;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1get_1source_1info_1list
    (JNIEnv *env, jclass clazz, jlong c, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;
    pa_operation *o;

    if ((*env)->ExceptionCheck(env))
        o = NULL;
    else
    {
        o
            = pa_context_get_source_info_list(
                    (pa_context *) (intptr_t) c,
                    weakCb ? PulseAudio_sourceInfoCallback : NULL,
                    (void *) weakCb);
    }

    return (intptr_t) o;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1get_1state
    (JNIEnv *env, jclass clazz, jlong c)
{
    return pa_context_get_state((pa_context *) (intptr_t) c);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1new_1with_1proplist
    (JNIEnv *env, jclass clazz, jlong mainloop, jstring name, jlong proplist)
{
    const char *nameChars
        = name
            ? (*env)->GetStringUTFChars(env, name, NULL)
            : NULL;
    pa_context *ret;

    if ((*env)->ExceptionCheck(env))
        ret = NULL;
    else
    {
        ret
            = pa_context_new_with_proplist(
                    (pa_mainloop_api *) (intptr_t) mainloop,
                    nameChars,
                    (pa_proplist *) (intptr_t) proplist);
        if (nameChars)
            (*env)->ReleaseStringUTFChars(env, name, nameChars);
    }
    return (intptr_t) ret;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1set_1sink_1input_1volume
    (JNIEnv *env, jclass clazz, jlong c, jint idx, jlong volume, jobject cb)
{
    return
        (intptr_t)
            pa_context_set_sink_input_volume(
                    (pa_context *) (intptr_t) c,
                    (uint32_t) idx,
                    (const pa_cvolume *) (intptr_t) volume,
                    NULL,
                    NULL);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1set_1source_1output_1volume
    (JNIEnv *env, jclass clazz, jlong c, jint idx, jlong volume, jobject cb)
{
    return
        (intptr_t)
            pa_context_set_source_output_volume(
                    (pa_context *) (intptr_t) c,
                    (uint32_t) idx,
                    (const pa_cvolume *) (intptr_t) volume,
                    NULL,
                    NULL);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1set_1state_1callback
    (JNIEnv *env, jclass clazz, jlong c, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;

    pa_context_set_state_callback(
            (pa_context *) (intptr_t) c,
            weakCb ? PulseAudio_contextStateCallback : NULL,
            (void *) weakCb);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_context_1unref
    (JNIEnv *env, jclass clazz, jlong c)
{
    pa_context_unref((pa_context *) (intptr_t) c);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_cvolume_1free
    (JNIEnv *env, jclass clazz, jlong cv)
{
    pa_xfree((void *) (intptr_t) cv);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_cvolume_1new
    (JNIEnv *env, jclass clazz)
{
    pa_cvolume *cv = pa_xmalloc(sizeof(pa_cvolume));

    if (cv)
        cv = pa_cvolume_init(cv);
    return (intptr_t) cv;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_cvolume_1set
    (JNIEnv *env, jclass clazz, jlong cv, jint channels, jint v)
{
    return
        (intptr_t)
            pa_cvolume_set(
                    (pa_cvolume *) (intptr_t) cv,
                    (unsigned) channels,
                    (pa_volume_t) v);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_format_1info_1get_1encoding
    (JNIEnv *env, jclass clazz, jlong f)
{
    return ((pa_format_info *) (intptr_t) f)->encoding;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_format_1info_1get_1plist
    (JNIEnv *env, jclass clazz, jlong f)
{
    return (intptr_t) (((pa_format_info *) (intptr_t) f)->plist);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_format_1info_1get_1prop_1int
    (JNIEnv *env, jclass clazz, jlong f, jstring key)
{
    const char *keyChars
        = key ? (*env)->GetStringUTFChars(env, key, NULL) : NULL;
    int ret;

    if ((*env)->ExceptionCheck(env))
        ret = 0;
    else
    {
        pa_proplist *plist = ((pa_format_info *) (intptr_t) f)->plist;

        if (plist)
        {
            const void *data = NULL;
            size_t nbytes = 0;

            pa_proplist_get(plist, keyChars, &data, &nbytes);
            ret = (data && (nbytes == sizeof(int))) ? *((const int *) data) : 0;
        }
        else
            ret = 0;
        if (keyChars)
            (*env)->ReleaseStringUTFChars(env, key, keyChars);
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_operation_1get_1state
    (JNIEnv *env, jclass clazz, jlong o)
{
    return pa_operation_get_state((pa_operation *) (intptr_t) o);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_operation_1unref
    (JNIEnv *env, jclass clazz, jlong o)
{
    pa_operation_unref((pa_operation *) (intptr_t) o);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_proplist_1free
    (JNIEnv *env, jclass clazz, jlong p)
{
    pa_proplist_free((pa_proplist *) (intptr_t) p);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_proplist_1new
    (JNIEnv *env, jclass clazz)
{
    return (intptr_t) pa_proplist_new();
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_proplist_1sets
  (JNIEnv *env, jclass clazz, jlong p, jstring key, jstring value)
{
    const char *keyChars
        = key
            ? (*env)->GetStringUTFChars(env, key, NULL)
            : NULL;
    int ret;

    if ((*env)->ExceptionCheck(env))
        ret = -1;
    else
    {
        const char *valueChars
            = value
                ? (*env)->GetStringUTFChars(env, value, NULL)
                : NULL;

        if ((*env)->ExceptionCheck(env))
            ret = -1;
        else
        {
            ret
                = pa_proplist_sets(
                        (pa_proplist *) (intptr_t) p,
                        keyChars,
                        valueChars);
            if (valueChars)
                (*env)->ReleaseStringUTFChars(env, value, valueChars);
        }
        if (keyChars)
            (*env)->ReleaseStringUTFChars(env, key, keyChars);
    }
    return ret;
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sample_1spec_1free
    (JNIEnv *env, jclass clazz, jlong ss)
{
    pa_xfree((void *) (intptr_t) ss);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sample_1spec_1new
    (JNIEnv *env, jclass clazz, jint format, jint rate, jint channels)
{
    pa_sample_spec *ss = pa_xmalloc(sizeof(pa_sample_spec));

    if (ss)
    {
        ss->format = (pa_sample_format_t) format;
        ss->rate = (uint32_t) rate;
        ss->channels = (uint8_t) channels;
    }
    return (intptr_t) ss;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1description
    (JNIEnv *env, jclass clazz, jlong i)
{
    const char *description = ((pa_sink_info *) (intptr_t) i)->description;

    return description ? (*env)->NewStringUTF(env, description) : NULL;
}

JNIEXPORT jlongArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1formats
    (JNIEnv *env, jclass clazz, jlong i)
{
    pa_sink_info *sinkInfo = (pa_sink_info *) (intptr_t) i;

    return
        PulseAudio_getFormatInfos(
                env, clazz,
                sinkInfo->n_formats, sinkInfo->formats);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1index
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_sink_info *) (intptr_t) i)->index;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1monitor_1source
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_sink_info *) (intptr_t) i)->monitor_source;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1monitor_1source_1name
    (JNIEnv *env, jclass clazz, jlong i)
{
    const char *monitorSourceName
        = ((pa_sink_info *) (intptr_t) i)->monitor_source_name;

    return
        monitorSourceName ? (*env)->NewStringUTF(env, monitorSourceName) : NULL;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1name
    (JNIEnv *env, jclass clazz, jlong i)
{
    const char *name = ((pa_sink_info *) (intptr_t) i)->name;

    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1sample_1spec_1channels
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_sink_info *) (intptr_t) i)->sample_spec.channels;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1sample_1spec_1format
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_sink_info *) (intptr_t) i)->sample_spec.format;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sink_1info_1get_1sample_1spec_1rate
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_sink_info *) (intptr_t) i)->sample_spec.rate;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1description
    (JNIEnv *env, jclass clazz, jlong i)
{
    const char *description = ((pa_source_info *) (intptr_t) i)->description;

    return description ? (*env)->NewStringUTF(env, description) : NULL;
}

JNIEXPORT jlongArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1formats
    (JNIEnv *env, jclass clazz, jlong i)
{
    pa_source_info *sourceInfo = (pa_source_info *) (intptr_t) i;

    return
        PulseAudio_getFormatInfos(
                env, clazz,
                sourceInfo->n_formats, sourceInfo->formats);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1index
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_source_info *) (intptr_t) i)->index;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1monitor_1of_1sink
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_source_info *) (intptr_t) i)->monitor_of_sink;
}

JNIEXPORT jstring JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1name
    (JNIEnv *env, jclass clazz, jlong i)
{
    const char *name = ((pa_source_info *) (intptr_t) i)->name;

    return name ? (*env)->NewStringUTF(env, name) : NULL;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1sample_1spec_1channels
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_source_info *) (intptr_t) i)->sample_spec.channels;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1sample_1spec_1format
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_source_info *) (intptr_t) i)->sample_spec.format;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_source_1info_1get_1sample_1spec_1rate
    (JNIEnv *env, jclass clazz, jlong i)
{
    return ((pa_source_info *) (intptr_t) i)->sample_spec.rate;
}

JNIEXPORT jint
JNICALL Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1connect_1playback
  (JNIEnv *env, jclass clazz, jlong s, jstring dev, jlong attr, jint flags, jlong volume, jlong syncStream)
{
    const char *devChars
        = dev ? (*env)->GetStringUTFChars(env, dev, NULL) : NULL;
    jint ret;

    if ((*env)->ExceptionCheck(env))
        ret = -1;
    else
    {
        ret
            = pa_stream_connect_playback(
                    (pa_stream *) (intptr_t) s,
                    devChars,
                    (const pa_buffer_attr *) (intptr_t) attr,
                    (pa_stream_flags_t) flags,
                    (const pa_cvolume *) (intptr_t) volume,
                    (pa_stream *) (intptr_t) syncStream);
        (*env)->ReleaseStringUTFChars(env, dev, devChars);
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1connect_1record
    (JNIEnv *env, jclass clazz, jlong s, jstring dev, jlong attr, jint flags)
{
    const char *devChars
        = dev ? (*env)->GetStringUTFChars(env, dev, NULL) : NULL;
    jint ret;

    if ((*env)->ExceptionCheck(env))
        ret = -1;
    else
    {
        ret
            = pa_stream_connect_record(
                    (pa_stream *) (intptr_t) s,
                    devChars,
                    (const pa_buffer_attr *) (intptr_t) attr,
                    (pa_stream_flags_t) flags);
        (*env)->ReleaseStringUTFChars(env, dev, devChars);
    }
    return ret;
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1cork
    (JNIEnv *env, jclass clazz, jlong s, jboolean b, jobject cb)
{
    return (intptr_t) pa_stream_cork((pa_stream *) (intptr_t) s, b, NULL, NULL);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1disconnect
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_disconnect((pa_stream *) (intptr_t) s);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1drop
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_drop((pa_stream *) (intptr_t) s);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1get_1index
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_get_index((pa_stream *) (intptr_t) s);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1get_1state
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_get_state((pa_stream *) (intptr_t) s);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1new_1with_1proplist
    (JNIEnv *env, jclass clazz, jlong c, jstring name, jlong ss, jlong map, jlong p)
{
    const char *nameChars
        = name ? (*env)->GetStringUTFChars(env, name, NULL) : NULL;
    pa_stream *stream;

    if ((*env)->ExceptionCheck(env))
        stream = NULL;
    else
    {
        stream
            = pa_stream_new_with_proplist(
                    (pa_context *) (intptr_t) c,
                    nameChars,
                    (const pa_sample_spec *) ss,
                    (const pa_channel_map *) map,
                    (pa_proplist *) p);
        (*env)->ReleaseStringUTFChars(env, name, nameChars);
    }
    return (intptr_t) stream;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1peek
    (JNIEnv *env, jclass clazz, jlong s, jbyteArray data, jint dataOffset)
{
    const void *bytes = NULL;
    size_t nbytes = 0;
    jsize length;

    pa_stream_peek((pa_stream *) (intptr_t) s, &bytes, &nbytes);
    if (bytes && nbytes)
    {
        length = (*env)->GetArrayLength(env, data) - dataOffset;
        if (nbytes < length)
            length = nbytes;
        (*env)->SetByteArrayRegion(
                env,
                data, dataOffset, length, (jbyte *) bytes);
    }
    else
        length = 0;
    return length;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1readable_1size
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_readable_size((pa_stream *) (intptr_t) s);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1set_1read_1callback
    (JNIEnv *env, jclass clazz, jlong s, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;

    pa_stream_set_read_callback(
            (pa_stream *) (intptr_t) s,
            weakCb ? PulseAudio_streamRequestCallback : NULL,
            (void *) weakCb);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1set_1state_1callback
    (JNIEnv *env, jclass clazz, jlong s, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;

    pa_stream_set_state_callback(
            (pa_stream *) (intptr_t) s,
            weakCb ? PulseAudio_streamStateCallback : NULL,
            (void *) weakCb);
}

JNIEXPORT void
JNICALL Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1set_1write_1callback
    (JNIEnv *env, jclass clazz, jlong s, jobject cb)
{
    jweak weakCb = cb ? (*env)->NewWeakGlobalRef(env, cb) : NULL;

    pa_stream_set_write_callback(
            (pa_stream *) (intptr_t) s,
            weakCb ? PulseAudio_streamRequestCallback : NULL,
            (void *) weakCb);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1unref
    (JNIEnv *env, jclass clazz, jlong s)
{
    pa_stream_unref((pa_stream *) (intptr_t) s);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1writable_1size
    (JNIEnv *env, jclass clazz, jlong s)
{
    return pa_stream_writable_size((pa_stream *) (intptr_t) s);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_stream_1write
    (JNIEnv *env, jclass clazz, jlong s, jbyteArray data, jint dataOffset, jint dataLength, jobject freeCb, jlong offset, jint seek)
{
    pa_stream *stream = (pa_stream *) (intptr_t) s;
    jbyte *bytes = NULL;
    size_t nbytes = dataLength;
    int ret;

    pa_stream_begin_write(stream, (void **) &bytes, &nbytes);
    if (bytes && nbytes)
    {
        if (nbytes < dataLength)
            dataLength = nbytes;
        (*env)->GetByteArrayRegion(env, data, dataOffset, dataLength, bytes);
        if ((*env)->ExceptionCheck(env))
            ret = 0;
        else
        {
            pa_stream_write(
                    stream,
                    bytes,
                    (size_t) dataLength,
                    NULL,
                    (int64_t) offset,
                    (pa_seek_mode_t) seek);
            ret = dataLength;
        }
    }
    else
    {
        bytes = (*env)->GetByteArrayElements(env, data, NULL);
        if ((*env)->ExceptionCheck(env))
            ret = 0;
        else
        {
            pa_stream_write(
                    stream,
                    bytes + dataOffset,
                    (size_t) dataLength,
                    NULL,
                    (int64_t) offset,
                    (pa_seek_mode_t) seek);
            (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
            ret = dataLength;
        }
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_sw_1volume_1from_1linear
    (JNIEnv *env, jclass clazz, jdouble v)
{
    return pa_sw_volume_from_linear(v);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1free
    (JNIEnv *env, jclass clazz, jlong m)
{
    pa_threaded_mainloop_free((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1get_1api
    (JNIEnv *env, jclass clazz, jlong m)
{
    return
        (intptr_t)
            pa_threaded_mainloop_get_api((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1lock
    (JNIEnv *env, jclass clazz, jlong m)
{
    pa_threaded_mainloop_lock((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT jlong JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1new
    (JNIEnv *env, jclass clazz)
{
    return (intptr_t) pa_threaded_mainloop_new();
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1signal
    (JNIEnv *env, jclass clazz, jlong m, jboolean waitForAccept)
{
    pa_threaded_mainloop_signal(
            (pa_threaded_mainloop *) (intptr_t) m,
            (int) waitForAccept);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1start
    (JNIEnv *env, jclass clazz, jlong m)
{
    return pa_threaded_mainloop_start((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1stop
    (JNIEnv *env, jclass clazz, jlong m)
{
    pa_threaded_mainloop_stop((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1unlock
    (JNIEnv *env, jclass clazz, jlong m)
{
    pa_threaded_mainloop_unlock((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_neomedia_pulseaudio_PA_threaded_1mainloop_1wait
    (JNIEnv *env, jclass clazz, jlong m)
{
    pa_threaded_mainloop_wait((pa_threaded_mainloop *) (intptr_t) m);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;
    jint version = JNI_VERSION_1_4;

    PulseAudio_vm = vm;
    if (JNI_OK == (*vm)->GetEnv(vm, (void **) &env, version))
    {
        jclass clazz = (*env)->FindClass(env, "java/lang/Runnable");

        if (clazz)
        {
            jmethodID methodID = (*env)->GetMethodID(env, clazz, "run", "()V");

            if (methodID)
            {
                clazz = (*env)->NewGlobalRef(env, clazz);
                if (clazz)
                {
                    PulseAudio_runnableClass = clazz;
                    PulseAudio_runnableMethodID = methodID;
                }
            }
        }
        if (PulseAudio_runnableMethodID)
        {
            clazz
                = (*env)->FindClass(
                        env,
                        "net/java/sip/communicator/impl/neomedia/pulseaudio/PA$sink_info_cb_t");

            if (clazz)
            {
                jmethodID methodID
                    = (*env)->GetMethodID(env, clazz, "callback", "(JJI)V");

                if (methodID)
                {
                    clazz = (*env)->NewGlobalRef(env, clazz);
                    if (clazz)
                    {
                        PulseAudio_sinkInfoCbClass = clazz;
                        PulseAudio_sinkInfoCbMethodID = methodID;
                    }
                }
            }
        }
        if (PulseAudio_sinkInfoCbMethodID)
        {
            clazz
                = (*env)->FindClass(
                        env,
                        "net/java/sip/communicator/impl/neomedia/pulseaudio/PA$source_info_cb_t");

            if (clazz)
            {
                jmethodID methodID
                    = (*env)->GetMethodID(env, clazz, "callback", "(JJI)V");

                if (methodID)
                {
                    clazz = (*env)->NewGlobalRef(env, clazz);
                    if (clazz)
                    {
                        PulseAudio_sourceInfoCbClass = clazz;
                        PulseAudio_sourceInfoCbMethodID = methodID;
                    }
                }
            }
        }
        if (PulseAudio_sourceInfoCbMethodID)
        {
            clazz
                = (*env)->FindClass(
                        env,
                        "net/java/sip/communicator/impl/neomedia/pulseaudio/PA$stream_request_cb_t");

            if (clazz)
            {
                jmethodID methodID
                    = (*env)->GetMethodID(env, clazz, "callback", "(JI)V");

                if (methodID)
                {
                    clazz = (*env)->NewGlobalRef(env, clazz);
                    if (clazz)
                    {
                        PulseAudio_streamRequestCbClass = clazz;
                        PulseAudio_streamRequestCbMethodID = methodID;
                    }
                }
            }
        }
    }
    return PulseAudio_streamRequestCbMethodID ? version : JNI_ERR;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    jclass runnableClass = PulseAudio_runnableClass;
    jclass sinkInfoCbClass = PulseAudio_sinkInfoCbClass;
    jclass sourceInfoCbClass = PulseAudio_sourceInfoCbClass;
    jclass streamRequestCbClass = PulseAudio_streamRequestCbClass;

    PulseAudio_runnableClass = NULL;
    PulseAudio_runnableMethodID = 0;
    PulseAudio_sinkInfoCbClass = NULL;
    PulseAudio_sinkInfoCbMethodID = 0;
    PulseAudio_sourceInfoCbClass = NULL;
    PulseAudio_sourceInfoCbMethodID = 0;
    PulseAudio_streamRequestCbClass = NULL;
    PulseAudio_streamRequestCbMethodID = 0;
    PulseAudio_vm = NULL;
    if (runnableClass
            || sinkInfoCbClass
            || sourceInfoCbClass
            || streamRequestCbClass)
    {
        JNIEnv *env;
        jint version = JNI_VERSION_1_4;

        if (JNI_OK == (*vm)->GetEnv(vm, (void **) &env, version))
        {
            if (runnableClass)
                (*env)->DeleteGlobalRef(env, runnableClass);
            if (sinkInfoCbClass)
                (*env)->DeleteGlobalRef(env, sinkInfoCbClass);
            if (sourceInfoCbClass)
                (*env)->DeleteGlobalRef(env, sourceInfoCbClass);
            if (streamRequestCbClass)
                (*env)->DeleteGlobalRef(env, streamRequestCbClass);
        }
    }
}

static void
PulseAudio_contextStateCallback(pa_context *c, void *userdata)
{
    PulseAudio_stateCallback(userdata);
}

static jlongArray
PulseAudio_getFormatInfos
    (JNIEnv *env, jclass clazz, jsize length, pa_format_info **formats)
{
    jlongArray ret = (*env)->NewLongArray(env, length);

    if (ret)
    {
        jsize i;

        for (i = 0; i < length; i++, formats++)
        {
            jlong format = (intptr_t) (*formats);

            (*env)->SetLongArrayRegion(env, ret, i, 1, &format);
        }
    }
    return ret;
}

static void
PulseAudio_infoCallback
    (pa_context *c, jlong i, int eol, void *userdata, jmethodID methodID)
{
    jweak weakCb = (jweak) userdata;

    if (weakCb)
    {
        JavaVM *vm = PulseAudio_vm;

        if (vm)
        {
            JNIEnv *env;

            if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL)
                    == 0)
            {
                jobject cb = (*env)->NewLocalRef(env, weakCb);

                if (cb)
                {
                    (*env)->CallVoidMethod(
                            env,
                            cb,
                            methodID,
                            (jlong) (intptr_t) c,
                            i,
                            (jint) eol);
                    (*env)->DeleteLocalRef(env, cb);
                }
            }
        }
    }
}

static void
PulseAudio_sinkInfoCallback
    (pa_context *c, const pa_sink_info *i, int eol, void *userdata)
{
    PulseAudio_infoCallback(
            c,
            (intptr_t) i,
            eol,
            userdata,
            PulseAudio_sinkInfoCbMethodID);
}

static void
PulseAudio_sourceInfoCallback
    (pa_context *c, const pa_source_info *i, int eol, void *userdata)
{
    PulseAudio_infoCallback(
            c,
            (intptr_t) i,
            eol,
            userdata,
            PulseAudio_sourceInfoCbMethodID);
}

static void
PulseAudio_stateCallback(void *userdata)
{
    jweak weakCb = (jobject) userdata;

    if (weakCb)
    {
        JavaVM *vm = PulseAudio_vm;

        if (vm)
        {
            JNIEnv *env;

            if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL)
                    == 0)
            {
                jobject cb = (*env)->NewLocalRef(env, weakCb);

                if (cb)
                {
                    (*env)->CallVoidMethod(
                            env,
                            cb,
                            PulseAudio_runnableMethodID);
                    (*env)->DeleteLocalRef(env, cb);
                }
            }
        }
    }
}

static void
PulseAudio_streamRequestCallback(pa_stream *s, size_t nbytes, void *userdata)
{
    jweak weakCb = (jweak) userdata;

    if (weakCb)
    {
        JavaVM *vm = PulseAudio_vm;

        if (vm)
        {
            JNIEnv *env;

            if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **) &env, NULL)
                    == 0)
            {
                jobject cb = (*env)->NewLocalRef(env, weakCb);

                if (cb)
                {
                    (*env)->CallVoidMethod(
                            env,
                            cb,
                            PulseAudio_streamRequestCbMethodID,
                            (jlong) (intptr_t) s,
                            (jint) nbytes);
                    (*env)->DeleteLocalRef(env, cb);
                }
            }
        }
    }
}

static void
PulseAudio_streamStateCallback(pa_stream *s, void *userdata)
{
    PulseAudio_stateCallback(userdata);
}
