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
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Represents the Jabber protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a Jabber icon image in two different sizes.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ProtocolIconJabberImpl
    implements ProtocolIcon
{
    /**
     * The <tt>Logger</tt> used by the <tt>ProtocolIconJabberImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolIconJabberImpl.class);

    /**
     * The path where all protocol icons are placed.
     */
    private final String iconPath;

    private static ResourceManagementService resourcesService;

    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private final Hashtable<String, byte[]> iconsTable
        = new Hashtable<String, byte[]>();

    /**
     * A hash table containing the path to the protocol icon in different sizes.
     */
    private final Hashtable<String, String> iconPathsTable
        = new Hashtable<String, String>();

    /**
     * Creates an instance of this class by passing to it the path, where all
     * protocol icons are placed.
     *
     * @param iconPath the protocol icon path
     */
    public ProtocolIconJabberImpl(String iconPath)
    {
        this.iconPath = iconPath;

        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16, loadIcon(iconPath
            + "/status16x16-online.png"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_32x32, loadIcon(iconPath
            + "/logo32x32.png"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48, loadIcon(iconPath
            + "/logo48x48.png"));

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_16x16,
            iconPath + "/status16x16-online.png");

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_32x32,
            iconPath + "/logo32x32.png");

        iconPathsTable.put(ProtocolIcon.ICON_SIZE_48x48,
            iconPath + "/logo48x48.png");
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
     * Returns TRUE if a icon with the given size is supported, FALSE-otherwise.
     *
     * @return TRUE if a icon with the given size is supported, FALSE-otherwise.
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
        return loadIcon(iconPath + "/status16x16-connecting.gif");
    }

    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath) {

        InputStream is = null;

        try
        {
            // try to load path it maybe valid url
            is = new URL(imagePath).openStream();
        }
        catch (Exception e)
        {}

        if(is == null)
            is = getResources().getImageInputStreamForPath(imagePath);

        if(is == null)
                return new byte[0];

        byte[] icon = null;
        try {
            icon = new byte[is.available()];
            is.read(icon);
        } catch (IOException e) {
            logger.error("Failed to load icon: " + imagePath, e);
        }
        return icon;
    }

    /**
     * Get the <tt>ResourceMaangementService</tt> registered.
     *
     * @return <tt>ResourceManagementService</tt> registered
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = JabberActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService
                = (ResourceManagementService)JabberActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
