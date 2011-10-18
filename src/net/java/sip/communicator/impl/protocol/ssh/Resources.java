/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * Resources.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */
package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.resources.*;

/**
 * @author Shobhit Jindal
 */
public class Resources
{
    public static ImageID SSH_LOGO = new ImageID("protocolIconSsh");
    
    /**
     * Returns an string corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return a string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return SSHActivator.getResources().getI18NString(key);
    }
    
    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID)
    {
        return SSHActivator.getResources().getImageInBytes(imageID.getId());
    }
}
