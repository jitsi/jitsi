/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import java.io.*;

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
    private static final Logger logger
        = Logger.getLogger(ZeroconfStatusEnum.class);

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final ZeroconfStatusEnum OFFLINE
        = new ZeroconfStatusEnum(
            0
            , "Offline"
            , loadIcon("resources/images/protocol/zeroconf/zeroconf-offline.png"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final ZeroconfStatusEnum DO_NOT_DISTURB
        = new ZeroconfStatusEnum(
            30
            ,"Do Not Disturb",//, "Do Not Disturb",
            loadIcon("resources/images/protocol/zeroconf/zeroconf-dnd.png"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final ZeroconfStatusEnum INVISIBLE
        = new ZeroconfStatusEnum(
            45
            , "Invisible"
            , loadIcon( "resources/images/protocol/zeroconf/zeroconf-invisible.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final ZeroconfStatusEnum ONLINE
        = new ZeroconfStatusEnum(
            65
            ,"Available"//, "Online"
            , loadIcon("resources/images/protocol/zeroconf/zeroconf-online.png"));


    /**
     * Initialize the list of supported status states.
     */
    private static List supportedStatusSet = new LinkedList();
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
    static Iterator supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * @param status String representation of the status
     * @return ZeroconfStatusEnum corresponding the supplied String value
     */
    static ZeroconfStatusEnum statusOf(String status)
    {
        Iterator statusIter = supportedStatusSet();
        while (statusIter.hasNext())
        {
            ZeroconfStatusEnum state = (ZeroconfStatusEnum)statusIter.next();
            if (state.statusName.equalsIgnoreCase(status))
                return state;
        }
        return null;
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The path to the image resource.
     * @return The image extracted from the resource at the specified path.
     */
    public static byte[] loadIcon(String imagePath)
    {
        InputStream is = ZeroconfStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);

        byte[] icon = null;
        try
        {
            icon = new byte[is.available()];
            is.read(icon);
        }
        catch (IOException exc)
        {
            logger.error("Failed to load icon: " + imagePath, exc);
        }
        return icon;
    }

}
