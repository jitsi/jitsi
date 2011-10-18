/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
