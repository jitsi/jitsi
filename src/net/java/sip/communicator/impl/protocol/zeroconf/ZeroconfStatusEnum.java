/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a Zeroconf contact can fall into.
 *
 * @author Christian Vincenot
 * @author Jonathan Martin
 */
public class ZeroconfStatusEnum
    extends PresenceStatus
{

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final ZeroconfStatusEnum OFFLINE
        = new ZeroconfStatusEnum(
            0,
            "Offline",
            ProtocolIconZeroconfImpl.getImageInBytes(
                "service.protocol.zeroconf.OFFLINE_STATUS_ICON"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final ZeroconfStatusEnum DO_NOT_DISTURB
        = new ZeroconfStatusEnum(
            30,
            "Do Not Disturb",//, "Do Not Disturb",
            ProtocolIconZeroconfImpl.getImageInBytes(
                "service.protocol.zeroconf.DND_STATUS_ICON"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final ZeroconfStatusEnum INVISIBLE
        = new ZeroconfStatusEnum(
            45,
            "Invisible",
            ProtocolIconZeroconfImpl.getImageInBytes(
                "service.protocol.zeroconf.INVISIBLE_STATUS_ICON"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final ZeroconfStatusEnum ONLINE
        = new ZeroconfStatusEnum(
            65,
            "Available",//, "Online"
            ProtocolIconZeroconfImpl.getImageInBytes(
                "service.protocol.zeroconf.ONLINE_STATUS_ICON"));


    /**
     * Initialize the list of supported status states.
     */
    private static List<PresenceStatus> supportedStatusSet = new LinkedList<PresenceStatus>();
    static
    {
        supportedStatusSet.add(OFFLINE);
        supportedStatusSet.add(DO_NOT_DISTURB);

        /* INVISIBLE STATUS could be supported by unregistering JmDNS and
         * accepting unknown contacts' messages */
        //supportedStatusSet.add(INVISIBLE);

        supportedStatusSet.add(ONLINE);
    }

    /**
     * Creates an instance of <tt>ZeroconfPresneceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private ZeroconfStatusEnum(int status,
                                String statusName,
                                byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supproted by the zeroconf
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * zeroconf provider.
     */
    static Iterator<PresenceStatus> supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * @param status String representation of the status
     * @return ZeroconfStatusEnum corresponding the supplied String value
     */
    static ZeroconfStatusEnum statusOf(String status)
    {
        Iterator<PresenceStatus> statusIter = supportedStatusSet();
        while (statusIter.hasNext())
        {
            ZeroconfStatusEnum state = (ZeroconfStatusEnum)statusIter.next();
            if (state.statusName.equalsIgnoreCase(status))
                return state;
        }
        return null;
    }
}
