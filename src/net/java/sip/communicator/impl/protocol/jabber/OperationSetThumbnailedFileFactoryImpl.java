/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>OperationSetThumbnailedFileFactory</tt> is meant to be used by
 * bundles interested in making files with thumbnails. For example the user
 * interface can be interested in sending files with thumbnails through the
 * <tt>OperationSetFileTransfer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class OperationSetThumbnailedFileFactoryImpl
    implements OperationSetThumbnailedFileFactory
{
    /**
     * Creates a file, by attaching the thumbnail, given by the details, to it.
     *
     * @param file the base file
     * @param thumbnailWidth the width of the thumbnail
     * @param thumbnailHeight the height of the thumbnail
     * @param thumbnailMimeType the mime type of the thumbnail
     * @param thumbnail the thumbnail data
     * @return a file with a thumbnail
     */
    public File createFileWithThumbnail(File file,
                                        int thumbnailWidth,
                                        int thumbnailHeight,
                                        String thumbnailMimeType,
                                        byte[] thumbnail)
    {
        return new ThumbnailedFile(
            file, thumbnailWidth, thumbnailHeight,
            thumbnailMimeType, thumbnail);
    }
}
