package net.java.sip.communicator.service.protocol.icqconstants;

import net.java.sip.communicator.service.protocol.*;
import java.util.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the ICQ (Oscar) protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <code>List</code> containing
 * all of the status fields.
 *
 * @author Emil Ivov
 */
public class IcqStatusEnum
    extends PresenceStatus
{

    /**
     * The Free For Chat ICQ status. Indicates that the user is eager to
     * communicate.
     */
    public static final IcqStatusEnum FREE_FOR_CHAT
        = new IcqStatusEnum(70, "Free For Chat");

    /**
     * The Online ICQ status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final IcqStatusEnum ONLINE = new IcqStatusEnum(65, "Online");

    /**
     * The Invisible ICQ status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final IcqStatusEnum INVISIBLE
        = new IcqStatusEnum(45, "Invisible");

    /**
     * The Away ICQ status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final IcqStatusEnum AWAY = new IcqStatusEnum(40, "Away");


    /**
     * The Not Available ICQ status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final IcqStatusEnum NOT_AVAILABLE
        = new IcqStatusEnum(35, "Not Available");

    /**
     * The DND ICQ status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final IcqStatusEnum DO_NOT_DISTURB
        = new IcqStatusEnum(30, "Do Not Disturb");

    /**
     * The Occupied ICQ status. Indicates that the user has connectivity and
     * communication is particularly unwanted.
     */
    public static final IcqStatusEnum OCCUPIED
        = new IcqStatusEnum(25, "Occupied");

    /**
     * The Offline ICQ status. Indicates the user does not seem to be connected
     * to the ICQ network or at least does not want us to know she is
     */
    public static final IcqStatusEnum OFFLINE = new IcqStatusEnum(0, "Offline");

    /**
     * The minimal set of states that any ICQ implementation must support.
     */
    public static final ArrayList icqStatusSet =new ArrayList();
    static{
            icqStatusSet.add(FREE_FOR_CHAT);
            icqStatusSet.add(ONLINE);
            icqStatusSet.add(INVISIBLE);
            icqStatusSet.add(AWAY);
            icqStatusSet.add(NOT_AVAILABLE);
            icqStatusSet.add(DO_NOT_DISTURB);
            icqStatusSet.add(OCCUPIED);
            icqStatusSet.add(OFFLINE);
    }

    /**
     * Creates a status with the specified connectivity coeff and name
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     */
    protected IcqStatusEnum(int status, String statusName)
    {
        super(status, statusName);
    }
}
