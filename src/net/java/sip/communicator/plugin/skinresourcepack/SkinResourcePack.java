/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.skinresourcepack;

import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The skin resource pack.
 *
 * @author Adam Netocny
 */
public class SkinResourcePack
    implements  BundleActivator,
                SkinPack
{
    /**
     * The <tt>Logger</tt> used by the <tt>SkinResourcePack</tt> and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(SkinResourcePack.class);

    /**
     * The default resource path.
     */
    private static final String DEFAULT_RESOURCE_PATH = "info";

    /**
     * The resource path of skin images.
     */
    private static final String DEFAULT_IMAGE_RESOURCE_PATH = "images.images";

    /**
     * The resource path of skin colors.
     */
    private static final String DEFAULT_COLOR_RESOURCE_PATH = "colors.colors";

    /**
     * The resource path f skin styles.
     */
    private static final String DEFAULT_STYLE_RESOURCE_PATH = "styles.styles";

    /**
     * The resource path f skin settings.
     */
    private static final String DEFAULT_SETTINGS_RESOURCE_PATH
                                                        = "settings.settings";

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext;

    /**
     * Buffer for resource files found.
     */
    private static Hashtable<String, Iterator<String>> ressourcesFiles
        = new Hashtable<String, Iterator<String>>();

    /**
     * A map of all skin image resources.
     */
    private Map<String, String> imageResources = null;

    /**
     * A map of all skin style resources.
     */
    private Map<String, String> styleResources = null;

    /**
     * A map of all skin color resources.
     */
    private Map<String, String> colorResources = null;

    /**
     * A map of all skin settings resources.
     */
    private Map<String, String> sttingsResources = null;

    /**
     * Starts the bundle.
     * @param bc BundleContext
     * @throws Exception -
     */
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(ResourcePack.RESOURCE_NAME,
                  SkinPack.RESOURCE_NAME_DEFAULT_VALUE);

        bundleContext.registerService(SkinPack.class.getName(),
                                        this,
                                        props);
    }

    /**
     * Stops the bundle.
     * @param bc BundleContext
     * @throws Exception -
     */
    public void stop(BundleContext bc) throws Exception {}

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     */
    public Map<String, String> getImageResources()
    {
        if(imageResources != null)
        {
            return imageResources;
        }

        Map<String, String> resources = new TreeMap<String, String>();

        try
        {
            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(DEFAULT_IMAGE_RESOURCE_PATH);

            this.initResources(resourceBundle, resources);
        }
        catch (MissingResourceException ex)
        {
            logger.info("Failed to obtain bundle from image resource path.", ex);
        }

        this.initImagePluginResources(resources);

        imageResources = resources;

        return resources;
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     */
    public Map<String, String> getStyleResources()
    {
        if(styleResources != null)
        {
            return styleResources;
        }

        Map<String, String> resources = new TreeMap<String, String>();

        try
        {
            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(DEFAULT_STYLE_RESOURCE_PATH);

            this.initResources(resourceBundle, resources);
        }
        catch (MissingResourceException ex)
        {
            logger.info("Failed to obtain bundle from style resource path.", ex);
        }

        this.initStylePluginResources(resources);

        styleResources = resources;

        return resources;
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     */
    public Map<String, String> getColorResources()
    {
        if(colorResources != null)
        {
            return colorResources;
        }

        Map<String, String> resources = new TreeMap<String, String>();

        try
        {
            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(DEFAULT_COLOR_RESOURCE_PATH);

            this.initResources(resourceBundle, resources);
        }
        catch (MissingResourceException ex)
        {
            logger.info("Failed to obtain bundle from color resource path.", ex);
        }

        this.initColorPluginResources(resources);

        colorResources = resources;

        return resources;
    }

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     */
    public Map<String, String> getSettingsResources()
    {
        if(sttingsResources != null)
        {
            return sttingsResources;
        }

        Map<String, String> resources = new TreeMap<String, String>();

        try
        {
            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(DEFAULT_SETTINGS_RESOURCE_PATH);

            this.initResources(resourceBundle, resources);
        }
        catch (MissingResourceException ex)
        {
            logger.info("Failed to obtain bundle from color resource path.", ex);
        }

        this.initSettingsPluginResources(resources);

        sttingsResources = resources;

        return resources;
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
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH);

        Map<String, String> resources = new TreeMap<String, String>();

        this.initResources(resourceBundle, resources);

        resources.putAll(getImageResources());

        resources.putAll(getStyleResources());

        resources.putAll(getColorResources());

        return resources;
    }

    /**
     * Returns the name of this resource pack.
     *
     * @return the name of this resource pack.
     */
    public String getName()
    {
        Map<String, String> resources = getResources();
        String name = resources.get("display_name");
        if(name != null)
        {
            return name + " Skin Resources";
        }
        else
        {
            return "Skin Resources";
        }
    }

    /**
     * Returns the description of this resource pack.
     *
     * @return the description of this resource pack.
     */
    public String getDescription()
    {
        Map<String, String> resources = getResources();
        String name = resources.get("display_name");
        if(name != null)
        {
            return "Provide Jitsi " + name + " skin resource pack.";
        }
        else
        {
            return "Provide Jitsi skin resource pack.";
        }
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
     * Finds all plugin image resources, matching the "images-*.properties"
     * pattern and adds them to this resource pack.
     * @param resources the map of key, value image resource pairs
     */
    private void initImagePluginResources(Map<String, String> resources)
    {
        Iterator<String> pluginProperties
            = findResourcePaths("images", "images-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();

            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(
                    resourceBundleName.substring(
                        0, resourceBundleName.indexOf(".properties")));

            initResources(resourceBundle, resources);
        }
    }

    /**
     * Finds all plugin style resources, matching the "styles-*.properties"
     * pattern and adds them to this resource pack.
     * @param resources the map of key, value stype resource pairs
     */
    private void initStylePluginResources(Map<String, String> resources)
    {
        Iterator<String> pluginProperties
            = findResourcePaths("styles", "styles-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();

            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(
                    resourceBundleName.substring(
                        0, resourceBundleName.indexOf(".properties")));

            initResources(resourceBundle, resources);
        }
    }

    /**
     * Finds all plugin color resources, matching the "colors-*.properties"
     * pattern and adds them to this resource pack.
     * @param resources the map of key, value color resource pairs
     */
    private void initColorPluginResources(Map<String, String> resources)
    {
        Iterator<String> pluginProperties
            = findResourcePaths("colors", "colors-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();

            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(
                    resourceBundleName.substring(
                        0, resourceBundleName.indexOf(".properties")));

            initResources(resourceBundle, resources);
        }
    }

    /**
     * Finds all plugin style resources, matching the "styles-*.properties"
     * pattern and adds them to this resource pack.
     *
     * @param resources the map of key, value type resource pairs
     */
    private void initSettingsPluginResources(Map<String, String> resources)
    {
        Iterator<String> pluginProperties
            = findResourcePaths("settings", "settings-*.properties");

        while (pluginProperties.hasNext())
        {
            String resourceBundleName = pluginProperties.next();

            ResourceBundle resourceBundle
                = ResourceBundle.getBundle(
                    resourceBundleName.substring(
                        0, resourceBundleName.indexOf(".properties")));

            initResources(resourceBundle, resources);
        }
    }

    /**
     * Finds all properties files for the given path in this bundle.
     *
     * @param path the path pointing to the properties files.
     * @param pattern the pattern for properties files
     * (ex. "colors-*.properties")
     * @return an <tt>Iterator</tt> over a list of all properties files found
     * for the given path and pattern
     */
    protected static Iterator<String> findResourcePaths(String path,
                                                        String pattern)
    {
        Iterator<String> bufferedResult
            = ressourcesFiles.get(path + "/" + pattern);
        if (bufferedResult != null) {
            return bufferedResult;
        }

        ArrayList<String> propertiesList = new ArrayList<String>();

        @SuppressWarnings ("unchecked")
        Enumeration<URL> propertiesUrls = bundleContext.getBundle()
            .findEntries(path,
                        pattern,
                        false);

        if (propertiesUrls != null)
        {
            while (propertiesUrls.hasMoreElements())
            {
                URL propertyUrl = propertiesUrls.nextElement();

                // Remove the first slash.
                String propertyFilePath
                    = propertyUrl.getPath().substring(1);

                // Replace all slashes with dots.
                propertyFilePath = propertyFilePath.replaceAll("/", ".");

                propertiesList.add(propertyFilePath);
            }
        }

        Iterator<String> result = propertiesList.iterator();
        ressourcesFiles.put(path + pattern, result);

        return result;
    }
}
