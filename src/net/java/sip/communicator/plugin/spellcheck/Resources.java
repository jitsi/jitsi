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
package net.java.sip.communicator.plugin.spellcheck;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Damian Johnson
 * @author Yana Stamcheva
 */
public class Resources
{
    private static Logger logger = Logger.getLogger(Resources.class);

    private static ResourceManagementService resourceService;

    /**
     * Location of flag resources.
     */
    private static final String FLAG_PATH =
        "resources/images/plugin/spellcheck/flags/";

    /**
     * The spell check plugin icon, shown in the configuration form.
     */
    public static final String PLUGIN_ICON = "plugin.spellcheck.PLUGIN_ICON";

    /**
     * The add word icon.
     */
    public static final String ADD_WORD_ICON =
        "plugin.spellcheck.ADD_WORD_ICON";

    /**
     * The personal dictionary icon.
     */
    public static final String PERSONAL_DICTIONARY =
        "plugin.spellcheck.PERSONAL_DIR";

    /**
     * The word include icon.
     */
    public static final String WORD_INCLUDE = "plugin.spellcheck.WORD_INCLUDE";

    /**
     * The word exclude icon.
     */
    public static final String WORD_EXCLUDE = "plugin.spellcheck.WORD_EXCLUDE";

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return getResources().getI18NString(key);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIcon getImage(String imageID)
    {
        return getResources().getImage(imageID);
    }

    /**
     * Loads a flag image from a given image identifier.
     *
     * @param resource iso code for flag to be retrieved.
     * @return icon reflecting iso code
     * @throws IOException if no such resource is available
     */
    public static ImageIcon getFlagImage(String resource) throws IOException
    {
        String path = FLAG_PATH + resource + ".png";

        InputStream input = getResources().getImageInputStreamForPath(path);
        if (input == null)
            logger.info("Unable to obtain flag image for path: " + path);

        BufferedImage image = ImageIO.read(input);

        return new ImageIcon(image);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        return getResources().getImageInBytes(imageID);
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> through which we obtain
     * resources like images and localized texts.
     *
     * @return the <tt>ResourceManagementService</tt>
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService != null)
            return resourceService;

        ServiceReference configServiceRef =
            SpellCheckActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

        resourceService =
            (ResourceManagementService) SpellCheckActivator.bundleContext
                .getService(configServiceRef);

        return resourceService;
    }
}
