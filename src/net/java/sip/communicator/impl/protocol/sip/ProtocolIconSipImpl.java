/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Represents the Sip protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide an sip icon image in two different sizes.
 * 
 * @author Yana Stamcheva
 */
public class ProtocolIconSipImpl
    implements ProtocolIcon
{
    private static Logger logger = Logger.getLogger(ProtocolIconSipImpl.class);

    private String iconPath;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private Hashtable iconsTable = new Hashtable();

    /**
     * Creates an instance of this class by passing to it the path, where all
     * protocol icons are placed.
     * 
     * @param iconPath the protocol icon path
     */
    public ProtocolIconSipImpl(String iconPath)
    {
        this.iconPath = iconPath;

        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            loadIcon(iconPath + "/sip16x16.png"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            loadIcon(iconPath + "/sip64x64.png"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returns <code>true</code> if an icon with the given size is supported,
     * <code>false</code> - otherwise.
     * 
     * @param iconSize the size of the icon to search for. One of ICON_SIZE_XXX
     * constants.
     * @return <code>true</code> if an icon with the given size is supported,
     * <code>false</code> - otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }

    /**
     * Returns the icon image in the given size.
     * 
     * @param iconSize the size of the icon we're looking for. One of
     * ICON_SIZE_XXX constants.
     * @return the byte array representing the icon.
     */
    public byte[] getIcon(String iconSize)
    {
        return (byte[])iconsTable.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     * 
     * @return the icon image used to represent the protocol connecting state.
     */
    public byte[] getConnectingIcon()
    {
        return loadIcon(iconPath + "/sip-connecting.gif");
    }

    /**
     * Loads an image from a given image path.
     * 
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath)
    {
        InputStream is = ProtocolIconSipImpl.class
            .getClassLoader().getResourceAsStream(imagePath);

        byte[] icon = null;
        try
        {
            icon = new byte[is.available()];
            is.read(icon);
        }
        catch (IOException e)
        {
            logger.error("Failed to load protocol icon: " + imagePath, e);
        }
        return icon;
    }
}
