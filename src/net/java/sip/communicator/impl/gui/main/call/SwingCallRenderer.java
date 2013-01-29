/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.service.gui.call.*;

/**
 * The <tt>CallRenderer</tt> represents a renderer for a call. All user
 * interfaces representing a call should implement this interface.
 *
 * @author Yana Stamcheva
 */
public interface SwingCallRenderer
    extends CallRenderer
{
    /**
     * Returns the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     *
     * @return the parent, container which created this <tt>CallRenderer</tt>
     * and in which this <tt>CallRenderer</tt> is added
     */
    public CallPanel getCallContainer();
}
