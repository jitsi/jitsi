/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.util.*;

import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;

import net.java.sip.communicator.impl.protocol.sip.sdp.SdpUtils;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * MSRP implementation of an incoming file transfer request.
 * @author Tom Uijldert
 */
public class IncomingFileTransferRequestSipImpl
    implements IncomingFileTransferRequest
{
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestSipImpl.class);

    private CallPeerMsrpImpl handler;

    private OperationSetFileTransferMsrpImpl fileTransferOpSet;

    private String filename;

    private long size;

    private Contact sender = null;

    private Date date;

    private String id;

    /**
     * Construct the request
     * @param handler   peer connection handling this transfer
     * @param fileTransferOpSet the transfer operation set
     * @param transferRequest   the actual request for transfer (offer)
     * @param sender    who requested it
     * @param date      when was it requested
     * @throws SdpParseException
     */
    public IncomingFileTransferRequestSipImpl(
        CallPeerMsrpImpl handler,
        OperationSetFileTransferMsrpImpl fileTransferOpSet,
        MediaDescription transferRequest,
        Contact sender,
        Date date) throws SdpParseException
    {
        this.handler = handler;
        this.fileTransferOpSet = fileTransferOpSet;
        /*
         * Parse the media description for relevant parameters.
         * TODO: include other file-defining attributes.
         */
        this.id = transferRequest.getAttribute("file-transfer-id");

        String fdesc = transferRequest.getAttribute(SdpUtils.FILE_SELECTOR);
        String[] selectors = fdesc.split(" ");
        for (String selector : selectors)
        {
            String[] nv = selector.split(":");
            if (nv[0].equalsIgnoreCase("name"))
                this.filename = nv[1];
            else if (nv[0].equalsIgnoreCase("size"))
                this.size = Long.parseLong(nv[1]);
        }

        this.sender = sender;
        this.date = date;
        if (filename != null)
        {
            filename = filename.replace('"', ' ').trim();
        }
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getID()
     */
    public String getID()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getFileName()
     */
    public String getFileName()
    {
        return filename;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getFileDescription()
     */
    public String getFileDescription()
    {
        return "incoming file";
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getFileSize()
     */
    public long getFileSize()
    {
        return size;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getSender()
     */
    public Contact getSender()
    {
        return sender;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#acceptFile(java.io.File)
     */
    public FileTransfer acceptFile(File file)
    {
        try
        {
            logger.debug("accepting file transfer with id: " + getID());

            handler.answer();

            FileTransferImpl transfer = new FileTransferImpl(
                                handler, id, sender, file, FileTransfer.IN);
            handler.setTransferActivity(transfer);
            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(transfer, date);

            fileTransferOpSet.fireFileTransferCreated(event);

            transfer.fireStatusChangeEvent(
                FileTransferStatusChangeEvent.PREPARING);

            return transfer;
        }
        catch (Exception e)
        {
            // TODO: handle - log to screen and stop/cleanup transfer...
            logger.warn("Error accepting file transfer: ", e);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#rejectFile()
     */
    public void rejectFile()
    {
        // TODO: implement
        try
        {
//            incomingFileTransfer.close();

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

    /* (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.
     *      IncomingFileTransferRequest#getThumbnail()
     */
    public byte[] getThumbnail()
    {
        return null;
    }
}
