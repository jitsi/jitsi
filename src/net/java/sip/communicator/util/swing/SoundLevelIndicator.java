/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

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
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final String SOUND_LEVEL_ACTIVE
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE";

    private static final String SOUND_LEVEL_INACTIVE
        = "service.gui.soundlevel.SOUND_LEVEL_INACTIVE";

    /**
     * The maximum possible sound level.
     */
    private final int maxSoundLevel;

    /**
     * The minimum possible sound level.
     */
    private final int minSoundLevel;

    /**
     * The number of (distinct) sound bars displayed by this instance.
     */
    private int soundBarCount;

    /**
     * The sound level which is currently depicted by this
     * <tt>SoundLevelIndicator</tt>.
     */
    private int soundLevel;

    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImage;

    /**
     * Image when a sound level block is not active
     */
    private ImageIcon soundLevelInactiveImage;

    /**
     * Initializes a new <tt>SoundLevelIndicator</tt> instance.
     *
     * @param minSoundLevel the minimum possible sound level
     * @param maxSoundLevel the maximum possible sound level
     */
    public SoundLevelIndicator(int minSoundLevel, int maxSoundLevel)
    {
        this.minSoundLevel = minSoundLevel;
        this.maxSoundLevel = maxSoundLevel;
        this.soundLevel = minSoundLevel;

        loadSkin();

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
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

        int activeSoundBarCount
            = Math.round(this.soundLevel * soundBarCount / (float) range);

        /*
         * We cannot use getComponentCount() and then call getComponent(int)
         * because there are multiple threads involved and the code bellow is
         * not executed on the UI thread i.e. ArrayIndexOutOfBounds may and do
         * happen.
         */
        Component[] components = getComponents();

        for (int i = 0; i < components.length; i++)
        {
            Component c = getComponent(i);

            if (c instanceof JLabel)
            {
                ((JLabel) c).setIcon(
                        (i < activeSoundBarCount)
                            ? soundLevelActiveImage
                            : soundLevelInactiveImage);
            }
        }
        repaint();
    }

    public void resetSoundLevel()
    {
        soundLevel = minSoundLevel;
        updateSoundLevel(minSoundLevel);
    }

    @Override
    public void setBounds(int x, int y, int width, int height)
    {
        super.setBounds(x, y, width, height);

        int newSoundBarCount = getSoundBarCount(getWidth());

        if (newSoundBarCount > 0)
        {
            while (newSoundBarCount < soundBarCount)
            {
                for (int i = getComponentCount() - 1; i >= 0; i--)
                {
                    Component c = getComponent(i);

                    if (c instanceof JLabel)
                    {
                        remove(c);
                        soundBarCount--;
                        break;
                    }
                }
            }
            while (soundBarCount < newSoundBarCount)
            {
                JLabel soundBar = new JLabel(soundLevelInactiveImage);

                soundBar.setVerticalAlignment(JLabel.CENTER);
                add(soundBar);
                soundBarCount++;
            }
        }

        updateSoundLevel(soundLevel);
        revalidate();
        repaint();
    }

    /**
     * Returns the number of sound level bars that we could currently show in
     * this panel.
     *
     * @param windowWidth the current width of the call window
     * @return the number of sound level bars that we could currently show in
     * this panel
     */
    private int getSoundBarCount(int width)
    {
        int soundBarWidth = soundLevelActiveImage.getIconWidth();

        return width / soundBarWidth;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        ResourceManagementService resources = UtilActivator.getResources();

        soundLevelActiveImage = resources.getImage(SOUND_LEVEL_ACTIVE);
        soundLevelInactiveImage = resources.getImage(SOUND_LEVEL_INACTIVE);

        if (!isPreferredSizeSet())
        {
            int preferredHeight = 0;
            int preferredWidth = 0;

            if (soundLevelActiveImage != null)
            {
                int height = soundLevelActiveImage.getIconHeight();
                int width = soundLevelActiveImage.getIconWidth();

                if (preferredHeight < height)
                    preferredHeight = height;
                if (preferredWidth < width)
                    preferredWidth = width;
            }
            if (soundLevelInactiveImage != null)
            {
                int height = soundLevelInactiveImage.getIconHeight();
                int width = soundLevelInactiveImage.getIconWidth();

                if (preferredHeight < height)
                    preferredHeight = height;
                if (preferredWidth < width)
                    preferredWidth = width;
            }
            if ((preferredHeight > 0) && (preferredWidth > 0))
                setPreferredSize(
                        new Dimension(
                                10 * preferredWidth,
                                preferredHeight));
        }

        updateSoundLevel(soundLevel);
    }
}
