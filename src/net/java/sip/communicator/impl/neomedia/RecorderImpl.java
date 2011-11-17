/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;
import java.util.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.MediaException; // disambiguation
import net.java.sip.communicator.util.*;

/**
 * The call recording implementation.
 * Provides the capability to start and stop call recording.
 *
 * @author Dmitri Melnikov
 * @author Lubomir Marinov
 */
public class RecorderImpl
    implements Recorder
{

    /**
     * The list of formats in which <tt>RecorderImpl</tt> instances support
     * recording media.
     */
    static final String[] SUPPORTED_FORMATS
        = new String[]
                {
                    SoundFileUtils.aif,
                    SoundFileUtils.au,
                    SoundFileUtils.gsm,
                    SoundFileUtils.mp3,
                    SoundFileUtils.wav
                };

    /**
     * The <tt>AudioMixerMediaDevice</tt> which is to be or which is already
     * being recorded by this <tt>Recorder</tt>.
     */
    private final AudioMixerMediaDevice device;

    /**
     * The <tt>MediaDeviceSession</tt> is used to create an output data source.
     */
    private MediaDeviceSession deviceSession;

    /**
     * The <tt>List</tt> of <tt>Recorder.Listener</tt>s interested in
     * notifications from this <tt>Recorder</tt>.
     */
    private final List<Recorder.Listener> listeners
        = new ArrayList<Recorder.Listener>();

    /**
     * <tt>DataSink</tt> used to save the output data.
     */
    private DataSink sink;

    /**
     * The indicator which determines whether this <tt>Recorder</tt>
     * is set to skip media from mic.
     */
    private boolean mute = false;

    /**
     * The filename we will use to record data, supplied
     * when Recorder is started.
     */
    private String filename = null;

    /**
     * Constructs the <tt>RecorderImpl</tt> with the provided session.
     *
     * @param device device that can create a session that provides the output
     * data source
     */
    public RecorderImpl(AudioMixerMediaDevice device)
    {
        if (device == null)
            throw new NullPointerException("device");

        this.device = device;
    }

    /**
     * Adds a new <tt>Recorder.Listener</tt> to the list of listeners interested
     * in notifications from this <tt>Recorder</tt>.
     *
     * @param listener the new <tt>Recorder.Listener</tt> to be added to the
     * list of listeners interested in notifications from this <tt>Recorder</tt>
     * @see Recorder#addListener(Recorder.Listener)
     */
    public void addListener(Recorder.Listener listener)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        synchronized (listeners)
        {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    /**
     * Returns a content descriptor to create a recording session with.
     *
     * @param format the format that corresponding to the content descriptor
     * @return content descriptor
     * @throws IllegalArgumentException if the specified <tt>format</tt> is not
     * a supported recording format
     */
    private ContentDescriptor getContentDescriptor(String format)
        throws IllegalArgumentException
    {
        String type;

        if (SoundFileUtils.wav.equalsIgnoreCase(format))
            type = FileTypeDescriptor.WAVE;
        else if (SoundFileUtils.mp3.equalsIgnoreCase(format))
            type = FileTypeDescriptor.MPEG_AUDIO;
        else if (SoundFileUtils.gsm.equalsIgnoreCase(format))
            type = FileTypeDescriptor.GSM;
        else if (SoundFileUtils.au.equalsIgnoreCase(format))
            type = FileTypeDescriptor.BASIC_AUDIO;
        else if (SoundFileUtils.aif.equalsIgnoreCase(format))
            type = FileTypeDescriptor.AIFF;
        else
        {
            throw
                new IllegalArgumentException(
                        format + " is not a supported recording format.");
        }

        return new ContentDescriptor(type);
    }

    /**
     * Gets a list of the formats in which this <tt>Recorder</tt> supports
     * recording media.
     *
     * @return a <tt>List</tt> of the formats in which this <tt>Recorder</tt>
     * supports recording media
     * @see Recorder#getSupportedFormats()
     */
    public List<String> getSupportedFormats()
    {
        return Arrays.asList(SUPPORTED_FORMATS);
    }

    /**
     * Removes a existing <tt>Recorder.Listener</tt> from the list of listeners
     * interested in notifications from this <tt>Recorder</tt>.
     *
     * @param listener the existing <tt>Recorder.Listener</tt> to be removed
     * from the list of listeners interested in notifications from this
     * <tt>Recorder</tt>
     * @see Recorder#removeListener(Recorder.Listener)
     */
    public void removeListener(Recorder.Listener listener)
    {
        if (listener != null)
        {
            synchronized (listeners)
            {
                listeners.remove(listener);
            }
        }
    }

    /**
     * Starts the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) into a file
     * with a specific name.
     *
     * @param format the format into which the media associated with this
     * <tt>Recorder</tt> is to be recorded into the specified file
     * @param filename the name of the file into which the media associated with
     * this <tt>Recorder</tt> is to be recorded
     * @throws IOException if anything goes wrong with the input and/or output
     * performed by this <tt>Recorder</tt>
     * @throws MediaException if anything else goes wrong while starting the
     * recording of media performed by this <tt>Recorder</tt>
     * @see Recorder#start(String, String)
     */
    public void start(String format, String filename)
        throws IOException,
               MediaException
    {
        if (this.sink == null)
        {
            if (format == null)
                throw new NullPointerException("format");
            if (filename == null)
                throw new NullPointerException("filename");

            this.filename = filename;

            /*
             * A file without an extension may not only turn out to be a touch
             * more difficult to play but is suspected to also cause an
             * exception inside of JMF.
             */
            int extensionBeginIndex = filename.lastIndexOf('.');

            if (extensionBeginIndex < 0)
                filename += '.' + format;
            else if (extensionBeginIndex == filename.length() - 1)
                filename += format;

            MediaDeviceSession deviceSession = device.createSession();

            try
            {
                deviceSession.setContentDescriptor(getContentDescriptor(format));

                // set initial mute state, if mute was set before starting
                // the recorder
                deviceSession.setMute(mute);

                /*
                 * This RecorderImpl will use deviceSession to get a hold of the
                 * media being set to the remote peers associated with the same
                 * AudioMixerMediaDevice i.e. this RecorderImpl needs
                 * deviceSession to only capture and not play back.
                 */
                deviceSession.start(MediaDirection.SENDONLY);

                this.deviceSession = deviceSession;
            }
            finally
            {
                if (this.deviceSession == null)
                {
                    throw new MediaException(
                            "Failed to create MediaDeviceSession from"
                                + " AudioMixerMediaDevice for the purposes of"
                                + " recording");
                }
            }

            Throwable exception = null;

            try
            {
                DataSource outputDataSource
                    = deviceSession.getOutputDataSource();
                DataSink sink
                    = Manager.createDataSink(
                            outputDataSource,
                            new MediaLocator("file:" + filename));

                sink.open();
                sink.start();

                this.sink = sink;
            }
            catch (NoDataSinkException ndsex)
            {
                exception = ndsex;
            }
            finally
            {
                if ((this.sink == null) || (exception != null))
                {
                    stop();

                    throw new MediaException(
                            "Failed to start recording into file " + filename,
                            exception);
                }
            }
        }
    }

    /**
     * Stops the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) if it has
     * been started and prepares this <tt>Recorder</tt> for garbage collection.
     *
     * @see Recorder#stop()
     */
    public void stop()
    {
        if (deviceSession != null)
        {
            deviceSession.close();
            deviceSession = null;
        }

        if (sink != null)
        {
            sink.close();
            sink = null;

            /*
             * RecorderImpl creates the sink upon start() and it does it only if
             * it is null so this RecorderImpl has really stopped only if it has
             * managed to close() the (existing) sink. Notify the registered
             * listeners.
             */
            Recorder.Listener[] listeners;

            synchronized (this.listeners)
            {
                listeners
                    = this.listeners.toArray(
                            new Recorder.Listener[this.listeners.size()]);
            }
            for (Recorder.Listener listener : listeners)
                listener.recorderStopped(this);
        }
    }

    /**
     * Put the recorder in mute state. It won't record the local input.
     * This is used when the local call is muted and we don't won't to record
     * the local input.
     * @param mute the new value of the mute property
     */
    public void setMute(boolean mute)
    {
        this.mute = mute;

        if(deviceSession != null)
        {
            deviceSession.setMute(mute);
        }
    }

    /**
     * Returns the filename we are last started or stopped recording to,
     * null if not started.
     * @return the filename we are last started or stopped recording to,
     * null if not started.
     */
    public String getFilename()
    {
        return filename;
    }
}
