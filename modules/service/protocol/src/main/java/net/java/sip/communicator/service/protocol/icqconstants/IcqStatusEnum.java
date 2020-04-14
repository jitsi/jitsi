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
package net.java.sip.communicator.service.protocol.icqconstants;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the ICQ (Oscar) protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Emil Ivov
 */
public class IcqStatusEnum
    extends PresenceStatus
{
    /**
     * The <tt>Logger</tt> used by the <tt>IcqStatusEnum</tt> class and its
     * instances for logging output.
     */
    private static Logger logger = Logger.getLogger(IcqStatusEnum.class);

    /**
     * The Free For Chat ICQ status. Indicates that the user is eager to
     * communicate.
     */
    public static final IcqStatusEnum FREE_FOR_CHAT
        = new IcqStatusEnum(85, "Free For Chat",
                loadIcon("resources/images/protocol/icq/icq16x16-ffc.png"));

    /**
     * The Online ICQ status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final IcqStatusEnum ONLINE
        = new IcqStatusEnum(65, "Online",
                loadIcon("resources/images/protocol/icq/icq16x16-online.png"));

    /**
     * The Away ICQ status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final IcqStatusEnum AWAY
        = new IcqStatusEnum(48, "Away",
                loadIcon("resources/images/protocol/icq/icq16x16-away.png"));

    /**
     * The Invisible ICQ status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final IcqStatusEnum INVISIBLE
        = new IcqStatusEnum(45, "Invisible",
                loadIcon("resources/images/protocol/icq/icq16x16-invisible.png"));

    /**
     * The Not Available ICQ status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final IcqStatusEnum NOT_AVAILABLE
        = new IcqStatusEnum(35, "Not Available",
                loadIcon("resources/images/protocol/icq/icq16x16-na.png"));

    /**
     * The DND ICQ status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final IcqStatusEnum DO_NOT_DISTURB
        = new IcqStatusEnum(30, "Do Not Disturb",
                loadIcon("resources/images/protocol/icq/icq16x16-dnd.png"));

    /**
     * The Occupied ICQ status. Indicates that the user has connectivity and
     * communication is particularly unwanted.
     */
    public static final IcqStatusEnum OCCUPIED
        = new IcqStatusEnum(25, "Occupied",
                loadIcon("resources/images/protocol/icq/icq16x16-occupied.png"));

    /**
     * The Offline ICQ status. Indicates the user does not seem to be connected
     * to the ICQ network or at least does not want us to know she is
     */
    public static final IcqStatusEnum OFFLINE
        = new IcqStatusEnum(0, "Offline",
                loadIcon("resources/images/protocol/icq/icq16x16-offline.png"));

    /**
     * The minimal set of states that any ICQ implementation must support.
     */
    public static final ArrayList<IcqStatusEnum> icqStatusSet
        = new ArrayList<IcqStatusEnum>();
    static{
            icqStatusSet.add(FREE_FOR_CHAT);
            icqStatusSet.add(ONLINE);
            icqStatusSet.add(INVISIBLE);
            icqStatusSet.add(AWAY);
            icqStatusSet.add(NOT_AVAILABLE);
            icqStatusSet.add(DO_NOT_DISTURB);
            icqStatusSet.add(OCCUPIED);
            icqStatusSet.add(OFFLINE);
    }

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected IcqStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {
        InputStream is = IcqStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);

        if(is == null)
            return null;

        byte[] icon = null;
        try {
            icon = new byte[is.available()];
            is.read(icon);
        } catch (IOException e) {
            logger.error("Failed to load icon: " + imagePath, e);
        }
        return icon;
    }
}
