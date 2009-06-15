/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

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
    /**
     * The jabber incoming file transfer.
     */
    private IncomingFileTransfer jabberTransfer;

    public IncomingFileTransferJabberImpl(IncomingFileTransfer jabberTransfer)
    {
        this.jabberTransfer = jabberTransfer;
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
}
