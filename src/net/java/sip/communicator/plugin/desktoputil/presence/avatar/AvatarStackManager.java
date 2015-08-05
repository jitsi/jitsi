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
package net.java.sip.communicator.plugin.desktoputil.presence.avatar;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;

/**
 * Take cares of storing(deleting, moving) images with the given indexes.
 */
public class AvatarStackManager
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(AvatarStackManager.class);

    /**
     * The folder where user avatars are stored.
     */
    private static final String STORE_DIR = "avatarcache" + File.separator
        + "userimages" + File.separator;

    /**
     * Deletes the stored image.
     * @param index of the image to delete.
     */
    public static void deleteImage(int index)
    {
        String fileName = STORE_DIR + index + ".png";

        try
        {
            File imageFile
                = DesktopUtilActivator.getFileAccessService()
                    .getPrivatePersistentFile(fileName, FileCategory.CACHE);

            if (imageFile.exists() && !imageFile.delete())
                logger.error("Failed to delete stored image at index " + index);
        }
        catch (Exception e)
        {
            // Can't access file
            logger.info("Failed to access file: " + fileName, e);
        }
    }

    /**
     * Load the image at the defined index from user directory
     * @param index the image index
     * @return the image
     */
    public static BufferedImage loadImage(int index)
    {
        File imageFile;

        try
        {
            String imagePath = STORE_DIR + index + ".png";

            imageFile
                = DesktopUtilActivator.getFileAccessService().
                    getPrivatePersistentFile(imagePath, FileCategory.CACHE);
        }
        catch (Exception e)
        {
            logger.error("Unable to access stored image at index " + index, e);
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
            logger.error("Failed to read file " + imageFile, ioe);
            return null;
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
            FileAccessService fas = DesktopUtilActivator.getFileAccessService();
            File oldFile = fas.getPrivatePersistentFile(oldImagePath,
                FileCategory.CACHE);

            if (oldFile.exists())
            {
                File newFile = fas.getPrivatePersistentFile(newImagePath,
                    FileCategory.CACHE);

                oldFile.renameTo(newFile);
            }
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            //e.printStackTrace();
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
            moveImage(i, i-1);
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
            FileAccessService fas = DesktopUtilActivator.getFileAccessService();
            File storeDir = fas.getPrivatePersistentDirectory(STORE_DIR,
                FileCategory.CACHE);

            // if dir doesn't exist create it
            storeDir.mkdirs();

            File file = fas.getPrivatePersistentFile(imagePath,
                FileCategory.CACHE);

            ImageIO.write(image, "png", file);
        }
        catch (Exception e)
        {
            logger.error("Failed to store image at index " + index, e);
        }
    }
}
