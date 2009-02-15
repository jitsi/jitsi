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

    /**
     * The event constructor.
     * 
     * @param callParticipant
     *              the call participant associated with this event
     * @param sessionType
     *              the type of the session: audio or video
     */
    public CallParticipantSecurityOffEvent( CallParticipant callParticipant,
                                            int sessionType)
    {
        super(callParticipant, sessionType);
    }
}
