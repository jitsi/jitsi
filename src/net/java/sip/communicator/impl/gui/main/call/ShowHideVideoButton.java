/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The local video button is the button used to start/stop video in a
 * conversation.
 *
 * @author Yana Stamcheva
 */
public class ShowHideVideoButton
    extends AbstractCallToggleButton
{
    private static final long serialVersionUID = 0L;

    /**
     * The call peer renderer.
     */
    private CallPeerRenderer renderer;

    /**
     * Creates a <tt>LocalVideoButton</tt> by specifying the corresponding
     * <tt>call</tt>.
     *
     * @param call the parent call
     */
    public ShowHideVideoButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Creates a <tt>LocalVideoButton</tt> by specifying the corresponding
     * <tt>call</tt>.
     *
     * @param call the parent call
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public ShowHideVideoButton( Call call,
                                boolean fullScreen,
                                boolean selected)
    {
        super(  call,
            fullScreen,
            true,
            selected,
            ImageLoader.SHOW_LOCAL_VIDEO_BUTTON,
            ImageLoader.SHOW_LOCAL_VIDEO_BUTTON_PRESSED,
            "service.gui.SHOW_LOCAL_VIDEO_BUTTON_TOOL_TIP");
    }

    /**
     * Sets the peer renderer.
     * 
     * @param renderer the peer renderer
     */
    public void setPeerRenderer(CallPeerRenderer renderer)
    {
        this.renderer = renderer;
    }

    /**
     * Enables or disables local video when the button is toggled/untoggled.
     */
    public void buttonPressed()
    {
        renderer.setLocalVideoVisible(!renderer.isLocalVideoVisible());
    }
}
