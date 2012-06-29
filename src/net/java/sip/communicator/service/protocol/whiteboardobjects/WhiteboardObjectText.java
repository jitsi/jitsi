/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.service.protocol.whiteboardobjects;

import net.java.sip.communicator.service.protocol.WhiteboardPoint;

/**
 * Used to access the content of instant whiteboard objects that are sent or
 * received via the WhiteboardOperationSet.
 *
 * @author Julien Waechter
 */
public interface WhiteboardObjectText extends WhiteboardObject
{
    /**
     * A type string constant indicating that an object is of type text.
     */
    public static final String NAME = "WHITEBOARDOBJECTTEXT";
    
    /**
     * Returns the coordinates of this whiteboard object.
     *
     * @return the coordinates of this object.
     */
    public WhiteboardPoint getWhiteboardPoint ();
    
    /**
     * Sets the coordinates of this whiteboard object.
     *
     * @param whiteboardPoint the coordinates of this object.
     */
    public void setWhiteboardPoint (WhiteboardPoint whiteboardPoint);
    
    /**
     * Returns the WhiteboardObjectText's text.
     *
     * @return the WhiteboardObjectText's text.
     */
    public String getText();
    
    /**
     * Sets the WhiteboardObjectText's text.
     *
     * @param text the new WhiteboardObjectText's text.
     */
    public void setText(String text);
    
    /**
     * Returns the WhiteboardObjectText's font size.
     *
     * @return the WhiteboardObjectText's font size.
     */
    public int getFontSize();
    
    /**
     * Sets the WhiteboardObjectText's font size.
     *
     * @param fontSize the new WhiteboardObjectText's font size.
     */
    public void setFontSize(int fontSize);
    
    /**
     * Returns the WhiteboardObjectText's font name.
     * (By default Dialog)
     *
     * @return the new WhiteboardObjectText's font name.
     */
    public String getFontName();
    
    /**
     * Sets the WhiteboardObjectText's font name.
     *
     * @param fontName the new WhiteboardObjectText's font name.
     */
    public void setFontName(String fontName);
}
