/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.ssh;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * SSH implementation of the <tt>AbstractFileTransfer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class FileTransferSSHImpl
    extends AbstractFileTransfer
{
    private final SSHFileTransferDaemon fileTransfer;

    private final Date initialDate;

    /**
     * Creates an SSH implementation of the file transfer interface.
     * 
     * @param fileTransfer the SSH file transfer
     * @param date the initial date of the transfer
     */
    public FileTransferSSHImpl( SSHFileTransferDaemon fileTransfer,
                                Date date)
    {
        this.fileTransfer = fileTransfer;
        this.initialDate = date;
    }

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    public void cancel()
    {
        // TODO: Implement cancel() for SSH file transfer.
    }

    /**
     * Returns the number of bytes already transfered through this file transfer.
     * 
     * @return the number of bytes already transfered through this file transfer
     */
    public long getTransferedBytes()
    {
        // TODO: Implement getTransferedBytes() for SSH file transfer.
        return 0;
    }

    public int getDirection()
    {
        return IN;
    }

    public File getLocalFile()
    {
        return null;
    }

    public Contact getContact()
    {
        return null;
    }

    public String getID()
    {
        return null;
    }

    public Date getInitialDate()
    {
        return initialDate;
    }
}
