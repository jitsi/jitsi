/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import net.java.sip.communicator.util.*;
import java.awt.Color;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

import net.java.sip.communicator.service.protocol.WhiteboardPoint;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 *  WhiteboardObjectLineJabberImpl
 * <p>
 * WhiteboardObjectLineJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectLineJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectLineJabberImpl
  extends WhiteboardObjectJabberImpl
  implements WhiteboardObjectLine
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectLineJabberImpl.class);
    /**
     * The start coordinates for this line.
     */
    private WhiteboardPoint whiteboardPointStart = new WhiteboardPoint(0, 0);
    /**
     * The end coordinates for this line.
     */
    private WhiteboardPoint whiteboardPointEnd = new WhiteboardPoint(0, 0);
    
    /**
     * Default WhiteboardObjectLineJabberImpl constructor.
     */
    public WhiteboardObjectLineJabberImpl ()
    {
        super ();
    }
    
    /**
     * WhiteboardObjectLineJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectLineJabberImpl (String xml)
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
            if (elementName.equals ("line"))
            {
                //we have a line
                String id = e.getAttribute ("id");
                double x1 = Double.parseDouble (e.getAttribute ("x1"));
                double y1 = Double.parseDouble (e.getAttribute ("y1"));
                double x2 = Double.parseDouble (e.getAttribute ("x2"));
                double y2 = Double.parseDouble (e.getAttribute ("y2"));
                String stroke = e.getAttribute ("stroke");
                String stroke_width = e.getAttribute ("stroke-width");
                
                this.setID (id);
                this.setThickness (Integer.parseInt (stroke_width));
                this.setColor (Color.decode (stroke).getRGB ());
                this.setWhiteboardPointStart (new WhiteboardPoint (x1,y1));
                this.setWhiteboardPointEnd (new WhiteboardPoint (x2,y2));
            }
        }
        catch (ParserConfigurationException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }
        catch (IOException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }
    }
    
    /**
     * Returns the coordinates of  start point for the line
     *
     * @return the start coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointStart ()
    {
        return this.whiteboardPointStart;
    }
    
    /**
     * Returns the coordinates of  end point for the line
     *
     * @return the end coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointEnd ()
    {
        return this.whiteboardPointEnd;
    }
    
    /**
     * Sets the coordinates of start point for the line
     *
     * @param whiteboardPointStart the new start coordinates for this line.
     */
    public void setWhiteboardPointStart (WhiteboardPoint whiteboardPointStart)
    {
        this.whiteboardPointStart = whiteboardPointStart;
    }
    
    /**
     * Sets the coordinates of end point for the line
     *
     * @param whiteboardPointEnd the new end coordinates for this line.
     */
    public void setWhiteboardPointEnd (WhiteboardPoint whiteboardPointEnd)
    {
        this.whiteboardPointEnd = whiteboardPointEnd;
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
        String s
            = "<line id=\"#i\" x1=\"#x1\" y1=\"#y1\" x2=\"#x2\" y2=\"#y2\" "
                + "stroke=\"#s\" stroke-width=\"#w\"/> ";

        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#s",  colorToHex (getColor ()));
        s = s.replaceAll ("#w", ""+getThickness ());
        WhiteboardPoint p1 = getWhiteboardPointStart ();
        WhiteboardPoint p2 = getWhiteboardPointEnd ();

        s = s.replaceAll ("#x1", "" + p1.getX ());
        s = s.replaceAll ("#y1", "" + p1.getY ());

        s = s.replaceAll ("#x2", "" + p2.getX ());
        s = s.replaceAll ("#y2", "" + p2.getY ());

        return s;
    }
}