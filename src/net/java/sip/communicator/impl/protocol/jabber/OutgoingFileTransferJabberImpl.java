/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

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
    public OutgoingFileTransferJabberImpl(OutgoingFileTransfer jabberTransfer)
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
     * Returns the number of bytes already sent to the recipient.
     * 
     * @return the number of bytes already sent to the recipient.
     */
    public long getTransferedBytes()
    {
        return jabberTransfer.getBytesSent();
    }
}