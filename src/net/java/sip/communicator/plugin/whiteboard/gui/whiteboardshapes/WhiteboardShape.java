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
package net.java.sip.communicator.plugin.whiteboard.gui.whiteboardshapes;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Abstract WhiteboardShape (Shape for the WhitheboardFrame)
 *
 * @author Julien Waechter
 */
public abstract class WhiteboardShape implements WhiteboardObject
{
    /**
     * String that uniquely identifies this WhiteboardObject.
     */
    private String id;

    /**
     * WhiteboardShape's color (or rather it's border)
     */
    private Color color = Color.BLACK;

    /**
     * Indicates if this shape is selected.
     */
    private boolean selected;

    /**
     * WhiteboardShape's opacity.
     */

    private float opacity = 1F;

    /**
     * Number of pixels that this object (or its border) should be thick.
     */
    private int thickness =1;

    /**
     * A selected point that starts a modification.
     */
    private WhiteboardPoint modifyPoint;

    /**
     * WhiteboardShape constructor
     * @param id String that uniquely identifies this WhiteboardObject.
     */
    public WhiteboardShape (String id)
    {
        this.id = id;
    }

    /**
     * Code when shape is preselected
     * @param g graphics context
     * @param t 2D affine transform
     */
    public void paint (Graphics g, AffineTransform t)
    {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor (color);
        Composite oldComposite = g2.getComposite ();
        g2.setComposite (AlphaComposite.getInstance (
          AlphaComposite.SRC_OVER, getOpacity ()));

        paintShape (g2, t);

        if (isSelected ())
        {
            select (g2, t);
        }

        if (getModifyPoint() != null)
        {
            drawSelectedPoint (g, t, modifyPoint, Color.red);
        }

        g2.setComposite (oldComposite);
    }

    /**
     * Code to paint the specific shape
     * @param g graphics context
     * @param t 2D affine transform
     */
    public abstract void paintShape (Graphics2D g, AffineTransform t);

    /**
     * method to test if the shape contains a point
     * @param p coord point
     * @return true if shape contains p
     */
    public abstract boolean contains (Point2D p);

    /**
     * Sets color of the WhiteboardShape (or rather it's border)
     *
     * @param color color shape
     */
    public void setColor (Color color)
    {
        this.color = color;
    }

    /**
     * Returns WhiteboardShape's opacity
     *
     * @return current WhiteboardShape's opacity
     */
    public float getOpacity ()
    {
        return opacity;
    }

    /**
     * Sets WhiteboardShape's opacity
     *
     * @param opacity opacity of the shape
     */
    public void setOpacity (float opacity)
    {
        this.opacity = opacity;
    }

    /**
     * Returns true if the Shape is selected
     *
     * @return true if the Shape is selected
     */
    public boolean isSelected ()
    {
        return selected;
    }

    /**
     * Sets selected the shape
     *
     * @param selected true for select the shape
     */
    public void setSelected (boolean selected)
    {
        this.selected = selected;
    }

    /**
     * Sets the point from which a modification could start.
     *
     * @param point the point from which a modification could start.
     */
    public void setModifyPoint(WhiteboardPoint point)
    {
        this.modifyPoint = point;
    }

    /**
     * The last selected for modification point.
     *
     * @return the last selected for modification point.
     */
    public WhiteboardPoint getModifyPoint()
    {
        return modifyPoint;
    }

    /**
     * Code when shape is selected
     * @param g graphics context
     * @param t 2D affine transform
     */
    public void select (Graphics g, AffineTransform t)
    {
        drawSelectionPoints (g, t, Color.cyan);
    }

    /**
     * Draws selection points when a shape is preselected.
     *
     * @param g graphics context
     * @param t 2D affine transform
     */
    public void preselect (Graphics g, AffineTransform t)
    {
        drawSelectionPoints (g, t, Color.lightGray);
    }

    /**
     * Draw all the points on the shape
     *
     * @param g graphics context
     * @param t 2D affine transform
     * @param color color for the point
     */
    private void drawSelectionPoints (  Graphics g,
                                        AffineTransform t,
                                        Color color)
    {
        List<WhiteboardPoint> list = getSelectionPoints ();
        WhiteboardPoint point;

        for (int i = 0; i < list.size (); i++)
        {
            point = list.get (i);
            drawSelectedPoint (g, t, point, color);
        }
    }

    /**
     * Draw a point on the shape
     *
     * @param g graphics context
     * @param t 2D affine transform
     * @param point point coord for the 2D affine transform
     * @param color color for the point
     */
    public void drawSelectedPoint ( Graphics g,
                                    AffineTransform t,
                                    WhiteboardPoint point,
                                    Color color)
    {
        g = g.create();
        try
        {
            Point2D v0 =
                t.transform(new Point2D.Double(point.getX(), point.getY()),
                    null);

            int x = (int) v0.getX();
            int y = (int) v0.getY();

            AntialiasingManager.activateAntialiasing(g);

            g.setColor(new Color(color.getRed(), color.getGreen(), color
                .getBlue(), 160));

            g.fillOval(x - 5, y - 5, 10, 10);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Returns the list of selected points
     *
     * @return list of selected points
     */
    public abstract List<WhiteboardPoint> getSelectionPoints ();
    /**
     * Translates the shape
     *
     * @param deltaX x coord
     * @param deltaY y coord
     */
    public abstract void translate (double deltaX, double deltaY);

    /**
     * Translates the shape point at p
     *
     * @param deltaX x coord
     * @param deltaY y coord
     */
    public abstract void translateSelectedPoint (double deltaX, double deltaY);

    /**
     * Returns a String uniquely identifying this WhiteboardShape.
     *
     * @return a String that uniquely identifies this WhiteboardShape.
     */
    public String getID ()
    {
        return this.id;
    }

    /**
     * Sets a new identification for this WhiteboardShape
     *
     * @param id a String that uniquely identifies this WhiteboardShape.
     */
    public void setID (String id)
    {
        this.id = id;
    }

    /**
     * Returns an integer indicating the thickness (represented as number of
     * pixels) of this whiteboard shape (or its border).
     *
     * @return the thickness (in pixels) of this object (or its border).
     */
    public int getThickness ()
    {
        return this.thickness;
    }

    /**
     * Sets the thickness (in pixels) of this whiteboard shape.
     *
     * @param thickness the number of pixels that this object (or its border)
     * should be thick.
     */
    public void setThickness (int thickness)
    {
        this.thickness = thickness;
    }

    /**
     * Returns an integer representing the color of this object. The return
     * value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are red,
     * 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the color of this object.
     */
    public int getColor ()
    {
        return this.color.getRGB ();
    }

    /**
     * Sets the color of this whiteboard shape (or rather it's border). The
     * color parameter must be encoded with standard RGB encoding: bits 24-31
     * are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set on this object (using
     * standard RGB encoding).
     */
    public void setColor (int color)
    {
        this.color = Color.getColor ("",color);
    }


    /**
     * Indicates whether some other WhiteboardShape is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     * argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals (Object obj)
    {
        if(obj == null
          || !(obj instanceof WhiteboardShape))
            return false;
        if (obj == this
          || ((WhiteboardShape)obj).getID ().equals ( getID () ))
            return true;

        return false;
    }
    /**
     * Returns a selection point contained in this <tt>WhiteboardShape</tt>,
     * which corresponds to the given point (i.e. is in a near radius close
     * to it).
     *
     * @param p point to check
     * @return the nearest selection point
     */
    public abstract WhiteboardPoint getSelectionPoint (Point2D p);

}
