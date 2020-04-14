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
