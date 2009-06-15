/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
     * The timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * Creates a <tt>FileTransferRequestEvent</tt> representing reception
     * of an incoming file transfer request.
     *
     * @param request the <tt>IncomingFileTranferRequest</tt> whose reception
     * this event represents.
     * @param timestamp the timestamp indicating the exact date when the event
     * occurred
     */
    public FileTransferRequestEvent(IncomingFileTransferRequest request,
                                    Date timestamp)
    {
        super(request);

        this.timestamp = timestamp;
    }

     /**
     * Returns the incoming file transfer request that triggered this event.
     * 
     * @return the <tt>IncomingFileTransferRequest</tt> that triggered this
     * event.
     */
    public IncomingFileTransferRequest getRequest()
    {
        return (IncomingFileTransferRequest) getSource();
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
