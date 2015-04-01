/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.protocol.*;

/**
 * The status to display for the contact srouces.
 * @author Damian Minkov
 */
public class BLFPresenceStatus
    extends PresenceStatus
{
    /**
     * The Online status. Indicate that the line is available and free.
     */
    public static final String AVAILABLE = "Available";

    /**
     * On The Phone Chat status.
     * Indicates that the line is used.
     */
    public static final String BUSY = "Busy";

    /**
     * Ringing status.
     * Indicates that the line is currently ringing.
     */
    public static final String RINGING = "On the phone";

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final String OFFLINE = "Offline";

    /**
     * The Online status. Indicate that the line is free.
     */
    public static final BLFPresenceStatus BLF_FREE = new BLFPresenceStatus(
        65, AVAILABLE);

    /**
     * The Offline  status. Indicates the line status retrieval
     * is not available.
     */
    public static final BLFPresenceStatus BLF_OFFLINE = new BLFPresenceStatus(
        0, OFFLINE);

    /**
     * Indicates an On The Phone status.
     */
    public static final BLFPresenceStatus BLF_BUSY = new BLFPresenceStatus(
        30, BUSY);

    /**
     * Indicates Ringing status.
     */
    public static final BLFPresenceStatus BLF_RINGING = new BLFPresenceStatus(
        31, RINGING);

    /**
     * Creates an instance of <tt>BLFPresenceStatus</tt> with the
     * specified parameters.
     *
     * @param status the connectivity level of the new presence status
     *            instance
     * @param statusName the name of the presence status.
     */
    private BLFPresenceStatus(int status, String statusName)
    {
        super(status, statusName);
    }
}
