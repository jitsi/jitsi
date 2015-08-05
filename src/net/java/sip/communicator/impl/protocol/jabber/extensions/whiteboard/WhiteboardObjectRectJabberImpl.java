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

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import java.awt.*;
import java.io.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 *  WhiteboardObjectRectJabberImpl
 * <p>
 * WhiteboardObjectRectJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectRectJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectRectJabberImpl
  extends WhiteboardObjectJabberImpl implements WhiteboardObjectRect
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectRectJabberImpl.class);

    /**
     * The height value of this object (in pixel)
     */
    private double height;
    /**
     * The width value of this object (in pixel)
     */
    private double width;
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
     * Default WhiteboardObjectRectJabberImpl constructor.
     */
    public WhiteboardObjectRectJabberImpl ()
    {
        super ();
    }

    /**
     * WhiteboardObjectRectJabberImpl constructor.
     *
     * @param id String that uniquely identifies this WhiteboardObject
     * @param thickness number of pixels that this object (or its border)
     * should be thick
     * @param color WhiteboardObjectRectJabberImpl's color (or rather it's border)
     * @param backColor background color of this WhiteboardObjectRectJabberImpl
     * @param whiteboardPoint coordinates of this object.
     * @param width width value of this object (in pixel)
     * @param height height value of this object (in pixel)
     * @param fill  True is filled, false is unfilled
     */
    public WhiteboardObjectRectJabberImpl ( String id,
                                            int thickness,
                                            int color,
                                            int backColor,
                                            WhiteboardPoint whiteboardPoint,
                                            double width,
                                            double height,
                                            boolean fill)
    {
        super (id,thickness,color);
        this.setBackgroundColor (backColor);
        this.setWhiteboardPoint (whiteboardPoint);
        this.setWidth (width);
        this.setHeight (height);
        this.setFill (fill);
    }

    /**
     * WhiteboardObjectRectJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectRectJabberImpl (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("rect"))
            {
                //we have a rectangle
                String id = e.getAttribute ("id");
                double x = Double.parseDouble (e.getAttribute ("x"));
                double y = Double.parseDouble (e.getAttribute ("y"));
                double width = Double.parseDouble (e.getAttribute ("width"));
                double height = Double.parseDouble (e.getAttribute ("height"));
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");
                String fill = e.getAttribute ("fill");

                this.setID (id);
                this.setWhiteboardPoint (new WhiteboardPoint (x, y));
                this.setWidth (width);
                this.setHeight (height);
                this.setFill (!fill.equals ("none"));
                this.setThickness (Integer.parseInt (stroke_width));
                this.setColor (Color.decode (stroke).getRGB ());
            }
        }
        catch (ParserConfigurationException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : "+xml);
        }
        catch (IOException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : "+xml);
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : "+xml);
        }
    }

    /**
     * Gets the height (in pixels) of the WhiteboardShapeRect.
     *
     * @return The height.
     */
    public double getHeight ()
    {
        return this.height;
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
     * Returns the fill state of the WhiteboardShapeRect.
     *
     * @return True is filled, false is unfilled.
     */
    public boolean isFill ()
    {
        return this.fill;
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
     * Sets the width (in pixels) of the WhiteboardShapeRect.
     *
     * @param height The new height.
     */
    public void setHeight (double height)
    {
        this.height = height;
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
     * Returns the XML reppresentation of the PacketExtension.
     *
     * @return the packet extension as XML.
     * @todo Implement this org.jivesoftware.smack.packet.PacketExtension
     *   method
     */
    @Override
    public String toXML ()
    {
        String s = "<rect id=\"#i\" x=\"#x\" y=\"#y\" width=\"#w\" height=\"#h\" " +
          "fill=\"#f\" stroke=\"#s\" stroke-width=\"#ow\"/>";

        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s",  colorToHex (getColor ()));
        s = s.replaceAll ("#ow", ""+getThickness ());
        WhiteboardPoint p = getWhiteboardPoint ();
        s = s.replaceAll ("#x", ""+p.getX ());
        s = s.replaceAll ("#y", ""+p.getY ());
        s = s.replaceAll ("#w", ""+getWidth ());
        s = s.replaceAll ("#h", ""+getHeight ());
        s = s.replaceAll ("#f", ((isFill ())?(""+getColor ()):"none"));

        return s;
    }
}
