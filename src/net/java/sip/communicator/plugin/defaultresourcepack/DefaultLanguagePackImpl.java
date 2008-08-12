/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * 
 * @author Damian Minkov
 */
public class DefaultLanguagePackImpl
    implements LanguagePack
{
    private Logger logger = Logger.getLogger(DefaultLanguagePackImpl.class);

    private static final String DEFAULT_RESOURCE_PATH
        = "resources.languages.resources";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     * 
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources()
    {
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle( DEFAULT_RESOURCE_PATH,
                                        Locale.getDefault());

        Map<String, String> resources = new TreeMap<String, String>();

        this.initResources(resourceBundle, resources);

        this.initPluginResources(resources, Locale.getDefault());

        return resources;
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for the given
     * locale.
     * 
     * @param locale The <tt>Locale</tt> we're looking for.
     * @return a <tt>Map</tt>, containing all [key, value] pairs for the given
     * locale.
     */
    public Map<String, String> getResources(Locale locale)
    {
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH, locale);

        Map<String, String> resources = new Hashtable<String, String>();

        this.initResources(resourceBundle, resources);

        this.initPluginResources(resources, locale);

        return resources;
    }

    /**
     * Returns the name of this resource pack.
     * 
     * @return the name of this resource pack.
     */
    public String getName()
    {
        return "Default Language Resources";
    }

    /**
     * Returns the description of this resource pack.
     * 
     * @return the description of this resource pack.
     */
    public String getDescription()
    {
        return "Provide SIP Communicator default Language resource pack.";
    }

    /**
     * Fills the given resource map with all (key,value) pairs obtained from the
     * given <tt>ResourceBundle</tt>. This method will look in the properties
     * files for references to other properties files and will include in the
     * final map data from all referenced files.
     * 
     * @param resourceBundle The initial <tt>ResourceBundle</tt>, corresponding
     * to the "main" properties file.
     * @param locale The locale we're looking for.
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

            resources.put(key, value);
        }
    }
    
    /**
     * Finds all plugin color resources, matching the "images-*.properties"
     * pattern and adds them to this resource pack.
     */
    private void initPluginResources(Map<String, String> resources,
                                    Locale locale)
    {
        Iterator<String> pluginProperties = DefaultResourcePackActivator
            .findResourcePaths(   "resources/languages",
                                    "strings-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();

            if (resourceBundleName.indexOf("_") == -1)
            {
                ResourceBundle resourceBundle
                    = ResourceBundle.getBundle(
                        resourceBundleName.substring(
                            0, resourceBundleName.indexOf(".properties")),
                            locale);

                initResources(resourceBundle, resources);
            }
        }
    }
}
