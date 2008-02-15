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
 * Accesses all Look&Feel properties saved in the lookandfeel.properties file.
 * 
 * @author Yana Stamcheva
 */
public class LookAndFeelProperties
{
    /**
     * Logger for this class.
     */
    private static Logger log = Logger.getLogger(LookAndFeelProperties.class);

    /**
     * Name of the bundle where we will search for look and feel resources.
     */
    private static final String BUNDLE_NAME
        = "resources.lookandfeel";

    /**
     * Bundle which handle access to look and feel resources.
     */
    private static final ResourceBundle PROPERTIES_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME);

    /**
     * Returns the look and feel property corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return the look and feel property corresponding to the given key
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