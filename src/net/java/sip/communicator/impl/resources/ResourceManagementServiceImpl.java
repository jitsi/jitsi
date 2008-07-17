/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import java.io.*;
import java.net.*;
import java.text.MessageFormat;
import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 *
 * @author Damian Minkov
 */
public class ResourceManagementServiceImpl
    implements ResourceManagementService,
               ServiceListener
{
    private static Logger logger =
        Logger.getLogger(ResourceManagementServiceImpl.class);

    private ResourceBundle colorResourceBundle;
    private ResourcePack colorPack = null;

    private ResourceBundle imageResourceBundle;
    private ResourcePack imagePack = null;

    private LanguagePack languagePack = null;

    private ResourceBundle settingsResourceBundle;
    private ResourcePack settingsPack = null;

    private ResourceBundle soundResourceBundle;
    private ResourcePack soundPack = null;
    
    ResourceManagementServiceImpl()
    {
        ResourceManagementActivator.bundleContext.addServiceListener(this);

        colorPack = 
            registerDefaultPack(ColorPack.class.getName(),
                ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (colorPack != null)
            colorResourceBundle = getResourceBundle(colorPack);

        imagePack = 
            registerDefaultPack(ImagePack.class.getName(),
                ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        if (imagePack != null)
            imageResourceBundle = getResourceBundle(imagePack);

        languagePack = 
            (LanguagePack) registerDefaultPack(LanguagePack.class.getName(),
                LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        settingsPack = 
            registerDefaultPack(SettingsPack.class.getName(),
                SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (settingsPack != null)
            settingsResourceBundle = getResourceBundle(settingsPack);

        soundPack = 
            registerDefaultPack(SoundPack.class.getName(),
                SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (soundPack != null)
            soundResourceBundle = getResourceBundle(soundPack);
    }

    private ResourcePack registerDefaultPack(   String className,
                                                String typeName)
    {
        ServiceReference[] serRefs = null;

        String osgiFilter =
            "(" + ResourcePack.RESOURCE_NAME + "=" + typeName + ")";

        try
        {
            serRefs = ResourceManagementActivator
                .bundleContext.getServiceReferences(
                    className,
                    osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain resource packs reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                ResourcePack rp =
                    (ResourcePack) ResourceManagementActivator.bundleContext.
                        getService(serRefs[i]);

                return rp;
            }
        }

        return null;
    }

    private ResourceBundle getResourceBundle(ResourcePack resourcePack)
    {
        String baseName = resourcePack.getResourcePackBaseName();

        return ResourceBundle.getBundle(
                baseName,
                Locale.getDefault(),
                resourcePack.getClass().getClassLoader());
    }

    private ResourceBundle getResourceBundle(ResourcePack resourcePack, Locale l)
    {
        String baseName = resourcePack.getResourcePackBaseName();

        return ResourceBundle.getBundle(
                baseName,
                l,
                resourcePack.getClass().getClassLoader());
    }

    private String findString(String key, ResourceBundle resourceBundle)
    {
        try
        {
            String value = resourceBundle.getString(key);
            if (value != null)
            {
                return value;
            }
        }
        catch (MissingResourceException e)
        {
            logger.error("Missing resource.", e);
        }

        // nothing found
        return null;
    }

    public void serviceChanged(ServiceEvent event)
    {
        Object sService = ResourceManagementActivator.bundleContext.getService(
            event.getServiceReference());

        if (!(sService instanceof ResourcePack))
        {
            return;
        }

        ResourcePack resource = (ResourcePack) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger.info("Resource registered " + resource);

            String resourceBaseName = resource.getResourcePackBaseName();

            ResourceBundle resourceBundle = ResourceBundle.getBundle(
                resourceBaseName,
                Locale.getDefault(),
                resource.getClass().getClassLoader());

            if(resource instanceof ColorPack && colorPack == null)
            {
                colorPack = resource;
                colorResourceBundle = resourceBundle;
            }
            else if(resource instanceof ImagePack && imagePack == null)
            {
                imagePack = resource;
                imageResourceBundle = resourceBundle;
            }
            else if(resource instanceof LanguagePack && languagePack == null)
            {
                languagePack = (LanguagePack) resource;
            }
            else if(resource instanceof SettingsPack && settingsPack == null)
            {
                settingsPack = resource;
                settingsResourceBundle = resourceBundle;
            }
            else if(resource instanceof SoundPack && soundPack == null)
            {
                soundPack = resource;
                soundResourceBundle = resourceBundle;
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(resource instanceof ColorPack
                    && colorPack.equals(resource))
            {
                colorPack = null;
                colorResourceBundle = null;
            }
            else if(resource instanceof ImagePack
                    && imagePack.equals(resource))
            {
                imagePack = null;
                imageResourceBundle = null;
            }
            else if(resource instanceof LanguagePack
                    && languagePack.equals(resource))
            {
                languagePack = null;
            }
            else if(resource instanceof SettingsPack
                    && settingsPack.equals(resource))
            {
                settingsPack = null;
                settingsResourceBundle = null;
            }
            else if(resource instanceof SoundPack
                    && soundPack.equals(resource))
            {
                soundPack = null;
                soundResourceBundle = null;
            }
        }
    }

    // Color pack methods
    public int getColor(String key)
    {
        String res = findString(key, colorResourceBundle);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return 0xFFFFFF;
        }
        else
            return Integer.parseInt(res, 16);
    }
    
    public String getColorString(String key)
    {
        String res = findString(key, colorResourceBundle);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return "0xFFFFFF";
        }
        else
            return res;
    }
    
    // Image pack methods
    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
       return imagePack.getClass().getClassLoader().getResourceAsStream(path);
    }
    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStream(String streamKey)
    {
        String path = findString(streamKey, imageResourceBundle);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }
        
        return getImageInputStreamForPath(path);
    }
    
    /**
     * Loads an url from a given identifier.
     * 
     * @param urlKey The identifier of the url.
     * @return The url for the given identifier.
     */
    public URL getImageURL(String urlKey)
    {
        String path = findString(urlKey, imageResourceBundle);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey
                    + " / " + imageResourceBundle);
            return null;
        }
        return getImageURLForPath(path);
    }
    
    public String getImagePath(String key)
    {
        return findString(key, imageResourceBundle);
    }

    public URL getImageURLForPath(String path)
    {
        return imagePack.getClass().getClassLoader().getResource(path);
    }

    // Language pack methods
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key)
    {
        return getI18NString(key, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @param l The locale.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, Locale l)
    {
        ResourceBundle resourceBundle
            = getResourceBundle(languagePack, l);

        String resourceString = findString(key, resourceBundle);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return '!' + key + '!';
        }

        int mnemonicIndex = resourceString.indexOf('&');

        if (mnemonicIndex > -1)
        {
            String firstPart = resourceString.substring(0, mnemonicIndex);
            String secondPart = resourceString.substring(mnemonicIndex + 1);

            resourceString = firstPart.concat(secondPart);
        }

        return resourceString;
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params)
    {
        return getI18NString(key, params, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @param l The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale l)
    {
        ResourceBundle resourceBundle
            = getResourceBundle(languagePack, l);

        String resourceString = findString(key, resourceBundle);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return '!' + key + '!';
        }

        int mnemonicIndex = resourceString.indexOf('&');

        if (mnemonicIndex > -1)
        {
            String firstPart = resourceString.substring(0, mnemonicIndex);
            String secondPart = resourceString.substring(mnemonicIndex + 1);

            resourceString = firstPart.concat(secondPart);
        }
        
        resourceString = MessageFormat.format(resourceString, (Object[])params);

        return resourceString;
    }
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key)
    {
        return getI18nMnemonic(key, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key, Locale l)
    {
        ResourceBundle resourceBundle
            = getResourceBundle(languagePack, l);

        String resourceString = findString(key, resourceBundle);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }

        int mnemonicIndex = resourceString.indexOf('&');

        if (mnemonicIndex > -1)
        {
            return resourceString.charAt(mnemonicIndex + 1);
        }

        return 0;
    }
    
    // Settings pack methods
    public String getSettingsString(String key)
    {
        return findString(key, settingsResourceBundle);
    }
    
    public int getSettingsInt(String key)
    {
        String resourceString = findString(key, settingsResourceBundle);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }
        
        return Integer.parseInt(resourceString);
    }
    
    /**
     * Loads an url from a given identifier.
     * 
     * @param urlKey The identifier of the url.
     * @return The url for the given identifier.
     */
    public URL getSettingsURL(String urlKey)
    {
        String path = findString(urlKey, settingsResourceBundle);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return settingsPack.getClass().getClassLoader().getResource(path);
    }
    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(String streamKey)
    {
        String path = findString(streamKey, settingsResourceBundle);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }
        
        return settingsPack.getClass().getClassLoader().getResourceAsStream(path);
    }
    
    
    // Sound pack methods
    public URL getSoundURL(String urlKey)
    {
        String path = findString(urlKey, soundResourceBundle);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getSoundURLForPath(path);
    }
    
    public URL getSoundURLForPath(String path)
    {
        return soundPack.getClass().getClassLoader().getResource(path);
    }

    public Iterator getCurrentColors()
    {
        Enumeration colorKeys = colorResourceBundle.getKeys();

        List colorList = new ArrayList();
        while (colorKeys.hasMoreElements())
        {
            colorList.add(colorKeys.nextElement());
        }

        return colorList.iterator();
    }

    public Iterator getCurrentImages()
    {
        Enumeration imageKeys = imageResourceBundle.getKeys();

        List imageList = new ArrayList();
        while (imageKeys.hasMoreElements())
        {
            imageList.add(imageKeys.nextElement());
        }

        return imageList.iterator();
    }

    public Iterator getCurrentSettings()
    {
        Enumeration settingKeys = settingsResourceBundle.getKeys();

        List settingList = new ArrayList();
        while (settingKeys.hasMoreElements())
        {
            settingList.add(settingKeys.nextElement());
        }

        return settingList.iterator();
    }

    public Iterator getCurrentSounds()
    {
        Enumeration soundKeys = soundResourceBundle.getKeys();

        List soundList = new ArrayList();
        while (soundKeys.hasMoreElements())
        {
            soundList.add(soundKeys.nextElement());
        }

        return soundList.iterator();
    }

    public Iterator getAvailableLocales()
    {
        return languagePack.getAvailableLocales();
    }

    public Iterator getI18nStringsByLocale(Locale l)
    {
        Enumeration languageKeys = getResourceBundle(languagePack, l).getKeys();

        List languageList = new ArrayList();
        while (languageKeys.hasMoreElements())
        {
            languageList.add(languageKeys.nextElement());
        }

        return languageList.iterator();
    }
}
