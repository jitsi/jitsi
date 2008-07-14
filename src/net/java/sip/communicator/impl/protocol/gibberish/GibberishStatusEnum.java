/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import java.io.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a Gibberish contact can fall into.
 *
 * @author Emil Ivov
 */
public class GibberishStatusEnum
    extends PresenceStatus
{
    private static final Logger logger
        = Logger.getLogger(GibberishStatusEnum.class);

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final GibberishStatusEnum OFFLINE
        = new GibberishStatusEnum(
            0
            , "Offline"
            , loadIcon("resources/images/protocol/gibberish/gibberish-offline.png"));

    /**
     * An Occupied status. Indicates that the user has connectivity and
     * communication is particularly unwanted.
     */
    public static final GibberishStatusEnum OCCUPIED
        = new GibberishStatusEnum(
            20
            , "Occupied"
            , loadIcon("resources/images/protocol/gibberish/gibberish-occupied.png"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final GibberishStatusEnum DO_NOT_DISTURB
        = new GibberishStatusEnum(
            30
            , "Do Not Disturb",
            loadIcon("resources/images/protocol/gibberish/gibberish-dnd.png"));

    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final GibberishStatusEnum NOT_AVAILABLE
        = new GibberishStatusEnum(
            35
            , "Not Available"
            , loadIcon("resources/images/protocol/gibberish/gibberish-na.png"));

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final GibberishStatusEnum AWAY
        = new GibberishStatusEnum(
            40
            , "Away"
            , loadIcon("resources/images/protocol/gibberish/gibberish-away.png"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final GibberishStatusEnum INVISIBLE
        = new GibberishStatusEnum(
            45
            , "Invisible"
            , loadIcon( "resources/images/protocol/gibberish/gibberish-invisible.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final GibberishStatusEnum ONLINE
        = new GibberishStatusEnum(
            65
            , "Online"
            , loadIcon("resources/images/protocol/gibberish/gibberish-online.png"));

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final GibberishStatusEnum FREE_FOR_CHAT
        = new GibberishStatusEnum(
            85
            , "Free For Chat"
            , loadIcon("resources/images/protocol/gibberish/gibberish-ffc.png"));

    /**
     * Initialize the list of supported status states.
     */
    private static List supportedStatusSet = new LinkedList();
    static
    {
        supportedStatusSet.add(OFFLINE);
        supportedStatusSet.add(OCCUPIED);
        supportedStatusSet.add(DO_NOT_DISTURB);
        supportedStatusSet.add(NOT_AVAILABLE);
        supportedStatusSet.add(AWAY);
        supportedStatusSet.add(INVISIBLE);
        supportedStatusSet.add(ONLINE);
        supportedStatusSet.add(FREE_FOR_CHAT);
    }

    /**
     * Creates an instance of <tt>GibberishPresneceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private GibberishStatusEnum(int status,
                                String statusName,
                                byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supproted by the gibberish
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * gibberish provider.
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
        return ProtocolIconGibberishImpl.loadIcon(imagePath);
    }

}
