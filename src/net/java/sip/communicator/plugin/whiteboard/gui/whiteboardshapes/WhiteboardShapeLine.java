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
 *
 * a WhiteboardShapeLine, in XML :
 * <line id="" x1="x1" y1="y1" x2="x2" y2="y2"/>
 *
 * @author Julien Waechter
 */
public class WhiteboardShapeLine
  extends WhiteboardShape implements WhiteboardObjectLine
{
    /**
     * The start coordinates for this line.
     */
    private WhiteboardPoint startPoint;
    /**
     * The end coordinates for this line.
     */
    private WhiteboardPoint endPoint;

    /**
     * WhiteboardShapeLine constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject.
     * @param t number of pixels that this object (or its border)
     * should be thick.
     * @param c WhiteboardShapeLine's color (or rather it's border)
     * @param startPoint the start coordinates of this line.
     * @param endPoint the end coordinates of this line.
     */
    public WhiteboardShapeLine (String id, int t, Color c,
      WhiteboardPoint startPoint, WhiteboardPoint endPoint)
    {
        super (id);
        this.setThickness (t);
        setColor (c);
        setColor (c.getRGB ());
        this.endPoint = endPoint;
        this.startPoint = startPoint;
    }

    /**
     * WhiteboardShapeLine constructor
     * @param id String that uniquely identifies this WhiteboardObject.
     * @param t  number of pixels that this object (or its border)
     * @param c WhiteboardShapeLine's color (or rather it's border)
     * @param startPoint the start coordinates of this line.
     * @param endPoint the end coordinates of this line.
     * @param v2w 2D affine transform
     */
    public WhiteboardShapeLine (String id, int t, Color c,
      WhiteboardPoint startPoint, WhiteboardPoint endPoint, AffineTransform v2w)
    {
        super (id);
        this.setThickness (t);
        setColor (c);

        Point2D v0 = new Point2D.Double (startPoint.getX (), startPoint.getY ());
        Point2D w0 = v2w.transform (v0, null);

        this.startPoint = new WhiteboardPoint (w0.getX (), w0.getY ());

        Point2D v1 = new Point2D.Double (endPoint.getX (), endPoint.getY ());
        Point2D w1 = v2w.transform (v1, null);

        this.endPoint = new WhiteboardPoint (w1.getX (), w1.getY ());
    }

    /**
     * Code to paint the specific shape
     * @param w2v 2D affine transform
     * @param g graphics context
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform w2v)
    {
        double x = startPoint.getX ();
        double y = startPoint.getY ();
        double xEnd = endPoint.getX ();
        double yEnd = endPoint.getY ();

        g.setStroke (new BasicStroke (this.getThickness (),
          BasicStroke.CAP_ROUND,BasicStroke.CAP_ROUND));

        Point2D v0 = w2v.transform (new Point2D.Double (x, y), null);
        int ix = (int) v0.getX ();
        int iy = (int) v0.getY ();

        Point2D v1 = w2v.transform (new Point2D.Double (xEnd, yEnd), null);

        g.drawLine (ix, iy, (int)v1.getX (), (int)v1.getY ());
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
        double x = startPoint.getX ();
        double y = startPoint.getY ();

        double xEnd = endPoint.getX ();
        double yEnd = endPoint.getY ();

        Line2D line = new Line2D.Double (x, y, xEnd, yEnd);

        return line.intersects (p.getX (), p.getY (), 10, 10);
    }

    /**
     * Returns the list of selected points.
     *
     * @return list of selected points
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        ArrayList<WhiteboardPoint> selectionPoints = new ArrayList<WhiteboardPoint>();

        selectionPoints.add (startPoint);
        selectionPoints.add (endPoint);

        return selectionPoints;
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
        double x = startPoint.getX ();
        double y = startPoint.getY ();
        double xEnd = endPoint.getX ();
        double yEnd = endPoint.getY ();
        x += deltaX;
        xEnd += deltaX;
        y += deltaY;
        yEnd += deltaY;
        startPoint = new WhiteboardPoint (x, y);
        endPoint = new WhiteboardPoint (xEnd, yEnd);
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

        if (getModifyPoint().equals(startPoint))
        {
            startPoint.setX (startPoint.getX() + deltaX);
            startPoint.setY (startPoint.getY() + deltaY);

            this.setModifyPoint(startPoint);
        }
        else if (getModifyPoint().equals(endPoint))
        {
            endPoint.setX (endPoint.getX() + deltaX);
            endPoint.setY (endPoint.getY() + deltaY);

            this.setModifyPoint(endPoint);
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
        WhiteboardPoint givenPoint = new WhiteboardPoint(p.getX(), p.getY());

        if(startPoint.distance (givenPoint) < 10)
            return startPoint;
        else if(endPoint.distance (givenPoint) < 10)
            return endPoint;

        return null;
    }

    /**
     * Returns the coordinates of  start point for the line
     *
     * @return the start coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointStart ()
    {
        return this.startPoint;
    }

    /**
     * Returns the coordinates of  end point for the line
     *
     * @return the end coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointEnd ()
    {
        return this.endPoint;
    }

    /**
     * Sets the coordinates of start point for the line
     *
     * @param whiteboardPointStart the new start coordinates for this line.
     */
    public void setWhiteboardPointStart (WhiteboardPoint whiteboardPointStart)
    {
        this.startPoint = whiteboardPointStart;
    }

    /**
     * Sets the coordinates of end point for the line
     *
     * @param whiteboardPointEnd the new end coordinates for this line.
     */
    public void setWhiteboardPointEnd (WhiteboardPoint whiteboardPointEnd)
    {
        this.endPoint = whiteboardPointEnd;
    }
}
