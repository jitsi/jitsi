/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.avatar;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import net.java.sip.communicator.impl.gui.*;

import net.java.sip.communicator.util.*;

/**
 * Take cares of storing(deleting, moving) images with the given indexes. 
 */
public class AvatarStackManager
{
    /**
     * The logger for this class.
     */
    private static Logger logger = Logger.getLogger(AvatarStackManager.class);

    /**
     * The folder where user avatars are stored.
     */
    private static final String STORE_DIR = "avatarcache" + File.separator
        + "userimages" + File.separator;
    
    /**
     * Load the image at the defined index from user directory
     * @param index the image index
     * @return the image
     */
    public static BufferedImage loadImage(int index)
    {
        String imagePath;
        File imageFile;
        
        try
        {
            imagePath = STORE_DIR + index + ".png";
            
            imageFile = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(imagePath);
        }
        catch (Exception e)
        {
            // Unable to access stored image
            System.err.println("Unable to access stored image as index " + index);
            return null;
        }    
        
        // If the file don't exists, there is no more images to get
        if (!imageFile.exists())
            return null;
        
        try
        {
            return ImageIO.read(imageFile);
        }
        catch (IOException ioe)
        {
            // Error while reading file
            ioe.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Removes the oldest image and as its with lower index.
     * Moves all indexes. Ant this way we free one index.
     * @param nbImages
     */
    public static void popFirstImage(int nbImages)
    {
        for (int i=nbImages-1; i>0; i--)
        {
            moveImage(i, i-1);
        }
    }

    /**
     * Moves images.
     * @param oldIndex the old index.
     * @param newIndex the new index.
     */
    private static void moveImage(int oldIndex, int newIndex)
    {
        String oldImagePath = STORE_DIR + oldIndex + ".png";
        String newImagePath = STORE_DIR + newIndex + ".png";
        
        try
        {
            File oldFile = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(oldImagePath);
            
            File newFile = GuiActivator.getFileAccessService()
            .getPrivatePersistentFile(newImagePath);
            
            if (oldFile.exists())
                oldFile.renameTo(newFile);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    /**
     * Stores an image.
     * @param image the image
     * @param index of the image.
     */
    public static void storeImage(BufferedImage image, int index)
    {
        String imagePath = STORE_DIR + index + ".png";
        
        try
        {
            File storeDir = GuiActivator.getFileAccessService()
                .getPrivatePersistentDirectory(STORE_DIR);

            // if dir doesn't exist create it
            storeDir.mkdirs();

            File file = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(imagePath);

            ImageIO.write(image, "png", file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            // Error while writing
        }
        catch (Exception e)
        {
            // Unable to access image path
            e.printStackTrace();
        }
    }

    /**
     * Deletes the stored image.
     * @param index of the image to delete.
     */
    public static void deleteImage(int index)
    {
        try
        {
            File imageFile = GuiActivator.getFileAccessService()
                .getPrivatePersistentFile(STORE_DIR + index + ".png");
            
            if (imageFile.exists() && !imageFile.delete())
                logger.error("Can't delete stored image at index " + index);
        }
        catch (Exception e)
        {
            // Can't access file
            logger.info("Can't access to file: " + STORE_DIR + index + ".png");
        }
    }
}
