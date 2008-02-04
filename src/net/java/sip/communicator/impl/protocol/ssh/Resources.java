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
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 *
 * @author Shobhit Jindal
 */
public class Resources
{
    private static Logger log = Logger.getLogger(Resources.class);
    
    private static final String BUNDLE_NAME
            = "net.java.sip.communicator.impl.protocol.ssh.resources";
    
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);
    
    public static ImageID SSH_LOGO = new ImageID("protocolIcon");
    
    /**
     * Returns an string corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return a string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return RESOURCE_BUNDLE.getString(key);
            
        }
        catch (MissingResourceException exc)
        {
            return '!' + key + '!';
        }
    }
    
    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID)
    {
        byte[] image= null;
        InputStream inputStream;
        
        String path = Resources.getString(imageID.getId());
        
        try
        {
            inputStream = Resources.class.getClassLoader()
                .getResourceAsStream(path);
            
            image = new byte[inputStream.available()];
            
            inputStream.read(image);
        }
        catch (IOException exc)
        {
            log.error("Failed to load image:" + path, exc);
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
