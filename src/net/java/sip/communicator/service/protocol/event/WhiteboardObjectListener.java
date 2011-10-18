/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that would gather events notifying of WhiteboardObject
 * delivery status.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardObjectListener
    extends EventListener
{
    /**
     * Called when a new incoming <tt>WhiteboardObject</tt> has been received.
     *
     * @param evt the <tt>WhiteboardObjectReceivedEvent</tt> containing
     * the newly received WhiteboardObject, its sender and other details.
     */
    public void whiteboardObjectReceived(WhiteboardObjectReceivedEvent evt);

    /**
     *Called when a deleted <tt>WhiteboardObject</tt> has been received.
     *
     * @param evt the <tt>WhiteboardObjectDeletedEvent</tt> containing
     * the identification of the deleted WhiteboardObject, its sender and 
     * other details.
     */
    public void whiteboardObjectDeleted (WhiteboardObjectDeletedEvent evt);

    /**
     * Called when a modified <tt>WhiteboardObject</tt> has been modified
     * remotely.
     *
     * @param evt the <tt>WhiteboardObjectModifiedEvent</tt> containing the
     * modified WhiteboardObject, its sender and other details.
     */
    public void whiteboardObjecModified(WhiteboardObjectModifiedEvent evt);

    /**
     * Called when the underlying implementation has received an indication
     * that a WhiteboardObject, sent earlier has been successfully received
     * by the destination.
     *
     * @param evt the WhiteboardObjectDeliveredEvent containing the id of the
     * WhiteboardObject that has caused the event.
     */
    public void whiteboardObjectDelivered(WhiteboardObjectDeliveredEvent evt);

    /**
     * Called to indicated that delivery of a WhiteboardObject sent earlier
     * has failed. Reason code and phrase are contained by the
     * <tt>WhiteboardObjectDeliveryFailedEvent</tt>
     *
     * @param evt the <tt>WhiteboardObjectDeliveryFailedEvent</tt>
     * containing the ID of the WhiteboardObject whose delivery has failed.
     */
    public void whiteboardObjectDeliveryFailed(
                                    WhiteboardObjectDeliveryFailedEvent evt);
}
