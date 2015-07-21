/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
