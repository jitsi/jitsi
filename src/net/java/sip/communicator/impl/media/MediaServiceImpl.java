/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;


import javax.media.*;
import javax.sdp.*;

import javax.swing.*;

import net.java.sip.communicator.impl.media.configuration.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.media.event.MediaEvent;
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

    private SdpFactory sdpFactory;
    private boolean isInitialized = false;

    private Player player = null;
    private JPanel videoPanel = null;

    /**
     * Our event dispatcher.
     */
    private MediaDispatcher mediaDispatcher = new MediaDispatcher();

    /**
     * Our configuration helper.
     */
    private MediaConfiguration mediaConfiguration = new MediaConfiguration();

    /**
     * Our media control helper.
     */
    private MediaControl mediaControl = new MediaControl(mediaConfiguration);

    /**
     * Default constructor
     */
    public MediaServiceImpl() {
    }

    /**
     * Set the configuration service.
     *
     * @param configurationService
     */
    public void setConfigurationService(ConfigurationService configurationService) {
        mediaConfiguration.setConfigurationService(configurationService);
    }

    /**
     * Remove a configuration service.
     *
     * @param configurationService
     */
    public void unsetConfigurationService(ConfigurationService configurationService) {
        mediaConfiguration.unsetConfigurationService(configurationService);
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. It is at that point
     * that the media service would open a port where it would be waiting for
     * data coming from the specified call participant. Subsequent sdpoffers
     * requested for the call that the original call participant belonged to,
     * would receive, the same IP/port couple as the first one in order to allow
     * conferencing. The associated port will be released once the call has
     * ended. See RFC3264 for details on Offer/Answer model with SDP.
     *
     * @param callParticipant the call participant meant to receive the offer
     * @return a String containing an SDP offer.
     */
    public String generateSdpOffer(CallParticipant callParticipant)
    {
        String testSdp = "v=0\n"
            + "o=emcho 0 0 IN IP4 192.168.100.101\n"
            + "s=-\n"
            + "c=IN IP4 192.168.100.101\n"
            + "t=0 0\n"
            + "m=audio 22224 RTP/AVP 0 3 4 5 6 8 15 18\n"
            + "m=video 22222 RTP/AVP 26 34 31\n";

        return testSdp;
    }

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. It is
     * at that point that the media service would open a port where it would
     * wait for data coming from the specified call participant. Subsequent sdp
     * offers/answers requested for the call that the original call participant
     * belonged to will receive the same IP/port couple as the first one in
     * order to allow conferencing. The associated port will be released once
     * the call has ended. See RFC3264 for details on Offer/Answer model with
     * SDP.
     *
     * @param callParticipant the call participant meant to receive the offer
     * @return a String containing an SDP offer.
     */
    public String generateSdpAnswer(CallParticipant callParticipant)
    {
        String testSdp = "v=0\n"
            + "o=emcho 0 0 IN IP4 192.168.100.101\n"
            + "s=-\n"
            + "c=IN IP4 192.168.100.101\n"
            + "t=0 0\n"
            + "m=audio 22224 RTP/AVP 0 3 4 5 6 8 15 18\n"
            + "m=video 22222 RTP/AVP 26 34 31\n";

        return testSdp;
    }

    /**
     * Adds a listener that will be listening for incoming media and changes
     * in the state of the media listener
     * @param listener the listener to register
     */
    public void addMediaListener(MediaListener listener) {
        mediaDispatcher.addMediaListener(listener);
    }

    /**
     * Removes a listener that was listening for incoming media and changes
     * in the state of the media listener
     * @param listener the listener to remove
     */
    public void removeMediaListener(MediaListener listener) {
        mediaDispatcher.removeMediaListener(listener);
    }

    /**
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     */
    public void initialize() {
        //openCaptureDevices();
        //createPlayer();

        videoPanel = new JPanel();

        // Now alert mediaListeners
        MediaEvent mediaEvent = new MediaEvent(videoPanel);
        mediaDispatcher.fireReceivedMediaStream(mediaEvent);

        isInitialized = true;
    }

    /**
     * Returns true if the media service implementation is initialized and ready
     * for use by other services, and false otherwise.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Open capture devices specified by configuration service.
     */
    private void openCaptureDevices()
    {
        mediaControl.openCaptureDevices();
//        javax.media.protocol.DataSource dataSource = mediaControl.getDataSource();
//        dataSource.disconnect();
    }

    /**
     * Makes the service implementation close all release any devices or other
     * resources that it might have allocated and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown() {
        isInitialized = false;
        return;
    }
}
