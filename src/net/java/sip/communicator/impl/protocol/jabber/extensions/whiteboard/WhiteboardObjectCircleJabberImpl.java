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
 *  WhiteboardObjectCircleJabberImpl
 * <p>
 * WhiteboardObjectCircleJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectCircleJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectCircleJabberImpl
  extends WhiteboardObjectJabberImpl  implements WhiteboardObjectCircle
{
    private static final Logger logger =
        Logger.getLogger(WhiteboardObjectCircleJabberImpl.class);

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
    private int bgColor;
    /**
     * The number of pixels for the radius.
     */
    private double radius;

    /**
     * Default WhiteboardObjectCircleJabberImpl constructor.
     */
    public WhiteboardObjectCircleJabberImpl ()
    {
        super();
    }

    /**
     * WhiteboardObjectCircleJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectCircleJabberImpl (String xml)
    {
        try
        {
            DocumentBuilder builder
                = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("circle"))
            {
                //we have a circle
                String id = e.getAttribute ("id");
                double cx =  Double.parseDouble (e.getAttribute ("cx"));
                double cy =  Double.parseDouble (e.getAttribute ("cy"));
                double r =  Double.parseDouble (e.getAttribute ("r"));
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");
                String fill = e.getAttribute ("fill");

                this.setID (id);
                this.setWhiteboardPoint (new WhiteboardPoint (cx,cy));
                this.setRadius (r);
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
        this.bgColor = backColor;
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
        return this.bgColor;
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
        String s = "<circle id=\"#i\" cx=\"#cx\" cy=\"#cy\" r=\"#r\" " +
          "fill=\"#f\" stroke=\"#s\" stroke-width=\"#ow\" />";

        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s",  colorToHex (getColor ()));
        s = s.replaceAll ("#ow", ""+getThickness ());
        WhiteboardPoint p = getWhiteboardPoint ();
        s = s.replaceAll ("#cx", ""+p.getX ());
        s = s.replaceAll ("#cy", ""+p.getY ());
        s = s.replaceAll ("#r", ""+getRadius ());
        s = s.replaceAll ("#f", ((fill)?colorToHex (getColor ()):"none"));

        return s;
    }
}
