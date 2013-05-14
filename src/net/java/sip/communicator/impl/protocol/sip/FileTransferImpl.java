/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.util.UUID;

import javax.net.msrp.FileDataContainer;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The file transfer implementation for MSRP.
 * @author Tom Uijldert
 */
public class FileTransferImpl
    extends AbstractFileTransfer
{
    private CallPeerMsrpImpl handler;
    private String id = null;
    private Contact contact = null;
    private File file = null;
    private FileDataContainer container = null;
    private int direction = -1;

    public FileTransferImpl(
        CallPeerMsrpImpl handler,
        String id, Contact contact, File file, int direction)
            throws FileNotFoundException, SecurityException
    {
        this.handler = handler;
        if (id != null)
            this.id = id;
        else
            this.id = UUID.randomUUID().toString();
        this.contact = contact;
        this.file = file;
        this.direction = direction;
        if (direction == FileTransfer.IN)
            this.container = new FileDataContainer(file);
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.AbstractFileTransfer#cancel()
     */
    public void cancel()
    {
        if (handler != null)
            handler.close();
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.AbstractFileTransfer#getTransferedBytes()
     */
    public long getTransferedBytes()
    {
        if (container != null)
            return container.currentReadOffset();
        return 0L;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.FileTransfer#getID()
     */
    public String getID()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.FileTransfer#getDirection()
     */
    public int getDirection()
    {
        return direction;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.FileTransfer#getContact()
     */
    public Contact getContact()
    {
        return contact;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.FileTransfer#getLocalFile()
     */
    public File getLocalFile()
    {
        return file;
    }

    /**
     * @return the data container object we use for the transfer
     */
    public FileDataContainer getDataContainer()
    {
        return container;
    }

    /**
     * Done transferring.
     */
    protected void completed()
    {
        fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED);
    }

    /**
     * @param handler the handler to set
     */
    public void setHandler(CallPeerMsrpImpl handler)
    {
        this.handler = handler;
    }

    /**
     * @return string representation, file-selection attribute format.
     */
    public String toString()
    {
        return String.format("name:\"%s\" type:%s size:%d",
            this.file.getName(), getContentType(), this.file.length());
    }

    /**
     * @return the content type of this file
     */
    public String getContentType()
    {
        // TODO: generic format, put some more intelligence here..
        return "application/octet-stream";
    }
}
