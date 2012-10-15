/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * Implements an AWT/Swing button which toggles the display of the visual
 * <tt>Component</tt> which depicting the video streaming from the local
 * peer/user to the remote peer(s).
 * <p>
 * Though <tt>ShowHideVideoButton</tt> extends
 * <tt>AbstractCallToggleButton</tt>, it is not associated with any
 * <tt>Call</tt> and its model is a <tt>UIVideoHandler2</tt>.
 * </p>
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class ShowHideVideoButton
    extends AbstractCallToggleButton
{
    private static final long serialVersionUID = 0L;

    /**
     * The facility which aids this instance with the dealing with the
     * video-related information and which is the model of the view represented
     * by this instance.
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * Initializes a new <tt>ShowHideVideoButton</tt> instance which is to
     * toggle the display of the visual <tt>Component</tt> which depicts the
     * video streaming from the local peer/user to the remote peer(s).
     *
     * @param uiVideoHandler the <tt>UIVideoHandler</tt> which is to be the
     * model of the view represented by the new instance
     */
    public ShowHideVideoButton(UIVideoHandler2 uiVideoHandler)
    {
        this(uiVideoHandler, false, false);
    }

    /**
     * Initializes a new <tt>ShowHideVideoButton</tt> instance which is to
     * toggle the display of the visual <tt>Component</tt> which depicts the
     * video streaming from the local peer/user to the remote peer(s).
     *
     * @param uiVideoHandler the <tt>UIVideoHandler</tt> which is to be the
     * model of the view represented by the new instance
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public ShowHideVideoButton(
            UIVideoHandler2 uiVideoHandler,
            boolean fullScreen,
            boolean selected)
    {
        super(
                null,
                fullScreen,
                true,
                selected,
                ImageLoader.SHOW_LOCAL_VIDEO_BUTTON,
                ImageLoader.SHOW_LOCAL_VIDEO_BUTTON_PRESSED,
                "service.gui.SHOW_LOCAL_VIDEO_BUTTON_TOOL_TIP");

        this.uiVideoHandler = uiVideoHandler;
    }

    /**
     * Toggles the display of the visual <tt>Component</tt> which depicts the
     * video streaming from the local peer/user to the remote peer(s).
     */
    public void buttonPressed()
    {
        uiVideoHandler.setLocalVideoVisible(
                !uiVideoHandler.isLocalVideoVisible());
    }
}
