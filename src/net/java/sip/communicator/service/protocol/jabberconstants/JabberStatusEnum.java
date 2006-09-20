package net.java.sip.communicator.service.protocol.jabberconstants;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;

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
    private static Logger logger = Logger.getLogger(IcqStatusEnum.class);

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final JabberStatusEnum FREE_FOR_CHAT
        = new JabberStatusEnum(70, "Free For Chat",
                loadIcon("resources/images/jabber/jabber16x16-ffc.png"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final JabberStatusEnum AVAILABLE
        = new JabberStatusEnum(65, "Available",
                loadIcon("resources/images/jabber/jabber16x16-online.png"));

    /**
     * The Invisible status. Indicates that the user has connectivity even
     * though it may appear otherwise to others, to whom she would appear to be
     * offline.
     */
    public static final JabberStatusEnum INVISIBLE
        = new JabberStatusEnum(45, "Invisible",
                loadIcon("resources/images/jabber/jabber16x16-invisible.png"));

    /**
     * The Away  status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final JabberStatusEnum AWAY
        = new JabberStatusEnum(40, "Away",
                loadIcon("resources/images/jabber/jabber16x16-away.png"));


    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately than
     * when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final JabberStatusEnum EXTENDED_AWAY
        = new JabberStatusEnum(35, "Extended Away",
                loadIcon("resources/images/jabber/jabber16x16-na.png"));

    /**
     * The DND status. Indicates that the user has connectivity but prefers
     * not to be contacted.
     */
    public static final JabberStatusEnum DO_NOT_DISTURB
        = new JabberStatusEnum(30, "Do Not Disturb",
                loadIcon("resources/images/jabber/jabber16x16-dnd.png"));

    /**
     * The Offline status. Indicates the user does not seem to be connected
     * to the network or at least does not want us to know she is
     */
    public static final JabberStatusEnum OFFLINE
        = new JabberStatusEnum(0, "Offline",
                loadIcon("resources/images/jabber/jabber16x16-offline.png"));

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
     * Creates a status with the specified connectivity coeff, name and icon.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     * @param statusIcon the icon associated with this status
     */
    protected JabberStatusEnum(int status, String statusName, byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }
    
    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {
        InputStream is = JabberStatusEnum.class.getClassLoader()
            .getResourceAsStream(imagePath);
        
        byte[] icon = null;
        try {
            icon = new byte[is.available()];
            is.read(icon);
        } catch (IOException e) {
            logger.error("Failed to load icon: " + imagePath, e);
        }
        return icon;
    }
}
