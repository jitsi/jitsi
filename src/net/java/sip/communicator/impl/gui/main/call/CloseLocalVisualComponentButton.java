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

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * Implements a <tt>Button</tt> which allows closing/hiding the visual
 * <tt>Component</tt> displaying the video streaming from the local peer/user to
 * the remote peers.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class CloseLocalVisualComponentButton
    extends Button
    implements MouseListener
{
    private static final long serialVersionUID = 0L;

    private final Image image;

    /**
     * The facility which aids this instance in the dealing with the
     * video-related information. Since this <tt>Button</tt> is displayed on top
     * of the visual <tt>Component</tt> depicting the video streaming from the
     * local peer/user to the remote peer(s), it only invokes
     * {@link UIVideoHandler2#setLocalVideoVisible(boolean)} with <tt>false</tt>
     * in order to close/hide the <tt>Component</tt> in question.
     */
    private final UIVideoHandler2 uiVideoHandler;

    /**
     * Initializes a new <tt>CloseLocalVisualComponentButton</tt> instance which
     * is to allow closing/hiding the visual <tt>Component</tt> displaying the
     * video streaming from the local peer/user to the remote peer(s).
     *
     * @param uiVideoHandler the facility which is to aid the new instance in
     * the dealing with the video-related information and which represents the
     * indicator which determines whether the visual <tt>Component</tt>
     * displaying the video streaming from the local peer/user to the remote
     * peer(s) is to be visible
     */
    public CloseLocalVisualComponentButton(UIVideoHandler2 uiVideoHandler)
    {
        if (uiVideoHandler == null)
            throw new NullPointerException("uiVideoHandler");

        this.uiVideoHandler = uiVideoHandler;

        image = ImageLoader.getImage(ImageLoader.CLOSE_VIDEO);

        int buttonWidth = image.getWidth(this) + 5;
        int buttonHeight = image.getHeight(this) + 5;

        setPreferredSize(new Dimension(buttonWidth, buttonHeight));
        setSize(new Dimension(buttonWidth, buttonHeight));

        addMouseListener(this);
    }

    @Override
    public void paint(Graphics g)
    {
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.drawImage(image,
            getWidth()/2 - image.getWidth(this)/2,
            getHeight()/2 - image.getHeight(this)/2, this);
    }

    public void mouseClicked(MouseEvent event)
    {
        uiVideoHandler.setLocalVideoVisible(false);
    }

    public void mouseEntered(MouseEvent event) {}

    public void mouseExited(MouseEvent event) {}

    public void mousePressed(MouseEvent event) {}

    public void mouseReleased(MouseEvent event) {}
}
