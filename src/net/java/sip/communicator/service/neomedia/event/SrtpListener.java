/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import net.java.sip.communicator.service.neomedia.*;

/**
 * The <tt>ZrtpListener</tt> is meant to be used by the media stream creator, as
 * the name indicates in order to be notified when a security event has occurred
 * that concerns ZRTP.
 *
 * @author Yana Stamcheva
 */
public interface SrtpListener
{
    /**
     * Indicates that the security has been turned on. When we are in the case
     * of using multistreams when the master stream ZRTP is initialized and
     * established the param multiStreamData holds the data needed for the
     * slave streams to establish their sessions. If this is a securityTurnedOn
     * event on non master stream the multiStreamData is null.
     * 
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the security cipher that encrypts the call
     * @param sender the control that initiated the event.
     */
    public void securityTurnedOn( int sessionType,
                            String cipher,
                            SrtpControl sender);

    /**
     * Indicates that the security has been turned off.
     * 
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityTurnedOff(int sessionType);

    /**
     * Indicates that a security message has occurred associated with a
     * failure/warning or information coming from the encryption protocol.
     * 
     * @param message the message.
     * @param i18nMessage the internationalized message
     * @param severity severity level 
     */
    public void securityMessageReceived(String message,
                                String i18nMessage,
                                int severity);

    /**
     * Indicates that the other party has timed out replying to our
     * offer to secure the connection.
     *
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityTimeout(int sessionType);

    /**
     * Indicates that we started the process of securing the the connection.
     *
     * @param sessionType the type of the call session - audio or video.
     * @param sender the control that initiated the event.
     */
    public void securityNegotiationStarted(int sessionType, SrtpControl sender);
}
