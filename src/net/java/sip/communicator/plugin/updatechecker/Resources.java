/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatechecker;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

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
        if (configProps == null)
        {
            configProps = new Properties();

            File configPropsFile = new File(CONFIG_PROP_FILE_NAME);
            if (!configPropsFile.exists())
            {
                if (logger.isInfoEnabled())
                    logger.info("No config file specified for update checker. Disabling update checks");
                return null;
            }

            InputStream configPropsInputStream = null;
            try
            {
                configPropsInputStream = new FileInputStream(configPropsFile);
                configProps.load(configPropsInputStream);
            }
            catch (IOException ex)
            {
                logger.error("Could not open config file.");
                if (logger.isDebugEnabled())
                    logger.debug("Error was: " + ex);
                return null;
            }
            finally
            {
                if (configPropsInputStream != null)
                {
                    try
                    {
                        configPropsInputStream.close();
                    }
                    catch (IOException ex)
                    {
                        logger.error("Could not close config file.");
                    }
                }
            }
        }

        return configProps.getProperty(key);
    }
}
