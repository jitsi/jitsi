/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

import net.java.sip.communicator.service.media.event.*;
import net.java.sip.communicator.service.protocol.*;

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
public interface MediaService
{
    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee. It is at that point
     * that the media service would open a port where it would be waiting for
     * data coming from the specified call participant. Subsequent sdpoffers
     * requested for the call that the original call participant belonged to,
     * would receive, the same IP/port couple as the first one in order to allow
     * conferencing. The associated port will be released once the call has
     * ended.
     *
     * @param callParticipant the call participant meant to receive the offer
     * @return a String containing an SDP offer.
     */
    public String generateSdpOffer(CallParticipant callParticipant);

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. It is
     * at that point that the media service would open a port where it would
     * wait for data coming from the specified call participant. Subsequent sdp
     * offers/answers requested for the call that the original call participant
     * belonged to will receive the same IP/port couple as the first one in
     * order to allow conferencing. The associated port will be released once
     * the call has ended.
     *
     * @param callParticipant the call participant meant to receive the answer
     * @return a String containing an SDP answer.
     */
    public String generateSdpAnswer(CallParticipant callParticipant);

    /**
     * Adds a listener that will be listening for incoming media and changes
     * in the state of the media listener
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
     * Initializes the service implementation, and puts it in a state where it
     * could interoperate with other services.
     */
    public void initialize();

    /**
     * Returns true if the media service implementation is initialized and ready
     * for use by other services, and false otherwise.
     *
     * @return true if the service implementation is initialized and ready for
     * use and false otherwise.
     */
    public boolean isInitialized();

    /**
     * Makes the service implementation close all release any devices or other
     * resources that it might have allocated and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown();
}
