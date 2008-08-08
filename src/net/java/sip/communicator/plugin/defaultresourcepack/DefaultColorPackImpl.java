/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * The default color resource pack.
 * 
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class DefaultColorPackImpl
    implements ColorPack
{
    private Logger logger = Logger.getLogger(DefaultColorPackImpl.class);

    private static final String META_RESOURCE_PATH
        = "resources.colors.meta-colors";

    private static final String DEFAULT_RESOURCE_PATH
        = "resources.colors.colors";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     * 
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources()
    {
        ResourceBundle resourceBundle = null;
        try
        {
            resourceBundle = ResourceBundle.getBundle(META_RESOURCE_PATH);
        }
        catch (MissingResourceException ex)
        {
            logger.info("Missing meta resource for colors.");
        }

        if (resourceBundle == null)
            resourceBundle = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH);

        Map<String, String> resources = new TreeMap<String, String>();

        this.initResources(resourceBundle, resources);

        return resources;
    }

    /**
     * Returns the name of this resource pack.
     * 
     * @return the name of this resource pack.
     */
    public String getName()
    {
        return "Default Color Resources";
    }

    /**
     * Returns the description of this resource pack.
     * 
     * @return the description of this resource pack.
     */
    public String getDescription()
    {
        return "Provide SIP Communicator default color resource pack.";
    }

    /**
     * Fills the given resource map with all (key,value) pairs obtained from the
     * given <tt>ResourceBundle</tt>. This method will look in the properties
     * files for references to other properties files and will include in the
     * final map data from all referenced files.
     * 
     * @param resourceBundle The initial <tt>ResourceBundle</tt>, corresponding
     * to the "main" properties file.
     * @param resources A <tt>Map</tt> that would store the data.
     */
    private void initResources( ResourceBundle resourceBundle,
                                Map<String, String> resources)
    {
        Enumeration colorKeys = resourceBundle.getKeys();

        while (colorKeys.hasMoreElements())
        {
            String key = (String) colorKeys.nextElement();
            String value = resourceBundle.getString(key);

            if (key.startsWith("$reference"))
            {
                ResourceBundle referenceBundle
                    = ResourceBundle.getBundle(value);

                initResources(  referenceBundle,
                                resources);
            }
            else
            {
                resources.put(key, value);
            }
        }
    }
}
