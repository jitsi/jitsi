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
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the playback volume of your call.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class OutputVolumeControlButton
{
    /**
     * The <tt>CallConference</tt> (i.e. telephony conference-related state)
     * depicted by this instance.
     */
    private final CallConference callConference;

    /**
     * The indicator which determines whether the user interface
     * (representation) of this instance is (to be) displayed in full-screen
     * mode.
     */
    private final boolean fullScreen;

    /**
     * The icon image.
     */
    private ImageID iconImageID;

    private final boolean inButtonToolBar;

    /**
     * Creates volume control button.
     *
     * @param callConference
     * @param iconImageID the image.
     * @param fullScreen is full screen.
     * @param inButtonToolBar indicates if this button is shown in the button
     * tool bar
     */
    public OutputVolumeControlButton(
            CallConference callConference,
            ImageID iconImageID,
            boolean fullScreen,
            boolean inButtonToolBar)
    {
        this.callConference = callConference;
        this.iconImageID = iconImageID;
        this.fullScreen = fullScreen;
        this.inButtonToolBar = inButtonToolBar;
    }

    /**
     * Creates the slider component for the full screen interface.
     *
     * @return the created component
     */
    public Component createSliderComponent()
    {
        ResourceManagementService r = GuiActivator.getResources();
        final Color bgColor
            = new Color(r.getColor("service.gui.CALL_TOOL_BAR_SOUND_BG"));

        @SuppressWarnings("serial")
        TransparentPanel soundPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0))
            {
                @Override
                public void paintComponent(Graphics g)
                {
                    super.paintComponent(g);

                    g = g.create();
                    try
                    {
                        AntialiasingManager.activateAntialiasing(g);

                        g.setColor(bgColor);
                        g.fillRoundRect(
                                0, 0, getWidth() - 1, getHeight() - 1,
                                8, 8);
                    }
                    finally
                    {
                        g.dispose();
                    }
                }
            };

        soundPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        VolumeControl volumeControl = getOutputVolumeControl();
        // Creates the menu that would contain the volume control component.
        VolumeControlSlider slider
            = new VolumeControlSlider(volumeControl, JSlider.HORIZONTAL);

        soundPanel.add(
                new JLabel(r.getImage("service.gui.icons.NO_SOUND_ICON")));
        soundPanel.add(slider);
        soundPanel.add(
                new JLabel(r.getImage("service.gui.icons.SOUND_MENU_ICON")));

        return soundPanel;
    }

    /**
     * Initializes the volume control button.
     *
     * @param isButtonBar indicates if this button is shown in the button
     * tool bar
     * @param iconImageID the identifier of the button icon
     * @param toolTipTextKey the key of the tool tip text
     */
    public Component createVolumeControlButton( boolean isButtonBar,
                                                ImageID iconImageID,
                                                String toolTipTextKey)
    {
        this.iconImageID = iconImageID;

        final SIPCommButton volumeControlButton
            = new VolumeControlButton(isButtonBar);

        if (toolTipTextKey != null)
        {
            volumeControlButton.setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        VolumeControl volumeControl = getOutputVolumeControl();
        // Creates the menu that would contain the volume control component.
        final JPopupMenu sliderMenu
            = new VolumeControlSlider(volumeControl, JSlider.VERTICAL)
                .getPopupMenu();

        sliderMenu.setInvoker(volumeControlButton);

        volumeControlButton.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(ActionEvent ev)
                    {
                        Point location
                            = new Point(
                                    volumeControlButton.getX(),
                                    volumeControlButton.getY()
                                        + volumeControlButton.getHeight());

                        SwingUtilities.convertPointToScreen(
                                location,
                                volumeControlButton.getParent());

                        sliderMenu.setLocation(location);
                        sliderMenu.setVisible(!sliderMenu.isVisible());
                    }
                });

        return volumeControlButton;
    }

    /**
     * Returns the component associated with this output volume control button.
     *
     * @return the component associated with this output volume control button
     */
    public Component getComponent()
    {
        if (fullScreen)
            return createSliderComponent();
        else
        {
            return
                createVolumeControlButton(
                        inButtonToolBar,
                        iconImageID,
                        "service.gui.VOLUME_CONTROL_TOOL_TIP");
        }
    }

    /**
     * Gets the <tt>VolumeControl</tt> instance (to be) depicted by this
     * instance.
     *
     * @return the <tt>VolumeControl</tt> instance (to be) depicted by this
     * instance
     */
    private VolumeControl getOutputVolumeControl()
    {
        VolumeControl volumeControl = null;

        if (callConference instanceof MediaAwareCallConference)
        {
            volumeControl
                = ((MediaAwareCallConference) callConference)
                    .getOutputVolumeControl();
        }
        if (volumeControl == null)
        {
            volumeControl
                = GuiActivator.getMediaService().getOutputVolumeControl();
        }
        return volumeControl;
    }

    /**
     * The <tt>VolumeControlButton</tt>
     */
    @SuppressWarnings("serial")
    private class VolumeControlButton
        extends SIPCommButton
    {
        /**
         * The background image.
         */
        private ImageID bgImage;

        /**
         * The pressed image.
         */
        private ImageID pressedImage;

        public VolumeControlButton(boolean inSettingsPanel)
        {
            super(
                ImageLoader.getImage(ImageLoader.SOUND_SETTING_BUTTON_PRESSED),
                ImageLoader.getImage(iconImageID));

            if(inSettingsPanel)
            {
                bgImage = ImageLoader.CALL_SETTING_BUTTON_BG;
                pressedImage = ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG;
            }
            else
            {
                bgImage = ImageLoader.SOUND_SETTING_BUTTON_BG;
                pressedImage = ImageLoader.SOUND_SETTING_BUTTON_PRESSED;
            }

            // Loads the skin of this button.
            loadSkin();
        }

        /**
         * Loads images.
         */
        public void loadSkin()
        {
            setBackgroundImage(ImageLoader.getImage(bgImage));
            setPressedImage(ImageLoader.getImage(pressedImage));

            if (iconImageID != null)
            {
                if (!fullScreen && !inButtonToolBar)
                    setIconImage(ImageUtils.scaleImageWithinBounds(
                        ImageLoader.getImage(iconImageID), 18, 18));
                else
                    setIconImage(ImageLoader.getImage(iconImageID));
            }
        }
    }
}
