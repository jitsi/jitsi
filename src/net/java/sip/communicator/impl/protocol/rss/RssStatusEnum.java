/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
            0,
            "Offline",
            getImageInBytes("service.protocol.rss.OFFLINE_STATUS_ICON"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final RssStatusEnum ONLINE
        = new RssStatusEnum(
            65,
            "Online",
            getImageInBytes("service.protocol.rss.RSS_16x16"));

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
    static Iterator<PresenceStatus> supportedStatusSet()
    {
        return supportedStatusSet.iterator();
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
        InputStream in = RssActivator.getResources().
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
