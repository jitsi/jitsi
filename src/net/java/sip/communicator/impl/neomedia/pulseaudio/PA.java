/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.pulseaudio;

public final class PA
{
    public interface context_success_cb_t
    {
        void callback(long c, boolean success);
    }

    public interface sink_info_cb_t
    {
        void callback(long c, long i, int eol);
    }

    public interface source_info_cb_t
    {
        void callback(long c, long i, int eol);
    }

    public interface stream_request_cb_t
    {
        void callback(long s, int nbytes);
    }

    public interface stream_success_cb_t
    {
        void callback(long s, boolean success);
    }

    public static final int CONTEXT_AUTHORIZING = 2;

    public static final int CONTEXT_CONNECTING = 1;

    public static final int CONTEXT_FAILED = 5;

    public static final int CONTEXT_NOFAIL = 2;

    public static final int CONTEXT_NOFLAGS = 0;

    public static final int CONTEXT_READY = 4;

    public static final int CONTEXT_SETTING_NAME = 3;

    public static final int CONTEXT_TERMINATED = 6;

    public static final int CONTEXT_UNCONNECTED = 0;

    public static final int ENCODING_ANY = 0;

    public static final int ENCODING_INVALID = -1;

    public static final int ENCODING_PCM = 1;

    public static final int INVALID_INDEX = -1;

    public static final int OPERATION_CANCELLED = 2;

    public static final int OPERATION_DONE = 1;

    public static final int OPERATION_RUNNING = 0;

    public static final String PROP_APPLICATION_NAME = "application.name";

    public static final String PROP_APPLICATION_VERSION = "application.version";

    public static final String PROP_FORMAT_CHANNELS = "format.channels";

    public static final String PROP_FORMAT_RATE = "format.rate";

    public static final String PROP_MEDIA_NAME = "media.name";

    public static final String PROP_MEDIA_ROLE = "media.role";

    public static final int SAMPLE_S16LE = 3;

    public static final int SEEK_RELATIVE = 0;

    public static final int STREAM_ADJUST_LATENCY = 0x2000;

    public static final int STREAM_FAILED = 3;

    public static final int STREAM_NOFLAGS = 0x0000;

    public static final int STREAM_READY = 2;

    public static final int STREAM_START_CORKED = 0x0001;

    public static final int STREAM_TERMINATED = 4;

    static
    {
        System.loadLibrary("jnpulseaudio");
    }

    public static native void buffer_attr_free(long attr);

    public static native long buffer_attr_new(
            int maxlength,
            int tlength,
            int prebuf,
            int minreq,
            int fragsize);

    public static native int context_connect(
            long c,
            String server,
            int flags,
            long api);

    public static native void context_disconnect(long c);

    public static native long context_get_sink_info_list(
            long c,
            sink_info_cb_t cb);

    public static native long context_get_source_info_list(
            long c,
            source_info_cb_t cb);

    public static native int context_get_state(long c);

    public static native long context_new_with_proplist(
            long mainloop,
            String name,
            long proplist);

    public static native long context_set_sink_input_volume(
            long c,
            int idx,
            long volume,
            context_success_cb_t cb);

    public static native long context_set_source_output_volume(
            long c,
            int idx,
            long volume,
            context_success_cb_t cb);

    public static native void context_set_state_callback(long c, Runnable cb);

    public static native void context_unref(long c);

    public static native void cvolume_free(long cv);

    public static native long cvolume_new();

    public static native long cvolume_set(long cv, int channels, int v);

    public static native int format_info_get_encoding(long f);

    public static native long format_info_get_plist(long f);

    public static native int format_info_get_prop_int(long f, String key);

    public static native String get_library_version();

    public static native int operation_get_state(long o);

    public static native void operation_unref(long o);

    public static native void proplist_free(long p);

    public static native long proplist_new();

    public static native int proplist_sets(long p, String key, String value);

    public static native void sample_spec_free(long ss);

    public static native long sample_spec_new(
            int format,
            int rate,
            int channels);

    public static native String sink_info_get_description(long i);

    public static native long[] sink_info_get_formats(long i);

    public static native int sink_info_get_index(long i);

    public static native int sink_info_get_monitor_source(long i);

    public static native String sink_info_get_monitor_source_name(long i);

    public static native String sink_info_get_name(long i);

    public static native int sink_info_get_sample_spec_channels(long i);

    public static native int sink_info_get_sample_spec_format(long i);

    public static native int sink_info_get_sample_spec_rate(long i);

    public static native String source_info_get_description(long i);

    public static native long[] source_info_get_formats(long i);

    public static native int source_info_get_index(long i);

    public static native int source_info_get_monitor_of_sink(long i);

    public static native String source_info_get_name(long i);

    public static native int source_info_get_sample_spec_channels(long i);

    public static native int source_info_get_sample_spec_format(long i);

    public static native int source_info_get_sample_spec_rate(long i);

    public static native int stream_connect_playback(
            long s,
            String dev,
            long attr,
            int flags,
            long volume,
            long syncStream);

    public static native int stream_connect_record(
            long s,
            String dev,
            long attr,
            int flags);

    public static native long stream_cork(
            long s,
            boolean b,
            stream_success_cb_t cb);

    public static native int stream_disconnect(long s);

    public static native int stream_drop(long s);

    public static native int stream_get_index(long s);

    public static native int stream_get_state(long s);

    public static native long stream_new_with_proplist(
            long c,
            String name,
            long ss,
            long map,
            long p);

    public static native int stream_peek(long s, byte[] data, int dataOffset);

    public static native int stream_readable_size(long s);

    public static native void stream_set_read_callback(
            long s,
            stream_request_cb_t cb);

    public static native void stream_set_state_callback(long s, Runnable cb);

    public static native void stream_set_write_callback(
            long s,
            stream_request_cb_t cb);

    public static native void stream_unref(long s);

    public static native int stream_writable_size(long s);

    public static native int stream_write(
            long s,
            byte[] data,
            int dataOffset,
            int dataLength,
            Runnable freeCb,
            long offset,
            int seek);

    public static native int sw_volume_from_linear(double v);

    public static native void threaded_mainloop_free(long m);

    public static native long threaded_mainloop_get_api(long m);

    public static native void threaded_mainloop_lock(long m);

    public static native long threaded_mainloop_new();

    public static native void threaded_mainloop_signal(
            long m,
            boolean waitForAccept);

    public static native int threaded_mainloop_start(long m);

    public static native void threaded_mainloop_stop(long m);

    public static native void threaded_mainloop_unlock(long m);

    public static native void threaded_mainloop_wait(long m);

    /** Prevents the initialization of <tt>PA</tt> instances. */
    private PA()
    {
    }
}
