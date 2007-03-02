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

import net.java.sip.communicator.impl.media.device.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The service is meant to be a wrapper of media libraries such as JMF,
 * (J)FFMPEG, JMFPAPI, and others. It takes care of all media play and capture
 * as well as media transport (e.g. over RTP).
 *
 * Before being able to use this service calles would have to make sure that
 * it is initialized (i.e. consult the isInitialized() method).
 *
 * @author Emil Ivov
 * @author Martin Andre
 */
public class MediaServiceImpl
    implements MediaService
{
    private Logger logger = Logger.getLogger(MediaServiceImpl.class);

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
     * Our event dispatcher.
     */
    private MediaEventDispatcher mediaDispatcher = new MediaEventDispatcher();

    /**
     * Our configuration helper.
     */
    private DeviceConfiguration deviceConfiguration = new DeviceConfiguration();

    /**
     * Our media control helper.
     */
    private MediaControl mediaControl = new MediaControl();

    /**
     * Currently open call sessions.
     */
    private Hashtable activeCallSessions =  new Hashtable();

    /**
     * Default constructor
     */
    public MediaServiceImpl()
    {
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
        CallSessionImpl callSession = new CallSessionImpl(call, this);

        callSession.initialize();

        activeCallSessions.put(call, callSession);

        /** @todo make sure you remove the session once its over. */

        return callSession;
    }

    /**
     * Adds a listener that will be listening for incoming media and changes
     * in the state of the media listener
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
        throws MediaException
    {
        deviceConfiguration.initialize();
        mediaControl.initialize(deviceConfiguration);

        sdpFactory = SdpFactory.getInstance();

        /** @todo fire media state change event. */
        isStarted = true;
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
        /** @todo fire media state change event. */
    }

    /**
     * Returns true if the media service implementation is initialized and ready
     * for use by other services, and false otherwise.
     *
     * @return true if the media manager is initialized and false otherwise.
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     *
     * @throws MediaException
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
     * Open capture devices specified by configuration service.
     *
     * @throws MediaException if opening the devices fails.
     */
    private void openCaptureDevices()
        throws MediaException
    {
        mediaControl.initCaptureDevices();
    }

    /**
     * Close capture devices specified by configuration service.
     *
     * @throws MediaException if opening the devices fails.
     */
    private void closeCaptureDevices()
        throws MediaException
    {
        mediaControl.closeCaptureDevices();
    }


    /**
     * Makes the service implementation close all release any devices or other
     * resources that it might have allocated and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown() {
        isStarted = false;
        return;
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
     * @return a valid instance of the Media Control that the call session may
     * use to query for supported audio video encodings.
     */
    public MediaControl getMediaControl()
    {
        return mediaControl;
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


//------------------ main method for testing ---------------------------------
    /**
     * This method is here most probably only temporarily for the sache of
     * testing. @todo remove main method.
     * @param args String[]
     */
    public static void main(String[] args)
        throws Throwable
    {
        MediaServiceImpl msimpl = new MediaServiceImpl();
        msimpl.start();

        System.out.println("done");
    }
}
