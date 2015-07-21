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
package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the AIM (Oscar) protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Emil Ivov
 */
public class AimStatusEnum
    extends PresenceStatus
{

    private static Logger logger = Logger.getLogger(AimStatusEnum.class);

    /**
     * The Online AIM status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final AimStatusEnum ONLINE
        = new AimStatusEnum(65, "Online",
                getImageInBytes("service.protocol.aim.AIM_16x16"));

    /**
     * The Invisible AIM status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final AimStatusEnum INVISIBLE
        = new AimStatusEnum(45, "Invisible",
                getImageInBytes("service.protocol.aim.INVISIBLE_STATUS_ICON"));

    /**
     * The Away AIM status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final AimStatusEnum AWAY
        = new AimStatusEnum(40, "Away",
                getImageInBytes("service.protocol.aim.AWAY_STATUS_ICON"));

    /**
     * The Offline AIM status. Indicates the user does not seem to be connected
     * to the AIM network or at least does not want us to know she is
     */
    public static final AimStatusEnum OFFLINE
        = new AimStatusEnum(0, "Offline",
                getImageInBytes("service.protocol.aim.OFFLINE_STATUS_ICON"));

    /**
     * The minimal set of states that any AIM implementation must support.
     */
    public static final ArrayList<AimStatusEnum> aimStatusSet
        = new ArrayList<AimStatusEnum>();
    static{
            aimStatusSet.add(ONLINE);
            aimStatusSet.add(INVISIBLE);
            aimStatusSet.add(AWAY);
            aimStatusSet.add(OFFLINE);
    }

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected AimStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     * identifier.
     */
    private static byte[] getImageInBytes(String imageID)
    {
        InputStream in = IcqActivator.getResources().
            getImageInputStream(imageID);

        if (in == null)
            return null;
        byte[] image = null;
        try
        {
            image = new byte[in.available()];

            in.read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return image;
    }
}
