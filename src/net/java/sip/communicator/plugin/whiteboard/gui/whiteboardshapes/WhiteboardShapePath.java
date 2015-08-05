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
 * A WhiteboardShapePath, in XML :
 * <path d="M250 150 L150 350 L350 350 Z" />
 *
 * @author Julien Waechter
 */
public class WhiteboardShapePath
  extends WhiteboardShape implements WhiteboardObjectPath
{

    /**
     * List of WhiteboardPoint
     */
    private ArrayList<WhiteboardPoint> points;

    /**
     * WhiteboardShapePath constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param t number of pixels that this object (its border)
     * should be thick.
     * @param c WhiteboardShapePath's color (or rather it's border)
     * @param points list of WhiteboardPoint.
     */
    public WhiteboardShapePath (String id, int t, Color c, List<WhiteboardPoint> points)
    {
        super (id);
        this.setThickness (t);
        setColor (c.getRGB ());
        this.points = new ArrayList<WhiteboardPoint>(points);
    }

    /**
     * WhiteboardShapePath constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param t number of pixels that this object (its border)
     * @param c WhiteboardShapePath's color (it's border)
     * @param points list of points
     * @param v2w 2D affine transform
     */
    public WhiteboardShapePath (String id, int t, Color c,
      List<WhiteboardPoint> points, AffineTransform v2w)
    {
        super (id);
        this.setThickness (t);
        setColor (c.getRGB ());

        this.points = new ArrayList<WhiteboardPoint>();
        for (WhiteboardPoint p : points)
        {
            Point2D w = v2w.transform (
              new Point2D.Double (p.getX (), p.getY ()), null);
            this.points.add (new WhiteboardPoint (w.getX (), w.getY ()));
        }
    }

    /**
     * Returns a list of all the <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @return the list of <tt>WhiteboardPoint</tt>s composing this object.
     */
    public List<WhiteboardPoint> getPoints ()
    {
        return points;
    }

    /**
     * Returns the list of selected points.
     *
     * @return list of selected points
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        List<WhiteboardPoint> list = new ArrayList<WhiteboardPoint>();

        for(WhiteboardPoint p : points)
            list.add (new WhiteboardPoint (p.getX (), p.getY ()));
        return list;
    }

    /**
     * Code to paint the specific shape
     * @param g graphics context
     * @param t 2D affine transform
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        g.setStroke (new BasicStroke (this.getThickness (),
          BasicStroke.CAP_ROUND,BasicStroke.CAP_ROUND));
        double startX = -1;
        double startY = -1;
        int size = points.size ();
        for (int i = 0; i < size; i++)
        {
            WhiteboardPoint point = points.get (i);
            Point2D p0 = t.transform (
              new Point2D.Double (startX, startY), null);
            Point2D p1 = t.transform (
              new Point2D.Double (point.getX (), point.getY ()), null);

            int x0 = (int) p0.getX ();
            int y0 = (int) p0.getY ();
            int x1 = (int) p1.getX ();
            int y1 = (int) p1.getY ();

            if (i > 0)
            {
                g.drawLine (x0, y0, x1, y1);
                startX = point.getX ();
                startY = point.getY ();
            }
            startX = point.getX ();
            startY = point.getY ();
        }
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
        double startX = -1;
        double startY = -1;
        int size = points.size ();
        for (int i = 0; i < size; i++)
        {
            WhiteboardPoint point = points.get (i);

            if (i > 0)
            {
                Line2D line = new Line2D.Double (
                  startX, startY, point.getX (), point.getY ());
                if (line.intersects (p.getX (), p.getY (), 1, 1))
                {
                    return true;
                }
                startX = point.getX ();
                startY = point.getY ();
            }
            startX = point.getX ();
            startY = point.getY ();
        }
        return false;
    }

    /**
     * Sets the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @param points the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     */
    public void setPoints (List<WhiteboardPoint> points)
    {
        this.points = new ArrayList<WhiteboardPoint>(points);
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
        WhiteboardPoint point;
        for (int i = 0; i< points.size ();i++)
        {
            point = points.get (i);

            points.set (i, new WhiteboardPoint (
              point.getX () + deltaX, point.getY () + deltaY));
        }
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
        if (getModifyPoint() == null)
            return;

        WhiteboardPoint point;

        for (int i = 0; i < points.size (); i++)
        {
            point = points.get (i);

            if(getModifyPoint().equals(point))
            {
                WhiteboardPoint newPoint
                    = new WhiteboardPoint (
                        point.getX () + deltaX, point.getY () + deltaY);

                points.set (i, newPoint);

                this.setModifyPoint(newPoint);
            }
        }
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
        for (WhiteboardPoint point : points)
            if((new Point2D.Double (point.getX (),  point.getY ())).distance (p)
                    < 18)
                return point;
        return null;
    }
}
