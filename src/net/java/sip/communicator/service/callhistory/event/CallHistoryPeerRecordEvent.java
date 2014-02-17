/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.callhistory.event;

import java.util.*;


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
     * Constructs new <tt>CallHistoryPeerRecordEvent</tt> event.
     * @param peer the peer associated with the event.
     */
    public CallHistoryPeerRecordEvent(String peerAddress, Date startDate)
    {
        super(peerAddress);
        this.startDate = startDate;
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

}
