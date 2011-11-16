/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

/**
 * A listener notified each time the local video component is shown or hidden.
 *
 * @author Yana Stamcheva
 */
public interface LocalVideoComponentListener
{
    /**
     * Indicates that the local video component has been shown to the user.
     */
    public void localVideoShown();

    /**
     * Indicates that the local video component has been hidden.
     */
    public void localVideoHidden();
}
