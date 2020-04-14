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
package net.java.sip.communicator.service.gui;

import java.awt.*;

public abstract class AbstractExportedWindow<T extends Window>
    implements ExportedWindow
{

    /**
     * The <code>Window</code> wrapped by this instance and to which
     * <code>ExportedWindow</code> capabilities are provided.
     */
    protected T window;

    /*
     * Implements ExportedWindow#bringToFront().
     */
    public void bringToFront()
    {
        Window window = getWindow();

        if (window instanceof Frame)
        {
            Frame frame = (Frame) window;

            if ((frame.getExtendedState() & Frame.ICONIFIED) == Frame.ICONIFIED)
                frame.setExtendedState(Frame.NORMAL);
        }

        window.toFront();
    }

    /**
     * Creates the <code>Window</code> instance to be wrapped by this instance
     * and to which <code>ExportedWindow</code> capabilities are provided.
     *
     * @return the <code>Window</code> instance to be wrapped by this instance
     *         and to which <code>ExportedWindow</code> capabilities are
     *         provided
     */
    protected abstract T createWindow();

    /*
     * Implements ExportedWindow#getSource().
     */
    public Object getSource()
    {
        return getWindow();
    }

    /**
     * Gets the <code>Window</code> wrapped by this instance and to which
     * <code>ExportedWindow</code> capabilities are provided.
     *
     * @return the <code>Window</code> wrapped by this instance and to which
     *         <code>ExportedWindow</code> capabilities are provided
     */
    protected T getWindow()
    {
        if (window == null)
            window = createWindow();
        return window;
    }

    /*
     * Implements ExportedWindow#isFocused().
     */
    public boolean isFocused()
    {
        return (window == null) ? false : window.isFocused();
    }

    /*
     * Implements ExportedWindow#isVisible().
     */
    public boolean isVisible()
    {
        boolean visible;

        if (window == null)
            visible = false;
        else
        {
            visible = window.isVisible();

            if (visible && (window instanceof Frame))
            {
                Frame frame = (Frame) window;

                visible
                    = (frame.getExtendedState() & Frame.ICONIFIED)
                        != Frame.ICONIFIED;
            }
        }
        return visible;
    }

    /**
     * Implements {@link ExportedWindow#maximize()}. Maximizes the wrapped
     * <code>Window</code> instance if it is a <code>Frame</code>.
     */
    public void maximize()
    {
        Window window = getWindow();

        if (window instanceof Frame)
            ((Frame) window).setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    /**
     * Implements {@link ExportedWindow#minimize()}. Minimizes the wrapped
     * <code>Window</code> instance if it is a <code>Frame</code>.
     */
    public void minimize()
    {
        Window window = getWindow();

        if (window instanceof Frame)
            ((Frame) window).setExtendedState(Frame.ICONIFIED);
    }

    /*
     * Implements ExportedWindow#setLocation(int, int).
     */
    public void setLocation(int x, int y)
    {
        getWindow().setLocation(x, y);
    }

    /**
     * Implements {@link ExportedWindow#setParams(Object[])}. Does nothing.
     *
     * @param windowParams
     *            the parameters to set to the <code>Window</code> wrapped in
     *            this instance
     */
    public void setParams(Object[] windowParams)
    {
    }

    /*
     * Implements ExportedWindow#setSize(int, int).
     */
    public void setSize(int width, int height)
    {
        getWindow().setSize(width, height);
    }

    /*
     * Implements ExportedWindow#setVisible(boolean).
     */
    public void setVisible(boolean visible)
    {
        getWindow().setVisible(visible);
    }
}
