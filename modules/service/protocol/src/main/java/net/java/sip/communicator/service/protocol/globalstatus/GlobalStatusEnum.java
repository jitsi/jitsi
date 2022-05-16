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
package net.java.sip.communicator.service.protocol.globalstatus;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The global statuses available to the system.
 * @author Damian Minkov
 */
public class GlobalStatusEnum
    extends PresenceStatus
{
    /**
     * Indicates that the user is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * Indicates that the user is disconnected.
     */
    public static final String OFFLINE_STATUS = "Offline";

    /**
     * Indicates that the user is away.
     */
    public static final String AWAY_STATUS = "Away";

    /**
     * Indicates that the user is extended away.
     */
    public static final String EXTENDED_AWAY_STATUS = "Extended Away";

    /**
     * Indicates that the user is connected and eager to communicate.
     */
    public static final String FREE_FOR_CHAT_STATUS = "Free For Chat";

    /**
     * Indicates that the user is connected and eager to communicate.
     */
    public static final String DO_NOT_DISTURB_STATUS = "Do Not Disturb";

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final GlobalStatusEnum ONLINE
        = new GlobalStatusEnum(
                65,
                ONLINE_STATUS,
                loadIcon("service.gui.statusicons.USER_ONLINE_ICON"),
                "service.gui.ONLINE");

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final GlobalStatusEnum FREE_FOR_CHAT
        = new GlobalStatusEnum(
                85,
                FREE_FOR_CHAT_STATUS,
                loadIcon("service.gui.statusicons.USER_FFC_ICON"),
                "service.gui.FFC_STATUS");


    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final GlobalStatusEnum AWAY
        = new GlobalStatusEnum(
                48,
                AWAY_STATUS,
                loadIcon("service.gui.statusicons.USER_AWAY_ICON"),
                "service.gui.AWAY_STATUS");

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final GlobalStatusEnum EXTENDED_AWAY
        = new GlobalStatusEnum(
                35,
                EXTENDED_AWAY_STATUS,
                loadIcon("service.gui.statusicons.USER_EXTENDED_AWAY_ICON"),
                "service.gui.EXTENDED_AWAY_STATUS");

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final GlobalStatusEnum DO_NOT_DISTURB
        = new GlobalStatusEnum(
                30,
                DO_NOT_DISTURB_STATUS,
                loadIcon("service.gui.statusicons.USER_DND_ICON"),
                "service.gui.DND_STATUS");

    /**
     * The Offline  status. Indicates the user does not seem to be connected
     * to any network.
     */
    public static final GlobalStatusEnum OFFLINE
        = new GlobalStatusEnum(
                0,
                OFFLINE_STATUS,
                loadIcon("service.gui.statusicons.USER_OFFLINE_ICON"),
                "service.gui.OFFLINE");

    /**
     * The set of states currently supported.
     */
    public static final ArrayList<GlobalStatusEnum> globalStatusSet
        = new ArrayList<GlobalStatusEnum>();
    static
    {
        globalStatusSet.add(ONLINE);
        globalStatusSet.add(FREE_FOR_CHAT);
        globalStatusSet.add(AWAY);

        if(!ConfigurationUtils.isHideExtendedAwayStatus())
            globalStatusSet.add(EXTENDED_AWAY);

        globalStatusSet.add(DO_NOT_DISTURB);
        globalStatusSet.add(OFFLINE);
    }

    private String i18NKey;

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected GlobalStatusEnum(
        int status,
        String statusName,
        byte[] statusIcon,
        String i18NKey)
    {
        super(status, statusName, statusIcon);
        this.i18NKey = i18NKey;
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath)
    {
        return ProtocolProviderActivator.getResourceService()
            .getImageInBytes(imagePath);
    }

    /**
     * Returns the i18n name of the status.
     * @param status the status.
     * @return the i18n name of the status.
     */
    public static String getI18NStatusName(GlobalStatusEnum status)
    {
        return ProtocolProviderActivator.getResourceService()
            .getI18NString(status.i18NKey);
    }

    /**
     * Finds the status with appropriate name and return it.
     * @param name the name we search for.
     * @return the global status.
     */
    public static GlobalStatusEnum getStatusByName(String name)
    {
        for(GlobalStatusEnum gs : globalStatusSet)
        {
            if(gs.getStatusName().equals(name))
                return gs;
        }

        return null;
    }
}
