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

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.event.*;

/**
 * The volume control slider component.
 *
 * @author Yana Stamcheva
 * @author Vincent Lucas
 */
public class VolumeControlSlider
    extends TransparentPanel
    implements VolumeChangeListener
{
    /**
     * The slider component.
     */
    private final JSlider volumeSlider;

    /**
     * The VolumeControl that do the actual volume adjusting.
     */
    private final VolumeControl volumeControl;

    /**
     * The dedicate thread to set the volume.
     */
    private SetVolumeThread setVolumeThread = null;

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
    public VolumeControlSlider( final VolumeControl volumeControl,
                                int orientation)
    {
        super(new BorderLayout());

        this.volumeControl = volumeControl;
        volumeControl.addVolumeChangeListener(this);

        setVolumeThread = new SetVolumeThread(volumeControl);

        volumeSlider = new JSlider(orientation, 0, 100, 50);

        if (orientation == JSlider.VERTICAL)
            volumeSlider.setPreferredSize(new Dimension(20, 100));
        else
            volumeSlider.setPreferredSize(new Dimension(100, 20));

        // Sets the minimum, maximum and default volume values for the volume
        // slider.
        volumeSlider.setMinimum((int) (volumeControl.getMinValue()*MULTIPLIER));
        volumeSlider.setMaximum((int) (volumeControl.getMaxValue()*MULTIPLIER));
        volumeSlider.setValue((int) (volumeControl.getVolume()*MULTIPLIER));

        // Adds a change listener to the slider in order to correctly set
        // the volume through the VolumeControl service, on user change.
        volumeSlider.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider) e.getSource();
                int volume = source.getValue();

                // Set the volume to the volume control.
                setVolumeThread.setVolume((float) volume/MULTIPLIER);
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

    /**
     * Returns this slider in a popup menu.
     *
     * @return this slider in a popup menu
     */
    public JPopupMenu getPopupMenu()
    {
        SIPCommPopupMenu popupMenu = new SIPCommPopupMenu();

        popupMenu.add(this);

        return popupMenu;
    }

    /**
     * Makes this Component displayable by connecting it to a native screen
     * resource. Starts the thread loop to change volume.
     */
    @Override
    public void addNotify()
    {
        super.addNotify();

        // Updates the slider level in correspodance with the system volume
        // level.
        volumeChange(new VolumeChangeEvent(
                    volumeControl,
                    volumeControl.getVolume(),
                    volumeControl.getMute()));

        // Starts the thread loop to update the volume, as long as the slider is
        // shown.
        if(!setVolumeThread.isAlive())
        {
            setVolumeThread = new SetVolumeThread(volumeControl);
            setVolumeThread.start();
        }
    }

    /**
     *  Makes this Component undisplayable by destroying it native screen
     *  resource. Stops the thread loop to change volume.
     */
    @Override
    public void removeNotify()
    {
        super.removeNotify();

        // Stops the thread loop to update the volume, since the slider is
        // not shown anymore.
        if(setVolumeThread.isAlive())
        {
            setVolumeThread.end();
        }
    }

    /**
     * Create a dedicate thread to set the volume.
     */
    private class SetVolumeThread
        extends Thread
    {
        /**
         * A boolean set to true if the thread must continue to loop.
         */
        private boolean run;

        /**
         * The VolumeControl that do the actual volume adjusting.
         */
        private final VolumeControl volumeControl;

        /**
         * The volume wished by the UI.
         */
        private float volume;

        /**
         * The volume currently set.
         */
        private float lastVolumeSet;

        /**
         * Create a dedicate thread to set the volume.
         *
         * @param volumeControl The VolumeControl that do the actual volume
         * adjusting.
         */
        public SetVolumeThread(final VolumeControl volumeControl)
        {
            super("VolumeControlSlider: VolumeControl.setVolume");

            this.run = true;
            this.volumeControl = volumeControl;
            this.lastVolumeSet = volumeControl.getVolume();
            this.volume = this.lastVolumeSet;
        }

        /**
         * Updates and sets the volume if changed.
         */
        @Override
        public void run()
        {
            while(this.run)
            {
                synchronized(this)
                {
                    // Wait if there is no update yet.
                    if(volume == lastVolumeSet)
                    {
                        try
                        {
                            this.wait();
                        }
                        catch(InterruptedException iex)
                        {
                        }
                    }
                    lastVolumeSet = volume;
                }

                // Set the volume to the volume control.
                volumeControl.setVolume(lastVolumeSet);
            }
        }

        /**
         * Sets a new volume value.
         *
         * @param newVolume The new volume to set.
         */
        public void setVolume(float newVolume)
        {
            synchronized(this)
            {
                volume = newVolume;
                // If there is a change, then wake up the tread loop..
                if(volume != lastVolumeSet)
                {
                    this.notify();
                }
            }
        }

        /**
         * Ends the thread loop.
         */
        public void end()
        {
            synchronized(this)
            {
                this.run = false;
                this.notify();
            }
        }
    }
}
