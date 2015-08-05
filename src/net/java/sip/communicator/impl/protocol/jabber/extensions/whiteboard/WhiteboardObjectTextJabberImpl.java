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
 *  WhiteboardObjectTextJabberImpl
 * <p>
 * WhiteboardObjectTextJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectTextJabberImpl have whiteboard object id.
 * @author Julien Waechter
 */
public class WhiteboardObjectTextJabberImpl
  extends WhiteboardObjectJabberImpl implements WhiteboardObjectText
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectTextJabberImpl.class);

     /**
     * The WhiteboardObjectTextJabberImpl's text size
     */
    private int fontSize = 0;
    /**
     * The WhiteboardObjectTextJabberImpl's font name
     */
    private String fontName = "Dialog";
    /**
     * The WhiteboardObjectTextJabberImpl's text
     */
    private String text="";
    /**
     * The coordinates of this object.
     */
    private WhiteboardPoint whiteboardPoint;

    /**
     * Default WhiteboardObjectTextJabberImpl constructor.
     */
    public WhiteboardObjectTextJabberImpl ()
    {
        super ();

        this.setWhiteboardPoint (new WhiteboardPoint (0, 0));
        this.setFontName (fontName);
        this.setFontSize (fontSize);
        this.setText (text);
    }

    /**
     * WhiteboardObjectTextJabberImpl constructor.
     *
     * @param xml the XML string object to parse.
     */
    public WhiteboardObjectTextJabberImpl (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            if (elementName.equals ("text"))
            {
                //we have a text
                String id = e.getAttribute ("id");
                double x = Double.parseDouble (e.getAttribute ("x"));
                double y = Double.parseDouble (e.getAttribute ("y"));
                String fill = e.getAttribute ("fill");
                String fontFamily = e.getAttribute ("font-family");
                int fontSize = Integer.parseInt (e.getAttribute ("font-size"));
                String text = e.getTextContent ();

                this.setID (id);
                this.setWhiteboardPoint (new WhiteboardPoint (x, y));
                this.setFontName (fontFamily);
                this.setFontSize (fontSize);
                this.setText (text);
                this.setColor (Color.decode (fill).getRGB ());

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
        return this.whiteboardPoint;
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
     * Returns the WhiteboardObjectTextJabberImpl's text.
     *
     * @return the WhiteboardObjectTextJabberImpl's text.
     */
    public String getText ()
    {
        return this.text;
    }

    /**
     * Sets the WhiteboardObjectTextJabberImpl's text.
     *
     * @param text the new WhiteboardObjectTextJabberImpl's text.
     */
    public void setText (String text)
    {
        this.text = text;
    }

    /**
     * Returns the WhiteboardObjectTextJabberImpl's font size.
     *
     * @return the WhiteboardObjectTextJabberImpl's font size.
     */
    public int getFontSize ()
    {
        return this.fontSize;
    }

    /**
     * Sets the WhiteboardObjectTextJabberImpl's font size.
     *
     * @param fontSize the new WhiteboardObjectTextJabberImpl's font size.
     */
    public void setFontSize (int fontSize)
    {
        this.fontSize = fontSize;
    }

    /**
     * Returns the WhiteboardObjectTextJabberImpl's font name.
     * (By default Dialog)
     *
     * @return the new WhiteboardObjectTextJabberImpl's font name.
     */
    public String getFontName ()
    {
        return this.fontName;
    }

    /**
     * Sets the WhiteboardObjectTextJabberImpl's font name.
     *
     * @param fontName the new WhiteboardObjectTextJabberImpl's font name.
     */
    public void setFontName (String fontName)
    {
        this.fontName = fontName;
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
        String s = "<text id=\"#i\" x=\"#x\" y=\"#y\" " +
          "fill=\"#fi\" font-family=\"#ff\" font-size=\"#fs\">#t</text>";
        s = s.replaceAll ("#i", getID ());
        s = s.replaceAll ("#fi",  colorToHex (getColor ()));
        WhiteboardPoint p = getWhiteboardPoint ();
        s = s.replaceAll ("#x", "" + p.getX ());
        s = s.replaceAll ("#y", "" + p.getY ());
        s = s.replaceAll ("#ff", getFontName ());
        s = s.replaceAll ("#fs", "" + getFontSize ());
        s = s.replaceAll ("#t", getText ());
        return s;
    }

}
