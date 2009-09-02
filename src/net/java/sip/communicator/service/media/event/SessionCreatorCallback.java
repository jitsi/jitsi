/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media.event;

/**
 * The <tt>SessionCreatorCallback</tt> is meant to be used by the call session
 * creator, as the name indicates in order to be notified when a security event
 * has occured.
 * 
 * @author Yana Stamcheva
 */
public interface SessionCreatorCallback
{
    /**
     * Indicates that the security has been turned on.
     * 
     * @param sessionType the type of the call session - audio or video.
     * @param cipher the cipher
     * @param securityString the SAS
     * @param isVerified indicates if the SAS has been verified
     */
    public void securityOn( int sessionType,
                            String cipher,
                            String securityString,
                            boolean isVerified);

    /**
     * Indicates that the security has been turned off.
     * 
     * @param sessionType the type of the call session - audio or video.
     */
    public void securityOff(int sessionType);

    /**
     * Indicates that a security message has occurred associated with a
     * failure/warning or information coming from the encryption protocol.
     * 
     * @param message the message.
     * @param i18nMessage the internationalized message
     * @param severity severity level 
     */
    public void securityMessage(String message,
                                String i18nMessage,
                                int severity);
}
