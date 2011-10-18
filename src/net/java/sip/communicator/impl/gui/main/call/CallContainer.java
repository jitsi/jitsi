/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import javax.swing.*;

/**
 * The <tt>CallContainer</tt> interface is an abstraction of a window,
 * containing one or many <tt>CallPanel</tt>s.
 *
 * @author Yana Stamcheva
 */
public interface CallContainer
{
    /**
     * Closes the given <tt>CallPanel</tt>.
     *
     * @param callPanel the <tt>CallPanel</tt> that should be closed
     */
    public void closeWait(CallPanel callPanel);

    /**
     * Closes the given <tt>CallPanel</tt>.
     *
     * @param callPanel the <tt>CallPanel</tt> that should be closed
     */
    public void close(CallPanel callPanel);

    /**
     * Packs the content of this call window.
     */
    public void pack();

    /**
     * Adds the given <tt>CallPanel</tt> to this call window.
     *
     * @param callPanel the <tt>CallPanel</tt> to add
     */
    public void addCallPanel(CallPanel callPanel);

    /**
     * Returns the frame of this call window.
     *
     * @return the frame of this call window
     */
    public JFrame getFrame();
}
