package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the AIM (Oscar) protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Emil Ivov
 */
public class AimStatusEnum
    extends PresenceStatus
{
    
    private static Logger logger = Logger.getLogger(AimStatusEnum.class);

    /**
     * The Online AIM status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final AimStatusEnum ONLINE
        = new AimStatusEnum(65, "Online",
                loadIcon("resources/images/protocol/aim/aim16x16-online.png"));

    /**
     * The Invisible AIM status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final AimStatusEnum INVISIBLE
        = new AimStatusEnum(45, "Invisible",
                loadIcon("resources/images/protocol/aim/aim16x16-invisible.png"));

    /**
     * The Away AIM status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final AimStatusEnum AWAY
        = new AimStatusEnum(40, "Away",
                loadIcon("resources/images/protocol/aim/aim16x16-away.png"));

    /**
     * The Offline AIM status. Indicates the user does not seem to be connected
     * to the AIM network or at least does not want us to know she is
     */
    public static final AimStatusEnum OFFLINE
        = new AimStatusEnum(0, "Offline",
                loadIcon("resources/images/protocol/aim/aim16x16-offline.png"));

    /**
     * The minimal set of states that any AIM implementation must support.
     */
    public static final ArrayList aimStatusSet =new ArrayList();
    static{
            aimStatusSet.add(ONLINE);
            aimStatusSet.add(INVISIBLE);
            aimStatusSet.add(AWAY);
            aimStatusSet.add(OFFLINE);
    }

    /**
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected AimStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }
    
    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {
        return ProtocolIconAimImpl.loadIcon(imagePath);
    }
}
