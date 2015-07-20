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
package net.java.sip.communicator.impl.protocol.mock;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 *
 * @author Damian Minkov
 */
public class MockFileTransferImpl
    extends AbstractFileTransfer
{
    private String id = null;
    private final int direction;
    private final File file;
    private Contact contact = null;

    public MockFileTransferImpl(Contact c, File file, String id, int direction)
    {
        this.id = id;
        this.direction = direction;
        this.file = file;
        this.contact = c;
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferStatusChangeEvent</tt> occured.
     */
    @Override
    public void fireStatusChangeEvent(int newStatus)
    {
        super.fireStatusChangeEvent(newStatus);
    }

    @Override
    public void cancel()
    {
        fireStatusChangeEvent(FileTransferStatusChangeEvent.CANCELED);
    }

    @Override
    public long getTransferedBytes()
    {
        return 1;
    }

    public String getID()
    {
        return id;
    }

    public int getDirection()
    {
        return direction;
    }

    public File getLocalFile()
    {
        return file;
    }

    public Contact getContact()
    {
        return contact;
    }
}
