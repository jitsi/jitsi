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
package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.rvcmd.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.*;

/**
 * Icq implementation of the incoming file transfer request
 *
 * @author Nicolas Riegel
 * @author Damian Minkov
 */
public class IncomingFileTransferRequestIcqImpl
    implements IncomingFileTransferRequest
{
    /**
     * Logger
     */
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestIcqImpl.class);

    /**
     * The icq provider.
     */
    private ProtocolProviderServiceIcqImpl icqProvider;

    private OperationSetFileTransferIcqImpl fileTransferOpSet;

    /**
     * The ICQ file transfer request received
     */
    private IncomingFileTransfer incomingFileTransfer = null;

    private Contact sender = null;

    private Date date;

    private String id;

    /**
     * Constructor
     *
     * @param fileTransfer icq file transfer request that was received
     */
    public IncomingFileTransferRequestIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider,
        OperationSetFileTransferIcqImpl fileTransferOpSet,
        IncomingFileTransfer fileTransfer,
        Contact sender,
        Date date)
    {
        this.icqProvider = icqProvider;
        this.fileTransferOpSet = fileTransferOpSet;
        this.incomingFileTransfer = fileTransfer;
        this.sender = sender;
        this.date = date;

        id = String.valueOf(incomingFileTransfer.getRvSessionInfo()
                .getRvSession().getRvSessionId());
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
        return incomingFileTransfer.getRequestFileInfo().getFilename();
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
        return incomingFileTransfer.getInvitationMessage().getMessage();
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
        return incomingFileTransfer.getRequestFileInfo().getTotalFileSize();
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
        incomingFileTransfer.setFileMapper(new IcqFileMapper(file));

        FileTransferImpl inFileTransfer =
            new FileTransferImpl(
                incomingFileTransfer,
                id, sender, file,
                FileTransfer.IN);

        FileTransferCreatedEvent event
            = new FileTransferCreatedEvent(inFileTransfer, date);

        fileTransferOpSet.fireFileTransferCreated(event);

        incomingFileTransfer.accept();

        inFileTransfer.fireStatusChangeEvent(
            FileTransferStatusChangeEvent.PREPARING);

        return inFileTransfer;
    }

    /**
     * Function called to refuse the file.
     */
    public void rejectFile()
    {
        try
        {
            incomingFileTransfer.close();

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

    /**
     * Class to say where the incoming file should be save
     *
     * @author Nicolas Riegel
     */
    private class IcqFileMapper
        implements FileMapper
    {
        /**
         * Destination file
         */
        File file = null;

        /**
         * Constructor
         *
         * @param file is the destination file
         */
        public IcqFileMapper(File file)
        {
            this.file = file;
        }

        public File getDestinationFile(SegmentedFilename filename)
        {
            return file;
        }

        public File getUnspecifiedFilename()
        {
            return file;
        }
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
