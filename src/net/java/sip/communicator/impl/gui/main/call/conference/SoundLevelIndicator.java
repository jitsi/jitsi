/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 *Represents the sound level indicator for a particular peer
 *
 * @author Dilshan Amadoru
 * @author Yana Stamcheva
 */
public class SoundLevelIndicator
    extends TransparentPanel
    implements ComponentListener
{
    /**
     * Image when a sound level block is active
     */
    private final ImageIcon soundLevelActiveImage;

    /**
     * Image when a sound level block is not active
     */
    private final ImageIcon soundLevelInactiveImage;

    /**
     * Current number of distinct sound levels displayed in the UI.
     */
    private int soundBarNumber = 8;

    private boolean isComponentListenerAdded = false;

    /**
     * The minimum possible sound level.
     */
    private final int minSoundLevel;

    /**
     * The maximum possible sound level.
     */
    private final int maxSoundLevel;

    /**
     * Constructor
     * @param minSoundLevel the minimum possible sound level
     * @param maxSoundLevel the maximum possible sound level
     */
    public SoundLevelIndicator(int minSoundLevel, int maxSoundLevel)
    {
        this.minSoundLevel = minSoundLevel;
        this.maxSoundLevel = maxSoundLevel;

        soundLevelActiveImage = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SOUND_LEVEL_ACTIVE));
        soundLevelInactiveImage = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SOUND_LEVEL_INACTIVE));

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));

        this.initSoundBars();

        this.addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                if (!isComponentListenerAdded)
                {
                    Window parentWindow
                        = SwingUtilities.getWindowAncestor(
                            SoundLevelIndicator.this);

                    if (parentWindow != null)
                    {
                        parentWindow.addComponentListener(
                            SoundLevelIndicator.this);
                        isComponentListenerAdded = true;
                    }
                }
            }
        });
    }

    /**
     * Initializes sound bars.
     */
    private void initSoundBars()
    {
        for (int i = 0; i < soundBarNumber; i++)
        {
            JLabel block = new JLabel(soundLevelInactiveImage);
            this.add(block);
        }
    }

    /**
     * Update the sound level indicator component to fit the given values.
     *
     * @param soundLevel the sound level to show
     */
    public void updateSoundLevel(int soundLevel)
    {
        int range = 1;
        // Check if the given range values are correct.
        if (maxSoundLevel > -1 && minSoundLevel > -1
                && maxSoundLevel >= minSoundLevel)
            range = maxSoundLevel - minSoundLevel;

        int activeBarNumber = soundLevel*soundBarNumber/range;

        for (int i = 0; i < getComponentCount(); i++)
        {
            Component c = getComponent(i);
            if (c instanceof JLabel)
            {
                if (i < activeBarNumber)
                    ((JLabel) c).setIcon(soundLevelActiveImage);
                else
                    ((JLabel) c).setIcon(soundLevelInactiveImage);
            }
        }
        this.repaint();
    }

    public void componentHidden(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {}

    public void componentShown(ComponentEvent e) {}

    /**
     * Adds/removes sound level bars to this component when it's resized.
     * @param e the <tt>ComponentEvent</tt> which was triggered
     */
    public void componentResized(ComponentEvent e)
    {
        int windowWidth = e.getComponent().getWidth();

        int currentNumber = getSoundBarNumber(windowWidth);

        while (currentNumber > 0 && currentNumber < soundBarNumber)
        {
            for (int i = getComponentCount() - 1; i >= 0; i--)
            {
                if (getComponent(i) instanceof JLabel)
                {
                    this.remove(getComponent(i));

                    soundBarNumber--;
                }
            }
        }

        while (currentNumber > 0 && soundBarNumber < currentNumber)
        {
            JLabel block = new JLabel(soundLevelInactiveImage);

            this.add(block);

            soundBarNumber++;
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * Returns the number of sound level bars that we could currently show in
     * this panel.
     * 
     * @param windowWidth the current width of the call window
     * @return the number of sound level bars that we could currently show in
     * this panel
     */
    private int getSoundBarNumber(int windowWidth)
    {
        int barWidth = soundLevelActiveImage.getIconWidth();

        // We deduct 150px from the given windowWidth because this is not the
        // only component positioned on the horizontal axe.
        return (windowWidth - 150)/barWidth;
    }
}
