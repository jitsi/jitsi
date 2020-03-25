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
package net.java.sip.communicator.impl.googlecontacts.configform;

import javax.swing.*;

import net.java.sip.communicator.impl.googlecontacts.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return GoogleContactsActivator.getResourceManagementService()
            .getI18NString(key);
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @param params additional parameters
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key, String[] params)
    {
        return GoogleContactsActivator.getResourceManagementService()
            .getI18NString(key, params);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIcon getImage(String imageID)
    {
        return GoogleContactsActivator.getResourceManagementService().getImage(imageID);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        return GoogleContactsActivator.getResourceManagementService().
            getImageInBytes(imageID);
    }
}
