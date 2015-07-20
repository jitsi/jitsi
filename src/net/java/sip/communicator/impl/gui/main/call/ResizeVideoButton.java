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

import org.jitsi.service.neomedia.*;

/**
 * The button responsible to resize video for the associated participant.
 *
 * @author Yana Stamcheva
 */
public class ResizeVideoButton
    extends AbstractCallToggleButton
{
    /**
     * Is low option present in menu when button is pressed.
     */
    private boolean loOptionPresent = false;

    /**
     * Is SD option present in menu when button is pressed.
     */
    private boolean sdOptionPresent = false;

    /**
     * Is HD option present in menu when button is pressed.
     */
    private boolean hdOptionPresent = false;

    /**
     * Initializes a new <tt>DesktopSharingButton</tt> instance which is meant
     * to be used to initiate a desktop sharing during a call.
     *
     * @param call the <tt>Call</tt> to be associated with the desktop sharing
     * button instance
     */
    public ResizeVideoButton(Call call)
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
    public ResizeVideoButton(Call call, boolean selected)
    {
        super(
                call,
                selected,
                ImageLoader.SD_VIDEO_BUTTON,
                "service.gui.CHANGE_VIDEO_QUALITY");

        // catch everything, make sure we don't interrupt gui creation
        // if anything strange happens
        try
        {
            CallPeer peer = call.getCallPeers().next();
            OperationSetVideoTelephony videoOpSet = peer.getProtocolProvider()
                    .getOperationSet(OperationSetVideoTelephony.class);

            QualityControl qualityControls = videoOpSet.getQualityControl(peer);
            QualityPreset maxRemotePreset =
                    qualityControls.getRemoteSendMaxPreset();

            // if there is a setting for max preset lets look at it
            // and add only those presets that are remotely supported
            if( maxRemotePreset == null ||
                maxRemotePreset.compareTo(QualityPreset.HD_QUALITY) >= 0)
                hdOptionPresent = true;

            if( maxRemotePreset == null ||
                maxRemotePreset.compareTo(QualityPreset.SD_QUALITY) >= 0)
                sdOptionPresent = true;

            if( maxRemotePreset == null ||
                maxRemotePreset.compareTo(QualityPreset.LO_QUALITY) >= 0)
                loOptionPresent = true;
        }
        catch(Throwable t)
        {
            // do nothing
        }
    }

    /**
     * Shares the desktop with the peers in the current call.
     */
    @Override
    public void buttonPressed()
    {
        if (call != null)
        {
            //We'll select the button once the desktop sharing  has been
            // established.
            setSelected(false);

            JPopupMenu resizeMenu = createResizeVideoMenu();

            Point location = new Point(getX(), getY() + getHeight());

            SwingUtilities.convertPointToScreen(location, getParent());

            resizeMenu.setLocation(location);
            resizeMenu.setVisible(true);
        }
    }

    /**
     * Creates the menu responsible for desktop sharing when a single desktop
     * sharing contact is available.
     *
     * @return the created popup menu
     */
    private JPopupMenu createResizeVideoMenu()
    {
        final JPopupMenu popupMenu = new JPopupMenu(
            GuiActivator.getResources().getI18NString(
                "service.gui.CHANGE_VIDEO_QUALITY"));

        popupMenu.setInvoker(this);
        popupMenu.setFocusable(true);

        Dimension loDimension = QualityPreset.LO_QUALITY.getResolution();
        Dimension sdDimension = QualityPreset.SD_QUALITY.getResolution();
        Dimension hdDimension = QualityPreset.HD_QUALITY.getResolution();

        JMenuItem lowQuality = new JMenuItem(
            GuiActivator.getResources()
                .getI18NString("service.gui.LOW_QUALITY")
                + getFormattedDimension(loDimension),
            GuiActivator.getResources()
                .getImage("service.gui.icons.LO_VIDEO_ICON"));

        JMenuItem normalQuality = new JMenuItem(GuiActivator.getResources()
                .getI18NString("service.gui.SD_QUALITY")
                + getFormattedDimension(sdDimension),
            GuiActivator.getResources()
                .getImage("service.gui.icons.SD_VIDEO_ICON"));

        JMenuItem hdQuality = new JMenuItem(GuiActivator.getResources()
                .getI18NString("service.gui.HD_QUALITY")
                + getFormattedDimension(hdDimension),
            GuiActivator.getResources()
                .getImage("service.gui.icons.HD_VIDEO_ICON"));

        JLabel titleLabel = new JLabel(
            GuiActivator.getResources().getI18NString(
                "service.gui.CHANGE_VIDEO_QUALITY"));

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        popupMenu.add(titleLabel);
        popupMenu.addSeparator();

        if(hdOptionPresent)
            popupMenu.add(hdQuality);

        if(sdOptionPresent)
            popupMenu.add(normalQuality);

        if(loOptionPresent)
            popupMenu.add(lowQuality);

        lowQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.LO_VIDEO_BUTTON);

                CallManager.setVideoQualityPreset(
                        call.getCallPeers().next(),
                        QualityPreset.LO_QUALITY);
            }
        });

        normalQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.SD_VIDEO_BUTTON);

                CallManager.setVideoQualityPreset(
                        call.getCallPeers().next(), QualityPreset.SD_QUALITY);
            }
        });

        hdQuality.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                popupMenu.setVisible(false);

                setIconImageID(ImageLoader.HD_VIDEO_BUTTON);

                CallManager.setVideoQualityPreset(
                        call.getCallPeers().next(), QualityPreset.HD_QUALITY);
            }
        });

        return popupMenu;
    }

    /**
     * Returns a formatted string representing the given dimension.
     *
     * @param d the dimension to represent in the string
     * @return the formatted dimension string
     */
    private String getFormattedDimension(Dimension d)
    {
        return " (" + (int) d.getWidth() + "x" + (int) d.getHeight() + ")";
    }

    /**
     * Check the available options that will be shown
     * when button is pressed.
     * @return the number of options.
     */
    public int countAvailableOptions()
    {
        int count = 0;
        if(loOptionPresent) count++;
        if(sdOptionPresent) count++;
        if(hdOptionPresent) count++;

        return count;
    }
}
