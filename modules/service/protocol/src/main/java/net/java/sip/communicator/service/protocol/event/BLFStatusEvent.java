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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * The status event used when Busy Lamp Field state changes.
 * @author Damian Minkov
 */
public class BLFStatusEvent
    extends EventObject
{
    /**
     * Indicates that the <tt>BLFStatusEvent</tt> instance was triggered
     * by the change of the line to offline.
     */
    public static final int STATUS_OFFLINE = 0;

    /**
     * Indicates that the <tt>BLFStatusEvent</tt> instance was triggered
     * by the change of the line to ringing or setting up the call.
     */
    public static final int STATUS_RINGING = 1;

    /**
     * Indicates that the <tt>BLFStatusEvent</tt> instance was triggered
     * by the change of the line to busy, someone is on the phone.
     */
    public static final int STATUS_BUSY = 2;

    /**
     * Indicates that the <tt>BLFStatusEvent</tt> instance was triggered
     * by the change of the line to available, free, no one is using it.
     */
    public static final int STATUS_FREE = 3;

    /**
     * The type of the event.
     */
    private int type = STATUS_OFFLINE;

    /**
     * Constructs a BLFStatus event.
     *
     * @param source The object on which the Event initially occurred.
     * @param type the event type
     *
     * @throws IllegalArgumentException if source is null.
     */
    public BLFStatusEvent(Object source,
                          int type)
    {
        super(source);
        this.type = type;
    }

    /**
     * Returns the type of the event.
     * @return the type of the event.
     */
    public int getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        String statusName = null;
        switch(type)
        {
            case STATUS_OFFLINE :
                statusName = "Offline"; break;
            case STATUS_RINGING :
                statusName = "Ringing"; break;
            case STATUS_BUSY :
                statusName = "Busy"; break;
            case STATUS_FREE :
                statusName = "Free"; break;
        }
        return "BLFStatusEvent{" +
            "type=" + type +
            ", name=" + statusName +
            '}';
    }
}
