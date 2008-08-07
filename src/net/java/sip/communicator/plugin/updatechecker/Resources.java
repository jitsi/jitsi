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

    private static final String CONFIG_PROP_FILE_NAME 
        = "versionupdate.properties";

    private static Properties configProps = null;
    
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
            if(configProps == null)
            {
                configProps = new Properties();
                configProps.load(new FileInputStream(CONFIG_PROP_FILE_NAME));
            }
            
            return configProps.getProperty(key);
        }
        catch (IOException exc)
        {
            logger.error("Could not open config file.");
            logger.trace("Error was: " + exc);
            return null;
        }
    }
}
