/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.device.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * The call recording implementation.
 * Provides the capability to start and stop call recording. 
 * 
 * @author Dmitri Melnikov
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
     * The <tt>MediaDeviceSession</tt> is used to create an output data source.
     */
    private MediaDeviceSession deviceSession;

    /**
     * <tt>DataSink</tt> used to save the output data.
     */
    private DataSink sink;

    /**
     * <tt>true</tt> if recording was started, <tt>false</tt> 
     * otherwise. 
     */
    private boolean recording = false;

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

        ConfigurationService configurationService
            = NeomediaActivator.getConfigurationService();
        String format = configurationService.getString(Recorder.CALL_FORMAT);

        if (format == null)
            format = SoundFileUtils.mp2;
        deviceSession
            = device.createRecordingSession(getContentDescriptor(format));
    }

    /**
     * Starts the call recording.
     *
     * @param filename call filename, when <tt>null</tt> a default filename is
     * used
     */
    public void startRecording(String filename)
    {
        if (!recording)
        {
            if (filename == null)
                throw new NullPointerException("filename");

            DataSource outputDataSource = deviceSession.getOutputDataSource();

            try
            {
                sink
                    = Manager.createDataSink(
                            outputDataSource,
                            new MediaLocator("file:" + filename));
                sink.open();
                sink.start();
            }
            catch (NoDataSinkException ndsex)
            {
                logger.error("No datasink can be found", ndsex);
            }
            catch (IOException ioex)
            {
                logger.error("Writing to datasink failed", ioex);
            }

            recording = true;
        }
    }

    /**
     * Stops the call recording.
     */
    public void stopRecording()
    {
        if (recording)
        {
            deviceSession.close();
            deviceSession = null;

            if (sink != null)
            {
                sink.close();
                sink = null;
            }

            recording = false;
        }
    }

    /**
     * Returns a content descriptor to create a recording session with.
     *
     * @param format the format that corresponding to the content descriptor
     * @return content descriptor
     */
    private ContentDescriptor getContentDescriptor(String format)
    {
        String type = FileTypeDescriptor.MPEG_AUDIO;

        if (SoundFileUtils.wav.equals(format))
            type = FileTypeDescriptor.WAVE;
        else if (SoundFileUtils.gsm.equals(format))
            type = FileTypeDescriptor.GSM;
        else if (SoundFileUtils.au.equals(format))
            type = FileTypeDescriptor.BASIC_AUDIO;
        else if (SoundFileUtils.aif.equals(format))
            type = FileTypeDescriptor.AIFF;

        return new ContentDescriptor(type);
    }
}
