package net.java.sip.communicator.service.protocol.jabberconstants;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An enumeration containing all status instances that MUST be supported by
 * an implementation of the Jabber protocol. Implementations may
 * support other forms of PresenceStatus but they MUST ALL support those
 * enumerated here.
 * <p>
 * For testing purposes, this class also provides a <tt>List</tt> containing
 * all of the status fields.
 *
 * @author Damian Minkov
 */
public class JabberStatusEnum
    extends PresenceStatus
{

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final JabberStatusEnum FREE_FOR_CHAT
        = new JabberStatusEnum(70, "Free For Chat");

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final JabberStatusEnum AVAILABLE = new JabberStatusEnum(65, "Available");

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final JabberStatusEnum INVISIBLE
        = new JabberStatusEnum(45, "Invisible");

    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final JabberStatusEnum AWAY = new JabberStatusEnum(40, "Away");


    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final JabberStatusEnum EXTENDED_AWAY
        = new JabberStatusEnum(35, "Extended Away");

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final JabberStatusEnum DO_NOT_DISTURB
        = new JabberStatusEnum(30, "Do Not Disturb");

    /**
     * The Offline status. Indicates the user does not seem to be connected
     * to the network or at least does not want us to know she is
     */
    public static final JabberStatusEnum OFFLINE = new JabberStatusEnum(0, "Offline");

    /**
     * The minimal set of states that any implementation must support.
     */
    public static final ArrayList jabberStatusSet =new ArrayList();
    static{
            jabberStatusSet.add(FREE_FOR_CHAT);
            jabberStatusSet.add(AVAILABLE);
            jabberStatusSet.add(INVISIBLE);
            jabberStatusSet.add(AWAY);
            jabberStatusSet.add(EXTENDED_AWAY);
            jabberStatusSet.add(DO_NOT_DISTURB);
            jabberStatusSet.add(OFFLINE);
    }

    /**
     * Creates a status with the specified connectivity coeff and name
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     */
    protected JabberStatusEnum(int status, String statusName)
    {
        super(status, statusName);
    }
}
