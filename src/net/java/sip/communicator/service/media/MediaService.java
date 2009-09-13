/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

import java.util.*;

import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;
import java.net.*;

/**
 * The service is meant to be a wrapper of media libraries such as JMF,
 * (J)FFMPEG, JMFPAPI, and others. It takes care of all media play and capture
 * as well as media transport (e.g. over RTP).
 *
 * Before being able to use this service calls would have to make sure that
 * it is initialized (i.e. consult the isInitialized() method).
 *
 * @author Emil Ivov
 * @author Martin Andre
 * @author Ryan Ricard
 * @author Symphorien Wanko
 */
public interface MediaService
{
    /**
     * The name of the property containing the number of binds that a Media
     * Service Implementation should execute in case a port is already
     * bound to (each retry would be on a new random port).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.BIND_RETRIES";

    /**
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MIN_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MIN_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MAX_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.MAX_PORT_NUMBER";

    /**
     * The default number of binds that a Media Service Implementation should
     * execute in case a port is already bound to (each retry would be on a
     * new random port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * With this property video support can be disabled
     * (enabled by default).
     */
    public static final String DISABLE_VIDEO_SUPPORT_PROPERTY_NAME
        = "net.java.sip.communicator.service.media.DISABLE_VIDEO_SUPPORT";

    /**
     * Give an array of Strings containing audio formats in the order of
     * preference.
     *
     * @return an array of Strings containing audio formats in the order of
     * preference.
     */
    public String[] getSupportedAudioEncodings();

    /**
     * Give an array of Strings containing video formats in the order of
     * preference.
     *
     * @return an array of Strings containing video formats in the order of
     * preference.
     */
    public String[] getSupportedVideoEncodings();

    /**
     * Creates a call session for <tt>call</tt>. The method allocates audio
     * and video ports which won't be released until the corresponding call
     * gets into a DISCONNECTED state. If a session already exists for call,
     * it is returned and no new session is created. Once created a session
     * follows the state changes of the call it encapsulates and automatically
     * adapts to them by starting or stopping transmission and/or reception of
     * data. A CallSession would auto destroy when the <tt>Call</tt> it
     * encapsulates enters the CALL_ENDED <tt>CallState</tt>.
     * <p>
     *
     * @param call the Call that we'll be encapsulating in the newly created
     * session.
     * @return a <tt>CallSession</tt> encapsulating <tt>call</tt>.
     *
     * @throws MediaException with code IO_ERROR if we fail allocating ports.
     */
    public CallSession createCallSession(Call call)
        throws MediaException;

    /**
     * Create a RtpFlow which will manage media data transfer on the specified
     * addresses and ports, using the specified codec.
     *
     * @param localIP local IP of this flow
     * @param localPort local port of for this flow
     * @param remoteIP remote IP of this flow
     * @param remotePort remote port of for this flow
     * @param mediaEncodings encoding used for media on this flow
     * @return a <tt>RtpFlow</tt> with the corresponding parameters
     * @throws MediaException throw a media exception if we fail to create the
     * flow
     */
    public RtpFlow createRtpFlow(String localIP,
                                 int localPort,
                                 String remoteIP,
                                 int remotePort,
                                 Map<String, List<String>> mediaEncodings)
        throws MediaException;

    /**
     * Adds a listener that will be listening for incoming media and changes
     * in the state of the media listener.
     *
     * @param listener the listener to register
     */
    public void addMediaListener(MediaListener listener);

    /**
     * Removes a listener that was listening for incoming media and changes
     * in the state of the media listener
     * @param listener the listener to remove
     */
    public void removeMediaListener(MediaListener listener);

    /**
     * Sets the data source for <tt>call</tt> to the URL <tt>dataSourceURL</tt>
     * instead of the default data source. This is used (for instance) to play
     * audio from a file instead of the microphone.
     *
     * @param call the call whose data source will be changed
     * @param dataSourceURL the URL of the new data source
     * @throws MediaException if we fail to initialize the data source
     */
    public void setCallDataSource(Call call, URL dataSourceURL)
        throws MediaException;

    /**
     * Unsets any custom data sources that have been previously set for
     * <tt>call</tt> through the setCallDataSource(Call, URL) method and revert
     * it to the default data source. If no custom data sources have been set
     * for Call, the method has no effect.
     *
     * @param call the call whose data source mapping will be released
     */
    public void unsetCallDataSource(Call call);

    /**
     * Sets the Data Destination for <tt>call</tt> to the URL
     * <tt>dataSinkURL</tt> instead of the default data destination. This is
     * used (for instance) to record incoming data to a file instead of playing
     * it on the speakers/screen
     *
     * @param call the call whose data destination will be changed
     * @param dataSinkURL the URL of the new data sink.
     *
     * @throws MediaException if we fail to initialize the data sink
     */
    public void setCallDataSink(Call call, URL dataSinkURL)
        throws MediaException;

    /**
     * Unsets the data sink for <tt>call</tt>, which will now
     * send data to the default output devices (sound card and/or screen).
     *
     * @param call the call whose data sink mapping will be released
     */
    public void unsetCallDataSink(Call call);

    /**
     * Returns the duration (in milliseconds) of the data source being used for
     * the given call. If the data source is not time-based, i.e. a microphone,
     * the method returns -1.
     *
     * @param call the call whose data source duration will be retrieved
     * @return the duration of the data currently available in the <tt>call</tt>
     * specific data source or -1 if we are using a microphone/webcam.
     **/
    public double getDataSourceDurationSeconds(Call call);


    /**
     * Returns true if the media service implementation is initialized and
     * ready for use by other services, and false otherwise.
     *
     * @return true if the service implementation is initialized and ready for
     * use and false otherwise.
     */
    public boolean isStarted();
}
