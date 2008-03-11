/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatechecker;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;

import net.java.sip.communicator.util.Logger;

/**
 * 
 * @author Yana Stamcheva
 */
public class Resources
{
    private static Logger logger = Logger.getLogger(Resources.class);

    private static final String CONFIG_BUNDLE_NAME 
        = "resources.config.updatecheck";

    private static final ResourceBundle configBundle = ResourceBundle
        .getBundle(CONFIG_BUNDLE_NAME);
    
    private static final String LANG_BUNDLE_NAME 
        = "resources.languages.plugin.updatechecker.resources";

    private static final ResourceBundle langBundle = ResourceBundle
        .getBundle(LANG_BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getConfigString(String key)
    {
        try
        {
            return configBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            logger.error("Missing resources.", e);

            return null;
        }
    }
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getLangString(String key)
    {
        try
        {
            return langBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            logger.error("Missing resources.", e);

            return null;
        }
    }
}
