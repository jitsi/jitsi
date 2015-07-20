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
 * a WhiteboardShapeCircle, in XML :
 * <circle id="" cx="cx" cy="cy" r="l" />
 *
 * @author Julien Waechter
 */
public class WhiteboardShapeCircle
  extends WhiteboardShape implements WhiteboardObjectCircle
{
    /**
     * True is filled, false is unfilled.
     */
    private boolean fill;

    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint whiteboardPoint;

    /**
     * The background color of this object
     */
    private int backColor;

    /**
     * The number of pixels for the radius.
     */
    private double radius;

    /**
     * Stores all selection points for this shape.
     */
    private ArrayList<WhiteboardPoint> selectionPoints = new ArrayList<WhiteboardPoint>();

    /**
     * WhiteboardShapeCircle constructor
     *
     * @param id String that uniquely identifies this WhiteboardObject.
     * @param t number of pixels that this object (or its border)
     * should be thick.
     * @param c WhiteboardShape's color (or rather it's border).
     * @param whiteboardPoint coordinates of this object.
     * @param radius The number of pixels for the radius.
     * @param fill True is filled, false is unfilled.
     */
    public WhiteboardShapeCircle (String id, int t, Color c,
      WhiteboardPoint whiteboardPoint, double radius, boolean fill)
    {
        super (id);

        initShape(t, c, whiteboardPoint, radius, fill);
    }

    /**
     * WhiteboardShapeCircle constructor
     *
     * @param id String that uniquely identifies this WhiteboardObject.
     * @param thickness number of pixels that this object (or its border)
     * should be thick.
     * @param color WhiteboardShape's color (or rather it's border)
     * @param whiteboardPoint coordinates of this object.
     * @param radius The number of pixels for the radius.
     * @param fill True is filled, false is unfilled.
     * @param v2w 2D affine transform
     */
    public WhiteboardShapeCircle (  String id,
                                    int thickness,
                                    Color color,
                                    WhiteboardPoint whiteboardPoint,
                                    int radius,
                                    boolean fill,
                                    AffineTransform v2w)
    {
        super(id);

        Point2D v0 = new Point2D.Double (
          whiteboardPoint.getX (), whiteboardPoint.getY ());
        Point2D w0 = v2w.transform (v0, null);

        WhiteboardPoint point = new WhiteboardPoint (w0.getX (),w0.getY ());

        Point2D v1 = new Point2D.Double (
          whiteboardPoint.getX () + radius, whiteboardPoint.getY ());
        Point2D w1 = v2w.transform (v1, null);

        double r = w1.getX () - whiteboardPoint.getX ();

        initShape(thickness, color, point, r, fill);
    }

    /**
     * Initialize shape.
     *
     * @param thickness number of pixels that this object (or its border)
     * should be thick.
     * @param color WhiteboardShape's color (or rather it's border).
     * @param whiteboardPoint coordinates of this object.
     * @param radius The number of pixels for the radius.
     * @param fill True is filled, false is unfilled.
     */
    private void initShape( int thickness,
                            Color color,
                            WhiteboardPoint whiteboardPoint,
                            double radius,
                            boolean fill)
    {
        setThickness (thickness);
        setColor (color);
        setWhiteboardPoint (whiteboardPoint);
        setRadius (radius);
        setFill (fill);

        this.recalculateSelectionPoints();
    }

    /**
     * Returns the "bounding-box" of the circle
     *
     * @param w2v 2D affine transform
     * @return view rectangle
     */
    private int[] getViewRect (AffineTransform w2v)
    {
        double cx = getWhiteboardPoint ().getX ();
        double cy = getWhiteboardPoint ().getY ();
        double r = getRadius ();
        Point2D wx0 = new Point2D.Double (cx - r, cy);
        Point2D wy0 = new Point2D.Double (cx, cy - r);
        Point2D wx1 = new Point2D.Double (cx + r, cy);
        Point2D wy1 = new Point2D.Double (cx, cy + r);

        Point2D vx0 = w2v.transform (wx0, null);
        Point2D vy0 = w2v.transform (wy0, null);
        Point2D vx1 = w2v.transform (wx1, null);
        Point2D vy1 = w2v.transform (wy1, null);

        int ix = (int) vx0.getX ();
        int iy = (int) vy0.getY ();
        int iwidth = (int) vx1.getX () - ix;
        int iheight = (int) vy1.getY () - iy ;

        return new int[] {ix, iy, iwidth, iheight};
    }

    /**
     * Code to paint this WhiteboardShapeCircle
     *
     * @param g graphics context
     * @param t 2D affine transform
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        g.setStroke (new BasicStroke (this.getThickness (),
          BasicStroke.CAP_ROUND,BasicStroke.CAP_ROUND));
        int[] view = getViewRect (t);

        if (fill)
        {
            g.fillOval (view[0], view[1], view[2], view[3]);
        }
        else
        {
            g.drawOval (view[0], view[1], view[2], view[3]);
        }
    }

    /**
     * Tests if the shape contains a point
     *
     * @param p coord point
     * @return true if shape contains p
     */
    @Override
    public boolean contains (Point2D p)
    {
        double cx = getWhiteboardPoint ().getX ();
        double cy = getWhiteboardPoint ().getY ();
        double r = getRadius ();
        Ellipse2D ellipse = new Ellipse2D.Double (cx-r, cy-r, 2*r, 2*r);
        return ellipse.contains (p);
    }
    /**
     * Tests if a point p is on a selection point.
     *
     * @param p point
     * @return the nearest selection point
     */
    @Override
    public WhiteboardPoint getSelectionPoint (Point2D p)
    {
        WhiteboardPoint givenPoint = new WhiteboardPoint(p.getX(), p.getY());

        for (WhiteboardPoint point : selectionPoints)
            if (point.distance(givenPoint) < 10)
                return point;
        return null;
    }
    /**
     * Returns the list of selected points
     *
     * @return list of selected points
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        return selectionPoints;
    }
    /**
     * Translates the shape
     *
     * @param deltaX x coordinates
     * @param deltaY y coordinates
     */
    @Override
    public void translate (double deltaX, double deltaY)
    {
        double cx = getWhiteboardPoint ().getX ();
        double cy = getWhiteboardPoint ().getY ();

        this.getWhiteboardPoint().setX(cx + deltaX);
        this.getWhiteboardPoint().setY(cy + deltaY);

        this.recalculateSelectionPoints();
    }


    /**
     * Translates a point from the shape
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

        double cx = getWhiteboardPoint ().getX ();
        double cy = getWhiteboardPoint ().getY ();
        double r = getRadius ();

        if (modifyPoint.getX() == cx - r && modifyPoint.getY() == cy)
        {
            r -= deltaX;
            modifyPoint.setX(cx - r);
        }
        else if (modifyPoint.getX() == cx + r && modifyPoint.getY() == cy)
        {
            r += deltaX;
            modifyPoint.setX(cx + r);
        }
        else if (modifyPoint.getX() == cx && modifyPoint.getY() == cy - r)
        {
            r -= deltaY;
            modifyPoint.setY(cy - r);
        }
        else if (modifyPoint.getX() == cx && modifyPoint.getY() == cy + r)
        {
            r += deltaY;
            modifyPoint.setY(cy + r);
        }

        this.setRadius (r);

        this.setModifyPoint(modifyPoint);
        this.recalculateSelectionPoints();
    }

    /**
     * Returns the coordinates of this whiteboard object.
     *
     * @return the coordinates of this object.
     */
    public WhiteboardPoint getWhiteboardPoint ()
    {
        return whiteboardPoint;
    }

    /**
     * Sets the coordinates of this whiteboard object.
     *
     * @param whiteboardPoint the coordinates of this object.
     */
    public void setWhiteboardPoint (WhiteboardPoint whiteboardPoint)
    {
        this.whiteboardPoint = whiteboardPoint;
    }

    /**
     * Returns the radius (in pixels) of this whiteboard circle.
     *
     * @return the number of pixels for the radius.
     */
    public double getRadius ()
    {
        return this.radius;
    }

    /**
     * Sets the radius (in pixels) of this whiteboard circle.
     *
     * @param radius the number of pixels for the radius.
     */
    public void setRadius (double radius)
    {
        this.radius = radius;
    }

    /**
     * Returns the fill state of the WhiteboardObject.
     *
     * @return True is filled, false is unfilled.
     */
    public boolean isFill ()
    {
        return this.fill;
    }

    /**
     * Sets the fill state of the WhiteboardObject.
     * True is filled, false is unfilled.
     *
     * @param fill The new fill state.
     */
    public void setFill (boolean fill)
    {
        this.fill = fill;
    }

    /**
     * Specifies the background color for this object. The color parameter
     * must be encoded with standard RGB encoding: bits 24-31 are alpha, 16-23
     * are red, 8-15 are green, 0-7 are blue.
     *
     * @param backColor the color that we'd like to set for the background of this
     * <tt>WhiteboardObject</tt> (using standard RGB encoding).
     */
    public void setBackgroundColor (int backColor)
    {
        this.backColor = backColor;
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
        return this.backColor;
    }

    /**
     * Recalculates the selection points coordinates and adds the new selection
     * points to the list of selection points.
     */
    private void recalculateSelectionPoints()
    {
        selectionPoints.clear();

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX() - radius,
                                    whiteboardPoint.getY()));
        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX(),
                                    whiteboardPoint.getY() - radius));

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX() + radius,
                                    whiteboardPoint.getY()));

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX(),
                                    whiteboardPoint.getY() + radius));
    }
}
