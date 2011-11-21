/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.resources.*;

/**
 * @author Damian Minkov
 */
public class DefaultLanguagePackImpl
    implements LanguagePack
{
    private static final String DEFAULT_RESOURCE_PATH
        = "resources.languages.resources";

    /**
     * The locale used for the last resources request
     */
    private Locale localeInBuffer = null;

    /**
     * The result of the last resources request
     */
    private Map<String, String> lastResourcesAsked = null;

    /**
     * All language resource locales.
     */
    private Vector<Locale> availableLocales = new Vector<Locale>();

    public DefaultLanguagePackImpl()
    {
        // Finds all the files *.properties in the path : /resources/languages.
        Enumeration<?> fsEnum = DefaultResourcePackActivator.bundleContext.getBundle().
                findEntries("/resources/languages", "*.properties", false);

        if(fsEnum != null)
        {
            while (fsEnum.hasMoreElements())
            {
                String fileName = ((URL)fsEnum.nextElement()).getFile();
                int localeIndex = fileName.indexOf('_');

                if(localeIndex != -1)
                {
                    String localeId =
                        fileName.substring(
                            localeIndex + 1,
                            fileName.indexOf('.', localeIndex));

                    availableLocales.add(
                        ResourceManagementServiceUtils.getLocale(localeId));
                }
            }
        }
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources()
    {
        return getResources(Locale.getDefault());
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
        // check if we didn't computed it at the previous call
        if (locale.equals(localeInBuffer) && lastResourcesAsked != null) {
            return lastResourcesAsked;
        }

        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH, locale);

        Map<String, String> resources = new Hashtable<String, String>();

        this.initResources(resourceBundle, resources);

        this.initPluginResources(resources, locale);

        // keep it just in case of...
        localeInBuffer = locale;
        lastResourcesAsked = resources;

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
     * @param resources A <tt>Map</tt> that would store the data.
     */
    private void initResources( ResourceBundle resourceBundle,
                                Map<String, String> resources)
    {
        Enumeration<String> colorKeys = resourceBundle.getKeys();

        while (colorKeys.hasMoreElements())
        {
            String key = colorKeys.nextElement();
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

            if (resourceBundleName.indexOf('_') == -1)
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

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales()
    {
        return availableLocales.iterator();
    }
}
