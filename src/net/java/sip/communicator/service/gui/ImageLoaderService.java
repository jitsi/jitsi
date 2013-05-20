/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import net.java.sip.communicator.service.resources.*;

/**
 * Service responsible for loading images and possibly cache them.
 *
 * @param <T> the type of the image used in the implementers.
 *           In desktop/swing implementation BufferedImage(java.awt.Image)
 *           is used.
 *
 * @author Damian Minkov
 */
public interface ImageLoaderService<T>
{
    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public T getImage(ImageID imageID);

    /**
     * Loads an image from a given image identifier and return
     * bytes of the image.
     *
     * @param imageID The identifier of the image.
     * @return The image bytes for the given identifier.
     */
    public byte[] getImageBytes(ImageID imageID);

    /**
     * Clears the images cache.
     */
    public void clearCache();
}
