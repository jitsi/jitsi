/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.protocol.*;

/**
 * Wraps the {@link CallParticipant} class in order to provide an identical key
 * value corresponding to a <tt>CallParticipantPanel</tt>.
 * 
 * @author Yana Stamcheva
 */
public class CallParticipantWrapper
{
    private String participantName;
    
    private CallParticipant callParticipant;
    
    public CallParticipantWrapper(String participantName)
    {
        this.participantName = participantName;
    }
    
    public CallParticipantWrapper(CallParticipant participant)
    {
        this.callParticipant = participant;
        this.participantName = participant.getDisplayName();
    }

    public CallParticipant getCallParticipant()
    {
        return callParticipant;
    }

    public void setCallParticipant(CallParticipant callParticipant)
    {
        this.callParticipant = callParticipant;
    }

    public String getParticipantName()
    {
        return participantName;
    }

    public void setParticipantName(String participantName)
    {
        this.participantName = participantName;
    }
}
