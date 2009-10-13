/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Represents the Facebook protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a facebook logo image in two different sizes.
 * 
 * @author Dai Zhiwei
 */
public class ProtocolIconFacebookImpl
    implements ProtocolIcon
{
    private static Logger logger =
        Logger.getLogger(ProtocolIconFacebookImpl.class);

    private static ResourceManagementService resources;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static final Map<String, byte[]> iconsTable
        = new Hashtable<String, byte[]>();
    static
    {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            getImageInBytes("service.protocol.facebook.FACEBOOK_16x16"));
        iconsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            getImageInBytes("service.protocol.facebook.FACEBOOK_32x32"));
        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            getImageInBytes("service.protocol.facebook.FACEBOOK_48x48"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method.
     * Returns an iterator to a set containing the supported icon sizes.
     * 
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator<String> getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returne TRUE if a icon with the given size is supported, FALSE-otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }

    /**
     * Returns the icon image in the given size.
     * 
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     */
    public byte[] getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     * 
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return getImageInBytes("service.protocol.facebook.CONNECTING_ICON");
    }

    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     * 
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     *         identifier.
     */
    private static byte[] getImageInBytes(String imageID)
    {
        InputStream in = getResources().getImageInputStream(imageID);

        if (in == null)
            return null;
        byte[] image = null;
        try
        {
            image = new byte[in.available()];

            in.read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return image;
    }

    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources
                = ResourceManagementServiceUtils
                    .getService(FacebookActivator.bundleContext);
        return resources;
    }
}
