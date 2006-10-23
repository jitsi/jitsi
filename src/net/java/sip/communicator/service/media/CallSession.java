/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

import net.java.sip.communicator.service.protocol.*;
import java.text.*;

/**
 * A CallSession contains parameters associated with a particular Call such as
 * ports used for transmitting and sending media (audio video), a reference to
 * the call itself and others. Call session instances are created through the
 * <tt>openCallSession(Call)</tt> method of the MediaService.
 * <p>
 * One <tt>CallSession</tt> pertains to a single <tt>Call</tt> instance and a
 * single <tt>Call</tt> may only be associated one <tt>CallSession</tt>
 * instance.
 * <p>
 * A call session also allows signalling protocols to generate SDP offers and
 * construct sdp answers.
 *
 * @author Emil Ivov
 */
public interface CallSession
{
    /**
     * The method is meant for use by protocol service implementations when
     * willing to send an invitation to a remote callee.
     *
     * @return a String containing an SDP offer descibing parameters of the
     * <tt>Call</tt> associated with this session.
     * @throws MediaException code INTERNAL_ERROR if generating the offer fails
     * for some reason.
     */
    public String createSdpOffer()
        throws MediaException;

    /**
     * The method is meant for use by protocol service implementations when
     * willing to respond to an invitation received from a remote caller. Apart
     * from simply generating an SDP response description, the method records
     * details
     *
     * @param sdpOffer the SDP offer that we'd like to create an answer for.
     * @param offerer the participant that has sent the offer.
     *
     * @return a String containing an SDP answer descibing parameters of the
     * <tt>Call</tt> associated with this session and matching those advertised
     * by the caller in their <tt>sdpOffer</tt>.
     *
     * @throws MediaException code INTERNAL_ERROR if processing the offer and/or
     * generating the anser fail for some reason.
     * @throws ParseException if <tt>sdpOfferStr</tt> does not contain a valid
     * sdp string.
     */
    public String processSdpOffer(CallParticipant offerer, String sdpOffer)
        throws MediaException, ParseException;

    /**
     * The method is meant for use by protocol service implementations upon
     * reception of an SDP answer in response to an offer sent by us earlier.
     *
     * @param sdpAnswer the SDP answer that we'd like to handle.
     * @param responder the participant that has sent the answer.
     *
     * @throws MediaException code SERVICE_NOT_STARTED if this method is called
     * before the service was started.
     * @throws ParseException if <tt>sdpAnswerStr</tt> does not contain a valid
     * sdp string.
     */
    public void processSdpAnswer(CallParticipant responder, String sdpAnswer)
        throws MediaException, ParseException;

    /**
     * Returns the port that we are using for receiving video data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving video data in this
     * <tt>CallSession</tt>.
     */
    public int getVideoPort();

    /**
     * Returns the port that we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     * <p>
     * @return the port number we are using for receiving audio data in this
     * <tt>CallSession</tt>.
     */
    public int getAudioPort();
}
