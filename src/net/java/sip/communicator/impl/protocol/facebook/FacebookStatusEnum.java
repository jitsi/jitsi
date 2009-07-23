/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states
 * that a Facebook contact can fall into. There are only to status currently:<br>
 * <ol>
 * <li>Online</li>
 * <li>Offline</li>
 * </ol>
 * 
 * @author Dai Zhiwei
 */
public class FacebookStatusEnum
    extends PresenceStatus
{
    private static final Logger logger =
        Logger.getLogger(FacebookStatusEnum.class);

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final FacebookStatusEnum ONLINE =
        new FacebookStatusEnum(65, "Online",
            getImageInBytes("service.protocol.facebook.FACEBOOK_16x16"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to
     * be offline.
     */

    public static final FacebookStatusEnum INVISIBLE =
        new FacebookStatusEnum(45, "Invisible",
            getImageInBytes("service.protocol.facebook.INVISIBLE_STATUS_ICON"));

    /**
     * The Idle status. Friends are considered idle when they have not taken
     * any action on the site in the last 10 minutes.
     */
    public static final FacebookStatusEnum IDLE =
        new FacebookStatusEnum(40, "Idle",
            getImageInBytes("service.protocol.facebook.IDLE_STATUS_ICON"));
    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final FacebookStatusEnum OFFLINE =
        new FacebookStatusEnum(0, "Offline",
            getImageInBytes("service.protocol.facebook.OFFLINE_STATUS_ICON"));

    /**
     * An Occupied status. Indicates that the user has connectivity and
     * communication is particularly unwanted.
     */
    /*
     * public static final FacebookStatusEnum OCCUPIED = new FacebookStatusEnum(
     * 20 , "Occupied" , getImageInBytes("facebookOccupiedIcon"));
     * 
     *//**
         * The DND status. Indicates that the user has connectivity but prefers
         * not to be contacted.
         */
    /*
     * public static final FacebookStatusEnum DO_NOT_DISTURB = new
     * FacebookStatusEnum( 30 , "Do Not Disturb",
     * getImageInBytes("facebookDndIcon"));
     * 
     *//**
         * The Not Available status. Indicates that the user has connectivity
         * but might not be able to immediately act (i.e. even less immediately
         * than when in an Away status ;-P ) upon initiation of communication.
         * 
         */
    /*
     * public static final FacebookStatusEnum NOT_AVAILABLE = new
     * FacebookStatusEnum( 35 , "Not Available" ,
     * getImageInBytes("facebookNaIcon"));
     * 
     *//**
         * The Away status. Indicates that the user has connectivity but might
         * not be able to immediately act upon initiation of communication.
         */
    /*
     * public static final FacebookStatusEnum AWAY = new FacebookStatusEnum( 40 ,
     * "Away" , getImageInBytes("facebookAwayIcon"));
     * 
     *//**
         * The Free For Chat status. Indicates that the user is eager to
         * communicate.
         */
    /*
     * public static final FacebookStatusEnum FREE_FOR_CHAT = new
     * FacebookStatusEnum( 85 , "Free For Chat" ,
     * getImageInBytes("facebookFfcIcon"));
     */

    /**
     * Initialize the list of supported status states.
     */
    private static final List<PresenceStatus> supportedStatusSet
        = new LinkedList<PresenceStatus>();
    static
    {
        supportedStatusSet.add(ONLINE);
        supportedStatusSet.add(IDLE);
        supportedStatusSet.add(INVISIBLE);
        supportedStatusSet.add(OFFLINE);
        /*
         * supportedStatusSet.add(OCCUPIED);
         * supportedStatusSet.add(DO_NOT_DISTURB);
         * supportedStatusSet.add(NOT_AVAILABLE); supportedStatusSet.add(AWAY);
         * 
         * supportedStatusSet.add(FREE_FOR_CHAT);
         */
    }

    /**
     * Creates an instance of <tt>FacebookPresneceStatus</tt> with the
     * specified parameters.
     * 
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private FacebookStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supproted by the facebook
     * provider.
     * 
     * @return an <tt>Iterator</tt> over all status instances supported by the
     *         facebook provider.
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
     *         identifier.
     */
    private static byte[] getImageInBytes(String imageID)
    {
        InputStream in =
            FacebookActivator.getResources().getImageInputStream(imageID);

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
