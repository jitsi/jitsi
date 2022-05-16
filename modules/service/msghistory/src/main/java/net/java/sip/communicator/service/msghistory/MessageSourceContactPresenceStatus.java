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
package net.java.sip.communicator.service.msghistory;

import net.java.sip.communicator.service.protocol.*;

/**
 * Special message source contact status, can be used to display
 * only one (online) status for all message source contacts.
 * @author Damian Minkov
 */
public class MessageSourceContactPresenceStatus
    extends PresenceStatus
{
    /**
     * An integer for this status.
     */
    public static final int MSG_SRC_CONTACT_ONLINE_THRESHOLD = 89;

    /**
     * Indicates that  is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * An image that graphically represents the status.
     */
    private byte[] statusIcon;
    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate in the chat room.
     */
    public static final MessageSourceContactPresenceStatus
        MSG_SRC_CONTACT_ONLINE = new MessageSourceContactPresenceStatus(
                                        MSG_SRC_CONTACT_ONLINE_THRESHOLD,
                                        ONLINE_STATUS);

    /**
     * Constructs special message source contact status.
     * @param status
     * @param statusName
     */
    protected MessageSourceContactPresenceStatus(int status, String statusName)
    {
        super(status, statusName);
    }

    @Override
    public boolean isOnline()
    {
        return true;
    }

    /**
     * Sets the icon.
     * @param statusIcon
     */
    public void setStatusIcon(byte[] statusIcon)
    {
        this.statusIcon = statusIcon;
    }

    /**
     * Returns an image that graphically represents the status.
     *
     * @return a byte array containing the image that graphically represents the
     * status or null if no such image is available.
     */
    public byte[] getStatusIcon()
    {
        return statusIcon;
    }
}
