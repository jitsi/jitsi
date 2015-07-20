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
import java.util.regex.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 *  WhiteboardObjectPathJabberImpl
 * <p>
 * WhiteboardObjectPathJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectPathJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectPathJabberImpl
  extends WhiteboardObjectJabberImpl implements WhiteboardObjectPath
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectPathJabberImpl.class);

    /**
     * List of WhiteboardPoint
     */
    private List<WhiteboardPoint> listPoints
        = new LinkedList<WhiteboardPoint>();

    /**
     * Default WhiteboardObjectPathJabberImpl constructor.
     */
    public WhiteboardObjectPathJabberImpl ()
    {
        super ();
    }

    /**
     * WhiteboardObjectPathJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectPathJabberImpl (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("path"))
            {
                //we have a path
                String id = e.getAttribute ("id");
                String d = e.getAttribute ("d");
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");

                this.setID (id);
                this.setThickness (Integer.parseInt (stroke_width));
                this.setColor (Color.decode (stroke).getRGB ());
                this.setPoints (getPathPoints (d));
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
        this.listPoints = new LinkedList<WhiteboardPoint>(points);
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
     * Converts a String in a "M250 150 L150 350 L350 350 Z" format into
     * LinkedList of points.
     *
     * @param points the String to be converted to a LinkedList
     * of WhiteboardPoint.
     * @return a LinkedList (WhiteboardPoint) of the String points parameter
     */
    private List<WhiteboardPoint> getPathPoints (String points)
    {
        List<WhiteboardPoint> list = new LinkedList<WhiteboardPoint>();
        if (points == null)
        {
            return list;
        }
        String patternStr = "[ML]\\S+ \\S+ ";

        Pattern pattern = Pattern.compile (patternStr);
        Matcher matcher = pattern.matcher (points);
        while (matcher.find ())
        {
            String[] coords = matcher.group (0).substring (1).split (" ");
            WhiteboardPoint point = new WhiteboardPoint (
              Double.parseDouble (coords[0]),
              Double.parseDouble (coords[1]));
            list.add (point);
        }

        return list;
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
        String s
            = "<path id=\"#i\" d=\"#p Z\" stroke=\"#s\" stroke-width=\"#w\"/>";

        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s", colorToHex (getColor ()));
        s = s.replaceAll ("#w", ""+getThickness ());

        StringBuilder sb = new StringBuilder ();

        int size = listPoints.size ();
        for (int i = 0; i < size; i++)
        {
            WhiteboardPoint point = listPoints.get (i);
            sb.append ((i == 0) ? "M" : "L");
            sb.append (point.getX ());
            sb.append (" ");
            sb.append (point.getY ());
            sb.append (" ");
        }
        s = s.replaceAll ("#p", sb.toString ());
        return s;
    }
}
