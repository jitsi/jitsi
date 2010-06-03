/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

import java.net.*;
import java.util.*;

import javax.sdp.*;
import javax.media.Time;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.impl.media.notify.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The service is meant to be a wrapper of media libraries such as JMF,
 * (J)FFMPEG, JMFPAPI, and others. It takes care of all media play and capture
 * as well as media transport (e.g. over RTP).
 *
 * Before being able to use this service calls would have to make sure that it 
 * is initialized (i.e. consult the isInitialized() method).
 *
 * @author Emil Ivov
 * @author Martin Andre
 * @author Ryan Ricard
 * @author Symphorien Wanko
 * @author Ken Larson
 */
public class MediaServiceImpl
    implements MediaService
{
    /**
     * Our logger.
     */
    private final Logger logger = Logger.getLogger(MediaServiceImpl.class);

    /**
     * The SdpFactory instance that we use for construction of all sdp
     * descriptions.
     */
    private SdpFactory sdpFactory = null;

    /**
     * A flag indicating whether the media service implementation is ready
     * to be used.
     */
    private boolean isStarted = false;

    /**
     * A flag indicating whether the media service implementation is currently
     * in the process of being started.
     */
    private boolean isStarting = false;

    /**
     * The lock object that we use for synchronization during startup.
     */
    private final Object startingLock = new Object();

    /**
     * Our event dispatcher.
     */
    private MediaEventDispatcher mediaDispatcher = new MediaEventDispatcher();

    /**
     * Our device configuration helper.
     */
    private DeviceConfiguration deviceConfiguration = new DeviceConfiguration();

    /**
     * Our encoding configuration helper.
     */
    private EncodingConfiguration encodingConfiguration = 
        new EncodingConfiguration();

    /**
     * Our media control helper. The media control instance that we will be 
     * using for reading media for all calls that do not have a custom media
     * control mapping.
     */
    private final MediaControl defaultMediaControl = new MediaControl();

    /**
     * Mappings of calls to instances of <tt>MediaControl</tt>. In case a call
     * has been mapped to a media control instance, it is going to be used for
     * retrieving media that we are going to be sending inside this call. 
     * Calls that have custom media control mappings are for example calls that 
     * have been answered by a mailbox plug-in and that will be using a file
     * as their sound source. 
     */
    private final Map<Call, MediaControl> callMediaControlMappings
            = new Hashtable<Call, MediaControl>();

    /**
     * Mappings of calls to custom data sinks. Used by mailbox plug-ins for
     * sending audio/video flows to a file instead of the sound card or the 
     * screen.
     */
    private final Map<Call, URL> callDataSinkMappings
            = new Hashtable<Call, URL>();

    /**
     * Currently open call sessions.
     */
    //private Map activeCallSessions =  new Hashtable();

    /**
     * Default constructor
     */
    public MediaServiceImpl()
    {
    }

    /**
     * Implements <tt>getSupportedAudioEncodings</tt> from interface
     * <tt>MediaService</tt>
     *
     * @return an array of Strings containing audio formats in the order of
     * preference.
     */
    public String[] getSupportedAudioEncodings()
    {
        return getMediaControl().getSupportedAudioEncodings();
    }

    /**
     * Implements <tt>getSupportedVideoEncodings</tt> from interface
     * <tt>MediaService</tt>
     *
     * @return an array of Strings containing video formats in the order of
     * preference.
     */
    public String[] getSupportedVideoEncodings()
    {
        return getMediaControl().getSupportedVideoEncodings();
    }


    /**
     * Creates a call session for <tt>call</tt>. The method allocates audio
     * and video ports which won't be released until the corresponding call
     * gets into a DISCONNECTED state. If a session already exists for call,
     * it is returned and no new session is created.
     * <p>
     * @param call the Call that we'll be encapsulating in the newly created
     * session.
     * @return a <tt>CallSession</tt> encapsulating <tt>call</tt>.
     * @throws MediaException with code IO_ERROR if we fail allocating ports.
     */
    public CallSession createCallSession(Call call)
        throws MediaException
    {
        waitUntilStarted();

        //if we have this call mapped to a custom data destination, pass that
        //destination on to the callSession.
        CallSessionImpl callSession = new CallSessionImpl(
                call, this, callDataSinkMappings.get(call));

        // commented out because it leaks memory, and activeCallSessions isn't 
        // used anyway (by Michael Koch)
        // activeCallSessions.put(call, callSession);
        /** @todo make sure you remove the session once its over. */

        return callSession;
    }

    /**
     * A <tt>RtpFlow</tt> is an object which role is to handle media data
     * transfer, capture and playback. It's build between two points, a local 
     * and a remote. The media transfered will be in a format specified by the
     * <tt>mediaEncodings</tt> parameter.
     * 
     * @param localIP local address of this RtpFlow
     * @param localPort local port of this RtpFlow
     * @param remoteIP remote address of this RtpFlow
     * @param remotePort remote port of this RtpFlow
     * @param mediaEncodings format used to encode data on this flow
     * @return rtpFlow the newly created <tt>RtpFlow</tt>
     * @throws MediaException if operation fails
     */
    public RtpFlow createRtpFlow(String localIP,
                                 int localPort,
                                 String remoteIP,
                                 int remotePort,
                                 Map<String, List<String>> mediaEncodings)
        throws MediaException
    {
        waitUntilStarted();

        return new RtpFlowImpl(this, localIP, remoteIP,
                localPort, remotePort, new Hashtable<String, List<String>>(mediaEncodings));
    }


    /**
     * Adds a listener that will be listening for incoming media and changes
     * in the state of the media listener.
     *
     * @param listener the listener to register
     */
    public void addMediaListener(MediaListener listener)
    {
        mediaDispatcher.addMediaListener(listener);
    }

    /**
     * Removes a listener that was listening for incoming media and changes
     * in the state of the media listener
     * @param listener the listener to remove
     */
    public void removeMediaListener(MediaListener listener)
    {
        mediaDispatcher.removeMediaListener(listener);
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     */
    public void start()
    {
        /*
         * TODO The method is called only once (in MediaActivator) at the time
         * of this writing. However, it being public suggests it may be called
         * more than once and, if it becomes the case one day, care should be
         * taken to not start a new DeviceConfigurationThread while a previous
         * one is running.
         */
        new DeviceConfigurationThread().start();
    }

    /**
     * Releases all resources and prepares for shutdown.
     */
    public void stop()
    {
        try
        {
            this.closeCaptureDevices();
        }
        catch (MediaException ex)
        {
            logger.error("Failed to properly close capture devices.", ex);
        }
        isStarted = false;
    }

    /**
     * Returns true if the media service implementation is initialized and ready
     * for use by other services, and false otherwise.
     *
     * @return true if the media manager is initialized and false otherwise.
     */
    public boolean isStarted()
    {
        return isStarted;
    }

    /**
     * Verifies whether the media service is started and ready for use and 
     * throws an exception otherwise.
     *
     * @throws MediaException if the media service is not started and ready for
     * use.
     */
    protected void assertStarted()
        throws MediaException
    {
        if (!isStarted()) {
            logger.error("The MediaServiceImpl had not been properly started! "
                          + "Impossible to continue.");
            throw new MediaException(
                "The MediaManager had not been properly started! "
                + "Impossible to continue."
                , MediaException.SERVICE_NOT_STARTED);
        }
    }

    /**
     * Close capture devices specified by configuration service.
     *
     * @throws MediaException if opening the devices fails.
     */
    private void closeCaptureDevices()
        throws MediaException
    {
        getMediaControl().closeCaptureDevices();
    }


    /**
     * Makes the service implementation close all release any devices or other
     * resources that it might have allocated and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown()
    {
        isStarted = false;
    }

    /**
     * A valid instance of an SDP factory that call session may use for
     * manipulating sdp data.
     *
     * @return a valid instance of an SDP factory that call session may use for
     * manipulating sdp data.
     */
    public SdpFactory getSdpFactory()
    {
        return sdpFactory;
    }

    /**
     * A valid instance of the Media Control that the call session may use to
     * query for supported audio video encodings.
     *
     * @return the default instance of the Media Control that the call session
     * may use to query for supported audio video encodings. 
     */
    public MediaControl getMediaControl()
    {
        try
        {
            waitUntilStarted();
        }
        catch (MediaException ex)
        {
            throw new IllegalStateException(ex);
        }

        return defaultMediaControl;
    }

    /**
     * The MediaControl instance that is mapped to <tt>call</tt>. If
     * <tt>call</tt> is not mapped to a particular <tt>MediaControl</tt>
     * instance, the default instance will be returned
     *
     * @param call the call to fetch the MediaControl of
     * @return the instance of MediaControl that is mapped to <tt>call</tt>
     * or the <tt>defaultMediaControl</tt> if no custom one is registered for
     * <tt>call</tt>.
     */
    public MediaControl getMediaControl(Call call)
    {
        MediaControl mediaControl = callMediaControlMappings.get(call);
        return (mediaControl != null) ? mediaControl : getMediaControl();
    }

    /**
     * A valid instance of the DeviceConfiguration that a call session may use
     * to query for supported support of audio/video capture.
     *
     * @return a valid instance of the DeviceConfiguration that a call session
     * may use to query for supported support of audio/video capture.
     */
    public DeviceConfiguration getDeviceConfiguration()
    {
        return deviceConfiguration;
    }
    
    /**
     * A valid instance of the EncodingConfiguration that a call session may use
     * to query for encodings and their priority.
     *
     * @return a valid instance of the EncodingConfiguration
     */
    public EncodingConfiguration getEncodingConfiguration()
    {
        return encodingConfiguration;
    }

    /**
     * We use this thread to detect, initialize and configure all capture
     * devices.
     */
    private class DeviceConfigurationThread
        extends Thread
    {
        /**
         * Sets a thread name and gives the thread a daemon status.
         */
        public DeviceConfigurationThread()
        {
            super("DeviceConfigurationThread");
            setDaemon(true);
        }

        /**
         * Initializes device configuration.
         */
        public void run()
        {
            synchronized(startingLock)
            {
                isStarting = true;

                try
                {
                    deviceConfiguration.initialize();
                    defaultMediaControl.
                        initialize(deviceConfiguration, encodingConfiguration);
                    sdpFactory = SdpFactory.getInstance();

                    registerAudioNotifyService();

                    isStarted = true;
                }
                catch (Throwable ex)
                {
                    logger.error("Failed to initialize media control", ex);
                    isStarted = false;
                }

                isStarting = false;
                startingLock.notifyAll();
            }
        }
    }

    private void registerAudioNotifyService()
    {
        //Create the audio notifier service
        AudioNotifierServiceImpl audioNotifier = 
            new AudioNotifierServiceImpl(deviceConfiguration);

        audioNotifier.setMute(
                !MediaActivator.getConfigurationService()
                    .getBoolean(
                        "net.java.sip.communicator.impl.sound.isSoundEnabled",
                        true));

        MediaActivator.getBundleContext()
                .registerService(
                    AudioNotifierService.class.getName(),
                    audioNotifier,
                    null);

            if (logger.isInfoEnabled())
                logger.info("Audio Notifier Service ...[REGISTERED]");
    }

    /**
     * A utility method that would block until the media service has been
     * started or, in case it already is started, return immediately.
     *
     * @throws MediaException if the media service is not started and ready for
     * use.
     */
    private void waitUntilStarted()
        throws MediaException
    {
        synchronized (startingLock) {
            if (isStarting) {
                try {
                    startingLock.wait();
                } catch (InterruptedException ex) {
                    logger.warn(
                        "Interrupted while waiting for the stack to start", ex);
                }
            }
        }

        assertStarted();
    }

    /**
     * Sets the data source for <tt>call</tt> to the URL <tt>dataSourceURL</tt>
     * instead of the default data source. This is used (for instance)
     * to play audio from a file instead of from a the microphone.
     *
     * @param call the <tt>Call</tt> whose data source will be changed
     * @param dataSourceURL the <tt>URL</tt> of the new data source
     */
    public void setCallDataSource(Call call, URL dataSourceURL)
        throws MediaException
    {
        //create a new instance of MediaControl for this call
        MediaControl callMediaControl = new MediaControl();
        callMediaControl.initDataSourceFromURL(dataSourceURL);
        callMediaControlMappings.put(call, callMediaControl);
    }

    /**
     * Returns the duration (in milliseconds) of the data source
     * being used for the given call. If the data source is not time-based,
     * IE a microphone, or the duration cannot be determined, returns -1
     *
     * @param call the call whose data source duration will be retrieved
     * @return -1 or the duration of the data source
     */
    public double getDataSourceDurationSeconds(Call call)
    {
        Time duration = getMediaControl(call).getOutputDuration();

        if (duration == javax.media.Duration.DURATION_UNKNOWN)
            return -1;
        else return duration.getSeconds();
    }

    /**
     * Unsets the data source for <tt>call</tt>, which will now use the default
     * data source.
     *
     * @param call the call whose data source mapping will be released
     */
    public void unsetCallDataSource(Call call)
    {
        callMediaControlMappings.remove(call);
    }

    /**
     * Sets the Data Destination for <tt>call</tt> to the URL
     * <tt>dataSinkURL</tt> instead of the default data destination. This is
     * used (for instance) to record incoming data to a file instead of sending
     * it to the speakers/screen.
     *
     * @param call the call whose data destination will be changed
     * @param dataSinkURL the URL of the new data sink.
     */
    public void setCallDataSink(Call call, URL dataSinkURL)
    {
        callDataSinkMappings.put(call, dataSinkURL);
    }

    /**
     * Unsets the data destination for <tt>call</tt>, which will now send data
     * to the default destination.
     *
     * @param call the call whose data destination mapping will be released
     */
    public void unsetCallDataSink(Call call)
    {
        callDataSinkMappings.remove(call);
    }

//------------------ main method for testing ---------------------------------
    /**
     * This method is here most probably only temporarily for the sake of
     * testing. @todo remove main method.
     *
     * @param args String[]
     *
     * @throws java.lang.Throwable if it doesn't feel like executing today.
     */
    public static void main(String[] args)
        throws Throwable
    {
        new MediaServiceImpl().start();

        System.out.println("done");
    }
}
