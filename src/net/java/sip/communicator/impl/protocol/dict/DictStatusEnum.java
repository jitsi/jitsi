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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a Dict contact can fall into.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictStatusEnum
    extends PresenceStatus
{

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final DictStatusEnum OFFLINE
        = new DictStatusEnum(
            0, "Offline",
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.OFFLINE_STATUS_ICON"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final DictStatusEnum ONLINE
        = new DictStatusEnum(
            65, "Online",
            DictActivator.getResources()
                .getImageInBytes("service.protocol.dict.DICT_16x16"));

    /**
     * Initialize the list of supported status states.
     */
    private static List<PresenceStatus> supportedStatusSet = new LinkedList<PresenceStatus>();
    static
    {
        supportedStatusSet.add(OFFLINE);
        supportedStatusSet.add(ONLINE);
    }

    /**
     * Creates an instance of <tt>RssPresneceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private DictStatusEnum(int status,
                                String statusName,
                                byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supproted by the rss
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * rss provider.
     */
    static Iterator<PresenceStatus> supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }
}
