/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.File;
import java.util.*;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.filetransfer.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

/**
 * Jabber implementation of the incoming file transfer request
 * 
 * @author Nicolas Riegel
 *
 */
public class IncomingFileTransferRequestJabberImpl
    implements IncomingFileTransferRequest
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestJabberImpl.class);

    private String id;

    /**
     * The Jabber file transfer request.
     */
    private final FileTransferRequest fileTransferRequest;

    private final OperationSetFileTransferJabberImpl fileTransferOpSet;

    private Contact sender;

    /**
     * Creates an <tt>IncomingFileTransferRequestJabberImpl</tt> based on the
     * given <tt>fileTransferRequest</tt>, coming from the Jabber protocol.
     * 
     * @param jabberProvider the protocol provider
     * @param fileTransferOpSet file transfer operation set
     * @param fileTransferRequest the request coming from the Jabber protocol
     * @param date the date on which this request was received
     */
    public IncomingFileTransferRequestJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider,
        OperationSetFileTransferJabberImpl fileTransferOpSet,
        FileTransferRequest fileTransferRequest,
        Date date)
    {
        this.fileTransferOpSet = fileTransferOpSet;
        this.fileTransferRequest = fileTransferRequest;

        String fromUserID
            = StringUtils.parseBareAddress(fileTransferRequest.getRequestor());

        OperationSetPersistentPresenceJabberImpl opSetPersPresence
            = (OperationSetPersistentPresenceJabberImpl)
                jabberProvider.getOperationSet(
                    OperationSetPersistentPresence.class);

        sender = opSetPersPresence.findContactByID(fromUserID);

        this.id = String.valueOf( System.currentTimeMillis())
                    + String.valueOf(hashCode());
    }

    /**
     * Returns the <tt>Contact</tt> making this request.
     * 
     * @return the <tt>Contact</tt> making this request
     */
    public Contact getSender()
    {
        return sender;
    }

    /**
     * Returns the description of the file corresponding to this request.
     * 
     * @return the description of the file corresponding to this request
     */
    public String getFileDescription()
    {
        return fileTransferRequest.getDescription();
    }

    /**
     * Returns the name of the file corresponding to this request.
     * 
     * @return the name of the file corresponding to this request
     */
    public String getFileName()
    {
        return fileTransferRequest.getFileName();
    }

    /**
     * Returns the size of the file corresponding to this request.
     * 
     * @return the size of the file corresponding to this request
     */
    public long getFileSize()
    {
        return fileTransferRequest.getFileSize();
    }

    /**
     * Accepts the file and starts the transfer.
     * 
     * @return a boolean : <code>false</code> if the transfer fails,
     * <code>true</code> otherwise
     */
    public FileTransfer acceptFile(File file)
    {
        AbstractFileTransfer incomingTransfer = null;

        IncomingFileTransfer jabberTransfer = fileTransferRequest.accept();
        try
        {
            incomingTransfer
                = new IncomingFileTransferJabberImpl(
                        id, sender, file, jabberTransfer);

            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(incomingTransfer, new Date());

            fileTransferOpSet.fireFileTransferCreated(event);

            jabberTransfer.recieveFile(file);

            new OperationSetFileTransferJabberImpl
                .FileTransferProgressThread(
                jabberTransfer, incomingTransfer, getFileSize()).start();
        }
        catch (XMPPException e)
        {
            logger.debug("Receiving file failed.", e);
        }

        return incomingTransfer;
    }

    /**
     * Refuses the file transfer request.
     */
    public void rejectFile()
    {
        fileTransferRequest.reject();
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
