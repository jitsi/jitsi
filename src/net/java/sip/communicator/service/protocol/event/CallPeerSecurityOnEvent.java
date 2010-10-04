/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallPeerSecurityOnEvent</tt> is triggered whenever a
 * communication with a given peer is going secure.
 *
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public class CallPeerSecurityOnEvent
    extends CallPeerSecurityStatusEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final String securityString;

    private final boolean isVerified;

    private final String cipher;

    /**
     * The event constructor
     *
     * @param callPeer the call peer associated with this event
     * @param sessionType the type of the session, either AUDIO_SESSION or
     * VIDEO_SESSION
     * @param cipher the cipher used for the encryption
     * @param securityString the security string (SAS)
     * @param isVerified indicates if the security string has already been
     * verified
     */
    public CallPeerSecurityOnEvent( CallPeer callPeer,
                                    int sessionType,
                                    String cipher,
                                    String securityString,
                                    boolean isVerified)
    {
        super(callPeer, sessionType);

        this.cipher = cipher;
        this.securityString = securityString;
        this.isVerified = isVerified;
    }

    /**
     * Returns the <tt>CallPeer</tt> for which this event occurred.
     *
     * @return the <tt>CallPeer</tt> for which this event occurred.
     */
    public CallPeer getCallPeer()
    {
        return (CallPeer) getSource();
    }

    /**
     * Returns the cipher used for the encryption.
     *
     * @return the cipher used for the encryption.
     */
    public String getCipher()
    {
        return cipher;
    }

    /**
     * Returns the security string.
     *
     * @return the security string.
     */
    public String getSecurityString()
    {
        return securityString;
    }

    /**
     * Returns <code>true</code> if the security string was already verified
     * and <code>false</code> - otherwise.
     *
     * @return <code>true</code> if the security string was already verified
     * and <code>false</code> - otherwise.
     */
    public boolean isSecurityVerified()
    {
        return isVerified;
    }
}
