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
import java.awt.event.*;

import javax.swing.*;

import org.jvnet.lafwidget.animation.*;

/**
 * The <tt>SIPCommToggleButton</tt> is a flexible <tt>JToggleButton</tt> that
 * allows to configure its background, its icon, the look when a mouse is over
 * it, etc.
 *
 * @author Yana Stamcheva
 */
public class SIPCommToggleButton
    extends JToggleButton
    implements OrderedComponent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The background image shown in normal button state.
     */
    private Image bgImage;

    /**
     * The background image shown in rollover button state.
     */
    private Image bgRolloverImage;

    /**
     * The icon image shown in normal (non-pressed) button state.
     */
    private Image iconImage;

    /**
     * The background image shown in pressed button state.
     */
    private Image pressedImage;

    /**
     * The icon image shown in pressed button state.
     */
    private Image pressedIconImage;

    /**
     * The index of the button, used when we want to order our buttons.
     */
    private int index = -1;

    /**
     * Creates an instance of <tt>SIPCommToggleButton</tt>.
     */
    public SIPCommToggleButton()
    {
        // Explicitly remove all borders that may be set from the current
        // look and feel.
        this.setBorder(null);

        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);
    }

    /**
     * Creates a button with custom background image and rollover image.
     *
     * @param bgImage the background button image
     * @param rolloverImage the rollover button image
     */
    public SIPCommToggleButton(Image bgImage, Image rolloverImage)
    {
        this(bgImage, rolloverImage, null, null);
    }

    /**
     * Creates a button with custom background image, rollover image and
     * icon image.
     *
     * @param bgImage       The background image.
     * @param rolloverImage The roll over image.
     * @param iconImage     The icon.
     * @param pressedImage  The image used to paint the pressed state.
     */
    public SIPCommToggleButton( Image bgImage,
                                Image rolloverImage,
                                Image iconImage,
                                Image pressedImage)
    {
        this(bgImage, rolloverImage, iconImage, pressedImage, null);
    }

    /**
     * Creates a button with custom background image, rollover image and
     * icon image.
     *
     * @param bgImage       the background image
     * @param rolloverImage the roll over image
     * @param iconImage     the icon
     * @param pressedImage  the image used to paint the pressed state
     * @param pressedIconImage the icon image in a pressed state
     */
    public SIPCommToggleButton( Image bgImage,
                                Image rolloverImage,
                                Image iconImage,
                                Image pressedImage,
                                Image pressedIconImage)
    {
        this();

        this.bgImage = bgImage;
        this.bgRolloverImage = rolloverImage;
        this.iconImage = iconImage;
        this.pressedImage = pressedImage;
        this.pressedIconImage = pressedIconImage;

        this.setPreferredSize(
            new Dimension(  this.bgImage.getWidth(null),
                            this.bgImage.getHeight(null)));

        if (iconImage != null)
            this.setIcon(new ImageIcon(this.iconImage));
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt>
     * to paint the button background and icon, and all additional effects
     * of this configurable button.
     *
     * @param g the Graphics object
     */

    @Override
    public void paintComponent(Graphics g)
    {
        g = g.create();
        try
        {
            internalPaintComponent(g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints this button.
     *
     * @param g the Graphics object
     */
    private void internalPaintComponent(Graphics g)
    {
        AntialiasingManager.activateAntialiasing(g);

        if (this.bgImage != null)
        {
            // If there's no icon, we make grey the backgroundImage
            // when disabled.
            if (!isEnabled())
            {
                Image disabledImage = new ImageIcon(LightGrayFilter
                        .createDisabledImage(bgImage)).getImage();

                g.drawImage(disabledImage, 0, 0, this);
            }
            else {
                g.drawImage(this.bgImage, 0, 0, this);
            }
        }

        // Paint the roll over image.
        if (getModel().isRollover() && bgRolloverImage != null)
        {
            g.drawImage(bgRolloverImage, 0, 0, this);
        }

        // Paint the pressed image.
        if (getModel().isSelected() && pressedImage != null)
        {
            g.drawImage(pressedImage, 0, 0, this);
        }

        // Paint a roll over fade out.
        FadeTracker fadeTracker = FadeTracker.getInstance();

        float visibility = getModel().isRollover() ? 1.0f : 0.0f;
        if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
        {
            visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
        }
        visibility /= 2;

        g.setColor(new Color(1.0f, 1.0f, 1.0f, visibility));

        if (bgImage != null)
        {
            g.fillRoundRect(this.getWidth() / 2 - this.bgImage.getWidth(null)
                / 2, this.getHeight() / 2 - this.bgImage.getHeight(null) / 2,
                bgImage.getWidth(null), bgImage.getHeight(null), 10, 10);
        }
        else if (isContentAreaFilled() || (visibility != 0.0f))
        {
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 10, 10);
        }

        // Paint the icon image.
        Image iconImageFinal = null;
        if (getModel().isSelected() && pressedIconImage != null)
        {
            iconImageFinal = pressedIconImage;
        }
        else if (iconImage != null)
        {
            if (!isEnabled())
            {
                iconImageFinal = new ImageIcon(LightGrayFilter
                        .createDisabledImage(iconImage)).getImage();
            }
            else
            {
                iconImageFinal = iconImage;
            }
        }

        int bgWidth = (bgImage != null)
                        ? bgImage.getWidth(null)
                        : getWidth();
        int bgHeight = (bgImage != null)
                        ? bgImage.getHeight(null)
                        : getHeight();

        if (iconImageFinal != null)
            g.drawImage(iconImageFinal,
                (bgWidth - iconImageFinal.getWidth(null)) / 2,
                (bgHeight - iconImageFinal.getHeight(null)) / 2, this);
    }

    /**
     * Returns the background image of this button.
     *
     * @return the background image of this button
     */
    public Image getBgImage()
    {
        return bgImage;
    }

    /**
     * Sets the background image to this button.
     *
     * @param bgImage the background image to set
     */
    public void setBgImage(Image bgImage)
    {
        this.bgImage = bgImage;

        this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
            this.bgImage.getHeight(null)));
    }

    /**
     * Returns the background rollover image of this button.
     *
     * @return the background rollover image of this button
     */
    public Image getBgRolloverImage()
    {
        return bgRolloverImage;
    }

    /**
     * Sets the background rollover image to this button.
     *
     * @param bgRolloverImage the background rollover image to set
     */
    public void setBgRolloverImage(Image bgRolloverImage)
    {
        this.bgRolloverImage = bgRolloverImage;
    }

    /**
     * Returns the icon image of this button.
     *
     * @return the icon image of this button
     */
    public Image getIconImage()
    {
        return iconImage;
    }

    /**
     * Sets the icon image to this button.
     *
     * @param iconImage the icon image to set
     */
    public void setIconImage(Image iconImage)
    {
        this.iconImage = iconImage;
        this.repaint();
    }

    /**
     * Sets the icon image for the pressed state of this button.
     *
     * @param iconImage the icon image to set
     */
    public void setPressedIconImage(Image iconImage)
    {
        this.pressedIconImage = iconImage;
        this.repaint();
    }

    /**
     * Sets the image representing the pressed state of this button.
     *
     * @param pressedImage the image representing the pressed state of this
     * button
     */
    public void setPressedImage(Image pressedImage)
    {
        this.pressedImage = pressedImage;
        this.repaint();
    }

    /**
     * Change buttons index when we want to order it.
     * @param index the button index.
     */
    public void setIndex(int index)
    {
        this.index = index;
    }

    /**
     * Returns the current button index we have set, or -1 if none used.
     * @return
     */
    public int getIndex()
    {
        return this.index;
    }

    /**
     * The <tt>ButtonRepaintCallback</tt> is charged to repaint this button
     * when the fade animation is performed.
     */
    private class ButtonRepaintCallback implements FadeTrackerCallback
    {
        public void fadeEnded(FadeKind arg0)
        {
            repaintLater();
        }

        public void fadePerformed(FadeKind arg0, float arg1)
        {
            repaintLater();
        }

        private void repaintLater()
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    SIPCommToggleButton.this.repaint();
                }
            });
        }

        public void fadeReversed(FadeKind arg0, boolean arg1, float arg2)
        {
        }
    }

    /**
     * Perform a fade animation on mouse over.
     */
    private class MouseRolloverHandler
        implements  MouseListener,
                    MouseMotionListener
    {
        public void mouseMoved(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(false);

                FadeTracker fadeTracker = FadeTracker.getInstance();

                fadeTracker.trackFadeOut(FadeKind.ROLLOVER,
                    SIPCommToggleButton.this,
                    true,
                    new ButtonRepaintCallback());
            }
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
            if (isEnabled())
            {
                getModel().setRollover(true);

                FadeTracker fadeTracker = FadeTracker.getInstance();

                fadeTracker.trackFadeIn(FadeKind.ROLLOVER,
                    SIPCommToggleButton.this,
                    true,
                    new ButtonRepaintCallback());
            }
        }

        public void mousePressed(MouseEvent e)
        {
        }

        public void mouseReleased(MouseEvent e)
        {
        }

        public void mouseDragged(MouseEvent e)
        {
        }
    }
}
