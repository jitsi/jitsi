/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.jmfext.media.renderer.audio.*;
import net.java.sip.communicator.impl.neomedia.pulseaudio.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;

public class PulseAudioSystem
    extends AudioSystem
{
    public static final String LOCATOR_PROTOCOL = "pulseaudio";

    public static final String MEDIA_ROLE_EVENT = "event";

    public static final String MEDIA_ROLE_PHONE = "phone";

    private static final String NULL_DEV_CAPTURE_DEVICE_INFO_NAME = "Default";

    public static void corkStream(long stream, boolean b)
        throws IOException
    {
        if (stream == 0)
            throw new IOException("stream");

        long o = PA.stream_cork(stream, b, null);

        if (o == 0)
            throw new IOException("pa_stream_cork");

        PA.operation_unref(o);
    }

    public static PulseAudioSystem getPulseAudioSystem()
    {
        AudioSystem audioSystem
            = AudioSystem.getAudioSystem(PulseAudioSystem.LOCATOR_PROTOCOL);

        return
            (audioSystem instanceof PulseAudioSystem)
                ? (PulseAudioSystem) audioSystem
                : null;
    }

    private boolean createContext;

    private long context;

    private long mainloop;

    public PulseAudioSystem()
        throws Exception
    {
        super(LOCATOR_PROTOCOL, FEATURE_NOTIFY_AND_PLAYBACK_DEVICES);
    }

    private void createContext()
    {
        if (this.context != 0)
            throw new IllegalStateException("context");

        startMainloop();
        try
        {
            long proplist = PA.proplist_new();

            if (proplist == 0)
                throw new RuntimeException("pa_proplist_new");

            try
            {
                populateContextProplist(proplist);

                long context
                    = PA.context_new_with_proplist(
                            PA.threaded_mainloop_get_api(mainloop),
                            null /* PA_PROP_APPLICATION_NAME */,
                            proplist);

                if (context == 0)
                    throw new RuntimeException("pa_context_new_with_proplist");

                try
                {
                    if (proplist != 0)
                    {
                        PA.proplist_free(proplist);
                        proplist = 0;
                    }

                    Runnable stateCallback
                        = new Runnable()
                        {
                            public void run()
                            {
                                signalMainloop(false);
                            }
                        };

                    lockMainloop();
                    try
                    {
                        PA.context_set_state_callback(context, stateCallback);
                        PA.context_connect(
                                context,
                                null,
                                PA.CONTEXT_NOFAIL,
                                0);

                        try
                        {
                            int state
                                = waitForContextState(
                                        context,
                                        PA.CONTEXT_READY);

                            if (state != PA.CONTEXT_READY)
                                throw new IllegalStateException(
                                        "context.state");

                            this.context = context;
                        }
                        finally
                        {
                            if (this.context == 0)
                                PA.context_disconnect(context);
                        }
                    }
                    finally
                    {
                        unlockMainloop();
                    }
                }
                finally
                {
                    if (this.context == 0)
                        PA.context_unref(context);
                }
            }
            finally
            {
                if (proplist != 0)
                    PA.proplist_free(proplist);
            }
        }
        finally
        {
            if (this.context == 0)
                stopMainloop();
        }
    }

    @Override
    public Renderer createRenderer(boolean playback)
    {
        MediaLocator locator;

        if (playback)
            locator = null;
        else
        {
            CaptureDeviceInfo notifyDevice = getNotifyDevice();

            if (notifyDevice == null)
                return null;
            else
                locator = notifyDevice.getLocator();
        }

        PulseAudioRenderer renderer
            = new PulseAudioRenderer(
                    playback ? MEDIA_ROLE_PHONE : MEDIA_ROLE_EVENT);

        if ((renderer != null) && (locator != null))
            renderer.setLocator(locator);

        return renderer;
    }

    public long createStream(
            int sampleRate,
            int channels,
            String mediaName,
            String mediaRole)
        throws IllegalStateException,
               RuntimeException
    {
        long context = getContext();

        if (context == 0)
            throw new IllegalStateException("context");

        long sampleSpec
            = PA.sample_spec_new(PA.SAMPLE_S16LE, sampleRate, channels);

        if (sampleSpec == 0)
            throw new RuntimeException("pa_sample_spec_new");

        long ret = 0;

        try
        {
            long proplist = PA.proplist_new();

            if (proplist == 0)
                throw new RuntimeException("pa_proplist_new");

            try
            {
                PA.proplist_sets(proplist, PA.PROP_MEDIA_NAME, mediaRole);
                PA.proplist_sets(proplist, PA.PROP_MEDIA_ROLE, mediaRole);

                long stream
                    = PA.stream_new_with_proplist(
                            context,
                            null,
                            sampleSpec,
                            0,
                            proplist);

                if (stream == 0)
                    throw new RuntimeException(
                            "pa_stream_new_with_proplist");

                try
                {
                    ret = stream;
                }
                finally
                {
                    if (ret == 0)
                        PA.stream_unref(stream);
                }
            }
            finally
            {
                if (proplist != 0)
                    PA.proplist_free(proplist);
            }
        }
        finally
        {
            if (sampleSpec != 0)
                PA.sample_spec_free(sampleSpec);
        }

        return ret;
    }

    protected synchronized void doInitialize()
        throws Exception
    {
        long context = getContext();

        final List<CaptureDeviceInfo> captureDevices
            = new LinkedList<CaptureDeviceInfo>();
        final List<Format> captureDeviceFormats = new LinkedList<Format>();
        PA.source_info_cb_t sourceInfoListCallback
            = new PA.source_info_cb_t()
            {
                public void callback(long c, long i, int eol)
                {
                    try
                    {
                        if ((eol == 0) && (i != 0))
                        {
                            sourceInfoListCallback(
                                    c,
                                    i,
                                    captureDevices,
                                    captureDeviceFormats);
                        }
                    }
                    finally
                    {
                        signalMainloop(false);
                    }
                }
            };

        final List<CaptureDeviceInfo> playbackDevices
            = new LinkedList<CaptureDeviceInfo>();
        final List<Format> playbackDeviceFormats = new LinkedList<Format>();
        PA.sink_info_cb_t sinkInfoListCallback
            = new PA.sink_info_cb_t()
            {
                public void callback(long c, long i, int eol)
                {
                    try
                    {
                        if ((eol == 0) && (i != 0))
                        {
                            sinkInfoListCallback(
                                    c,
                                    i,
                                    playbackDevices,
                                    playbackDeviceFormats);
                        }
                    }
                    finally
                    {
                        signalMainloop(false);
                    }
                }
            };

        lockMainloop();
        try
        {
            long o
                = PA.context_get_source_info_list(
                        context,
                        sourceInfoListCallback);

            if (o == 0)
                throw new RuntimeException("pa_context_get_source_info_list");

            try
            {
                while (PA.operation_get_state(o) == PA.OPERATION_RUNNING)
                    waitMainloop();
            }
            finally
            {
                PA.operation_unref(o);
            }

            o
                = PA.context_get_sink_info_list(
                        context,
                        sinkInfoListCallback);

            if (o == 0)
                throw new RuntimeException("pa_context_get_sink_info_list");

            try
            {
                while (PA.operation_get_state(o) == PA.OPERATION_RUNNING)
                    waitMainloop();
            }
            finally
            {
                PA.operation_unref(o);
            }
        }
        finally
        {
            unlockMainloop();
        }

        if (!captureDeviceFormats.isEmpty())
        {
            captureDevices.add(
                    0,
                    new CaptureDeviceInfo(
                            NULL_DEV_CAPTURE_DEVICE_INFO_NAME,
                            new MediaLocator(LOCATOR_PROTOCOL + ":"),
                            captureDeviceFormats.toArray(
                                    new Format[captureDeviceFormats.size()])));
        }
        if (!playbackDevices.isEmpty())
        {
            playbackDevices.add(
                    0,
                    new CaptureDeviceInfo(
                            NULL_DEV_CAPTURE_DEVICE_INFO_NAME,
                            new MediaLocator(LOCATOR_PROTOCOL + ":"),
                            null));
        }

        setCaptureDevices(captureDevices);
        setPlaybackDevices(playbackDevices);
    }

    public synchronized long getContext()
    {
        if (context == 0)
        {
            if (!createContext)
            {
                createContext = true;
                createContext();
            }
            if (context == 0)
                throw new IllegalStateException("context");
        }
        return context;
    }

    public void lockMainloop()
    {
        PA.threaded_mainloop_lock(mainloop);
    }

    /**
     * Populates a specific <tt>pa_proplist</tt> which is to be used with a
     * <tt>pa_context</tt> with properties such as the application name and
     * version.
     *
     * @param proplist the <tt>pa_proplist</tt> which is to be populated with
     * <tt>pa_context</tt>-related properties such as the application name and
     * version
     */
    private void populateContextProplist(long proplist)
    {
        VersionService versionService
            = ServiceUtils.getService(
                    NeomediaActivator.getBundleContext(),
                    VersionService.class);

        if (versionService != null)
        {
            Version version = versionService.getCurrentVersion();

            if (version != null)
            {
                String applicationName = version.getApplicationName();

                if (applicationName != null)
                    PA.proplist_sets(
                            proplist,
                            PA.PROP_APPLICATION_NAME,
                            applicationName);

                String applicationVersion = version.toString();

                if (applicationVersion != null)
                    PA.proplist_sets(
                        proplist,
                        PA.PROP_APPLICATION_VERSION,
                        applicationVersion);
            }
        }
    }

    public void signalMainloop(boolean waitForAccept)
    {
        PA.threaded_mainloop_signal(mainloop, waitForAccept);
    }

    private void sinkInfoListCallback(
            long context,
            long sinkInfo,
            List<CaptureDeviceInfo> deviceList,
            List<Format> formatList)
    {
        int sampleSpecFormat = PA.sink_info_get_sample_spec_format(sinkInfo);

        if (sampleSpecFormat != PA.SAMPLE_S16LE)
            return;

        String description = PA.sink_info_get_description(sinkInfo);
        String name = PA.sink_info_get_name(sinkInfo);

        if (description == null)
            description = name;
        deviceList.add(
                new CaptureDeviceInfo(
                        description,
                        new MediaLocator(
                                LOCATOR_PROTOCOL
                                    + ":"
                                    + name),
                        null));
    }

    private void sourceInfoListCallback(
            long context,
            long sourceInfo,
            List<CaptureDeviceInfo> deviceList,
            List<Format> formatList)
    {
        int monitorOfSink = PA.source_info_get_monitor_of_sink(sourceInfo);

        if (monitorOfSink != PA.INVALID_INDEX)
            return;

        int sampleSpecFormat
            = PA.source_info_get_sample_spec_format(sourceInfo);

        if (sampleSpecFormat != PA.SAMPLE_S16LE)
            return;

        int channels = PA.source_info_get_sample_spec_channels(sourceInfo);
        int rate = PA.source_info_get_sample_spec_rate(sourceInfo);
        List<Format> sourceInfoFormatList = new LinkedList<Format>();

        if ((MediaUtils.MAX_AUDIO_CHANNELS != Format.NOT_SPECIFIED)
                && (MediaUtils.MAX_AUDIO_CHANNELS < channels))
            channels = MediaUtils.MAX_AUDIO_CHANNELS;
        if ((MediaUtils.MAX_AUDIO_SAMPLE_RATE != Format.NOT_SPECIFIED)
                && (MediaUtils.MAX_AUDIO_SAMPLE_RATE < rate))
            rate = (int) MediaUtils.MAX_AUDIO_SAMPLE_RATE;

        AudioFormat audioFormat
            = new AudioFormat(
                AudioFormat.LINEAR,
                rate,
                16,
                channels,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED,
                Format.NOT_SPECIFIED /* frameSizeInBits */,
                Format.NOT_SPECIFIED /* frameRate */,
                Format.byteArray);

        if (!sourceInfoFormatList.contains(audioFormat))
        {
            sourceInfoFormatList.add(audioFormat);
            if (!formatList.contains(audioFormat))
                formatList.add(audioFormat);
        }
        if (!formatList.isEmpty())
        {
            String description = PA.source_info_get_description(sourceInfo);
            String name = PA.source_info_get_name(sourceInfo);

            if (description == null)
                description = name;
            deviceList.add(
                    new CaptureDeviceInfo(
                            description,
                            new MediaLocator(
                                    LOCATOR_PROTOCOL
                                        + ":"
                                        + name),
                            sourceInfoFormatList.toArray(
                                    new Format[sourceInfoFormatList.size()])));
        }
    }

    private void startMainloop()
    {
        if (this.mainloop != 0)
            throw new IllegalStateException("mainloop");

        long mainloop = PA.threaded_mainloop_new();

        if (mainloop == 0)
            throw new RuntimeException("pa_threaded_mainloop_new");

        try
        {
            if (PA.threaded_mainloop_start(mainloop) < 0)
                throw new RuntimeException("pa_threaded_mainloop_start");

            this.mainloop = mainloop;
        }
        finally
        {
            if (this.mainloop == 0)
                PA.threaded_mainloop_free(mainloop);
        }
    }

    private void stopMainloop()
    {
        if (this.mainloop == 0)
            throw new IllegalStateException("mainloop");

        long mainloop = this.mainloop;

        this.mainloop = 0;
        PA.threaded_mainloop_stop(mainloop);
        PA.threaded_mainloop_free(mainloop);
    }

    @Override
    public String toString()
    {
        return "PulseAudio";
    }

    public void unlockMainloop()
    {
        PA.threaded_mainloop_unlock(mainloop);
    }

    private int waitForContextState(
            long context,
            int stateToWaitFor)
    {
        int state;

        while (true)
        {
            state = PA.context_get_state(context);
            if ((PA.CONTEXT_FAILED == state)
                    || (stateToWaitFor == state)
                    || (PA.CONTEXT_TERMINATED == state))
                break;

            waitMainloop();
        }

        return state;
    }

    public int waitForStreamState(
            long stream,
            int stateToWaitFor)
    {
        int state;

        while (true)
        {
            state = PA.stream_get_state(stream);
            if ((stateToWaitFor == state)
                    || (PA.STREAM_FAILED == state)
                    || (PA.STREAM_TERMINATED == state))
                break;

            waitMainloop();
        }

        return state;
    }

    public void waitMainloop()
    {
        PA.threaded_mainloop_wait(mainloop);
    }
}
