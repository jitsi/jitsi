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

import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 * WhiteboardObjectJabberImpl
 * <p>
 * WhiteboardObjectJabberImpl are created through
 * the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjectJabberImpl have whiteboard object id.
 *
 * @author Julien Waechter
 */
public abstract class WhiteboardObjectJabberImpl
    implements WhiteboardObject
{

    /**
     * An integer indicating the thickness (represented as number of
     * pixels) of this whiteboard object (or its border).
     */
    private int thickness = 1;
    /**
     * A String that uniquely identifies this WhiteboardObject.
     */
    private String ID;
    /**
     * The RGB value of the color of this object.
     */
    private int color;

    /**
     * Default WhiteboardObjectJabberImpl constructor
     */
    public WhiteboardObjectJabberImpl ()
    {
        this.setID (generateID ());
    }
    /**
     * WhiteboardObjectJabberImpl constructor
     *
     * @param id A String that uniquely identifies this WhiteboardObject.
     * @param thickness An integer indicating the thickness (number of pixels).
     * @param color The RGB value of the color of this object.
     */
    public WhiteboardObjectJabberImpl (String id, int thickness, int color)
    {
        this.setID (id);
        this.setColor (color);
        this.setThickness (thickness);
    }

    /**
     * Generate a String uniquely identifying this WhiteboardObject.
     *
     * @return a String that uniquely identifies this WhiteboardObject.
     */
    protected String generateID ()
    {
        return String.valueOf ( System.currentTimeMillis ())
        + String.valueOf (super.hashCode ());
    }
    /**
     * Returns a String uniquely identifying this WhiteboardObject.
     *
     * @return a String that uniquely identifies this WhiteboardObject.
     */
    public String getID ()
    {
        return this.ID;
    }
    /**
     * Sets a String uniquely identifying this WhiteboardObject.
     *
     * @param ID a String that uniquely identifies this WhiteboardObject.
     */
    protected void setID (String ID)
    {
        this.ID = ID;
    }

    /**
     * Returns an integer indicating the thickness (represented as number of
     * pixels) of this whiteboard object (or its border).
     *
     * @return the thickness (in pixels) of this object (or its border).
     */
    public int getThickness ()
    {
        return thickness;
    }

    /**
     * Indicates whether some other WhiteboardObject is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     * argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals (Object obj)
    {
        if(obj == null
          || !(obj instanceof WhiteboardObject))
            return false;
        if (obj == this
          || ((WhiteboardObject)obj).getID ().equals ( getID () ))
            return true;

        return false;
    }

    /**
     * Returns an integer representing the color of this object. The return
     * value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are red,
     * 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the color of this object.
     */
    public int getColor ()
    {
        return color;
    }
    /**
     * Sets the color of this whiteboard object (or rather it's border). The
     * color parameter must be encoded with standard RGB encoding: bits 24-31
     * are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set on this object (using
     * standard RGB encoding).
     */
    public void setColor (int color)
    {
        this.color = color;
    }
    /**
     * Converts a int to hexa
     * @param i int value
     * @return hexa value
     */
    private String hex (int i)
    {
        String h = Integer.toHexString (i);
        if (i < 10)
        {
            h = "0" + h;
        }
        return h.toUpperCase ();
    }
    /**
     * Converts a int color to a hexa color code
     * @param color color
     * @return hexa color code
     */
    protected String colorToHex (int color)
    {
        return colorToHex (Color.getColor ("",color));
    }
    /**
     * Converts a color to a hexa color code
     * @param color color
     * @return hexa color code
     */
    protected String colorToHex (Color color)
    {
        return "#" + hex (color.getRed ()) +
          hex (color.getGreen ()) +
          hex (color.getBlue ());
    }
    /**
     * Sets the thickness (in pixels) of this whiteboard object.
     *
     * @param thickness the number of pixels that this object (or its border)
     * should be thick.
     */
    public void setThickness (int thickness)
    {
        this.thickness = thickness;
    }
    /**
     * Returns the XML reppresentation of the PacketExtension.
     *
     * @return the packet extension as XML.
     * @todo Implement this org.jivesoftware.smack.packet.PacketExtension
     *   method
     */
    public abstract String toXML ();
}
