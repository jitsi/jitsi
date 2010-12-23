/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the volume of your call.
 * 
 * @author Yana Stamcheva
 */
public abstract class AbstractVolumeControlButton
    extends SIPCommButton
{
    /**
     * The background image.
     */
    protected ImageID bgImage;

    /**
     * The pressed image.
     */
    protected ImageID pressedImage;

    /**
     * The icon image.
     */
    protected ImageID iconImageID;

    /**
     * The multiplier would just convert the float volume value coming from
     * the service to the int value needed for the volume control slider
     * component.
     */
    private static final int MULTIPLIER = 100;

    /**
     * Creates an instance of <tt>VolumeControlButton</tt>.
     * @param inSettingsPanel <tt>true</tt> when the button is used in a menu,
     * to use different background.
     */
    public AbstractVolumeControlButton(
            final boolean fullScreen,
            boolean inSettingsPanel,
            ImageID iconImageID,
            String toolTipTextKey)
    {
        super(ImageLoader.getImage(ImageLoader.VOLUME_BUTTON_PRESSED),
                ImageLoader.getImage(iconImageID));

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
                bgImage = ImageLoader.VOLUME_BUTTON;
                pressedImage = ImageLoader.VOLUME_BUTTON_PRESSED;
            }
        }

        // Loads the skin of this button.
        loadSkin();

        if (toolTipTextKey != null)
        {
            setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        final JSlider volumeSlider
            = new JSlider(JSlider.VERTICAL, 0, 100, 50);

        volumeSlider.setPreferredSize(new Dimension(20, 100));

        final VolumeControl volumeControl = getVolumeControl();

        // Sets the minimum, maximum and default volume values for the volume
        // slider.
        if (volumeControl != null)
        {
            volumeSlider.setMinimum(
                (int) (volumeControl.getMinValue()*MULTIPLIER));
            volumeSlider.setMaximum(
                (int) (volumeControl.getMaxValue()*MULTIPLIER));

            volumeSlider.setValue(
                (int) (volumeControl.getVolume()*MULTIPLIER));
        }

        // Adds a change listener to the slider in order to correctly set
        // the volume through the VolumeControl service, on user change.
        volumeSlider.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider) e.getSource();
                int volume = source.getValue();

                // Set the volume to the volume control.
                volumeControl.setVolume((float) volume/MULTIPLIER);
            }
        });

        // Creates the menu that would contain the volume control component.
        final JPopupMenu sliderMenu = new JPopupMenu();
        sliderMenu.setInvoker(this);
        sliderMenu.add(volumeSlider);

        this.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                Point location = new Point(getX(), getY() + getHeight());

                SwingUtilities.convertPointToScreen(location,
                        AbstractVolumeControlButton.this.getParent());

                if(fullScreen)
                    location.setLocation(location.getX(),
                        location.getY()
                            - sliderMenu.getPreferredSize().getHeight()
                            - getHeight());

                if (volumeControl != null)
                    volumeSlider.setValue(
                        (int) (volumeControl.getVolume()*MULTIPLIER));

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

    /**
     * Volume control used by the button.
     * @return volume control used by the button.
     */
    public abstract VolumeControl getVolumeControl();
}