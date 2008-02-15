/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Manages the access to the size properties contained in the size.properties
 * file in the resource folder.
 * 
 * @author Yana Stamcheva
 */
public class SizeProperties
{
    /**
     * Logger for this class.
     */
    private static Logger log = Logger.getLogger(SizeProperties.class);

    /**
     * Name of the bundle where we will search for size properties.
     */
    private static final String BUNDLE_NAME
        = "resources.size.size";

    /**
     * Bundle which handles access to the size properties.
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME,
                        Locale.getDefault(),
                        SizeProperties.class.getClassLoader());

    /**
     * Returns the size corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return the size corresponding to the given key
     */
    public static int getSize(String key)
    {
        try
        {
            return Integer.parseInt(RESOURCE_BUNDLE.getString(key));
        }
        catch (MissingResourceException e)
        {
            log.error("Missing size resource.", e);

            return 0;
        }
    }
}
