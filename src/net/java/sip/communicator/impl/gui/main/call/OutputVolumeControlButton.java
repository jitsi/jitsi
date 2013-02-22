/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.neomedia.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the playback volume of your call.
 * 
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class OutputVolumeControlButton
{
    /**
     * The background image.
     */
    private ImageID bgImage;

    /**
     * The pressed image.
     */
    private ImageID pressedImage;

    /**
     * The icon image.
     */
    private ImageID iconImageID;

    /**
     * Indicates if we're in full screen mode.
     */
    private final boolean fullScreen;

    /**
     * 
     */
    private final boolean inButtonToolBar;

    /**
     * Creates not full screen button.
     */
    public OutputVolumeControlButton()
    {
        this(false);
    }

    /**
     * Creates volume control button.
     *
     * @param fullScreen is full screen.
     */
    public OutputVolumeControlButton(boolean fullScreen)
    {
        this(ImageLoader.VOLUME_CONTROL_BUTTON, fullScreen, true);
    }

    /**
     * Creates volume control button.
     *
     * @param iconImageID the image.
     * @param fullScreen is full screen.
     * @param inButtonToolBar indicates if this button is shown in the button
     * tool bar
     */
    public OutputVolumeControlButton(ImageID iconImageID,
                                     boolean fullScreen,
                                     boolean inButtonToolBar)
    {
        this.fullScreen = fullScreen;
        this.inButtonToolBar = inButtonToolBar;

        this.iconImageID = iconImageID;
    }

    /**
     * Returns the component associated with this output volume control button.
     *
     * @return the component associated with this output volume control button
     */
    public Component getComponent()
    {
        if (!fullScreen)
            return createVolumeControlButton(
                                        inButtonToolBar,
                                        iconImageID,
                                        "service.gui.VOLUME_CONTROL_TOOL_TIP");
        else
            return createSliderComponent();
    }

    /**
     * Creates the slider component for the full screen interface.
     *
     * @return the created component
     */
    public Component createSliderComponent()
    {
        final Color bgColor
            = new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_TOOL_BAR_SOUND_BG"));

        @SuppressWarnings("serial")
        TransparentPanel soundPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.LEFT, 0, 0))
        {
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);

                g = g.create();

                AntialiasingManager.activateAntialiasing(g);

                try
                {
                    g.setColor(bgColor);

                    g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                }
                finally
                {
                    g.dispose();
                }
            }
        };

        soundPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        final VolumeControl volumeControl
            = GuiActivator.getMediaService().getOutputVolumeControl();

        // Creates the menu that would contain the volume control component.
        VolumeControlSlider slider
            = new VolumeControlSlider(volumeControl, JSlider.HORIZONTAL);

        soundPanel.add(new JLabel(GuiActivator.getResources()
            .getImage("service.gui.icons.NO_SOUND_ICON")));
        soundPanel.add(slider);
        soundPanel.add(new JLabel(GuiActivator.getResources()
            .getImage("service.gui.icons.SOUND_MENU_ICON")));

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

        final VolumeControl volumeControl
            = GuiActivator.getMediaService().getOutputVolumeControl();

        // Creates the menu that would contain the volume control component.
        final JPopupMenu sliderMenu
            = new VolumeControlSlider(volumeControl, JSlider.VERTICAL)
                .getPopupMenu();

        sliderMenu.setInvoker(volumeControlButton);

        volumeControlButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                Point location = new Point(
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
     * The <tt>VolumeControlButton</tt>
     */
    @SuppressWarnings("serial")
    private class VolumeControlButton
        extends SIPCommButton
    {
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
