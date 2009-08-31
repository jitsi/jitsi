/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.msnconstants;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the msn protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Damian Minkov
 */
public class MsnStatusEnum
    extends PresenceStatus
{
    private static Logger logger = Logger.getLogger(MsnStatusEnum.class);

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final MsnStatusEnum ONLINE
        = new MsnStatusEnum(65, "Online",
                loadIcon("resources/images/protocol/msn/msn16x16-online.png"));

    /**
     * The Idle status. Indicates that the user is not using the messanger.
     */
    public static final MsnStatusEnum IDLE
        = new MsnStatusEnum(55, "Idle",
                loadIcon("resources/images/protocol/msn/msn16x16-na.png"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final MsnStatusEnum HIDE
        = new MsnStatusEnum(45, "Hide",
                loadIcon("resources/images/protocol/msn/msn16x16-invisible.png"));

    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final MsnStatusEnum AWAY
        = new MsnStatusEnum(40, "Away",
                loadIcon("resources/images/protocol/msn/msn16x16-away.png"));

    /**
     * The Out to lunch status. Indicates that the user is eating.
     */
    public static final MsnStatusEnum OUT_TO_LUNCH
        = new MsnStatusEnum(39, "Out to lunch",
                loadIcon("resources/images/protocol/msn/msn16x16-lunch.png"));

    /**
     * The On the phone status. Indicates that the user is talking to the phone.
     */
    public static final MsnStatusEnum ON_THE_PHONE
        = new MsnStatusEnum(37, "On the phone",
                loadIcon("resources/images/protocol/msn/msn16x16-phone.png"));

    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final MsnStatusEnum BE_RIGHT_BACK
        = new MsnStatusEnum(35, "Be Right Back",
                loadIcon("resources/images/protocol/msn/msn16x16-brb.png"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final MsnStatusEnum BUSY
        = new MsnStatusEnum(30, "Busy",
                loadIcon("resources/images/protocol/msn/msn16x16-busy.png"));

    /**
     * The Offline status. Indicates the user does not seem to be connected
     * to the network or at least does not want us to know she is
     */
    public static final MsnStatusEnum OFFLINE
        = new MsnStatusEnum(0, "Offline",
                loadIcon("resources/images/protocol/msn/msn16x16-offline.png"));

    /**
     * The minimal set of states that any implementation must support.
     */
    public static final ArrayList<MsnStatusEnum> msnStatusSet
        = new ArrayList<MsnStatusEnum>();
    static{
            msnStatusSet.add(OUT_TO_LUNCH);
            msnStatusSet.add(ON_THE_PHONE);
            msnStatusSet.add(ONLINE);
            msnStatusSet.add(OFFLINE);
            msnStatusSet.add(IDLE);
            msnStatusSet.add(HIDE);
            msnStatusSet.add(BUSY);
            msnStatusSet.add(BE_RIGHT_BACK);
            msnStatusSet.add(AWAY);
    }

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected MsnStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {
        InputStream is = MsnStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);

        byte[] icon = null;
        try {
            icon = new byte[is.available()];
            is.read(icon);
        } catch (IOException exc) {
            logger.error("Failed to load icon: " + imagePath, exc);
        }
        return icon;
    }
}
