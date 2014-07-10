/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.msghistory;

import net.java.sip.communicator.service.protocol.*;

/**
 * Special message source contact status, can be used to display
 * only one (online) status for all message source contacts.
 * @author Damian Minkov
 */
public class MessageSourceContactPresenceStatus
    extends PresenceStatus
{
    /**
     * An integer for this status.
     */
    public static final int MSG_SRC_CONTACT_ONLINE_THRESHOLD = 89;

    /**
     * Indicates that  is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * An image that graphically represents the status.
     */
    private byte[] statusIcon;
    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate in the chat room.
     */
    public static final MessageSourceContactPresenceStatus
        MSG_SRC_CONTACT_ONLINE = new MessageSourceContactPresenceStatus(
                                        MSG_SRC_CONTACT_ONLINE_THRESHOLD,
                                        ONLINE_STATUS);

    /**
     * Constructs special message source contact status.
     * @param status
     * @param statusName
     */
    protected MessageSourceContactPresenceStatus(int status, String statusName)
    {
        super(status, statusName);
    }

    @Override
    public boolean isOnline()
    {
        return true;
    }

    /**
     * Sets the icon.
     * @param statusIcon
     */
    public void setStatusIcon(byte[] statusIcon)
    {
        this.statusIcon = statusIcon;
    }

    /**
     * Returns an image that graphically represents the status.
     *
     * @return a byte array containing the image that graphically represents the
     * status or null if no such image is available.
     */
    public byte[] getStatusIcon()
    {
        return statusIcon;
    }
}
