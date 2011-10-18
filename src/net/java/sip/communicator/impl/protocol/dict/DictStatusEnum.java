/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
