/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.dict.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Represents the IRC protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide an IRC icon image in two different sizes.
 * 
 * @author Stephane Remy
 * @author Loic Kempf
 */
public class ProtocolIconIrcImpl
    implements ProtocolIcon
{    
    private static Logger logger = Logger.getLogger(ProtocolIconIrcImpl.class); 
    
    private static ResourceManagementService resourcesService;
    
    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private static Hashtable iconsTable = new Hashtable();
    static {
        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16,    
            getImageInBytes("protocolIconIrc"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_64x64,
            getImageInBytes("irc64x64Icon"));
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
     * Returns TRUE if a icon with the given size is supported, FALSE-otherwise.
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
        return (byte[])iconsTable.get(iconSize);
    }
    
    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return getImageInBytes("ircConnectingIcon");
    }
    
    /**
     * Returns the byte representation of the image corresponding to the given
     * identifier.
     * 
     * @param imageID the identifier of the image
     * @return the byte representation of the image corresponding to the given
     * identifier.
     */
    public static byte[] getImageInBytes(String imageID) 
    {
        InputStream in = DictActivator.getResources().
            getImageInputStream(imageID);

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
            ServiceReference serviceReference = IrcActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService)IrcActivator.bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }
}