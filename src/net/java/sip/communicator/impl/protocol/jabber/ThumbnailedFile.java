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

/**
 * A <tt>ThumbnailedFile</tt> is a file with a thumbnail.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailedFile
    extends File
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final int thumbnailWidth;

    private final int thumbnailHeight;

    private final String thumbnailMimeType;

    private final byte[] thumbnail;

    /**
     * Creates a <tt>ThumbnailedFile</tt>, by specifying the base <tt>file</tt>,
     * the <tt>thumbnailWidth</tt> and <tt>thumbnailHeight</tt>, the
     * <tt>thumbnailMimeType</tt> and the <tt>thumbnail</tt> itself.
     * @param file the base file
     * @param thumbnailWidth the width of the thumbnail
     * @param thumbnailHeight the height of the thumbnail
     * @param thumbnailMimeType the mime type
     * @param thumbnail the thumbnail
     */
    public ThumbnailedFile( File file,
                            int thumbnailWidth,
                            int thumbnailHeight,
                            String thumbnailMimeType,
                            byte[] thumbnail)
    {
        super(file.getPath());

        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailMimeType = thumbnailMimeType;
        this.thumbnail = thumbnail;
    }

    /**
     * Returns the thumbnail of this file.
     * @return the thumbnail of this file
     */
    public byte[] getThumbnailData()
    {
        return thumbnail;
    }

    /**
     * Returns the thumbnail width.
     * @return the thumbnail width
     */
    public int getThumbnailWidth()
    {
        return thumbnailWidth;
    }

    /**
     * Returns the thumbnail height.
     * @return the thumbnail height
     */
    public int getThumbnailHeight()
    {
        return thumbnailHeight;
    }

    /**
     * Returns the thumbnail mime type.
     * @return the thumbnail mime type
     */
    public String getThumbnailMimeType()
    {
        return thumbnailMimeType;
    }
}
