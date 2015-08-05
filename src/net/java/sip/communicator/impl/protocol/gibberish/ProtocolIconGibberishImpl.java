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
package net.java.sip.communicator.impl.protocol.gibberish;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Represents the Gibberish protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a gibberish logo image in two different sizes.
 *
 * @author Yana Stamcheva
 */
public class ProtocolIconGibberishImpl
    implements ProtocolIcon
{
    private static Logger logger
        = Logger.getLogger(ProtocolIconGibberishImpl.class);

    private static ResourceManagementService resourcesService;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static Hashtable<String, byte[]> iconsTable
        = new Hashtable<String, byte[]>();
    static
    {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            getImageInBytes("service.protocol.gibberish.GIBBERISH_16x16"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            getImageInBytes("service.protocol.gibberish.GIBBERISH_32x32"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            getImageInBytes("service.protocol.gibberish.GIBBERISH_48x48"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            getImageInBytes("service.protocol.gibberish.GIBBERISH_64x64"));
    }

    /**
     * A hash table containing the path to the protocol icon in different sizes.
     */
    private static Hashtable<String, String> iconPathsTable
        = new Hashtable<String, String>();
    static
    {
        iconPathsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            getResources().getImagePath(
                "service.protocol.gibberish.GIBBERISH_16x16"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            getResources().getImagePath(
                "service.protocol.gibberish.GIBBERISH_32x32"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            getResources().getImagePath(
                "service.protocol.gibberish.GIBBERISH_48x48"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            getResources().getImagePath(
                "service.protocol.gibberish.GIBBERISH_64x64"));
    }

    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
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
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     */
    public byte[] getIcon(String iconSize)
    {
        return iconsTable.get(iconSize);
    }

    /**
     * Returns a path to the icon with the given size.
     * @param iconSize the size of the icon we're looking for
     * @return the path to the icon with the given size
     */
    public String getIconPath(String iconSize)
    {
        return iconPathsTable.get(iconSize);
    }

    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return getImageInBytes("gibberishOnlineIcon");
    }

    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     *
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     * identifier.
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
        if (resourcesService == null)
        {
            ServiceReference serviceReference = GibberishActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService)GibberishActivator.bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }
}
