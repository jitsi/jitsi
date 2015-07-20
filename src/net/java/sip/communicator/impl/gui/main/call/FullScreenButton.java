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
import java.beans.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

/**
 * Represents the button which allows toggling the associated
 * <tt>CallContainer</tt> between full-screen and windowed mode.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class FullScreenButton
    extends SIPCommButton
    implements PropertyChangeListener,
               Skinnable
{
    /**
     * The <tt>CallPanel</tt> which has initialized this instance and which
     * contains it.
     */
    private final CallPanel callContainer;

    /**
     * Initializes a new <tt>FullScreenButton</tt> instance which is to
     * enter the full screen mode.
     *
     * @param callContainer the parent <tt>CallContainer</tt>, where this button
     * is contained
     */
    public FullScreenButton(CallPanel callContainer)
    {
        this.callContainer = callContainer;

        loadSkin();

        addActionListener(
                new ActionListener()
                {
                    /**
                     * {@inheritDoc}
                     *
                     * Toggles the associated <tt>CallContainer</tt> between
                     * full-screen and windowed mode.
                     */
                    public void actionPerformed(ActionEvent evt)
                    {
                        CallPanel callPanel
                            = FullScreenButton.this.callContainer;

                        callPanel.setFullScreen(!callPanel.isFullScreen());
                    }
                });
        this.callContainer.addPropertyChangeListener(
                CallContainer.PROP_FULL_SCREEN,
                this);
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        boolean fullScreen = this.callContainer.isFullScreen();

        setIconImage(
                ImageLoader.getImage(
                        fullScreen
                            ? ImageLoader.EXIT_FULL_SCREEN_BUTTON
                            : ImageLoader.ENTER_FULL_SCREEN_BUTTON));
        setPreferredSize(new Dimension(44, 38));
        setToolTipText(
                GuiActivator.getResources().getI18NString(
                        fullScreen
                            ? "service.gui.EXIT_FULL_SCREEN_TOOL_TIP"
                            : "service.gui.ENTER_FULL_SCREEN_TOOL_TIP"));
    }

    /**
     * Notifies this instance about a change in the value of a property of a
     * source which of interest to this instance. For example,
     * <tt>FullScreenButton</tt> updates its user interface-related properties
     * upon changes in the value of the {@link CallContainer#PROP_FULL_SCREEN}
     * property of its associated {@link #callContainer}.
     *
     * @param ev a <tt>PropertyChangeEvent</tt> which identifies the source, the
     * name of the property and the old and new values
     */
    public void propertyChange(PropertyChangeEvent ev)
    {
        if (CallContainer.PROP_FULL_SCREEN.equals(ev.getPropertyName())
                && this.callContainer.equals(ev.getSource()))
        {
            loadSkin();
        }
    }
}
