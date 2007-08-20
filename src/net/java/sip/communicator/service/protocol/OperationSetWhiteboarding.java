/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Provides basic functionality for whiteboard.
 *
 * @author Julien Waechter
 */
public interface OperationSetWhiteboarding
  extends OperationSet
{
    /**
     * Create a new whiteboard session and invite the specified Contact to
     * join it.
     *
     * @param whiteboardContact the address of the contact that we'd like to
     * invite to a our new whiteboard session.
     *
     * @return WhiteboardSession the newly  WhiteboardParticipant
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the whiteboard.
     */
    public WhiteboardSession createWhiteboardSession (Contact whiteboardContact)
        throws OperationFailedException;

    /**
     * Closes the specified <tt>whiteboardSession</tt> and destroys all objects
     * and states associated with it.
     *
     * @param whiteboardSession the session that we'd like to end.
     *
     * @throws OperationFailedException with the corresponding code if we fail
     * to finish the whiteboard.
     */
    public void endWhiteboardSession (WhiteboardSession whiteboardSession)
        throws OperationFailedException;
}
