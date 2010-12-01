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
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>VolumeControlButton</tt> is the button shown in the call window,
 * which allows to adjust the volume of your call.
 * 
 * @author Yana Stamcheva
 */
public class VolumeControlButton
    extends SIPCommButton
    implements Skinnable
{
    /**
     * The multiplier would just convert the float volume value coming from
     * the service to the int value needed for the volume control slider
     * component.
     */
    private static final int MULTIPLIER = 100;

    /**
     * Creates an instance of <tt>VolumeControlButton</tt>.
     */
    public VolumeControlButton()
    {
        // Loads the skin of this button.
        loadSkin();

        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.VOLUME_CONTROL_TOOL_TIP"));

        final JSlider volumeSlider
            = new JSlider(JSlider.VERTICAL, 0, 100, 50);

        volumeSlider.setPreferredSize(new Dimension(20, 100));

        final VolumeControl volumeControl
            = GuiActivator.getMediaService().getVolumeControl();

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
                        VolumeControlButton.this.getParent());

                sliderMenu.setLocation(location);

                sliderMenu.setVisible(!sliderMenu.isVisible());
            }
        });
    }

    /**
     * Loads volume control button skin.
     */
    public void loadSkin()
    {
        this.setBackgroundImage(ImageLoader.getImage(
            ImageLoader.CALL_SETTING_BUTTON_BG));

        this.setIconImage(ImageLoader.getImage(
            ImageLoader.VOLUME_CONTROL_BUTTON));
    }
}