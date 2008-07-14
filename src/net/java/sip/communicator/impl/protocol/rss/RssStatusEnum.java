/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import java.io.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a Rss contact can fall into.
 *
 * @author Jean-Albert Vescovo
 */
public class RssStatusEnum
    extends PresenceStatus
{
    private static final Logger logger
        = Logger.getLogger(RssStatusEnum.class);

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final RssStatusEnum OFFLINE
        = new RssStatusEnum(
            0
            , "Offline"
            , loadIcon("resources/images/protocol/rss/rss-offline.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final RssStatusEnum ONLINE
        = new RssStatusEnum(
            65
            , "Online"
            , loadIcon("resources/images/protocol/rss/rss-online.png"));

    /**
     * Initialize the list of supported status states.
     */
    private static List supportedStatusSet = new LinkedList();
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
    private RssStatusEnum(int status,
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
    static Iterator supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The path to the image resource.
     * @return The image extracted from the resource at the specified path.
     */
    public static byte[] loadIcon(String imagePath)
    {
        return ProtocolIconRssImpl.loadIcon(imagePath);
    }

}
