/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * an IRC contact can fall into.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Lubomir Marinov
 * @author Danny van Heumen
 */
public final class IrcStatusEnum
    extends PresenceStatus
{

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final IrcStatusEnum OFFLINE
        = new IrcStatusEnum(
            0,
            "service.gui.OFFLINE",
            getImageInBytes("service.protocol.irc.OFFLINE_STATUS_ICON"));

    /**
     * The Away status. Indicates that the user has connectivity but might
     * not be able to immediately act upon initiation of communication.
     */
    public static final IrcStatusEnum AWAY
        = new IrcStatusEnum(
            40,
            "service.gui.AWAY_STATUS",
            getImageInBytes("service.protocol.irc.AWAY_STATUS_ICON"));

    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final IrcStatusEnum ONLINE
        = new IrcStatusEnum(
            65,
            "service.gui.ONLINE",
            getImageInBytes("service.protocol.irc.IRC_16x16"));

    /**
     * The list of supported status states.
     */
    private static final List<IrcStatusEnum> SUPPORTED_STATUS_SET;

    /**
     * Initialize an unmodifiable set of supported statuses.
     */
    static
    {
        final LinkedList<IrcStatusEnum> statusSet =
            new LinkedList<IrcStatusEnum>();
        statusSet.add(ONLINE);
        statusSet.add(AWAY);
        statusSet.add(OFFLINE);
        SUPPORTED_STATUS_SET = Collections.unmodifiableList(statusSet);
    }

    /**
     * Creates an instance of <tt>IrcPresenceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private IrcStatusEnum(final int status, final String statusName,
        final byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }

    /**
     * Returns an iterator over all status instances supported by the irc
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * IRC provider.
     */
    static Iterator<IrcStatusEnum> supportedStatusSet()
    {
        return SUPPORTED_STATUS_SET.iterator();
    }

    /**
     * Return <em>i18n</em> IRC presence status name.
     *
     * @return returns i18n status name
     */
    @Override
    public String getStatusName()
    {
        return IrcActivator.getResources().getI18NString(super.getStatusName());
    }

    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     * identifier.
     */
    private static byte[] getImageInBytes(final String imageID)
    {
        return ProtocolIconIrcImpl.getImageInBytes(imageID);
    }
}
