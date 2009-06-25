/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smackx.filetransfer.*;

/**
 * The Jabber protocol extension of the <tt>AbstractFileTransfer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class OutgoingFileTransferJabberImpl
    extends AbstractFileTransfer
{
    private String id;

    private Contact receiver;

    private File file;

    /**
     * The jabber outgoing file transfer.
     */
    private OutgoingFileTransfer jabberTransfer;

    /**
     * Creates an <tt>OutgoingFileTransferJabberImpl</tt> by specifying the
     * Jabber transfer object.
     *  
     * @param jabberTransfer the Jabber transfer object, containing all transfer
     * information
     */
    public OutgoingFileTransferJabberImpl(  Contact receiver,
                                            File file,
                                            OutgoingFileTransfer jabberTransfer)
    {
        this.jabberTransfer = jabberTransfer;
        this.receiver = receiver;
        this.file = file;

        this.id = String.valueOf( System.currentTimeMillis())
            + String.valueOf(hashCode());
    }

    /**
     * Cancels the file transfer.
     */
    public void cancel()
    {
        this.jabberTransfer.cancel();
    }

    /**
     * Returns the number of bytes already sent to the recipient.
     * 
     * @return the number of bytes already sent to the recipient.
     */
    public long getTransferedBytes()
    {
        return jabberTransfer.getBytesSent();
    }

    /**
     * The direction is outgoing.
     * @return OUT.
     */
    public int getDirection()
    {
        return OUT;
    }

    /**
     * The file we are sending.
     * @return the file.
     */
    public File getFile()
    {
        return file;
    }

    /**
     * The contact we are sending the file.
     * @return the receiver.
     */
    public Contact getContact()
    {
        return receiver;
    }

    /**
     * The unique id.
     * @return the id.
     */
    public String getID()
    {
        return id;
    }
}
