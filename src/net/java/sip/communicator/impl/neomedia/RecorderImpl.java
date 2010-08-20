/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
     * The <tt>Logger</tt> used by the <tt>RecorderImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(RecorderImpl.class);

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
                    SoundFileUtils.wav
                };

    /**
     * The <tt>MediaDeviceSession</tt> is used to create an output data source.
     */
    private MediaDeviceSession deviceSession;

    /**
     * The format of {@link #deviceSession} in particular and of the recording
     * produced by this <tt>Recorder</tt> in general.
     */
    private final String format;

    /**
     * <tt>DataSink</tt> used to save the output data.
     */
    private DataSink sink;

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

        String format
            = NeomediaActivator
                .getConfigurationService()
                    .getString(Recorder.CALL_FORMAT);

        this.format
            = (format == null)
                ? SoundFileUtils.DEFAULT_CALL_RECORDING_FORMAT
                : format;

        deviceSession
            = device.createRecordingSession(getContentDescriptor(this.format));
    }

    /**
     * Returns a content descriptor to create a recording session with.
     *
     * @param format the format that corresponding to the content descriptor
     * @return content descriptor
     */
    private ContentDescriptor getContentDescriptor(String format)
    {
        String type;

        if (SoundFileUtils.wav.equalsIgnoreCase(format))
            type = FileTypeDescriptor.WAVE;
        else if (SoundFileUtils.gsm.equalsIgnoreCase(format))
            type = FileTypeDescriptor.GSM;
        else if (SoundFileUtils.au.equalsIgnoreCase(format))
            type = FileTypeDescriptor.BASIC_AUDIO;
        else if (SoundFileUtils.aif.equalsIgnoreCase(format))
            type = FileTypeDescriptor.AIFF;
        else
            throw new IllegalArgumentException("format");

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
     * Starts the recording of the media associated with this <tt>Recorder</tt>
     * (e.g. the media being sent and received in a <tt>Call</tt>) into a file
     * with a specific name.
     *
     * @param filename the name of the file into which the media associated with
     * this <tt>Recorder</tt> is to be recorded
     * @throws IOException if anything goes wrong with the input and/or output
     * performed by this <tt>Recorder</tt>
     * @throws MediaException if anything else goes wrong while starting the
     * recording of media performed by this <tt>Recorder</tt>
     * @see Recorder#start(String)
     */
    public void start(String filename)
        throws IOException,
               MediaException
    {
        if (this.sink == null)
        {
            if (filename == null)
                throw new NullPointerException("filename");

            /*
             * A file without an extension may not only turn out to be a touch
             * more difficult to play but is suspected to also cause an
             * exception inside of JMF.
             */
            int extensionBeginIndex = filename.lastIndexOf('.');

            if (extensionBeginIndex < 0)
                filename += '.' + this.format;
            else if (extensionBeginIndex == filename.length() - 1)
                filename += this.format;

            DataSource outputDataSource = deviceSession.getOutputDataSource();

            try
            {
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
                throw
                    new MediaException(
                            "Failed to start recording into file " + filename,
                            ndsex);
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
        }
    }
}
