/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.io.File;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;

/**
 * Msn implementation of the incoming file transfer request
 *
 * @author Damian Minkov
 */
public class IncomingFileTransferRequestMsnImpl
    implements IncomingFileTransferRequest
{
    /**
     * Logger
     */
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestMsnImpl.class);

    private OperationSetFileTransferMsnImpl fileTransferOpSet;

    /**
     * The msn file transfer received
     */
    private MsnFileTransfer incomingFileTransfer = null;

    private Contact sender = null;

    private Date date;

    private String id;

    private boolean rejected = false;

    /**
     * Constructor
     *
     * @param incomingFileTransfer MSN file transfer request that was received
     */
    public IncomingFileTransferRequestMsnImpl(
        OperationSetFileTransferMsnImpl fileTransferOpSet,
        MsnFileTransfer incomingFileTransfer,
        Contact sender,
        Date date)
    {
        this.fileTransferOpSet = fileTransferOpSet;
        this.incomingFileTransfer = incomingFileTransfer;
        this.sender = sender;
        this.date = date;

        id = incomingFileTransfer.getID();
    }

    /**
     * Uniquie ID that is identifying the request and then the FileTransfer
     * if the request has been accepted.
     *
     * @return the id.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns a String that represents the name of the file that is being
     * received.
     * If there is no name, returns null.
     * @return a String that represents the name of the file
     */
    public String getFileName()
    {
        return incomingFileTransfer.getFile().getName();
    }

    /**
     * Returns a String that represents the description of the file that is
     * being received.
     * If there is no description available, returns null.
     *
     * @return a String that represents the description of the file
     */
    public String getFileDescription()
    {
        return "";
    }

    /**
     * Returns a long that represents the size of the file that is being
     * received.
     * If there is no file size available, returns null.
     *
     * @return a long that represents the size of the file
     */
    public long getFileSize()
    {
        return incomingFileTransfer.getFileTotalSize();
    }

    /**
     * Returns a String that represents the name of the sender of the file
     * being received.
     * If there is no sender name available, returns null.
     *
     * @return a String that represents the name of the sender
     */
    public Contact getSender()
    {
        return sender;
    }

    /**
     * Function called to accept and receive the file.
     *
     * @param file the file to accept
     * @return the <tt>FileTransfer</tt> object managing the transfer
     */
    public FileTransfer acceptFile(File file)
    {
        incomingFileTransfer.setFile(file);

        FileTransferImpl inFileTransfer =
            new FileTransferImpl(
                incomingFileTransfer,
                sender,
                file,
                FileTransfer.IN);

        FileTransferCreatedEvent event
            = new FileTransferCreatedEvent(inFileTransfer, date);

        fileTransferOpSet.fireFileTransferCreated(event);

        inFileTransfer.fireStatusChangeEvent(
            FileTransferStatusChangeEvent.PREPARING);

        incomingFileTransfer.start();

        return inFileTransfer;
    }

    /**
     * Function called to refuse the file.
     */
    public void rejectFile()
    {
        try
        {
            rejected = true;

            incomingFileTransfer.cancel();

            fileTransferOpSet.fireFileTransferRequestRejected(
                new FileTransferRequestEvent(fileTransferOpSet, this, date));
        }
        catch(IllegalStateException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Error rejecting file",e);
            return;
        }
    }

    public byte[] getThumbnail()
    {
        return null;
    }

    /**
     * @return the date
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * @return the rejected
     */
    public boolean isRejected()
    {
        return rejected;
    }
}
