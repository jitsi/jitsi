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
 * A WhiteboardShapeRect, in XML :
 * <rect x="0" y="0" width="100" height="100" />
 *
 * @author Julien Waechter
 */
public class WhiteboardShapeText
  extends WhiteboardShape implements WhiteboardObjectText
{
    /**
     * A type string constant indicating the default font size
     * of the text in this object.
     */
    public static int DEFAULT_FONT_SIZE = 18;
    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint point;
    /**
     * The WhiteboardShapeText's text size
     */
    private int fontSize;
    /**
     * The WhiteboardShapeText's font name
     */
    private String fontName;
    /**
     * The WhiteboardShapeText's text
     */
    private String text;
    /**
     * The advance width of the text
     * in the Font described by this FontMetrics.
     */
    private int textWidth;
    /**
     * Distance from the font's baseline to the top of most
     * alphanumeric characters.
     */
    private int textActualHeight;

    /**
     * WhiteboardShapeText constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param color font's color
     * @param point coordinates of this object
     * @param size font size
     * @param text WhiteboardObjectText's text
     */
    public WhiteboardShapeText (String id,
                                Color color,
                                WhiteboardPoint point,
                                int size,
                                String text)
    {
        super (id);

        this.initShape(color, point, size, text);
    }

    /**
     * WhiteboardShapeText constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param color font's color
     * @param point coordinates of this object.
     * @param size font size
     * @param text WhiteboardObjectText's text
     * @param v2w 2D affine transformation
     */
    public WhiteboardShapeText (String id,
                                Color color,
                                WhiteboardPoint point,
                                int size,
                                String text,
                                AffineTransform v2w)
    {
        super (id);

        Point2D v0 = new Point2D.Double (point.getX (), point.getY ());
        Point2D w0 = v2w.transform (v0, null);

        double x = w0.getX ();
        double y = w0.getY ();

        point.setX(x);
        point.setY(y);

        this.initShape(color, point, size, text);
    }

    /**
     * WhiteboardShapeText constructor.
     *
     * @param color font's color
     * @param point coordinates of this object
     * @param size font size
     * @param text WhiteboardObjectText's text
     */
    private void initShape (Color color,
                            WhiteboardPoint point,
                            int size,
                            String text)
    {
        this.setColor (color);
        this.setWhiteboardPoint (point);

        this.setFontSize (size);
        this.setFontName ("Dialog");
        this.setText (text);
    }

    /**
     * Code to paint the WhiteboardShapeText.
     *
     * @param g graphics context
     * @param t 2D affine transform
     */
    @Override
    public void paintShape (Graphics2D g, AffineTransform t)
    {
        g.setFont (getFont ());
        Point2D p = t.transform (
          new Point2D.Double (point.getX (), point.getY ()), null);
        g.drawString (text, (int) p.getX (), (int) p.getY ());

        FontMetrics fontMetrics = g.getFontMetrics ();
        textWidth = fontMetrics.stringWidth (text);
        textActualHeight = fontMetrics.getAscent () - fontMetrics.getDescent ();
    }

    /**
     * Gets the list of selected points.
     *
     * @return list of selected points
     */
    @Override
    public List<WhiteboardPoint> getSelectionPoints ()
    {
        List<WhiteboardPoint> list = new ArrayList<WhiteboardPoint>();
        list.add (point);

        return list;
    }


    /**
     * Returns the WhiteboardObjectText's text.
     *
     * @return the WhiteboardObjectText's text.
     */
    public String getText ()
    {
        return text;
    }

    /**
     * Sets the WhiteboardObjectText's text.
     *
     * @param text the new WhiteboardObjectText's text.
     */
    public void setText (String text)
    {
        this.text = text;
    }

    /**
     * Returns the current WhiteboardObjectText's font.
     *
     * @return the WhiteboardObjectText's font.
     */
    private Font getFont ()
    {
        return new Font (getFontName (), Font.BOLD, getFontSize ());
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
          point.getX (), point.getY () - textActualHeight,
          textWidth, textActualHeight);
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

        if(getModifyPoint().equals (point))
        {
            this.point = new WhiteboardPoint (  point.getX() + deltaX,
                                                point.getY() + deltaY);

            this.setModifyPoint(point);
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

        if(point.distance (givenPoint) < 18)
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
     * Returns the WhiteboardObjectText's font size.
     *
     * @return the WhiteboardObjectText's font size.
     */
    public int getFontSize ()
    {
        return this.fontSize;
    }

    /**
     * Sets the WhiteboardObjectText's font size.
     *
     * @param fontSize the new WhiteboardObjectText's font size.
     */
    public void setFontSize (int fontSize)
    {
        this.fontSize = fontSize;
    }

    /**
     * Returns the WhiteboardObjectText's font name.
     * (By default Dialog)
     *
     * @return the new WhiteboardObjectText's font name.
     */
    public String getFontName ()
    {
        return this.fontName;
    }

    /**
     * Sets the WhiteboardObjectText's font name.
     *
     * @param fontName the new WhiteboardObjectText's font name.
     */
    public void setFontName (String fontName)
    {
        this.fontName = fontName;
    }
}
