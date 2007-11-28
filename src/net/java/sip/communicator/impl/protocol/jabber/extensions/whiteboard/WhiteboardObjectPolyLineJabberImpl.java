/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import net.java.sip.communicator.util.*;
import java.awt.Color;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import net.java.sip.communicator.service.protocol.WhiteboardPoint;
import org.w3c.dom.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 *  WhiteboardObjectPolyLineJabberImpl
 * <p>
 * WhiteboardObjectPolyLineJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectPolyLineJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectPolyLineJabberImpl
  extends WhiteboardObjectJabberImpl implements WhiteboardObjectPolyLine
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectPolyLineJabberImpl.class);
    
    /**
     * list of WhiteboardPoint
     */
    private LinkedList listPoints = new LinkedList ();
    
    /**
     * Default WhiteboardObjectPolyLineJabberImpl constructor.
     */
    public WhiteboardObjectPolyLineJabberImpl ()
    {
        super ();
    }
    
    /**
     * WhiteboardObjectPolyLineJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectPolyLineJabberImpl (String xml)
    {
        DocumentBuilderFactory factory =
          DocumentBuilderFactory.newInstance ();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder ();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);
            
            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("polyline"))
            {
                //we have polyline
                String id = e.getAttribute ("id");
                String d = e.getAttribute ("points");
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");
                
                this.setID (id);
                this.setThickness (Integer.parseInt (stroke_width));
                this.setColor (Color.decode (stroke).getRGB ());
                this.setPoints (getPolyPoints (d));
                
            }
        }
        catch (ParserConfigurationException ex)
        {
            logger.debug ("Problem WhiteboardObject : "+xml);
        }
        catch (IOException ex)
        {
            logger.debug ("Problem WhiteboardObject : "+xml);
        }
        catch (Exception ex)
        {
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
    public void setPoints (List points)
    {
        this.listPoints =  new LinkedList (points);
    }
    
    /**
     * Returns a list of all the <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @return the list of <tt>WhiteboardPoint</tt>s composing this object.
     */
    public List getPoints ()
    {
        return this.listPoints;
    }
    
    /**
     * Converts a String in a "150,375 150,325 250,325 250,375" format into
     * List of <tt>WhiteboardPoint</tt>.
     *
     * @param points the String to be converted to a 
     * List of <tt>WhiteboardPoint</tt>.
     *
     * @return a List of the String points parameter
     */
    private List getPolyPoints (String points)
    {
        List list = new LinkedList ();
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
     * Returns the XML reppresentation of the PacketExtension.
     *
     * @return the packet extension as XML.
     * @todo Implement this org.jivesoftware.smack.packet.PacketExtension
     *   method
     */
    public String toXML ()
    {
        String s = "<polyline id=\"#i\" points=\"#p\" " +
          "fill=\"#f\" stroke=\"#s\" stroke-width=\"#w\"/>";
        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s",  colorToHex (getColor ()));
        s = s.replaceAll ("#w", ""+getThickness ());
        s = s.replaceAll ("#f", "none");
        
        StringBuilder sb = new StringBuilder ();
        
        for (int i = 0; i < listPoints.size (); i++)
        {
            WhiteboardPoint point = (WhiteboardPoint) listPoints.get (i);
            sb.append (point.getX ());
            sb.append (",");
            sb.append (point.getY ());
            sb.append (" ");
        }
        s = s.replaceAll ("#p", sb.toString ());
        return s;
    }
}