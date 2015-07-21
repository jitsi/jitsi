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
package net.java.sip.communicator.util;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 * The <tt>GraphicUtils</tt> is an utility class that gives access to some
 * simple graphics operations, like an easy creating of a clipped shape or
 * image, or painting of a border glow. Most of the code in this class is based
 * on advices and examples in the "Java 2D Trickery: Soft Clipping".
 *
 * @author Yana Stamcheva
 */
public class GraphicUtils
{
    /**
     * Creates a rounded clipped shape with the given <tt>shapeWidth</tt>,
     * <tt>shapeHeight</tt>, arc width and arc height.
     * @param shapeWidth the width of the shape to create
     * @param shapeHeight the height of the shape to create
     * @param arcW the width of the arc to use to round the corners of the
     * newly created shape
     * @param arcH the height of the arc to use to round the corners of the
     * newly created shape
     * @return the created shape
     */
    public static Shape createRoundedClipShape( int shapeWidth, int shapeHeight,
                                                int arcW, int arcH)
    {
        return new RoundRectangle2D.Float(  0, 0,
                                            shapeWidth, shapeHeight,
                                            arcW, arcH);
    }

    /**
     * Creates a clipped image from the given <tt>shape</tt>.
     * @param shape the shape from which to create the image
     * @param g the <tt>Graphics</tt> object giving access to the graphics
     * device configuration
     * @return the created <tt>BufferedImage</tt>
     */
    public static BufferedImage createClipImage(Graphics2D g, Shape shape)
    {
        // Create a translucent intermediate image in which we can perform
        // the soft clipping
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        BufferedImage img = gc.createCompatibleImage(
            shape.getBounds().width, shape.getBounds().height,
            Transparency.TRANSLUCENT);
        Graphics2D g2 = img.createGraphics();

        // Clear the image so all pixels have zero alpha
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, shape.getBounds().width, shape.getBounds().height);

        // Render our clip shape into the image.  Note that we enable
        // antialiasing to achieve the soft clipping effect.  Try
        // commenting out the line that enables antialiasing, and
        // you will see that you end up with the usual hard clipping.
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(shape);
        g2.dispose();

        return img;
    }

    /**
     * Paints border glow over the given <tt>clipShape</tt> with the given
     * glow high and low colors and the given <tt>glowWidth</tt>.
     * @param g2 the <tt>Graphics</tt> object to use for painting
     * @param glowWidth the width of the glow
     * @param clipShape the shape where to paint the glow
     * @param glowOuterHigh the color which will paint the higher glow
     * @param glowOuterLow the color which will paint the lower glow
     */
    public static void paintBorderGlow( Graphics2D g2,
                                        int glowWidth,
                                        Shape clipShape,
                                        Color glowOuterHigh,
                                        Color glowOuterLow)
    {
        int gw = glowWidth*2;
        for (int i = gw; i >= 2; i -= 2)
        {
            float pct = (float)(gw - i) / (gw - 1);

            Color mixHi = getMixedColor(glowOuterHigh, pct,
                                    new Color(255, 255, 255, 200), 1.0f - pct);
            Color mixLo = getMixedColor(glowOuterLow, pct,
                                    new Color(255, 255, 255, 200), 1.0f - pct);
            g2.setPaint(new GradientPaint(
                0, clipShape.getBounds().height*0.25f,  mixHi,
                0, clipShape.getBounds().height, mixLo));

            g2.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, pct));
            g2.setStroke(new BasicStroke(i));
            g2.draw(clipShape);
        }
    }

    /**
     * Returns a mixed color from color <tt>c1</tt> and <tt>c2</tt>.
     * @param c1 the start color
     * @param pct1 the first color coefficient of the mix between 0 and 1
     * @param c2 the end color
     * @param pct2 the second color coefficient of the mix between 0 and 1
     * @return the new mixed color
     */
    private static Color getMixedColor( Color c1,
                                        float pct1,
                                        Color c2,
                                        float pct2)
    {
        float[] clr1 = c1.getComponents(null);
        float[] clr2 = c2.getComponents(null);
        for (int i = 0; i < clr1.length; i++)
        {
            clr1[i] = (clr1[i] * pct1) + (clr2[i] * pct2);
        }
        return new Color(clr1[0], clr1[1], clr1[2], clr1[3]);
    }
}
