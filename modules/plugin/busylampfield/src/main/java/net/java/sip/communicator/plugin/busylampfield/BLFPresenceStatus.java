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
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.protocol.*;

/**
 * The status to display for the contact srouces.
 * @author Damian Minkov
 */
public class BLFPresenceStatus
    extends PresenceStatus
{
    /**
     * The Online status. Indicate that the line is available and free.
     */
    public static final String AVAILABLE = "Available";

    /**
     * On The Phone Chat status.
     * Indicates that the line is used.
     */
    public static final String BUSY = "Busy";

    /**
     * Ringing status.
     * Indicates that the line is currently ringing.
     */
    public static final String RINGING = "On the phone";

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final String OFFLINE = "Offline";

    /**
     * The Online status. Indicate that the line is free.
     */
    public static final BLFPresenceStatus BLF_FREE = new BLFPresenceStatus(
        65, AVAILABLE);

    /**
     * The Offline  status. Indicates the line status retrieval
     * is not available.
     */
    public static final BLFPresenceStatus BLF_OFFLINE = new BLFPresenceStatus(
        0, OFFLINE);

    /**
     * Indicates an On The Phone status.
     */
    public static final BLFPresenceStatus BLF_BUSY = new BLFPresenceStatus(
        30, BUSY);

    /**
     * Indicates Ringing status.
     */
    public static final BLFPresenceStatus BLF_RINGING = new BLFPresenceStatus(
        31, RINGING);

    /**
     * Creates an instance of <tt>BLFPresenceStatus</tt> with the
     * specified parameters.
     *
     * @param status the connectivity level of the new presence status
     *            instance
     * @param statusName the name of the presence status.
     */
    private BLFPresenceStatus(int status, String statusName)
    {
        super(status, statusName);
    }
}
