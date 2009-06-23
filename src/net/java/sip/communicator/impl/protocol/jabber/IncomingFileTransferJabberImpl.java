/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;

import org.jivesoftware.smackx.filetransfer.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber protocol extension of the <tt>AbstractFileTransfer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class IncomingFileTransferJabberImpl
    extends AbstractFileTransfer
{
    private Contact sender = null;
    private File file = null;

    /**
     * The jabber incoming file transfer.
     */
    private IncomingFileTransfer jabberTransfer;

    public IncomingFileTransferJabberImpl(Contact sender,
        File file,
        IncomingFileTransfer jabberTransfer)
    {
        this.jabberTransfer = jabberTransfer;
        this.sender = sender;
        this.file = file;
    }

    /**
     * Cancels the file transfer.
     */
    public void cancel()
    {
        this.jabberTransfer.cancel();
    }

    /**
     * Returns the number of bytes already received from the recipient.
     * 
     * @return the number of bytes already received from the recipient.
     */
    public long getTransferedBytes()
    {
        return jabberTransfer.getAmountWritten();
    }

    /**
     * The direction is incoming.
     * @return IN.
     */
    public int getDirection()
    {
        return IN;
    }

    /**
     * The file we are receiving.
     * @return the file.
     */
    public File getFile()
    {
        return file;
    }

    public Contact getContact()
    {
        return sender;
    }
}
