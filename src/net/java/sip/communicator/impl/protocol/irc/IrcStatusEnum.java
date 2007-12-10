/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import java.io.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * an IRC contact can fall into.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class IrcStatusEnum
    extends PresenceStatus
{
    private static final Logger logger
        = Logger.getLogger(IrcStatusEnum.class);

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final IrcStatusEnum OFFLINE
        = new IrcStatusEnum(
            0
            , "Offline"
            , loadIcon("resources/images/protocol/irc/cr16-action-irc_offline.png"));

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final IrcStatusEnum AWAY
        = new IrcStatusEnum(
            40
            , "Away"
            , loadIcon("resources/images/protocol/gibberish/gibberish-away.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final IrcStatusEnum ONLINE
        = new IrcStatusEnum(
            65
            , "Online"
            , loadIcon("resources/images/protocol/irc/cr16-action-irc_online.png"));

    /**
     * Initialize the list of supported status states.
     */
    private static List supportedStatusSet = new LinkedList();
    static
    {
        supportedStatusSet.add(OFFLINE);
        supportedStatusSet.add(AWAY);
        supportedStatusSet.add(ONLINE);
    }

    /**
     * Creates an instance of <tt>IrcPresenceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private IrcStatusEnum(int status,
                                String statusName,
                                byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supported by the irc
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * irc provider.
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
        InputStream is = IrcStatusEnum.class.getClassLoader()
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