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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The button responsible to start(the <tt>Call</tt> of) an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Yana Stamcheva
 */
public class DesktopSharingButton
    extends AbstractCallToggleButton
{
    /**
     * Whether desktop sharing is enabled. If false, calls to
     * <tt>setEnabled(true)</tt> will NOT enable the button.
     */
    private boolean desktopSharingAvailable;

    /**
     * Initializes a new <tt>DesktopSharingButton</tt> instance which is meant
     * to be used to initiate a desktop sharing during a call.
     *
     * @param call the <tt>Call</tt> to be associated with the desktop sharing
     * button instance
     */
    public DesktopSharingButton(Call call)
    {
        this(call, false);
    }

    /**
     * Initializes a new <tt>HoldButton</tt> instance which is to put a specific
     * <tt>CallPeer</tt> on/off hold.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public DesktopSharingButton(Call call, boolean selected)
    {
        super(  call,
                selected,
                ImageLoader.CALL_DESKTOP_BUTTON,
                "service.gui.SHARE_DESKTOP_WITH_CONTACT");

        OperationSetDesktopStreaming desktopSharing
            = call.getProtocolProvider().getOperationSet(
                OperationSetDesktopStreaming.class);

        if (desktopSharing == null)
        {
            setToolTipText(GuiActivator.getResources().getI18NString(
                    "service.gui.NO_DESKTOP_SHARING_FOR_PROTOCOL"));
            desktopSharingAvailable = false;
        }
        else if(!ConfigurationUtils.hasEnabledVideoFormat(
                call.getProtocolProvider()))
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_VIDEO_ENCODINGS"));
            desktopSharingAvailable = false;
        }
        else
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.SHARE_DESKTOP_WITH_CONTACT"));
            desktopSharingAvailable = true;
        }
        super.setEnabled(desktopSharingAvailable);
    }

    /**
     * Shares the desktop with the peers in the current call.
     */
    @Override
    public void buttonPressed()
    {
        if (call != null)
        {
            // If it's already enabled, we disable it.
            if (CallManager.isDesktopSharingEnabled(call))
            {
                CallManager.enableDesktopSharing(call, false);
            }
            // Otherwise we enable the desktop sharing.
            else
            {
                //We'll select the button once the desktop sharing  has been
                // established.
                setSelected(false);

                JPopupMenu sharingMenu = createDesktopSharingMenu();

                Point location = new Point(getX(), getY() + getHeight());

                SwingUtilities.convertPointToScreen(location, getParent());

                sharingMenu.setLocation(location);
                sharingMenu.setVisible(true);
            }
        }
    }

    /**
     * Creates the menu responsible for desktop sharing when a single desktop
     * sharing contact is available.
     *
     * @return the created popup menu
     */
    private JPopupMenu createDesktopSharingMenu()
    {
        final JPopupMenu popupMenu = new JPopupMenu(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHARE_DESKTOP"));

        popupMenu.setInvoker(this);
        popupMenu.setFocusable(true);

        JMenuItem shareFullScreen = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SHARE_FULL_SCREEN"),
            new ImageIcon(
                ImageLoader.getImage(ImageLoader.DESKTOP_SHARING)));

        JMenuItem shareRegion = new JMenuItem(GuiActivator.getResources()
            .getI18NString("service.gui.SHARE_REGION"),
            new ImageIcon(
                ImageLoader.getImage(ImageLoader.REGION_DESKTOP_SHARING)));

        popupMenu.add(shareFullScreen);
        popupMenu.add(shareRegion);

        shareFullScreen.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);
                CallManager.enableDesktopSharing(call, true);
            }
        });

        shareRegion.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                CallManager.enableRegionDesktopSharing(call, true);
            }
        });

        return popupMenu;
    }

    /**
     * Enables/disables the button. If <tt>this.desktopSharingAvailable</tt> is
     * false, keeps the button as it is (i.e. disabled).
     *
     * @param enable <tt>true</tt> to enable the button, <tt>false</tt> to
     * disable it.
     */
    @Override
    public void setEnabled(final boolean enable)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    setEnabled(enable);
                }
            });
            return;
        }

        if(desktopSharingAvailable)
            super.setEnabled(enable);
    }
}
