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

import javax.swing.*;

/**
 * The <tt>CallContainer</tt> interface is an abstraction of a window,
 * containing one or many <tt>CallPanel</tt>s.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public interface CallContainer
{
    /**
     * The name of the boolean property of the <tt>Window</tt> which represents
     * a <tt>CallContainer</tt> that indicates whether the <tt>Window</tt> is
     * displayed in full-screen or windowed mode.
     */
    static final String PROP_FULL_SCREEN = "fullScreen";

    /**
     * Adds the given <tt>CallPanel</tt> to this call window.
     *
     * @param callPanel the <tt>CallPanel</tt> to add
     */
    void addCallPanel(CallPanel callPanel);

    /**
     * Closes a specific <tt>CallPanel</tt>. Optionally, does it with an
     * implementation-specific delay
     *
     * @param callPanel the <tt>CallPanel</tt> to be closed
     * @param delay <tt>true</tt> to close the specified <tt>callPanel</tt> with
     * an implementation-specific delay or <tt>false</tt> to close it as soon as
     * possible
     */
    void close(CallPanel callPanel, boolean delay);

    /**
     * Attempts to give a specific <tt>Component</tt> a visible rectangle with a
     * specific width  and a specific height if possible and sane by resizing
     * the <tt>Window</tt> of this <tt>CallContainer</tt>.
     *
     * @param component the <tt>Component</tt> which requests a visible
     * rectangle with the specified <tt>width</tt> and <tt>height</tt>
     * @param width the width of the visible rectangle requested by the
     * specified <tt>component</tt>
     * @param height the height of the visible rectangle requested by the
     * specified <tt>component</tt>
     */
    void ensureSize(Component component, int width, int height);

    /**
     * Returns the frame of this call window.
     *
     * @return the frame of this call window
     */
    JFrame getFrame();

    /**
     * Determines whether the <tt>Window</tt> representation of this
     * <tt>CallContainer</tt> is displayed in full-screen mode.
     *
     * @return <tt>true</tt> if the <tt>Window</tt> representation of this
     * <tt>CallContainer</tt> is displayed in full-screen mode; otherwise,
     * <tt>false</tt>
     */
    boolean isFullScreen();

    /**
     * Packs the content of this call window.
     */
    void pack();

    /**
     * Sets the display of the <tt>Window</tt> representation of this
     * <tt>CallContainer</tt> to full-screen or windowed mode.
     *
     * @param fullScreen <tt>true</tt> if the <tt>Window</tt> representation of
     * this <tt>CallContainer</tt> is to be displayed in full-screen mode or
     * <tt>false</tt> for windowed mode
     */
    void setFullScreen(boolean fullScreen);
}
