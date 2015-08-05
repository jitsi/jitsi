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
package net.java.sip.communicator.impl.protocol.ssh;

import net.java.sip.communicator.service.resources.*;

/**
 * @author Shobhit Jindal
 */
public class Resources
{
    /**
     * The SSH logo imageID.
     */
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
        return SSHActivator.getResources().getImageInBytes(imageID.getId());
    }
}
