/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.msnaccregwizz;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;
/**
 * The Messages class manages the access to the internationalization
 * properties files.
 * @author Yana Stamcheva
 */
public class Resources {

    private static Logger log = Logger.getLogger(Resources.class);

    private static final String BUNDLE_NAME
        = "net.java.sip.communicator.plugin.msnaccregwizz.resources";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    public static ImageID MSN_LOGO = new ImageID("protocolIcon");
    
    public static ImageID PAGE_IMAGE = new ImageID("pageImage");

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID) {
        byte[] image = new byte[100000];

        String path = Resources.getString(imageID.getId());
        try {
            Resources.class.getClassLoader()
                    .getResourceAsStream(path).read(image);

        } catch (IOException e) {
            log.error("Failed to load image:" + path, e);
        }

        return image;
    }

    /**
     * Represents the Image Identifier.
     */
    public static class ImageID {
        private String id;

        private ImageID(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

}
