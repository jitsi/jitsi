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
 * A WhiteboardShapeLine, in XML :
 * <polyline id="" points="0,0 1,1 2,5"/>
 *
 * @author Julien Waechter
 */
public class WhiteboardShapePolyLine
  extends WhiteboardShape implements WhiteboardObjectPolyLine
{

    /**
     * list of WhiteboardPoint
     */
    private List<WhiteboardPoint> points;
    /**
     * True is filled, false is unfilled.
     */
    private boolean fill = false;

    /**
     * WhiteboardShapePolyLine constructor.
     *
     * @param id  String that uniquely identifies this WhiteboardObject
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapePolyLine's color (or rather it's border)
     * @param points list of WhiteboardPoint
     * @param fill True is filled, false is unfilled
     */
    public WhiteboardShapePolyLine (String id,
                                    int thickness,
                                    Color color,
                                    List<WhiteboardPoint> points,
                                    boolean fill)
    {
        super (id);

        this.initShape(thickness, color, points, fill);
    }

    /**
     * WhiteboardShapePolyLine constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapePolyLine's color (or rather it's border)
     * @param m_points list of WhiteboardPoint
     * @param fill True is filled, false is unfilled
     * @param at 2D affine transformation
     */
    public WhiteboardShapePolyLine (String id,
                                    int thickness,
                                    Color color,
                                    List<WhiteboardPoint> m_points,
                                    boolean fill,
                                    AffineTransform at)
    {
        super (id);

        ArrayList<WhiteboardPoint> pointsList = new ArrayList<WhiteboardPoint>();

        WhiteboardPoint p;
        for (int i = 0; i<m_points.size ();i++)
        {
            p = m_points.get (i);
            Point2D w = at.transform (
              new Point2D.Double (p.getX (), p.getY ()), null);
            pointsList.add (new WhiteboardPoint (w.getX (), w.getY ()));
        }

        this.initShape(thickness, color, pointsList, fill);
    }

    /**
     * Initializes this shape.
     *
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardShapePolyLine's color (or rather it's border)
     * @param points list of WhiteboardPoint
     * @param fill True is filled, false is unfilled
     */
    private void initShape (int thickness,
                            Color color,
                            List<WhiteboardPoint> points,
                            boolean fill)
    {
        this.setThickness (thickness);
        this.setColor (color);

        // need to clone because passed by reference
        this.points = new ArrayList<WhiteboardPoint>(points);
        this.fill = fill;
    }

    /**
     * Returns the fill state of the WhiteboardObject.
     *
     * @return True is filled, false is unfilled.
     */
    public boolean isFill ()
    {
        return fill;
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
     * Code to paint the WhiteboardShapePolyLine.
     *
     * @param g graphics context
     * @param t 2D affine transform
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        g.setStroke (new BasicStroke (this.getThickness (),
          BasicStroke.CAP_ROUND,BasicStroke.CAP_ROUND));
        if (fill)
        {
            g.fill (createPoly (t));
        }
        else
        {
            g.draw (createPoly (t));
        }
    }

    /**
     * Returns the list of selected WhiteboardPoints
     *
     * @return list of selected WhiteboardPoints
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        List<WhiteboardPoint> list = new ArrayList<WhiteboardPoint>();
        WhiteboardPoint p;
        for(int i =0; i< points.size (); i++)
        {
            p = points.get (i);
            list.add (new WhiteboardPoint (p.getX (), p.getY ()));
        }
        return list;
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
        return createPolyWorld ().contains (p);
    }

    /**
     * Creates a GeneralPath with all the WhiteboardPoint.
     * This GeneralPath is used for display.
     *
     * @param w2v 2D affine transform
     * @return a GeneralPath generated with all the WhiteboardPoint.
     */
    private GeneralPath createPoly (AffineTransform w2v)
    {
        GeneralPath polyline = new GeneralPath (
          GeneralPath.WIND_EVEN_ODD, points.size ());
        if(points.size ()<=0)
            return polyline;
        WhiteboardPoint start = points.get (0);
        Point2D w = new Point2D.Double (start.getX (), start.getY ());
        Point2D v = w2v.transform (w, null);
        polyline.moveTo ((int) v.getX (), (int) v.getY ());
        WhiteboardPoint p;
        for (int i =0; i<points.size ();i++)
        {
            p = points.get (i);
            w = new Point2D.Double (p.getX (), p.getY ());
            v = w2v.transform (w, null);
            polyline.lineTo ((int) v.getX (), (int) v.getY ());
        }

        return polyline;
    }

    /**
     * Creates a GeneralPath with all the WhiteboardPoint.
     * This GeneralPath is used for the contains test.
     *
     * @return a GeneralPath generated with all the WhiteboardPoint.
     */
    private GeneralPath createPolyWorld ()
    {
        GeneralPath polyline = new GeneralPath (
          GeneralPath.WIND_EVEN_ODD, points.size ());
        if(points.size ()<=0)
            return polyline;
        WhiteboardPoint start = points.get (0);
        polyline.moveTo ((float) start.getX (), (float) start.getY ());

        WhiteboardPoint p;
        for (int i =0; i<points.size ();i++)
        {
            p = points.get (i);
            polyline.lineTo ((float) p.getX (), (float) p.getY ());
        }

        return polyline;
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
     * Sets the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @param points the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     */
    public void setPoints (List<WhiteboardPoint> points)
    {
        this.points = new ArrayList<WhiteboardPoint> (points);
    }

    /**
     * Translates the shape.
     *
     * @param deltaX x coord
     * @param deltaY y coord
     */
    @Override
    public void translate (double deltaX, double deltaY)
    {
        WhiteboardPoint point ;
        for (int i =0; i<points.size ();i++)
        {
            point = points.get (i);
            points.set (i,
              new WhiteboardPoint(point.getX () + deltaX,
              point.getY () + deltaY));
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
        WhiteboardPoint point;
        for (int i = 0; i < points.size (); i++)
        {
            point = points.get (i);
            if(( new Point2D.Double (
              point.getX (),  point.getY ())).distance (p) < 18)
            {
                return point;
            }
        }
        return null;
    }

}
