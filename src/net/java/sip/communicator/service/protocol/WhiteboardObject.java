/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

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
     * A type string constant indicating that an object is of type binary image.
     */
    public static final String TYPE_IMAGE  = "IMAGE";

    /**
     * A type string constant indicating that an object is of type rectangle.
     */
    public static final String TYPE_RECT  = "RECT";

    /**
     * A type string constant indicating that an object is of type circle.
     */
    public static final String TYPE_CIRCLE  = "CIRCLE";

    /**
     * A type string constant indicating that an object is of type binary line.
     */
    public static final String TYPE_LINE  = "LINE";

    /**
     * A type string constant indicating that an object is of type polyline.
     */
    public static final String TYPE_POLYLINE  = "POLYLINE";

    /**
     * A type string constant indicating that an object is of type binary
     * plygon.
     */
    public static final String TYPE_POLYGON  = "POLYGON";

    /**
     * A type string constant indicating that an object is of type elipse.
     */
    public static final String TYPE_ELLIPSE  = "ELLIPSE";

    /**
     * A type string constant indicating that an object is of type path.
     */
    public static final String TYPE_PATH  = "PATH";

    /**
     * A type string constant indicating that an object is of type text.
     */
    public static final String TYPE_TEXT  = "TEXT";

    /**
     * Returns a String uniquely identifying this WhiteboardObject.
     *
     * @return a String that uniquely identifies this WhiteboardObject.
     */
    public String getID ();

    /**
     * Returns the type of this object. Currently this service defines
     * a limited set of types (i.e. the TYPE_XXX constants defined above) that
     * may be extended in the future.
     *
     * @return one of the TYPE_XXX constants defined by this interface,
     * indicating the type of this whiteboard object.
     */
    public String getType ();

    /**
     * Returns an integer indicating the thickness (represented as number of
     * pixels) of this whiteboard object (or its border).
     *
     * @return the thickness (in pixels) of this object (or its border).
     */
    public int getThickness ();

    /**
     * Sets the thickness (in pixels) of this whiteboard object.
     *
     * @param thickness the number of pixels that this object (or its border)
     * should be thick.
     */
    public void setThickness (int thickness);

    /**
     * Returns an integer representing the color of this object. The return
     * value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are red,
     * 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the color of this object.
     */
    public int getColor ();

    /**
     * Sets the color of this whiteboard object (or rather it's border). The
     * color parameter must be encoded with standard RGB encoding: bits 24-31
     * are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set on this object (using
     * standard RGB encoding).
     */
    public void setColor (int color);

    /**
     * Returns a list of all the <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @return the list of <tt>WhiteboardPoint</tt>s composing this object.
     */
    public List getPoints ();

    /**
     * Sets the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     *
     * @param points the list of <tt>WhiteboardPoint</tt> instances that this
     * <tt>WhiteboardObject</tt> is composed of.
     */
    public void setPoints(List points);

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

    /**
     * Specifies the background color for this object. The color parameter
     * must be encoded with standard RGB encoding: bits 24-31 are alpha, 16-23
     * are red, 8-15 are green, 0-7 are blue.
     *
     * @param color the color that we'd like to set for the background of this
     * <tt>WhiteboardObject</tt> (using standard RGB encoding).
     */
    public void setBackgroundColor(int color);

    /**
     * Returns an integer representing the background color of this object. The
     * return value uses standard RGB encoding: bits 24-31 are alpha, 16-23 are
     * red, 8-15 are green, 0-7 are blue.
     *
     * @return the RGB value of the background color of this object.
     */
    public int getBackgroundColor();

    /**
     * Sets <tt>value</tt> as the value of the <tt>key</tt> attribute.
     *
     * @param key the name of the attribute that we'd like to set.
     * @param value value to be associated with the specified key
     */
    public void addAttribute (String key, String value);

    /**
     * Returns the value to which the specified key is mapped, or null if this
     * map contains no value for the key.
     *
     * @param key the key the value of which we'd like to retrieve.
     *
     * @return the value associated with <tt>key</tt>, or <tt>null</tt>
     * if there was no such key.
     */
    public String getAttribute (String key);

    /**
     * Returns a <tt>HashMap</tt> containing all attributes currently set for
     * this <tt>WhiteboardObject</tt>.
     *
     * @return a <tt>HashMap</tt> mapping attribute keys to values for all
     * attributes currently set for this <tt>WhiteboardObject</tt>.
     */
    public HashMap getAttributes ();

    /**
     * Sets map as the complete set of attributes available for this
     * <tt>WhiteboardObject</tt>. Any existing attributes will be removed prior
     * to setting the new map.
     *
     * @param map the complete map of attributes defined for this
     * <tt>WhiteboardObject</tt>.
     */
    public void setAttributes (HashMap map);
}
