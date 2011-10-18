/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FileTransferRequestEvent</tt> indicates the reception of a file
 * transfer request.
 *
 * @author Nicolas Riegel
 * @author Yana Stamcheva
 */
public class FileTransferRequestEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The request that triggered this event.
     */
    private final IncomingFileTransferRequest request;

    /**
     * The timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * Creates a <tt>FileTransferRequestEvent</tt> representing reception
     * of an incoming file transfer request.
     *
     * @param fileTransferOpSet the operation set, where this event initially occurred
     * @param request the <tt>IncomingFileTranferRequest</tt> whose reception
     * this event represents.
     * @param timestamp the timestamp indicating the exact date when the event
     * occurred
     */
    public FileTransferRequestEvent(OperationSetFileTransfer fileTransferOpSet,
                                    IncomingFileTransferRequest request,
                                    Date timestamp)
    {
        super(fileTransferOpSet);

        this.request = request;
        this.timestamp = timestamp;
    }

    /**
     * Returns the <tt>OperationSetFileTransfer</tt>, where this event initially
     * occurred.
     *
     * @return the <tt>OperationSetFileTransfer</tt>, where this event initially
     * occurred
     */
    public OperationSetFileTransfer getFileTransferOperationSet()
    {
        return (OperationSetFileTransfer) getSource();
    }

     /**
     * Returns the incoming file transfer request that triggered this event.
     *
     * @return the <tt>IncomingFileTransferRequest</tt> that triggered this
     * event.
     */
    public IncomingFileTransferRequest getRequest()
    {
        return request;
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     *
     * @return a Date indicating when the event occurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }
}
