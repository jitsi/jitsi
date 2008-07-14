/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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

import java.io.*;

import net.java.sip.communicator.util.*;

/**
 *
 * @author Shobhit Jindal
 */
public class Resources
{
    private static Logger log = Logger.getLogger(Resources.class);
    
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
        byte[] image = null;
        InputStream inputStream;
        try
        {
            inputStream = 
                SSHActivator.getResources().getImageInputStream(imageID.getId());
            
            image = new byte[inputStream.available()];
            
            inputStream.read(image);
        }
        catch (IOException exc)
        {
            log.error("Failed to load image:" + imageID.getId(), exc);
        }
        
        return image;
    }
    
    /**
     * Represents the Image Identifier.
     */
    public static class ImageID
    {
        private String id;
        
        private ImageID(String id)
        {
            this.id = id;
        }
        
        public String getId()
        {
            return id;
        }
    }
    
}
