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
