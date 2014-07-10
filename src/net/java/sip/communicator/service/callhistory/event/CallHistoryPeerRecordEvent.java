/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;


/**
 * An event which is fired when a new call peer history record is added.
 * @author Hristo Terezov
 */
public class CallHistoryPeerRecordEvent
    extends EventObject
{

    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The date when the call peer have started the conversation.
     */
    private static Date startDate;

    /**
     * The provider associated with the call peer.
     */
    private static ProtocolProviderService provider;

    /**
     * Constructs new <tt>CallHistoryPeerRecordEvent</tt> event.
     * @param peerAddress the address of the peer associated with the event.
     * @param startDate the date when the peer has been added.
     * @param provider the provider associated with the peer.
     */
    public CallHistoryPeerRecordEvent(String peerAddress, Date startDate,
        ProtocolProviderService provider)
    {
        super(peerAddress);
        CallHistoryPeerRecordEvent.startDate = startDate;
        CallHistoryPeerRecordEvent.provider = provider;
    }

    /**
     * Returns the start date property of the event.
     * @return the start date property of the event.
     */
    public Date getStartDate()
    {
        return startDate;
    }

    /**
     * Returns the peer address of the event.
     * @return the peer address of the event.
     */
    public String getPeerAddress()
    {
        return (String) getSource();
    }

    /**
     * Returns the protocol provider service associated with the event.
     * @return the protocol provider service associated with the event.
     */
    public ProtocolProviderService getProvider()
    {
        return provider;
    }

}
