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
import java.awt.geom.*;

import javax.swing.*;

import org.jvnet.lafwidget.animation.*;

/**
 * The <tt>FadeInBaloonPanel</tt> is a semi-transparent "balloon" panel, which
 * could be shown in a glass pane for example. You can define a begin point,
 * where the balloon triangle would show.
 *
 * @author Yana Stamcheva
 */
public class FadeInBalloonPanel
    extends TransparentPanel
{
    /**
     * The begin point, where the balloon triangle will be shown.
     */
    private Point beginPoint;

    /**
     * The begin point shift, which defines the rectangle point shift.
     */
    private final static int beginPointShift = 6;

    /**
     * Sets the begin point.
     *
     * @param beginPoint the begin point
     */
    public void setBeginPoint(Point beginPoint)
    {
        this.beginPoint = beginPoint;
    }

    /**
     * Overrides the <code>paintComponent</code> method of <tt>JButton</tt> to
     * paint the button background and icon, and all additional effects of this
     * configurable button.
     *
     * @param g The Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        g = g.create();
        try
        {
            internalPaintComponent((Graphics2D) g);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Paints this button.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    private void internalPaintComponent(Graphics2D g)
    {
        AntialiasingManager.activateAntialiasing(g);
        /*
         * As JComponent#paintComponent says, if you do not invoke super's
         * implementation you must honor the opaque property, that is if this
         * component is opaque, you must completely fill in the background in a
         * non-opaque color. If you do not honor the opaque property you will
         * likely see visual artifacts.
         */
        if (isOpaque())
        {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // Paint a roll over fade out.
        FadeTracker fadeTracker = FadeTracker.getInstance();

        float visibility = isVisible() ? 0.8f : 0.0f;
        if (fadeTracker.isTracked(this, FadeKind.ROLLOVER))
        {
            visibility = fadeTracker.getFade(this, FadeKind.ROLLOVER);
        }

        g.setColor(new Color(0f, 0f, 0f, visibility));

        int y = 0;

        // draw triangle (polygon)
        if (beginPoint != null)
        {
            y = beginPointShift;

            int x1Points[] = {  beginPoint.x,
                                beginPoint.x + beginPointShift,
                                beginPoint.x - beginPointShift};

            int y1Points[] = {  beginPoint.y,
                                beginPoint.y + beginPointShift,
                                beginPoint.y + beginPointShift};

            GeneralPath polygon =
                    new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                    x1Points.length);

            polygon.moveTo(x1Points[0], y1Points[0]);

            for (int index = 1; index < x1Points.length; index++) {
                    polygon.lineTo(x1Points[index], y1Points[index]);
            };

            polygon.closePath();
            g.fill(polygon);
        }

        if (visibility != 0.0f)
        {
            g.fillRoundRect(
                0, y, this.getWidth(), this.getHeight(), 10, 10);
        }
    }

    /**
     * The <tt>ButtonRepaintCallback</tt> is charged to repaint this button
     * when the fade animation is performed.
     */
    private class PanelRepaintCallback implements FadeTrackerCallback
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
                    FadeInBalloonPanel.this.repaint();
                }
            });
        }

        public void fadeReversed(FadeKind arg0, boolean arg1, float arg2)
        {
        }
    }

    /**
     * Shows/hides this panel.
     *
     * @param isVisible <tt>true</tt> to show this panel, <tt>false</tt> to
     * hide it
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        FadeTracker fadeTracker = FadeTracker.getInstance();

        if (isVisible)
        {
            fadeTracker.trackFadeIn(FadeKind.ROLLOVER,
                FadeInBalloonPanel.this,
                true,
                new PanelRepaintCallback());
        }
        else
        {
            fadeTracker.trackFadeOut(FadeKind.ROLLOVER,
                FadeInBalloonPanel.this,
                true,
                new PanelRepaintCallback());
        }
    }
}
