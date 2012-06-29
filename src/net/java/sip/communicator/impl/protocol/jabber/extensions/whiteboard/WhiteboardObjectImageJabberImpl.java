/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import net.java.sip.communicator.util.*;
import java.io.*;
import javax.xml.parsers.*;
import net.java.sip.communicator.util.Base64;
import org.w3c.dom.*;

import net.java.sip.communicator.service.protocol.WhiteboardPoint;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 *  WhiteboardObjectImageJabberImpl
 * <p>
 * WhiteboardObjectImageJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectImageJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectImageJabberImpl
  extends WhiteboardObjectJabberImpl
  implements WhiteboardObjectImage
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectImageJabberImpl.class);
    
    /**
     * The height value of this object (in pixel)
     */
    private double height;
    /**
     * The width value of this object (in pixel)
     */
    private double width;
    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint whiteboardPoint;
    /**
     * A binary array containing the image that should be
     * displayed as the object background.
     */
    private byte[] background;
    
    /**
     * Default WhiteboardObjectImageJabberImpl constructor.
     */
    public WhiteboardObjectImageJabberImpl ()
    {
        super ();
    }
    
    /**
     * WhiteboardObjectImageJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectImageJabberImpl (String xml)
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
            if (elementName.equals ("image"))
            {
                //we have an image
                String id = e.getAttribute ("id");
                double x = Double.parseDouble (e.getAttribute ("x"));
                double y = Double.parseDouble (e.getAttribute ("y"));
                double width = Double.parseDouble (e.getAttribute ("width"));
                double height = Double.parseDouble (e.getAttribute ("height"));
                String img = e.getTextContent ();
                
                this.setID (id);
                this.setWhiteboardPoint (new WhiteboardPoint (x, y));
                this.setWidth (width);
                this.setHeight (height);
                this.setBackgroundImage(Base64.decode(img));
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
     * Returns the height (in pixels) of the WhiteboardObject.
     *
     * @return The height.
     */
    public double getHeight ()
    {
        return this.height;
    }
    
    /**
     * Returns the width (in pixels) of the WhiteboardObject.
     *
     * @return The width.
     */
    public double getWidth ()
    {
        return this.width;
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
     * Sets the width (in pixels) of the WhiteboardObject.
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
     * Specifies an image that should be displayed as the background of this
     * object.
     *
     * @param background a binary array containing the image that should be
     * displayed as the object background.
     */
    public void setBackgroundImage (byte[] background)
    {
        this.background = background;
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
        return this.background;
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
            = "<image id=\"#id\" x=\"#x\" y=\"#y\" width=\"#w\" height=\"#h\">"
                + "#img</image>";

        s = s.replaceAll ("#id", getID ());
        WhiteboardPoint p =  getWhiteboardPoint ();
        s = s.replaceAll ("#x", ""+p.getX ());
        s = s.replaceAll ("#y", ""+p.getY ());
        s = s.replaceAll ("#w", ""+getWidth ());
        s = s.replaceAll ("#h", ""+getHeight ());
        String img = new String (Base64.encode (getBackgroundImage ()));
        s = s.replaceAll ("#img", img);
        
        return s;
    }
}
