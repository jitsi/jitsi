/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import java.io.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a SIP contact can currently have.
 *
 * @author Emil Ivov
 */
public class SipStatusEnum
    extends PresenceStatus
{
    private static final Logger logger
        = Logger.getLogger(SipStatusEnum.class);

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final SipStatusEnum OFFLINE
        = new SipStatusEnum(
            0
            , "Offline"
            , loadIcon("resources/images/sip/sip16x16-offline.png"));

    /**
     * The busy status. Indicates that the user has connectivity but is doing
     * something else.
     */
    public static final SipStatusEnum BUSY
        = new SipStatusEnum(
            30,
            "Busy (DND)",
            loadIcon("resources/images/sip/sip16x16-busy.png"));
    
    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    public static final SipStatusEnum ON_THE_PHONE
        = new SipStatusEnum(
            37,
            "On the phone",
            loadIcon("resources/images/sip/sip16x16-phone.png"));
    
    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final SipStatusEnum AWAY
        = new SipStatusEnum(
            40,
            "Away",
            loadIcon("resources/images/sip/sip16x16-away.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final SipStatusEnum ONLINE
        = new SipStatusEnum(
            65
            , "Online"
            , loadIcon("resources/images/sip/sip16x16-online.png"));
    
    /**
     * The Unknown status. Indicate that we don't know if the user is present
     * or not.
     */
    public static final SipStatusEnum UNKNOWN = new SipStatusEnum(
            1,
            "Unknown",
            loadIcon("resources/images/sip/sip16x16-offline.png"));

    /**
     * Initialize the list of supported status states.
     */
    public static List supportedStatusSet = new LinkedList();
    static
    {
        supportedStatusSet.add(ONLINE);
        supportedStatusSet.add(AWAY);
        supportedStatusSet.add(ON_THE_PHONE);
        supportedStatusSet.add(BUSY);
        supportedStatusSet.add(OFFLINE);
    }

    /**
     * Creates an instance of <tt>SipPresneceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private SipStatusEnum(int status,
                                String statusName,
                                byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supproted by the sip
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * sip provider.
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
        InputStream is = SipStatusEnum.class.getClassLoader()
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
