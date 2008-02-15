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
 * Accesses all application properties saved in the application.properties file.
 * 
 * @author Yana Stamcheva
 */
public class ApplicationProperties
{
    /**
     * Logger for this class.
     */
    private static Logger log = Logger.getLogger(ApplicationProperties.class);

    /**
     * Name of the bundle where we will search for color resources.
     */
    private static final String BUNDLE_NAME
        = "resources.application";

    /**
     * Bundle which handle access to localized resources.
     */
    private static final ResourceBundle PROPERTIES_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME);

    /**
     * Returns the application property corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return the application property corresponding to the given key
     */
    public static String getProperty(String key)
    {
        try
        {
            return PROPERTIES_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing property.", e);

            return "";
        }
    }
}
