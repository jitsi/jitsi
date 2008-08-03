/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A default implementation of the ResourceManagementService.
 * 
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class ResourceManagementServiceImpl
    implements ResourceManagementService,
               ServiceListener
{
    private static Logger logger =
        Logger.getLogger(ResourceManagementServiceImpl.class);

    private Hashtable colorPacks;

    private Hashtable imagePacks;

    private Hashtable languagePacks;

    private Hashtable settingsPacks;

    private Hashtable soundPacks;

    ResourceManagementServiceImpl()
    {
        ResourceManagementActivator.bundleContext.addServiceListener(this);

        colorPacks = 
            getDefaultPacks(ColorPack.class.getName(),
                ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        imagePacks = 
            getDefaultPacks(ImagePack.class.getName(),
                            ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        languagePacks = 
            getDefaultPacks(LanguagePack.class.getName(),
                            LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        settingsPacks = 
            getDefaultPacks(SettingsPack.class.getName(),
                            SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        soundPacks = 
            getDefaultPacks( SoundPack.class.getName(),
                            SoundPack.RESOURCE_NAME_DEFAULT_VALUE);
    }

    /**
     * Returns a map of default <tt>ResourcePack</tt>s, corresponding to the
     * given className and typeName.
     * 
     * @param className The name of the class of resource packs, for example
     * ColorPacl class name.
     * @param typeName The name of type of resource packs.
     * @return a map of default <tt>ResourcePack</tt>s, corresponding to the
     * given className and typeName.
     */
    private Hashtable<ResourcePack, ResourceBundle> getDefaultPacks(
            String className,
            String typeName)
    {
        Hashtable<ResourcePack, ResourceBundle> resourcePackList
            = new Hashtable<ResourcePack, ResourceBundle>();

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

                ResourceBundle rb
                    = getResourceBundle(rp);

                resourcePackList.put(rp, rb);
            }
        }

        return resourcePackList;
    }

    /**
     * Returns the <tt>ResourceBundle</tt> corresponding to the given
     * <tt>ResourcePack</tt>.
     * 
     * @param resourcePack the <tt>ResourcePack</tt> for which we're searching
     * the bundle.
     * @return the <tt>ResourceBundle</tt> corresponding to the given
     * <tt>ResourcePack</tt>.
     */
    private ResourceBundle getResourceBundle(ResourcePack resourcePack)
    {
        String baseName = resourcePack.getResourcePackBaseName();

        return ResourceBundle.getBundle(
                baseName,
                Locale.getDefault(),
                resourcePack.getClass().getClassLoader());
    }

    /**
     * Returns a list of language <tt>ResourceBundle</tt>s obtained for the
     * given locale.
     * 
     * @param locale the <tt>Locale</tt> which we're searching for
     * @return the list of language <tt>ResourceBundle</tt>s obtained for the
     * given locale.
     */
    private Vector<ResourceBundle> getLanguagePacksForLocale(Locale locale)
    {
        Vector localePacks = new Vector();

        Enumeration packsEnum = languagePacks.keys();

        while (packsEnum.hasMoreElements())
        {
            LanguagePack langPack = (LanguagePack) packsEnum.nextElement();

            ResourceBundle rBundle = ResourceBundle.getBundle(
                    langPack.getResourcePackBaseName(),
                    locale,
                    langPack.getClass().getClassLoader());

            if (rBundle != null)
                localePacks.add(rBundle);
        }

        return localePacks;
    }

    /**
     * Returns the String corresponding to the given key.
     * 
     * @param key the key for the string we're searching for.
     * @param resourceBundles the Enumeration of <tt>ResourceBundle</tt>s, where
     * to search
     * @return the String corresponding to the given key.
     */
    private String findString(  String key,
                                Enumeration<ResourceBundle> resourceBundles)
    {
        ResourceBundle resourceBundle;
        while (resourceBundles.hasMoreElements())
        {
            resourceBundle = resourceBundles.nextElement();

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
                // If we don't find the resource in the this resource bundle
                // we continue the search.
                continue;
            }
        }

        // nothing found
        return null;
    }

    /**
     * Handles <tt>ServiceEvent</tt>s in order to update the list of registered
     * <tt>ResourcePack</tt>s.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = ResourceManagementActivator.bundleContext.getService(
            event.getServiceReference());

        if (!(sService instanceof ResourcePack))
        {
            return;
        }

        ResourcePack resourcePack = (ResourcePack) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger.info("Resource registered " + resourcePack);

            String resourceBaseName = resourcePack.getResourcePackBaseName();

            ResourceBundle resourceBundle = ResourceBundle.getBundle(
                resourceBaseName,
                Locale.getDefault(),
                resourcePack.getClass().getClassLoader());

            if(resourcePack instanceof ColorPack)
            {
                if (colorPacks == null)
                    colorPacks = new Hashtable();

                colorPacks.put(resourcePack, getResourceBundle(resourcePack));
            }
            else if(resourcePack instanceof ImagePack)
            {
                if (imagePacks == null)
                    imagePacks = new Hashtable();

                imagePacks.put(resourcePack, getResourceBundle(resourcePack));
            }
            else if(resourcePack instanceof LanguagePack)
            {
                if (languagePacks == null)
                    languagePacks = new Hashtable();

                languagePacks.put(resourcePack, getResourceBundle(resourcePack));
            }
            else if(resourcePack instanceof SettingsPack)
            {
                if (settingsPacks == null)
                    settingsPacks = new Hashtable();

                settingsPacks.put(resourcePack, getResourceBundle(resourcePack));
            }
            else if(resourcePack instanceof SoundPack)
            {
                if (soundPacks == null)
                    soundPacks = new Hashtable();

                soundPacks.put(resourcePack, getResourceBundle(resourcePack));
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(resourcePack instanceof ColorPack)
            {
                colorPacks = 
                    getDefaultPacks(ColorPack.class.getName(),
                                    ColorPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof ImagePack)
            {
                imagePacks = 
                    getDefaultPacks(ImagePack.class.getName(),
                                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof LanguagePack)
            {
                languagePacks = 
                    getDefaultPacks(LanguagePack.class.getName(),
                                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof SettingsPack)
            {
                settingsPacks = 
                    getDefaultPacks(SettingsPack.class.getName(),
                                    SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof SoundPack)
            {
                soundPacks = 
                    getDefaultPacks(SoundPack.class.getName(),
                                    SoundPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
        }
    }

    /**
     * Returns the int representation of the color corresponding to the given
     * key.
     * 
     * @return the int representation of the color corresponding to the given
     * key.
     */
    public int getColor(String key)
    {
        String res = findString(key, colorPacks.elements());

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return 0xFFFFFF;
        }
        else
            return Integer.parseInt(res, 16);
    }

    /**
     * Returns the String representation of the color corresponding to the given
     * key.
     * 
     * @return the String representation of the color corresponding to the given
     * key.
     */
    public String getColorString(String key)
    {
        String res = findString(key, colorPacks.elements());

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return "0xFFFFFF";
        }
        else
            return res;
    }

    /**
     * Loads a stream from a given identifier.
     * 
     * @param path The path to the image from a location that's present in the 
     * classpath.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
        // Get the first image pack class loader and try to obtain the image
        // input stream from the given path.
        if (imagePacks.keys().hasMoreElements())
        {
            return imagePacks.keys().nextElement().getClass()
                .getClassLoader().getResourceAsStream(path);
        }

       return null;
    }

    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStream(String streamKey)
    {
        String path = findString(streamKey, imagePacks.elements());

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
        String path = findString(urlKey, imagePacks.elements());

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getImageURLForPath(path);
    }

    /**
     * Returns the path corresponding to the given key.
     * 
     * @return the path corresponding to the given key.
     */
    public String getImagePath(String key)
    {
        return findString(key, imagePacks.elements());
    }

    /**
     * Returns the URL corresponding to the given path.
     * @return the URL corresponding to the given path.
     */
    public URL getImageURLForPath(String path)
    {
        // Get the first image pack class loader and try to obtain the image
        // input stream from the given path.
        if (imagePacks.keys().hasMoreElements())
        {
            return imagePacks.keys().nextElement().getClass()
                .getClassLoader().getResource(path);
        }

       return null;
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
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, Locale locale)
    {
        return getI18NString(key, null, locale);
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
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale locale)
    {
        Enumeration resourceBundles
            = getLanguagePacksForLocale(locale).elements();

        String resourceString = findString(key, resourceBundles);

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
        
        if(params != null)
             resourceString 
                = MessageFormat.format(resourceString, (Object[])params);

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
     * @param locale The locale that we'd like to receive the result in.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key, Locale locale)
    {
        Enumeration resourceBundles
            = getLanguagePacksForLocale(locale).elements();

        String resourceString = findString(key, resourceBundles);

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

    /**
     * Returns the configuration String corresponding to the given
     * key.
     * 
     * @return the configuration String corresponding to the given
     * key.
     */
    public String getSettingsString(String key)
    {
        return findString(key, settingsPacks.elements());
    }
    
    /**
     * Returns the int value corresponding to the given key.
     * 
     * @return the int value corresponding to the given key.
     */
    public int getSettingsInt(String key)
    {
        String resourceString = findString(key, settingsPacks.elements());

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
        String path = findString(urlKey, settingsPacks.elements());

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }

        return getSettingsURLForPath(path);
    }
    
    /**
     * Returns the URL corresponding to the given path.
     * @return the URL corresponding to the given path.
     */
    private URL getSettingsURLForPath(String path)
    {
        // Get the first settings pack class loader and try to obtain the image
        // input stream from the given path.
        if (settingsPacks.keys().hasMoreElements())
        {
            return settingsPacks.keys().nextElement().getClass()
                .getClassLoader().getResource(path);
        }

       return null;
    }

    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(String streamKey)
    {
        String path = findString(streamKey, settingsPacks.elements());

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return getSettingsInputStreamForPath(path);
    }

    /**
     * Returns the InputStream corresponding to the given path.
     * 
     * @return the InputStream corresponding to the given path.
     */
    private InputStream getSettingsInputStreamForPath(String path)
    {
        // Get the first settings pack class loader and try to obtain the image
        // input stream from the given path.
        if (settingsPacks.keys().hasMoreElements())
        {
            return settingsPacks.keys().nextElement().getClass()
                .getClassLoader().getResourceAsStream(path);
        }

       return null;
    }

    /**
     * Return the URL corresponding to the given key.
     * 
     * @return the URL corresponding to the given key.
     */
    public URL getSoundURL(String urlKey)
    {
        String path = findString(urlKey, soundPacks.elements());

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getSoundURLForPath(path);
    }
    
    public URL getSoundURLForPath(String path)
    {
     // Get the first settings pack class loader and try to obtain the image
        // input stream from the given path.
        if (soundPacks.keys().hasMoreElements())
        {
            return soundPacks.keys().nextElement().getClass()
                .getClassLoader().getResource(path);
        }

       return null;
    }


    public Iterator getCurrentColors()
    {
        ResourceBundle colorResourceBundle
            = (ResourceBundle) colorPacks.elements().nextElement();

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
        ResourceBundle imageResourceBundle
            = (ResourceBundle) imagePacks.elements().nextElement();

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
        ResourceBundle settingsResourceBundle
            = (ResourceBundle) settingsPacks.elements().nextElement();

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
        ResourceBundle soundResourceBundle
            = (ResourceBundle) soundPacks.elements().nextElement();

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
        LanguagePack languagePack
            = (LanguagePack) languagePacks.keys().nextElement();

        return languagePack.getAvailableLocales();
    }

    public Iterator getI18nStringsByLocale(Locale l)
    {
        ResourceBundle langResourceBundle
            = (ResourceBundle) languagePacks.elements().nextElement();

        Enumeration languageKeys = langResourceBundle.getKeys();

        List languageList = new ArrayList();
        while (languageKeys.hasMoreElements())
        {
            languageList.add(languageKeys.nextElement());
        }

        return languageList.iterator();
    }
    
    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public byte[] getImageInBytes(String imageID)
    {
        InputStream in = getImageInputStream(imageID);
        
        if(in == null)
            return null;
        
        byte[] image = null;

        try
        {
            image = new byte[in.available()];
            in.read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return image;
    }
    
    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public ImageIcon getImage(String imageID)
    {
        BufferedImage image = null;

        InputStream in = getImageInputStream(imageID);
        
        if(in == null)
            return null;
        
        try
        {
            image = ImageIO.read(in);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return new ImageIcon(image);
    }
}
