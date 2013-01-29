/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import net.java.sip.communicator.service.gui.call.*;

/**
 * The <tt>CallPeerRenderer</tt> interface is meant to be implemented by
 * different renderers of <tt>CallPeer</tt>s. Through this interface they would
 * could be updated in order to reflect the current state of the CallPeer.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface SwingCallPeerRenderer
    extends CallPeerRenderer
{
    /**
     * Returns the parent <tt>CallPanel</tt> containing this renderer.
     *
     * @return the parent <tt>CallPanel</tt> containing this renderer
     */
    public CallPanel getCallPanel();

    /**
     * Returns the AWT <tt>Component</tt> which is the user interface equivalent
     * of this <tt>CallPeerRenderer</tt>.
     *
     * @return the AWT <tt>Component</tt> which is the user interface equivalent
     * of this <tt>CallPeerRenderer</tt>
     */
    public Component getComponent();
}
