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
    @Override
    public void cancel()
    {
        // TODO: Implement cancel() for SSH file transfer.
    }

    /**
     * Returns the number of bytes already transfered through this file transfer.
     *
     * @return the number of bytes already transfered through this file transfer
     */
    @Override
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
