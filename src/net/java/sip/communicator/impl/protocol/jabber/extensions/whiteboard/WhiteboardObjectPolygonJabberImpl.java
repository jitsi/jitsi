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
import java.util.*;
import java.util.List;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 *  WhiteboardObjectPolygonJabberImpl
 * <p>
 * WhiteboardObjectPolygonJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectPolygonJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectPolygonJabberImpl
  extends WhiteboardObjectJabberImpl implements WhiteboardObjectPolygon
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectPolygonJabberImpl.class);

    /**
     * list of WhiteboardPoint
     */
    private List<WhiteboardPoint> listPoints
        = new LinkedList<WhiteboardPoint>();

    /**
     * True is filled, false is unfilled.
     */
    private boolean fill;

    /**
     * The background color of this object
     */
    private int backColor;

    /**
     * Default WhiteboardObjectPolygonJabberImpl constructor.
     */
    public WhiteboardObjectPolygonJabberImpl ()
    {
        super ();
    }

    /**
     * WhiteboardObjectPolygonJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectPolygonJabberImpl (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("polygon"))
            {
                //we have a polygon
                String id = e.getAttribute ("id");
                String d = e.getAttribute ("points");
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");
                String fill = e.getAttribute ("fill");

                this.setID (id);
                this.setThickness (Integer.parseInt (stroke_width));
                this.setColor (Color.decode (stroke).getRGB ());
                this.setPoints (getPolyPoints (d));
                this.setFill (!fill.equals ("none"));
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
     * Sets the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @param points the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     */
    public void setPoints (List<WhiteboardPoint> points)
    {
        this.listPoints =  new LinkedList<WhiteboardPoint>(points);
    }

    /**
     * Returns a list of all the <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @return the list of <tt>WhiteboardPoint</tt>s composing this object.
     */
    public List<WhiteboardPoint> getPoints ()
    {
        return this.listPoints;
    }

    /**
     * Converts a String in a "1,3 4,5 5,5 6,6" format into
     * List of <tt>WhiteboardPoint</tt>.
     *
     * @param points the String to be converted to a
     * List of <tt>WhiteboardPoint</tt>.
     * @return a List of the String points parameter
     */
    private List<WhiteboardPoint> getPolyPoints (String points)
    {
        List<WhiteboardPoint> list = new LinkedList<WhiteboardPoint>();
        if (points == null)
        {
            return list;
        }

        StringTokenizer tokenizer = new StringTokenizer (points);
        while (tokenizer.hasMoreTokens ())
        {
            String token = tokenizer.nextToken ();

            String[] coords = token.split (",");
            WhiteboardPoint p = new WhiteboardPoint (
              Double.parseDouble (coords[0]),
              Double.parseDouble (coords[1]));
            list.add (p);
        }

        return list;
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
     * Returns the XML representation of the PacketExtension.
     *
     * @return the packet extension as XML.
     * @todo Implement this org.jivesoftware.smack.packet.PacketExtension
     *   method
     */
    @Override
    public String toXML ()
    {
        String s = "<polygon id=\"#i\" points=\"#p\" " +
          "fill=\"#f\" stroke=\"#s\" stroke-width=\"#w\"/>";
        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s",  colorToHex (getColor ()));
        s = s.replaceAll ("#w", ""+getThickness ());
        s = s.replaceAll ("#f", ((isFill ())?colorToHex (getColor ()):"none"));

        StringBuilder sb = new StringBuilder ();

        for (int i = 0; i < listPoints.size (); i++)
        {
            WhiteboardPoint point = listPoints.get (i);
            sb.append (point.getX ());
            sb.append (",");
            sb.append (point.getY ());
            sb.append (" ");
        }
        s = s.replaceAll ("#p", sb.toString ());
        return s;
    }
}
