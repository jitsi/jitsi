/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.statusupdate;

import java.util.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 * 
 * @author Thomas Hofer;
 */
public class Resources
{

    private static final String BUNDLE_NAME = "resources.languages.plugin.statusupdate.resources";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key
     *                The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e)
        {

            return '!' + key + '!';
        }
    }
}
