/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.jabberconstants;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>JabberStatusEnum</tt> gives access to presence states for the Sip
 * protocol. All status icons corresponding to presence states are located with
 * the help of the <tt>imagePath</tt> parameter
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class JabberStatusEnum
{
    /**
     * The <tt>Logger</tt> used by the <tt>JabberStatusEnum</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(JabberStatusEnum.class);

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final String AVAILABLE = "Available";

    /**
     * The Away status. Indicates that the user has connectivity but might not
     * be able to immediately act upon initiation of communication.
     */
    public static final String AWAY = "Away";

    /**
     * The DND status. Indicates that the user has connectivity but prefers not
     * to be contacted.
     */
    public static final String DO_NOT_DISTURB = "Do Not Disturb";

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final String FREE_FOR_CHAT = "Free For Chat";

    /**
     * On The Phone Chat status.
     * Indicates that the user is talking to the phone.
     */
    public static final String ON_THE_PHONE = "On the phone";
    
    /**
     * In meeting Chat status.
     * Indicates that the user is in meeting.
     */
    public static final String IN_A_MEETING = "In a meeting";

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    public static final String EXTENDED_AWAY = "Extended Away";

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final String OFFLINE = "Offline";

    /**
     * The Unknown status. Indicate that we don't know if the user is present or
     * not.
     */
    public static final String UNKNOWN = "Unknown";

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    private final JabberPresenceStatus availableStatus;

    /**
     * The Away status. Indicates that the user has connectivity but might not
     * be able to immediately act upon initiation of communication.
     */
    private final JabberPresenceStatus awayStatus;

    /**
     * The DND status. Indicates that the user has connectivity but prefers not
     * to be contacted.
     */
    private final JabberPresenceStatus doNotDisturbStatus;

    /**
     * The Free For Chat status. Indicates that the user is eager to
     * communicate.
     */
    private final JabberPresenceStatus freeForChatStatus;

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    private final JabberPresenceStatus offlineStatus;

    /**
     * Indicates an On The Phone status.
     */
    private final JabberPresenceStatus onThePhoneStatus;
    
    /**
     * Indicates an On The Phone status.
     */
    private final JabberPresenceStatus inMeetingStatus;

    /**
     * Indicates an Extended Away status or status.
     */
    private final JabberPresenceStatus extendedAwayStatus;

    /**
     * The supported status set stores all statuses supported by this protocol
     * implementation.
     */
    public final List<PresenceStatus> supportedStatusSet =
        new LinkedList<PresenceStatus>();

    /**
     * The Unknown status. Indicate that we don't know if the user is present or
     * not.
     */
    private final JabberPresenceStatus unknownStatus;

    private static final Map<String, JabberStatusEnum> existingEnums =
        new Hashtable<String, JabberStatusEnum>();

    /**
     * Returns an instance of JabberStatusEnum for the specified
     * <tt>iconPath</tt> or creates a new one if it doesn't already exist.
     *
     * @param iconPath the location containing the status icons that should
     * be used by this enumeration.
     *
     * @return the newly created JabberStatusEnum instance.
     */
    public static JabberStatusEnum getJabberStatusEnum(String iconPath)
    {
        JabberStatusEnum statusEnum = existingEnums.get(iconPath);

        if(statusEnum != null)
            return statusEnum;

        statusEnum = new JabberStatusEnum(iconPath);

        existingEnums.put(iconPath, statusEnum);

        return statusEnum;
    }

    /**
     * Creates a new instance of JabberStatusEnum using <tt>iconPath</tt> as the
     * root path where it should be reading status icons from.
     *
     * @param iconPath the location containing the status icons that should
     * be used by this enumeration.
     */
    private JabberStatusEnum(String iconPath)
    {
        this.offlineStatus =
            new JabberPresenceStatus(0, OFFLINE, loadIcon(iconPath
                + "/status16x16-offline.png"));

        this.doNotDisturbStatus =
            new JabberPresenceStatus(30, DO_NOT_DISTURB, loadIcon(iconPath
                + "/status16x16-dnd.png"));

        this.onThePhoneStatus =
            new JabberPresenceStatus(31, ON_THE_PHONE, loadIcon(iconPath
                + "/status16x16-phone.png"));
        
        this.inMeetingStatus =
            new JabberPresenceStatus(32, IN_A_MEETING, loadIcon(iconPath
                + "/status16x16-meeting.png"));

        this.extendedAwayStatus =
            new JabberPresenceStatus(35, EXTENDED_AWAY, loadIcon(iconPath
                + "/status16x16-xa.png"));

        this.awayStatus =
            new JabberPresenceStatus(40, AWAY, loadIcon(iconPath
                + "/status16x16-away.png"));

        this.availableStatus =
            new JabberPresenceStatus(65, AVAILABLE, loadIcon(iconPath
                + "/status16x16-online.png"));

        this.freeForChatStatus =
            new JabberPresenceStatus(85, FREE_FOR_CHAT, loadIcon(iconPath
                + "/status16x16-ffc.png"));

        this.unknownStatus =
            new JabberPresenceStatus(1, UNKNOWN, loadIcon(iconPath
                + "/status16x16-offline.png"));

        // Initialize the list of supported status states.
        supportedStatusSet.add(freeForChatStatus);
        supportedStatusSet.add(availableStatus);
        supportedStatusSet.add(awayStatus);
        supportedStatusSet.add(onThePhoneStatus);
        supportedStatusSet.add(inMeetingStatus);
        supportedStatusSet.add(extendedAwayStatus);
        supportedStatusSet.add(doNotDisturbStatus);
        supportedStatusSet.add(offlineStatus);
    }

    /**
     * Returns the offline Jabber status.
     *
     * @param statusName the name of the status.
     * @return the offline Jabber status.
     */
    public JabberPresenceStatus getStatus(String statusName)
    {
        if (statusName.equals(AVAILABLE))
            return availableStatus;
        else if (statusName.equals(OFFLINE))
            return offlineStatus;
        else if (statusName.equals(FREE_FOR_CHAT))
            return freeForChatStatus;
        else if (statusName.equals(DO_NOT_DISTURB))
            return doNotDisturbStatus;
        else if (statusName.equals(AWAY))
            return awayStatus;
        else if (statusName.equals(ON_THE_PHONE))
            return onThePhoneStatus;
        else if(statusName.equals(IN_A_MEETING))
            return inMeetingStatus;
        else if (statusName.equals(EXTENDED_AWAY))
            return extendedAwayStatus;
        else
            return unknownStatus;
    }

    /**
     * Returns an iterator over all status instances supported by the sip
     * provider.
     *
     * @return an <tt>Iterator</tt> over all status instances supported by the
     *         sip provider.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }

    /**
     * Get all status name as array.
     *
     * @return array of <tt>String</tt> representing the different status name
     */
    public static String[] getStatusNames()
    {
        return new String[]
        { OFFLINE, DO_NOT_DISTURB, AWAY, AVAILABLE, FREE_FOR_CHAT };
    }

    /**
     * Loads an image from a given image path.
     *
     * @param imagePath The path to the image resource.
     * @return The image extracted from the resource at the specified path.
     */
    public static byte[] loadIcon(String imagePath)
    {
        return loadIcon(imagePath, JabberStatusEnum.class);
    }

    /**
     * Loads the icon.
     *
     * @param imagePath path of the image
     * @param clazz class name
     * @return the image bytes
     */
    public static byte[] loadIcon(String imagePath, Class<?> clazz)
    {
        InputStream is = getResourceAsStream(imagePath, clazz);

        if(is == null)
            return null;

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
        finally {
            try
            {
                if(is != null)
                    is.close();
            }
            catch (IOException ex)
            {
                /*
                 * We're closing an InputStream so there shouldn't be data loss
                 * because of it (in contrast to an OutputStream) and a warning
                 * in the log should be enough.
                 */
                logger.warn("Failed to close the InputStream of icon: "
                    + imagePath, ex);
            }
        }
        return icon;
    }

    private static InputStream getResourceAsStream(String name, Class<?> clazz)
    {
        if (name.indexOf("://") != -1)
        {
            try
            {
                return new URL(name).openStream();
            }
            catch (IOException ex)
            {
                /*
                 * Well, we didn't really know whether the specified name
                 * represented an URL so we just tried. We'll resort to
                 * Class#getResourceAsStream then.
                 */
            }
        }

        ResourceManagementService resourcesService
                = ProtocolProviderActivator.getResourceService();

        return resourcesService.getImageInputStreamForPath(name);
    }

    /**
     * An implementation of <tt>PresenceStatus</tt> that enumerates all states
     * that a Jabber contact can currently have.
     */
    private static class JabberPresenceStatus
        extends PresenceStatus
    {
        /**
         * Creates an instance of <tt>JabberPresenceStatus</tt> with the
         * specified parameters.
         *
         * @param status the connectivity level of the new presence status
         *            instance
         * @param statusName the name of the presence status.
         * @param statusIcon the icon associated with this status
         */
        private JabberPresenceStatus(int status, String statusName,
            byte[] statusIcon)
        {
            super(status, statusName, statusIcon);
        }
    }
}
