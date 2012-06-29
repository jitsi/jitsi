/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * implementation of the incoming file transfer request
 * @author Damian Minkov
 */
public class IncomingFileTransferRequestYahooImpl
    implements IncomingFileTransferRequest
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestYahooImpl.class);

    private String id;

    /**
     * The yahoo provider.
     */
    private ProtocolProviderServiceYahooImpl yahooProvider;

    private final OperationSetFileTransferYahooImpl fileTransferOpSet;

    private Contact sender;

    private Date date;

    private String fileName;

    private long fileSize;

    public IncomingFileTransferRequestYahooImpl(
        ProtocolProviderServiceYahooImpl yahooProvider,
        OperationSetFileTransferYahooImpl fileTransferOpSet,
        Contact sender,
        Date date,
        String fileName,
        String fileSize,
        String id)
    {
        this.yahooProvider = yahooProvider;
        this.fileTransferOpSet = fileTransferOpSet;
        this.sender = sender;
        this.date = date;
        this.fileName = fileName;

        try
        {
            this.fileSize = Long.valueOf(fileSize);
        }
        catch (NumberFormatException e)
        {}

        this.id = id;
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
        return fileName;
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
        return fileSize;
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
        AbstractFileTransfer incomingTransfer = null;

        incomingTransfer =
            new FileTransferImpl(yahooProvider,
                id, sender, file, FileTransfer.IN);

        yahooProvider.getYahooSession().fileTransferAccept(id, file);

        FileTransferCreatedEvent event
            = new FileTransferCreatedEvent(incomingTransfer, new Date());

        fileTransferOpSet.fireFileTransferCreated(event);

        incomingTransfer.fireStatusChangeEvent(
            FileTransferStatusChangeEvent.PREPARING);

        return incomingTransfer;
    }

    /**
     * Function called to refuse the file.
     */
    public void rejectFile()
    {
        yahooProvider.getYahooSession().fileTransferReject(id);

        fileTransferOpSet.fireFileTransferRequestRejected(
            new FileTransferRequestEvent(
                fileTransferOpSet, this, this.getDate()));
    }

    /**
     * @return the date
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Returns the thumbnail contained in this request.
     *
     * @return the thumbnail contained in this request
     */
    public byte[] getThumbnail()
    {
        return null;
    }
}
