/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.whiteboardobjects;

import net.java.sip.communicator.service.protocol.*;

/**
 * Used to access the content of instant whiteboard objects that are sent or
 * received via the WhiteboardOperationSet.
 *
 * @author Julien Waechter
 */
public interface WhiteboardObjectImage extends WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type circle.
     */
    public static final String NAME = "WHITEBOARDOBJECTIMAGE";
    
    /**
     * Returns the coordinates of this whiteboard object.
     *
     * @return the coordinates of this object.
     */
    public WhiteboardPoint getWhiteboardPoint();
    
    /**
     * Sets the coordinates of this whiteboard object.
     *
     * @param whiteboardPoint the coordinates of this object.
     */
    public void setWhiteboardPoint(WhiteboardPoint whiteboardPoint);
    
    /**
     * Returns the height (in pixels) of the WhiteboardObject. 
     *
     * @return The height.
     */
    public double getHeight();
    
    /**
     * Returns the width (in pixels) of the WhiteboardObject. 
     *
     * @return The width.
     */
    public double getWidth();
    
    /**
     * Sets the width (in pixels) of the WhiteboardObject.
     *
     * @param height The new height.
     */
    public void setHeight(double height);
    
    /**
     * Sets the width (in pixels) of the WhiteboardObject.
     *
     * @param width The new width. 
     */
    public void setWidth(double width);
    
    /**
     * Specifies an image that should be displayed as the background of this
     * object.
     *
     * @param background a binary array containing the image that should be
     * displayed as the object background.
     */
    public void setBackgroundImage(byte[] background);
    
    /**
     * Returns a binary array containing the image that should be displayed as
     * the background of this <tt>WhiteboardObject</tt>.
     *
     * @return a binary array containing the image that should be displayed as
     * the object background.
     */
    public byte[] getBackgroundImage();
}
