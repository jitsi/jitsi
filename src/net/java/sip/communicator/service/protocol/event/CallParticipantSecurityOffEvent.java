/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallParticipantSecurityAuthenticationEvent</tt> is triggered whenever
 * a the security strings are received in a secure call. 
 * 
 * @author Yana Stamcheva
 */
public class CallParticipantSecurityOffEvent
    extends CallParticipantSecurityStatusEvent
{
    private final String sessionType;

    /**
     * The event constructor.
     * 
     * @param callParticipant
     *              the call participant associated with this event
     * @param sessionType
     *              the type of the session: audio or video
     */
    public CallParticipantSecurityOffEvent( CallParticipant callParticipant,
                                            String sessionType)
    {
        super(callParticipant);

        this.sessionType = sessionType;
    }

    /**
     * Returns the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     * 
     * @return the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     */
    public String getSessionType()
    {
        return sessionType;
    }
}
