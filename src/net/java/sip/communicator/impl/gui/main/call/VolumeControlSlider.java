/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The volume control slider component.
 *
 * @author Yana Stamcheva
 */
public class VolumeControlSlider
    extends SIPCommPopupMenu
    implements VolumeChangeListener
{
    private final JSlider volumeSlider;

    /**
     * The multiplier would just convert the float volume value coming from
     * the service to the int value needed for the volume control slider
     * component.
     */
    private static final int MULTIPLIER = 100;

    /**
     * Creates a <tt>VolumeControlSlider</tt> for the given volumeControl
     * object.
     *
     * @param volumeControl the <tt>VolumeControl</tt> that do the actual volume
     * adjusting.
     */
    public VolumeControlSlider(final VolumeControl volumeControl)
    {
        volumeControl.addVolumeChangeListener(this);

        volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 50);

        volumeSlider.setPreferredSize(new Dimension(20, 100));

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

        this.add(volumeSlider);
    }

    /**
     * Event fired when volume has changed.
     *
     * @param volumeChangeEvent the volume change event.
     */
    public void volumeChange(VolumeChangeEvent volumeChangeEvent)
    {
        int newValue = (int) (volumeChangeEvent.getLevel()*MULTIPLIER);

        if (volumeSlider.getValue() != newValue)
            volumeSlider.setValue(newValue);
    }
}
