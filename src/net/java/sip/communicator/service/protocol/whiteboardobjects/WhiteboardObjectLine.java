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
public interface WhiteboardObjectLine extends WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type line.
     */
    public static final String NAME = "WHITEBOARDOBJECTLINE";
    
    /**
     * Returns the coordinates of  start point for the line
     *
     * @return the start coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointStart();
    
    /**
     * Returns the coordinates of  end point for the line
     *
     * @return the end coordinates of this line.
     */
    public WhiteboardPoint getWhiteboardPointEnd();
    
    /**
     * Sets the coordinates of start point for the line
     *
     * @param whiteboardPointStart the new start coordinates for this line.
     */
    public void setWhiteboardPointStart(WhiteboardPoint whiteboardPointStart);
    
    /**
     * Sets the coordinates of end point for the line
     *
     * @param whiteboardPointEnd the new end coordinates for this line.
     */
    public void setWhiteboardPointEnd(WhiteboardPoint whiteboardPointEnd);
}
