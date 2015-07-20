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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;

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

    private static final String SOUND_LEVEL_ACTIVE_LEFT
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE_LEFT";

    private static final String SOUND_LEVEL_ACTIVE_LEFT_GRADIENT
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE_LEFT_GRADIENT";

    private static final String SOUND_LEVEL_ACTIVE_MIDDLE
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE_MIDDLE";

    private static final String SOUND_LEVEL_ACTIVE_RIGHT
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE_RIGHT";

    private static final String SOUND_LEVEL_ACTIVE_RIGHT_GRADIENT
        = "service.gui.soundlevel.SOUND_LEVEL_ACTIVE_RIGHT_GRADIENT";

    private static final String SOUND_LEVEL_INACTIVE_LEFT
        = "service.gui.soundlevel.SOUND_LEVEL_INACTIVE_LEFT";

    private static final String SOUND_LEVEL_INACTIVE_MIDDLE
        = "service.gui.soundlevel.SOUND_LEVEL_INACTIVE_MIDDLE";

    private static final String SOUND_LEVEL_INACTIVE_RIGHT
        = "service.gui.soundlevel.SOUND_LEVEL_INACTIVE_RIGHT";

    /**
     * A runnable that will be used to update the sound level.
     */
    private final LevelUpdate levelUpdate = new LevelUpdate();

    /**
     * The <tt>Runnable</tt> which schedules the execution of
     * {@link #levelUpdate}. Introduced to better the garbage collection profile
     * of the utilization of <tt>LowPriorityEventQueue</tt>.
     *
     * @see LowPriorityEventQueue#createRepetitiveInvokeLater(Runnable)
     */
    private Runnable levelUpdateScheduler;

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
    private ImageIcon soundLevelActiveImageLeft;

    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImageLeftGradient;

    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImageMiddle;

    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImageRight;

    /**
     * Image when a sound level block is active
     */
    private ImageIcon soundLevelActiveImageRightGradient;

    /**
     * Image when a sound level block is not active
     */
    private ImageIcon soundLevelInactiveImageLeft;

    /**
     * Image when a sound level block is not active
     */
    private ImageIcon soundLevelInactiveImageMiddle;

    /**
     * Image when a sound level block is not active
     */
    private ImageIcon soundLevelInactiveImageRight;

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
     * Returns the number of sound level bars that we could currently show in
     * this panel.
     *
     * @param width the current width of the call window
     * @return the number of sound level bars that we could currently show in
     * this panel
     */
    private int getSoundBarCount(int width)
    {
        int soundBarWidth = soundLevelActiveImageLeft.getIconWidth();

        return width / soundBarWidth;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        ResourceManagementService resources = DesktopUtilActivator.getResources();

        soundLevelActiveImageLeft
            = resources.getImage(SOUND_LEVEL_ACTIVE_LEFT);
        soundLevelActiveImageLeftGradient
            = resources.getImage(SOUND_LEVEL_ACTIVE_LEFT_GRADIENT);
        soundLevelActiveImageMiddle
            = resources.getImage(SOUND_LEVEL_ACTIVE_MIDDLE);
        soundLevelActiveImageRight
            = resources.getImage(SOUND_LEVEL_ACTIVE_RIGHT);
        soundLevelActiveImageRightGradient
            = resources.getImage(SOUND_LEVEL_ACTIVE_RIGHT_GRADIENT);

        soundLevelInactiveImageLeft
            = resources.getImage(SOUND_LEVEL_INACTIVE_LEFT);
        soundLevelInactiveImageMiddle
            = resources.getImage(SOUND_LEVEL_INACTIVE_MIDDLE);
        soundLevelInactiveImageRight
            = resources.getImage(SOUND_LEVEL_INACTIVE_RIGHT);

        if (!isPreferredSizeSet())
        {
            int preferredHeight = 0;
            int preferredWidth = 0;

            if (soundLevelActiveImageLeft != null)
            {
                int height = soundLevelActiveImageLeft.getIconHeight();
                int width = soundLevelActiveImageLeft.getIconWidth();

                if (preferredHeight < height)
                    preferredHeight = height;
                if (preferredWidth < width)
                    preferredWidth = width;
            }
            if (soundLevelInactiveImageLeft != null)
            {
                int height = soundLevelInactiveImageLeft.getIconHeight();
                int width = soundLevelInactiveImageLeft.getIconWidth();

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
                JLabel soundBar;
                if (soundBarCount == 0)
                    soundBar = new JLabel(soundLevelInactiveImageLeft);
                else if (soundBarCount == newSoundBarCount - 1)
                    soundBar = new JLabel(soundLevelInactiveImageRight);
                else
                    soundBar = new JLabel(soundLevelInactiveImageMiddle);

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
     * Update the sound level indicator component to fit the given values.
     *
     * @param soundLevel the sound level to show
     */
    public void updateSoundLevel(int soundLevel)
    {
        levelUpdate.setSoundLevel(soundLevel);

        Runnable levelUpdateScheduler;

        synchronized (this)
        {
            if (this.levelUpdateScheduler == null)
            {
                this.levelUpdateScheduler
                    = LowPriorityEventQueue.createRepetitiveInvokeLater(
                            levelUpdate);
            }
            levelUpdateScheduler = this.levelUpdateScheduler;
        }

        levelUpdateScheduler.run();
    }

    /**
     * Update the sound level indicator component to fit the given values.
     *
     * @param soundLevel the sound level to show
     */
    private void updateSoundLevelInternal(int soundLevel)
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
                Icon activeIcon = null;
                Icon inactiveIcon = null;
                if (i == 0)
                {
                    if (activeSoundBarCount == 1)
                        activeIcon = soundLevelActiveImageLeftGradient;
                    else
                    {
                        activeIcon = soundLevelActiveImageLeft;
                        inactiveIcon = soundLevelInactiveImageLeft;
                    }
                }
                else if (i == activeSoundBarCount - 1)
                {
                    if (i == components.length - 1)
                        activeIcon = soundLevelActiveImageRight;
                    else
                        activeIcon = soundLevelActiveImageRightGradient;
                }
                else if (i == components.length - 1)
                {
                    inactiveIcon = soundLevelInactiveImageRight;
                }
                else
                {
                    activeIcon = soundLevelActiveImageMiddle;
                    inactiveIcon = soundLevelInactiveImageMiddle;
                }

                ((JLabel) c).setIcon(
                        (i < activeSoundBarCount)
                            ? activeIcon
                            : inactiveIcon);
            }
        }

        repaint();
    };

    /**
     * Runnable used to update sound levels.
     */
    private class LevelUpdate
        implements Runnable
    {
        /**
         * The current sound level to update.
         */
        private int soundLevel;

        /**
         * Update.
         */
        public void run()
        {
            updateSoundLevelInternal(soundLevel);
        }

        /**
         * Changes the sound level.
         * @param soundLevel changes the sound level.
         */
        public void setSoundLevel(int soundLevel)
        {
            this.soundLevel = soundLevel;
        }
    }
}
