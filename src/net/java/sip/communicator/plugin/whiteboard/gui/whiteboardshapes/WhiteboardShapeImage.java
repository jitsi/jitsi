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

import javax.swing.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 * a WhiteboardShapeCircle, in XML :
 * <image id="id" x="x" y="y" width="w" height="h">data</image>
 *
 * @author Julien Waechter
 */
public class WhiteboardShapeImage
  extends WhiteboardShape implements WhiteboardObjectImage
{
    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint whiteboardPoint;

    /**
     * The width value of this object (in pixel)
     */
    private double width;

    /**
     * The height value of this object (in pixel)
     */
    private double height;

    /**
     * The image that should be
     * displayed as the object background. (from the bytes binary array)
     */
    private Image image;

    /**
     * A binary array containing the image that should be
     * displayed as the object background.
     */
    private byte[] bytes;

    /**
     * Stores all selection points for this shape.
     */
    private ArrayList<WhiteboardPoint> selectionPoints = new ArrayList<WhiteboardPoint>();

    /**
     * WhiteboardShapImage constructor
     *
     * @param id String that uniquely identifies this WhiteboardObject.
     * @param p coordinates of this object.
     * @param width width value of this object (in pixel)
     * @param height height value of this object (in pixel)
     * @param bytes a binary array containing the image that should be
     * displayed as the object background.
     */
    public WhiteboardShapeImage (   String id,
                                    WhiteboardPoint p,
                                    double width,
                                    double height,
                                    byte[] bytes)
    {
        super (id);
        this.whiteboardPoint = p;
        this.width = width;
        this.height = height;
        this.bytes = bytes;
        ImageIcon ii = new ImageIcon (this.bytes);
        this.image = ii.getImage ();

        this.recalculateSelectionPoints();
    }

    /**
     * Code to paint this specific shape.
     *
     * @param g graphics context
     * @param t 2D affine transform
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        Point2D w0 = new Point2D.Double (   whiteboardPoint.getX (),
                                            whiteboardPoint.getY ());

        Point2D v0 = t.transform (w0, null);
        int ix = (int) v0.getX ();
        int iy = (int) v0.getY ();

        Point2D w1 = new Point2D.Double (   whiteboardPoint.getX () + width,
                                            whiteboardPoint.getY () + height);
        Point2D v1 = t.transform (w1, null);

        int iwidth = (int)v1.getX () - ix;
        int iheight = (int)v1.getY () - iy;

        if (image != null)
        {
            g.drawImage (image, ix, iy, iwidth, iheight, null);
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
        double x = whiteboardPoint.getX ();
        double y = whiteboardPoint.getY ();
        Rectangle2D rect = new Rectangle2D.Double (x, y, width, height);
        return rect.contains (p);
    }

    /**
     * Returns the width (in pixels) of the WhiteboardObject.
     *
     * @return The width.
     */
    public double getWidth ()
    {
        return width;
    }

    /**
     * Sets the width (in pixels) of the WhiteboardObject.
     *
     * @param width The new width.
     */
    public void setWidth (double width)
    {
        this.width = width;
    }

    /**
     * Returns the height (in pixels) of the WhiteboardObject.
     *
     * @return The height.
     */
    public double getHeight ()
    {
        return height;
    }

    /**
     * Sets the width (in pixels) of the WhiteboardObject.
     *
     * @param height The new height.
     */
    public void setHeight (double height)
    {
        this.height = height;
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
        double cx = whiteboardPoint.getX ();
        double cy = whiteboardPoint.getY ();

        this.getWhiteboardPoint().setX(cx + deltaX);
        this.getWhiteboardPoint().setY(cy + deltaY);

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

        double x = whiteboardPoint.getX ();
        double y = whiteboardPoint.getY ();

        if (modifyPoint.getX() == x && modifyPoint.getY() == y)
        {
            this.whiteboardPoint.setX(x + deltaX);
            this.whiteboardPoint.setY(y + deltaY);
            this.width -= deltaX;
            this.height -= deltaY;

            modifyPoint.setX(x + deltaX);
            modifyPoint.setY(y + deltaY);
        }
        else if (modifyPoint.getX() == x + width && modifyPoint.getY() == y)
        {
            this.whiteboardPoint.setY(y + deltaY);
            this.width += deltaX;
            this.height -= deltaY;

            modifyPoint.setX(x + width);
            modifyPoint.setY(y + deltaY);
        }
        else if (modifyPoint.getX() == x && modifyPoint.getY() == y + height)
        {
            this.whiteboardPoint.setX(x + deltaX);
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
     * Specifies an image that should be displayed as the background of this
     * object.
     *
     * @param background a binary array containing the image that should be
     * displayed as the object background.
     */
    public void setBackgroundImage (byte[] background)
    {
        this.bytes = background;
        ImageIcon ii = new ImageIcon (this.bytes);
        this.image = ii.getImage ();
    }

    /**
     * Returns a binary array containing the image that should be displayed as
     * the background of this <tt>WhiteboardObject</tt>.
     *
     * @return a binary array containing the image that should be displayed as
     * the object background.
     */
    public byte[] getBackgroundImage ()
    {
        return bytes;
    }

    /**
     * Recalculates the selection points coordinates and adds the new selection
     * points to the list of selection points.
     */
    private void recalculateSelectionPoints()
    {
        selectionPoints.clear();

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX(),
                                    whiteboardPoint.getY()));

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX() + width,
                                    whiteboardPoint.getY()));

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX(),
                                    whiteboardPoint.getY() + height));

        selectionPoints.add (
            new WhiteboardPoint (   whiteboardPoint.getX() + width,
                                    whiteboardPoint.getY() + height));
    }
}
