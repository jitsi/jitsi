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

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents the sound level indicator for a particular peer.
 *
 * @author Dilshan Amadoru
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class SoundLevelIndicator
    extends TransparentPanel
    implements ComponentListener,
               Skinnable
{
    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImage;

    /**
     * Image when a sound level block is not active
     */
    private ImageIcon soundLevelInactiveImage;

    /**
     * Current number of distinct sound levels displayed in the UI.
     */
    private int soundBarNumber = 0;

    /**
     * The sound level which is currently depicted by this
     * <tt>SoundLevelIndicator</tt>.
     */
    private int soundLevel;

    /**
     * Indicates if the component listener is added.
     */
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
     * The renderer of the corresponding call, for which this sound level
     * indicator is created.
     */
    private final CallRenderer callRenderer;

    /**
     * Constructor
     * @param callRenderer the renderer of the corresponding call, for which
     * this sound level indicator is created
     * @param minSoundLevel the minimum possible sound level
     * @param maxSoundLevel the maximum possible sound level
     */
    public SoundLevelIndicator( CallRenderer callRenderer,
                                int minSoundLevel,
                                int maxSoundLevel)
    {
        this.callRenderer = callRenderer;

        this.minSoundLevel = minSoundLevel;
        this.maxSoundLevel = maxSoundLevel;

        loadSkin();

        this.soundBarNumber
            = getSoundBarNumber(callRenderer.getCallContainer().getWidth());

        if (soundBarNumber <= 0)
            soundBarNumber = 8;

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        this.initSoundBars();

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
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
            block.setVerticalAlignment(JLabel.CENTER);

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
        if ((minSoundLevel > -1)
                && (maxSoundLevel > -1)
                && (minSoundLevel < maxSoundLevel))
        {
            range = maxSoundLevel - minSoundLevel;

            if (soundLevel < 40 /* A WHISPER */)
                soundLevel = minSoundLevel;
            else if (soundLevel > 85 /* BEGINNING OF HEARING DAMAGE */)
                soundLevel = maxSoundLevel;
            else
            {
                /*
                 * Depict the range between "A WHISPER" and "BEGINNING OF
                 * HEARING DAMAGE".
                 */
                soundLevel = (int) (((soundLevel - 40.0) / 45.0) * range);
                if (soundLevel < minSoundLevel)
                    soundLevel = minSoundLevel;
                else if (soundLevel > maxSoundLevel)
                    soundLevel = maxSoundLevel;
            }
        }

        /*
         * Audacity uses 0.9 for this.soundLevel and, consequently, 0.1 for
         * soundLevel but that makes the animation too slow.
         */
        this.soundLevel = (int) (this.soundLevel * 0.8 + soundLevel * 0.2);

        int activeBarNumber
            = Math.round(this.soundLevel * soundBarNumber / (float) range);

        for (int i = 0, count = getComponentCount(); i < count; i++)
        {
            Component c = getComponent(i);

            if (c instanceof JLabel)
            {
                ((JLabel) c).setIcon(
                        (i < activeBarNumber)
                            ? soundLevelActiveImage
                            : soundLevelInactiveImage);
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
        int windowWidth = callRenderer.getCallContainer().getWidth();
        int newNumber = getSoundBarNumber(windowWidth);

        if (newNumber > 0)
        {
            while (newNumber < soundBarNumber)
            {
                for (int i = getComponentCount() - 1; i >= 0; i--)
                {
                    Component c = getComponent(i);

                    if (c instanceof JLabel)
                    {
                        this.remove(c);
                        soundBarNumber--;
                    }
                }
            }

            while (soundBarNumber < newNumber)
            {
                JLabel block = new JLabel(soundLevelInactiveImage);

                this.add(block);
                soundBarNumber++;
            }
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
        // only component positioned on the horizontal axis.
        return (windowWidth - 130)/barWidth;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        soundLevelActiveImage = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SOUND_LEVEL_ACTIVE));
        soundLevelInactiveImage = new ImageIcon(
            ImageLoader.getImage(ImageLoader.SOUND_LEVEL_INACTIVE));

        updateSoundLevel(minSoundLevel);
    }
}
