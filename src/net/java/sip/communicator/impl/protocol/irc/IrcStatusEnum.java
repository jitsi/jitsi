/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.dict.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
            0,
            "Offline",
            getImageInBytes("service.protocol.irc.OFFLINE_STATUS_ICON"));

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final IrcStatusEnum AWAY
        = new IrcStatusEnum(
            40,
            "Away",
            getImageInBytes("service.protocol.irc.AWAY_STATUS_ICON"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final IrcStatusEnum ONLINE
        = new IrcStatusEnum(
            65,
            "Online",
            getImageInBytes("service.protocol.irc.PROTOCOL_ICON"));

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
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     * 
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     * identifier.
     */
    private static byte[] getImageInBytes(String imageID) 
    {
        InputStream in = DictActivator.getResources().
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