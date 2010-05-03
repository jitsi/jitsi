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
import javax.swing.plaf.basic.*;

import org.jvnet.lafwidget.animation.*;

/**
 * A custom JButton that contains only text. A custom background could be set,
 * which will result in a round cornered background behind the text. Note that
 * you can also set a semi-transparent background. The button also supports a
 * rollover effect.
 *
 * @author Yana Stamcheva
 */
public class SIPCommTextButton extends JButton
{
    private final float[] borderColor
        = Color.DARK_GRAY.getRGBComponents(null);

    private Image bgImage;

    /**
     * Creates a <tt>SIPCommTextButton</tt>.
     */
    public SIPCommTextButton()
    {
        this("", null);
    }

    /**
     * Creates a <tt>SIPCommTextButton</tt>
     * @param text the text of the button
     */
    public SIPCommTextButton(String text)
    {
        this(text, null);
    }

    public SIPCommTextButton(String text, Image bgImage)
    {
        super(text);

        this.bgImage = bgImage;

        MouseRolloverHandler mouseHandler = new MouseRolloverHandler();

        this.addMouseListener(mouseHandler);
        this.addMouseMotionListener(mouseHandler);

        this.setIcon(null);
        this.setIconTextGap(0);

        /*
         * Explicitly remove all borders that may be set from the current look
         * and feel.
         */
        this.setContentAreaFilled(false);
        this.setUI(new BasicButtonUI());
    }

    public void setBgImage(Image image)
    {
        this.bgImage = image;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt> to
     * paint the button background and icon, and all additional effects of this
     * configurable button.
     *
     * @param g The Graphics object.
     */
    protected void paintComponent(Graphics g)
    {
        Graphics2D g1 = (Graphics2D) g.create();
        try
        {
            internalPaintComponent(g1);
        }
        finally
        {
            g1.dispose();
        }

        super.paintComponent(g);
    }

    /**
     * Paints this button.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics2D g)
    {
        AntialiasingManager.activateAntialiasing(g);

        // Paint a roll over fade out.
        FadeTracker fadeTracker = FadeTracker.getInstance();

        float visibility = this.getModel().isRollover() ? 1.0f : 0.0f;
        if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
        {
            visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
        }

        visibility /= 2;

        if (visibility != 0.0f)
        {
            g.setColor(new Color(borderColor[0], borderColor[1],
                    borderColor[2], visibility));

            if (bgImage != null)
                g.fillRoundRect((this.getWidth() - bgImage.getWidth(null))/2,
                                (this.getHeight() - bgImage.getHeight(null))/2,
                                bgImage.getWidth(null) - 1,
                                bgImage.getHeight(null) - 1,
                                20, 20);
            else
                g.fillRoundRect(0, 0,
                                this.getWidth() - 1, this.getHeight() - 1,
                                20, 20);
        }

        if (bgImage != null)
        {
            g.drawImage(bgImage,
                (this.getWidth() - bgImage.getWidth(null))/2,
                (this.getHeight() - bgImage.getHeight(null))/2, null);
        }
        else
        {
            g.setColor(getBackground());
            g.fillRoundRect(1, 1,
                            this.getWidth() - 2, this.getHeight() - 2,
                            20, 20);
        }

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
                    SIPCommTextButton.this.repaint();
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
                    SIPCommTextButton.this,
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
                    SIPCommTextButton.this,
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
