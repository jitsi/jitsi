/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

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
{
    private Image bgImage;

    private Image bgRolloverImage;

    private Image iconImage;

    private Image pressedImage;

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
        this();

        this.bgImage = bgImage;
        this.bgRolloverImage = rolloverImage;
        this.iconImage = iconImage;
        this.pressedImage = pressedImage;

        this.setPreferredSize(
            new Dimension(  this.bgImage.getWidth(null),
                            this.bgImage.getHeight(null)));

        if (iconImage != null)
            this.setIcon(new ImageIcon(this.iconImage));
    }

    /**
     * Creates a button with custom background image and rollover image.
     *
     * @param bgImage The background button image.
     * @param rolloverImage The rollover button image.
     */
    public SIPCommToggleButton(Image bgImage, Image rolloverImage)
    {
        this(bgImage, rolloverImage, null, null);
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt>
     * to paint the button background and icon, and all additional effects
     * of this configurable button.
     *
     * @param g The Graphics object.
     */

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
     * @param g The Graphics object.
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
        if (this.getModel().isRollover() && this.bgRolloverImage != null)
        {
            g.drawImage(this.bgRolloverImage, 0, 0, this);
        }

        // Paint the pressed image.
        if (this.getModel().isSelected() && this.pressedImage != null)
        {
            g.drawImage(this.pressedImage, 0, 0, this);
        }

        // Paint a roll over fade out.
        FadeTracker fadeTracker = FadeTracker.getInstance();

        float visibility = this.getModel().isRollover() ? 1.0f : 0.0f;
        if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
        {
            visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
        }
        visibility /= 2;

        g.setColor(new Color(1.0f, 1.0f, 1.0f, visibility));

        if (this.bgImage != null)
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
        if (this.iconImage != null)
        {
            if (!isEnabled())
            {
                Image disabledImage = new ImageIcon(LightGrayFilter
                        .createDisabledImage(iconImage)).getImage();

                g.drawImage(disabledImage,
                        (this.bgImage.getWidth(null) - disabledImage
                                .getWidth(null)) / 2, (this.bgImage
                                .getHeight(null) - disabledImage
                                .getHeight(null)) / 2, this);
            }
            else
            {
                g.drawImage(this.iconImage,
                        (this.bgImage.getWidth(null) - this.iconImage
                                .getWidth(null)) / 2, (this.bgImage
                                .getHeight(null) - this.iconImage
                                .getHeight(null)) / 2, this);
            }
        }
    }

    /**
     * Returns the background image of this button.
     * @return the background image of this button.
     */
    public Image getBgImage()
    {
        return bgImage;
    }

    /**
     * Sets the background image to this button.
     * @param bgImage The background image to set.
     */
    public void setBgImage(Image bgImage)
    {
        this.bgImage = bgImage;

        this.setPreferredSize(new Dimension(this.bgImage.getWidth(null),
            this.bgImage.getHeight(null)));
    }

    /**
     * Returns the background rollover image of this button.
     * @return the background rollover image of this button.
     */
    public Image getBgRolloverImage()
    {
        return bgRolloverImage;
    }

    /**
     * Sets the background rollover image to this button.
     * @param bgRolloverImage The background rollover image to set.
     */
    public void setBgRolloverImage(Image bgRolloverImage)
    {
        this.bgRolloverImage = bgRolloverImage;
    }

    /**
     * Returns the icon image of this button.
     * @return the icon image of this button.
     */
    public Image getIconImage()
    {
        return iconImage;
    }

    /**
     * Sets the icon image to this button.
     * @param iconImage The icon image to set.
     */
    public void setIconImage(Image iconImage)
    {
        this.iconImage = iconImage;
        this.repaint();
    }

    /**
     * Sets the image representing the pressed state of this button.
     *
     * @param pressedImage The image representing the pressed state of this
     * button.
     */
    public void setPressedImage(Image pressedImage)
    {
        this.pressedImage = pressedImage;
        this.repaint();
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
