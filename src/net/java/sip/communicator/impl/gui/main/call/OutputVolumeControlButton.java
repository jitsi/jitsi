/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the playback volume of your call.
 * 
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class OutputVolumeControlButton
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

    /**
     * The icon image.
     */
    private ImageID iconImageID;

    /**
     * Creates not full screen button.
     */
    public OutputVolumeControlButton()
    {
        this(false);
    }

    /**
     * Creates volume control button.
     * @param fullScreen is full screen.
     */
    public OutputVolumeControlButton(boolean fullScreen)
    {
        this(ImageLoader.VOLUME_CONTROL_BUTTON, fullScreen, false);
    }

    /**
     * Creates volume control button.
     * @param iconImageID the image.
     * @param fullScreen is full screen.
     */
    public OutputVolumeControlButton(ImageID iconImageID,
                                     boolean fullScreen,
                                     boolean inSettingsPanel)
    {
        super(  ImageLoader.getImage(ImageLoader.SOUND_SETTING_BUTTON_PRESSED),
                ImageLoader.getImage(iconImageID));

        initVolumeControlButton(fullScreen, inSettingsPanel, iconImageID,
                "service.gui.VOLUME_CONTROL_TOOL_TIP");
    }

    /**
     * 
     * @param fullScreen
     * @param inSettingsPanel
     * @param iconImageID
     * @param toolTipTextKey
     */
    public void initVolumeControlButton(final boolean fullScreen,
                                        boolean inSettingsPanel,
                                        ImageID iconImageID,
                                        String toolTipTextKey)
    {
        this.iconImageID = iconImageID;

        if (fullScreen)
        {
            bgImage = ImageLoader.FULL_SCREEN_BUTTON_BG;
            pressedImage = ImageLoader.FULL_SCREEN_BUTTON_BG_PRESSED;
        }
        else
        {
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
        }

        // Loads the skin of this button.
        loadSkin();

        if (toolTipTextKey != null)
        {
            setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        final VolumeControl volumeControl
            = GuiActivator.getMediaService().getOutputVolumeControl();

        // Creates the menu that would contain the volume control component.
        final VolumeControlSlider sliderMenu
            = new VolumeControlSlider(volumeControl);

        sliderMenu.setInvoker(this);

        this.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                Point location = new Point(getX(), getY() + getHeight());

                SwingUtilities.convertPointToScreen(location,
                        OutputVolumeControlButton.this.getParent());

                if(fullScreen)
                    location.setLocation(location.getX(),
                        location.getY()
                            - sliderMenu.getPreferredSize().getHeight()
                            - getHeight());

                sliderMenu.setLocation(location);

                sliderMenu.setVisible(!sliderMenu.isVisible());
            }
        });
    }

    /**
     * Loads images.
     */
    public void loadSkin()
    {
        setBackgroundImage(ImageLoader.getImage(bgImage));
        setPressedImage(ImageLoader.getImage(pressedImage));
        setIconImage(ImageLoader.getImage(iconImageID));
    }
}
