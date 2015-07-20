/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        this(uiVideoHandler, false);
    }

    /**
     * Initializes a new <tt>ShowHideVideoButton</tt> instance which is to
     * toggle the display of the visual <tt>Component</tt> which depicts the
     * video streaming from the local peer/user to the remote peer(s).
     *
     * @param uiVideoHandler the <tt>UIVideoHandler</tt> which is to be the
     * model of the view represented by the new instance
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public ShowHideVideoButton(UIVideoHandler2 uiVideoHandler, boolean selected)
    {
        super(
                null,
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
    @Override
    public void buttonPressed()
    {
        uiVideoHandler.setLocalVideoVisible(
                !uiVideoHandler.isLocalVideoVisible());
    }
}
