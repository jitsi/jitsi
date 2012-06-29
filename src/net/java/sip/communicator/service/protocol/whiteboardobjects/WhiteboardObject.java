/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.whiteboardobjects;

/**
 * Used to access the content of instant whiteboard objects that are sent or
 * received via the WhiteboardOperationSet.
 * <p>
 * WhiteboardObject are created through the <tt>WhiteboardSession</tt> session.
 * <p>
 *
 * All WhiteboardObjects have whiteboard object id.
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public interface WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type object.
     */
    public static final String NAME = "WHITEBOARDOBJECT";
    
    /**
     * Returns a String uniquely identifying this WhiteboardObject.
     *
     * @return a String that uniquely identifies this WhiteboardObject.
     */
    public String getID();
    
    
    /**
     * Returns an integer indicating the thickness (represented as number of
     * pixels) of this whiteboard object (or its border).
     *
     * @return the thickness (in pixels) of this object (or its border).
     */
    public int getThickness();
    
    /**
     * Sets the thickness (in pixels) of this whiteboard object.
     *
     * @param thickness the number of pixels that this object (or its border)
     * should be thick.
     */
    public void setThickness(int thickness);
    
    /**
     * Returns an integer representing the color of this object. The return
     * value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are red,
     * 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the color of this object.
     */
    public int getColor();
    
    /**
     * Sets the color of this whiteboard object (or rather it's border). The
     * color parameter must be encoded with standard RGB encoding: bits 24-31
     * are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set on this object (using
     * standard RGB encoding).
     */
    public void setColor(int color);

   
}
