/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement.directimage;

import net.java.sip.communicator.service.replacement.*;

/**
 * 
 * @author Marin Dzhigarov
 *
 */
public interface DirectImageReplacementService
    extends ReplacementService
{
    /**
     * Returns the size of the image in bytes.
     * @param sourceString the image link.
     * @return the file size in bytes of the image link provided; -1 if the size
     * isn't available or exceeds the max allowed image size.
     */
    public int getImageSize(String sourceString);

    /**
     * Checks if the resource pointed by sourceString is an image.
     * @param sourceString the image link.
     * @return true if the content type of the resource
     * pointed by sourceString is an image.
     */
    public boolean isDirectImage(String sourceString);
}
