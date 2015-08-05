/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Implementation of the incoming file transfer request.
 *
 * @author Damian Minkov
 */
public class IncomingFileTransferRequestYahooImpl
    implements IncomingFileTransferRequest
{
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
     * Unique ID that is identifying the request and then the FileTransfer
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
