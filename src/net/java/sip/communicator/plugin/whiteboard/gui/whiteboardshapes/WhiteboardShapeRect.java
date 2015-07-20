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
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 * a WhiteboardShapeRect, in XML :
 * <rect x="0" y="0" width="100" height="100" />
 *
 * @author Julien Waechter
 */
public class WhiteboardShapeRect
  extends WhiteboardShape implements WhiteboardObjectRect
{
    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint point;

    /**
     * The width value of this object (in pixel)
     */
    private double width;

    /**
     * The height value of this object (in pixel)
     */
    private double height;

    /**
     * True is filled, false is unfilled.
     */
    private boolean fill = false;

    /**
     * The background color of this object
     */
    private Color backgroundColor;

    /**
     * Stores all selection points for this shape.
     */
    private List<WhiteboardPoint> selectionPoints
        = new ArrayList<WhiteboardPoint>();

    /**
     * WhiteboardShapeRect constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapeRect's color (or rather it's border)
     * @param point coordinates of this object.
     * @param width width value of this object (in pixel)
     * @param height height value of this object (in pixel)
     * @param fill True is filled, false is unfilled
     */
    public WhiteboardShapeRect (String id,
                                int thickness,
                                Color color,
                                WhiteboardPoint point,
                                double width,
                                double height,
                                boolean fill)
    {
        super (id);

        this.initShape(thickness, color, point, width, height, fill);
    }

    /**
     * WhiteboardShapeRect constructor.
     *
     * @param id String that uniquely identifies this WhiteboardShapeRect
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapeRect's color (or rather it's border)
     * @param point coordinates of this object.
     * @param width width value of this object (in pixel)
     * @param height height value of this object (in pixel)
     * @param fill True is filled, false is unfilled
     * @param transform 2D affine transformation
     */
    public WhiteboardShapeRect (String id,
                                int thickness,
                                Color color,
                                WhiteboardPoint point,
                                double width,
                                double height,
                                boolean fill,
                                AffineTransform transform)
    {
        super (id);

        Point2D v0 = new Point2D.Double (point.getX (), point.getY ());
        Point2D w0 = transform.transform (v0, null);

        double x = w0.getX ();
        double y = w0.getY ();

        point.setX(x);
        point.setY(y);

        Point2D v1 = new Point2D.Double (x + width, y + height);
        Point2D w1 = transform.transform (v1, null);

        double transformedWidth = w1.getX () - x;
        double transformedHeight = w1.getY () - y;

        this.initShape(thickness, color, point,
            transformedWidth, transformedHeight, fill);
    }

    /**
     * Initializes this shape.
     *
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapeRect's color (or rather it's border)
     * @param point coordinates of this object.
     * @param width width value of this object (in pixel)
     * @param height height value of this object (in pixel)
     * @param fill True is filled, false is unfilled
     */
    private void initShape (int thickness,
                            Color color,
                            WhiteboardPoint point,
                            double width,
                            double height,
                            boolean fill)
    {
        this.setThickness (thickness);
        this.setColor (color.getRGB ());
        this.setWhiteboardPoint (point);
        this.setWidth (width);
        this.setHeight (height);
        this.setFill (fill);

        this.recalculateSelectionPoints();
    }

    /**
     * Gets the height (in pixels) of the WhiteboardShapeRect.
     *
     * @return The height.
     */
    public double getHeight ()
    {
        return height;
    }

    /**
     * Sets the width (in pixels) of the WhiteboardShapeRect.
     *
     * @param height The new height.
     */
    public void setHeight (double height)
    {
        this.height = height;
    }

    /**
     * Returns the fill state of the WhiteboardShapeRect.
     *
     * @return True is filled, false is unfilled.
     */
    public boolean isFill ()
    {
        return fill;
    }

    /**
     * Sets the fill state of the WhiteboardShapeRect.
     * True is filled, false is unfilled.
     *
     * @param fill The new fill state.
     */
    public void setFill (boolean fill)
    {
        this.fill = fill;
    }

    /**
     * Code to paint the WhiteboardShapeRect.
     *
     * @param g graphics context
     * @param t 2D affine transformation
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        double x = point.getX ();
        double y = point.getY ();

        g.setStroke (new BasicStroke (this.getThickness (),
          BasicStroke.CAP_ROUND,BasicStroke.CAP_ROUND));

        Point2D w0 = new Point2D.Double (x, y);
        Point2D v0 = t.transform (w0, null);

        int x0 = (int) v0.getX ();
        int y0 = (int) v0.getY ();

        Point2D w1 = new Point2D.Double (x + width, y + height);
        Point2D v1 = t.transform (w1, null);

        int xWidth = (int)v1.getX () - x0;
        int yHeight = (int)v1.getY () - y0;

        g.setColor (Color.getColor ("",this.getColor ()));
        if (fill)
        {
            g.fillRect (x0, y0, xWidth, yHeight);
        }
        else
        {
            g.drawRect (x0, y0, xWidth, yHeight);
        }
    }

    /**
     * Returns the list of selected points.
     *
     * @return list of selected points
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        return selectionPoints;
    }
    /**
     * Tests if the shape contains a point.
     *
     * @param p coord point
     * @return true if shape contains p
     */
    @Override
    public boolean contains (Point2D p)
    {
        Rectangle2D rect = new Rectangle2D.Double (
          point.getX (), point.getY (), width, height);
        return rect.contains (p);
    }


    /**
     * Translates the shape.
     *
     * @param deltaX x coordinate
     * @param deltaY y coordinate
     */
    @Override
    public void translate (double deltaX, double deltaY)
    {
        double x = point.getX ();
        double y = point.getY ();

        x += deltaX;
        y += deltaY;

        this.point = new WhiteboardPoint (x, y);

        this.recalculateSelectionPoints();
    }

    /**
     * Translates a point from the shape.
     *
     * @param deltaX x coordinate
     * @param deltaY y coordinate
     */
    @Override
    public void translateSelectedPoint (double deltaX, double deltaY)
    {
        WhiteboardPoint modifyPoint = getModifyPoint();

        if (modifyPoint == null)
            return;

        double x = point.getX ();
        double y = point.getY ();

        if (modifyPoint.getX() == x && modifyPoint.getY() == y)
        {
            this.point.setX(x + deltaX);
            this.point.setY(y + deltaY);
            this.width -= deltaX;
            this.height -= deltaY;

            modifyPoint.setX(x + deltaX);
            modifyPoint.setY(y + deltaY);
        }
        else if (modifyPoint.getX() == x + width && modifyPoint.getY() == y)
        {
            this.point.setY(y + deltaY);
            this.width += deltaX;
            this.height -= deltaY;

            modifyPoint.setX(x + width);
            modifyPoint.setY(y + deltaY);
        }
        else if (modifyPoint.getX() == x && modifyPoint.getY() == y + height)
        {
            this.point.setX(x + deltaX);
            this.width -= deltaX;
            this.height += deltaY;

            modifyPoint.setX(x + deltaX);
            modifyPoint.setY(y + height);
        }
        else if (modifyPoint.getX() == x + width
                    && modifyPoint.getY() == y + height)
        {
            this.width += deltaX;
            this.height += deltaY;

            modifyPoint.setX(x + width);
            modifyPoint.setY(y + height);
        }

        this.setModifyPoint(modifyPoint);
        this.recalculateSelectionPoints();
    }

    /**
     * Tests if a point p is over a selection point.
     *
     * @param p point
     * @return nearest selection point
     */
    @Override
    public WhiteboardPoint getSelectionPoint (Point2D p)
    {
        WhiteboardPoint givenPoint = new WhiteboardPoint(p.getX(), p.getY());

        for (WhiteboardPoint point : selectionPoints)
            if (point.distance(givenPoint) < 18)
                return point;
        return null;
    }

    /**
     * Returns the coordinates of this whiteboard object.
     *
     * @return the coordinates of this object.
     */
    public WhiteboardPoint getWhiteboardPoint ()
    {
        return this.point;
    }

    /**
     * Sets the coordinates of this whiteboard object.
     *
     * @param whiteboardPoint the coordinates of this object.
     */
    public void setWhiteboardPoint (WhiteboardPoint whiteboardPoint)
    {
        this.point = whiteboardPoint;
    }

    /**
     * Gets the width (in pixels) of the WhiteboardObject.
     *
     * @return The width.
     */
    public double getWidth ()
    {
        return this.width;
    }

    /**
     * Sets the width (in pixels) of the WhiteboardObject.
     *
     * @param width The new width.
     */
    public void setWidth (double width)
    {
        this.width  = width;
    }

    /**
     * Specifies the background color for this object. The color parameter
     * must be encoded with standard RGB encoding: bits 24-31 are alpha, 16-23
     * are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set for the background of this
     * <tt>WhiteboardObject</tt> (using standard RGB encoding).
     */
    public void setBackgroundColor (int color)
    {
        this.backgroundColor = Color.getColor ("", color);
    }

    /**
     * Returns an integer representing the background color of this object. The
     * return value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are
     * red, 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the background color of this object.
     */
    public int getBackgroundColor ()
    {
        return this.backgroundColor.getRGB ();
    }

    /**
     * Recalculates the selection points coordinates and adds the new selection
     * points to the list of selection points.
     */
    private void recalculateSelectionPoints()
    {
        selectionPoints.clear();

        selectionPoints.add (
            new WhiteboardPoint (   point.getX(),
                                    point.getY()));

        selectionPoints.add (
            new WhiteboardPoint (   point.getX() + width,
                                    point.getY()));

        selectionPoints.add (
            new WhiteboardPoint (   point.getX(),
                                    point.getY() + height));

        selectionPoints.add (
            new WhiteboardPoint (   point.getX() + width,
                                    point.getY() + height));
    }
}
