/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * SSHStatusEnum.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 */
package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An implementation of <tt>PresenceStatus</tt> that enumerates all states that
 * a SSH contact can fall into.
 *
 * @author Shobhit Jindal
 */
public class SSHStatusEnum
        extends PresenceStatus
{

    /**
     * Indicates an Offline status or status with 0 connectivity.
     */
    public static final SSHStatusEnum OFFLINE
            = new SSHStatusEnum(
            0,
            "Offline",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.OFFLINE_STATUS_ICON"));
    
    /**
     * The Not Available status. Indicates that the user has connectivity
     * but might not be able to immediately act (i.e. even less immediately 
     * than when in an Away status ;-P ) upon initiation of communication.
     *
     */
    public static final SSHStatusEnum NOT_AVAILABLE
            = new SSHStatusEnum(
            35,
            "Not Available",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.NA_STATUS_ICON"));
    
    /**
     * The Connecting status. Indicate that the user is connecting to remote
     * server
     */
    public static final SSHStatusEnum CONNECTING
            = new SSHStatusEnum(
            55,
            "Connecting",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.CONNECTING_ICON"));
    
    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate.
     */
    public static final SSHStatusEnum ONLINE
            = new SSHStatusEnum(
            65,
            "Online",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.SSH_16x16"));


    /**
     * The Connecting status. Indicate that the user is connecting to remote
     * server
     */
    public static final SSHStatusEnum CONNECTED
            = new SSHStatusEnum(
            70,
            "Connecting",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.CONNECTED_ICON"));
    
    /**
     * The File Transfer status. Indicate that the user is transfering a file
     * to/from a remote server
     */
    public static final SSHStatusEnum FILE_TRANSFER
            = new SSHStatusEnum(
            75,
            "Transfering File",
            ProtocolIconSSHImpl
                .getImageInBytes("service.protocol.ssh.FILE_TRANSFER_ICON"));
    
    /**
     * Initialize the list of supported status states.
     */
    private static List<PresenceStatus> supportedStatusSet = new LinkedList<PresenceStatus>();
    static
    {
        supportedStatusSet.add(OFFLINE);
//        supportedStatusSet.add(NOT_AVAILABLE);
        supportedStatusSet.add(ONLINE);
//        supportedStatusSet.add(CONNECTING);
    }
    
    /**
     * Creates an instance of <tt>SSHPresneceStatus</tt> with the
     * specified parameters.
     * @param status the connectivity level of the new presence status instance
     * @param statusName the name of the presence status.
     * @param statusIcon the icon associated with this status
     */
    private SSHStatusEnum(int status,
            String statusName,
            byte[] statusIcon)
    {
        super(status, statusName, statusIcon);
    }
    
    /**
     * Returns an iterator over all status instances supproted by the ssh
     * provider.
     * @return an <tt>Iterator</tt> over all status instances supported by the
     * ssh provider.
     */
    static Iterator<PresenceStatus> supportedStatusSet()
    {
        return supportedStatusSet.iterator();
    }
}
