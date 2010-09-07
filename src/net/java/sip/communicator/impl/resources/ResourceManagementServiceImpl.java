/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.resources.util.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * A default implementation of the ResourceManagementService.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class ResourceManagementServiceImpl
    implements ResourceManagementService,
               ServiceListener
{
    private static Logger logger =
        Logger.getLogger(ResourceManagementServiceImpl.class);

    /**
     * Resources for currently loaded <tt>ColorPack</tt>.
     */
    private Map<String, String> colorResources;

    /**
     * Currently loaded color pack.
     */
    private ResourcePack colorPack = null;

    /**
     * Resources for currently loaded <tt>ImagePack</tt>.
     */
    private Map<String, String> imageResources;

    /**
     * Currently loaded image pack.
     */
    private ResourcePack imagePack = null;

    /**
     * Resources for currently loaded <tt>LanguagePack</tt>.
     */
    private Map<String, String> languageResources;

    /**
     * Currently loaded language pack.
     */
    private LanguagePack languagePack = null;

    /**
     * The {@link Locale} of <code>languageResources</code> so that the caching
     * of the latter can be used when a string with the same <code>Locale</code>
     * is requested.
     */
    private Locale languageLocale;

    /**
     * Resources for currently loaded <tt>SettingsPack</tt>.
     */
    private Map<String, String> settingsResources;

    /**
     * Currently loaded settings pack.
     */
    private ResourcePack settingsPack = null;

    /**
     * Resources for currently loaded <tt>SoundPack</tt>.
     */
    private Map<String, String> soundResources;

    /**
     * Currently loaded sound pack.
     */
    private ResourcePack soundPack = null;

    /**
     * Resources for currently loaded <tt>SkinPack</tt>.
     */
    private Map<String, String> skinResources;

    /**
     * Currently loaded <tt>SkinPack</tt>.
     */
    private SkinPack skinPack = null;

    /**
     * Initializes already registered default resource packs.
     */
    ResourceManagementServiceImpl()
    {
        ResourceManagementActivator.bundleContext.addServiceListener(this);

        colorPack =
            getDefaultResourcePack(ColorPack.class.getName(),
                ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (colorPack != null)
            colorResources = getResources(colorPack);

        imagePack =
            getDefaultResourcePack(ImagePack.class.getName(),
                ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        if (imagePack != null)
            imageResources = getResources(imagePack);

        // changes the default locale if set in the config
        String defaultLocale = (String)ResourceManagementActivator.
                getConfigurationService().getProperty(DEFAULT_LOCALE_CONFIG);
        if(defaultLocale != null)
            Locale.setDefault(
                ResourceManagementServiceUtils.getLocale(defaultLocale));

        languagePack =
            (LanguagePack) getDefaultResourcePack(LanguagePack.class.getName(),
                LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        if (languagePack != null)
        {
            languageLocale = Locale.getDefault();
            languageResources = languagePack.getResources(languageLocale);
        }

        settingsPack =
            getDefaultResourcePack(SettingsPack.class.getName(),
                SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (settingsPack != null)
            settingsResources = getResources(settingsPack);

        soundPack =
            getDefaultResourcePack(SoundPack.class.getName(),
                SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (soundPack != null)
            soundResources = getResources(soundPack);

        skinPack = (SkinPack) getDefaultResourcePack(
            SkinPack.class.getName(), SkinPack.RESOURCE_NAME_DEFAULT_VALUE);

        if (skinPack != null)
        {
            skinResources = getResources(skinPack);
            imageResources.putAll(skinPack.getImageResources());
            colorResources.putAll(skinPack.getColorResources());
        }
    }

    /**
     * Searches for the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     *
     * @param className The name of the resource class.
     * @param typeName The name of the type we're looking for.
     * For example: RESOURCE_NAME_DEFAULT_VALUE
     * @return the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     */
    private ResourcePack getDefaultResourcePack(String className,
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

        if ((serRefs != null) && (serRefs.length > 0))
        {
            return (ResourcePack)
                ResourceManagementActivator.bundleContext.getService(serRefs[0]);
        }
        return null;
    }

    /**
     * Returns the <tt>Map</tt> of (key, value) pairs contained in the given
     * resource pack.
     *
     * @param resourcePack The <tt>ResourcePack</tt> from which we're obtaining
     * the resources.
     * @return the <tt>Map</tt> of (key, value) pairs contained in the given
     * resource pack.
     */
    private Map<String, String> getResources(ResourcePack resourcePack)
    {
        return resourcePack.getResources();
    }

    /**
     * Handles all <tt>ServiceEvent</tt>s corresponding to <tt>ResourcePack</tt>
     * being registered or unregistered.
     *
     * @param event the <tt>ServiceEvent</tt> that notified us
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
            if (logger.isInfoEnabled())
                logger.info("Resource registered " + resourcePack);

            Map<String, String> resources = getResources(resourcePack);

            if(resourcePack instanceof ColorPack && colorPack == null)
            {
                colorPack = resourcePack;
                colorResources = resources;
            }
            else if(resourcePack instanceof ImagePack && imagePack == null)
            {
                imagePack = resourcePack;
                imageResources = resources;
            }
            else if(resourcePack instanceof LanguagePack && languagePack == null)
            {
                languagePack = (LanguagePack) resourcePack;
                languageLocale = Locale.getDefault();
                languageResources = resources;
            }
            else if(resourcePack instanceof SettingsPack && settingsPack == null)
            {
                settingsPack = resourcePack;
                settingsResources = resources;
            }
            else if(resourcePack instanceof SoundPack && soundPack == null)
            {
                soundPack = resourcePack;
                soundResources = resources;
            }
            else if(resourcePack instanceof SkinPack && skinPack == null)
            {
                skinPack = (SkinPack) resourcePack;

                if(imagePack!=null)
                {
                    imageResources = getResources(imagePack);
                }

                if(colorPack!=null)
                {
                    colorResources = getResources(colorPack);
                }

                skinResources = resources;
                imageResources.putAll(skinPack.getImageResources());
                colorResources.putAll(skinPack.getColorResources());
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(resourcePack instanceof ColorPack
                    && colorPack.equals(resourcePack))
            {
                colorPack =
                    getDefaultResourcePack(ColorPack.class.getName(),
                        ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

                if (colorPack != null)
                    colorResources = getResources(colorPack);
            }
            else if(resourcePack instanceof ImagePack
                    && imagePack.equals(resourcePack))
            {
                imagePack =
                    getDefaultResourcePack(ImagePack.class.getName(),
                        ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

                if (imagePack != null)
                    imageResources = getResources(imagePack);
            }
            else if(resourcePack instanceof LanguagePack
                    && languagePack.equals(resourcePack))
            {
                languagePack =
                    (LanguagePack) getDefaultResourcePack(
                        LanguagePack.class.getName(),
                        LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof SettingsPack
                    && settingsPack.equals(resourcePack))
            {
                settingsPack =
                    getDefaultResourcePack(SettingsPack.class.getName(),
                        SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

                if (settingsPack != null)
                    settingsResources = getResources(settingsPack);
            }
            else if(resourcePack instanceof SoundPack
                    && soundPack.equals(resourcePack))
            {
                soundPack =
                    getDefaultResourcePack(SoundPack.class.getName(),
                        SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

                if (soundPack != null)
                    soundResources = getResources(soundPack);
            }
            else if(resourcePack instanceof SkinPack
                    && skinPack.equals(resourcePack))
            {
                if(imagePack!=null)
                {
                    imageResources = getResources(imagePack);
                }

                if(colorPack!=null)
                {
                    colorResources = getResources(colorPack);
                }

                skinPack = (SkinPack) getDefaultResourcePack(
                    SkinPack.class.getName(),
                    SkinPack.RESOURCE_NAME_DEFAULT_VALUE);

                if (skinPack != null)
                {
                    skinResources = getResources(skinPack);
                    imageResources.putAll(skinPack.getImageResources());
                    colorResources.putAll(skinPack.getColorResources());
                }
            }
        }
    }

    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key)
    {
        String res = colorResources.get(key);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return 0xFFFFFF;
        }
        else
            return Integer.parseInt(res, 16);
    }

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the string representation of the color corresponding to the
     * given key.
     */
    public String getColorString(String key)
    {
        String res = colorResources.get(key);

        if(res == null)
        {
            logger.error("Missing color resource for key: " + key);

            return "0xFFFFFF";
        }
        else
            return res;
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     *
     * @param path The path to the image file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     */
    public InputStream getImageInputStreamForPath(String path)
    {
        if(skinPack!=null)
        {
            if(skinPack.getClass().getClassLoader()
                .getResourceAsStream(path)!=null)
            {
                return skinPack.getClass().getClassLoader()
                        .getResourceAsStream(path);
            }
        }

        return imagePack.getClass().getClassLoader().getResourceAsStream(path);
    }

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     *
     * @param streamKey The identifier of the image in the resource properties
     * file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     */
    public InputStream getImageInputStream(String streamKey)
    {
        String path = imageResources.get(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return getImageInputStreamForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given key.
     *
     * @param urlKey The identifier of the image in the resource properties file.
     * @return the <tt>URL</tt> of the image corresponding to the given key
     */
    public URL getImageURL(String urlKey)
    {
        String path = imageResources.get(urlKey);

        if (path == null || path.length() == 0)
        {
            if (logger.isInfoEnabled())
                logger.info("Missing resource for key: " + urlKey);
            return null;
        }
        return getImageURLForPath(path);
    }

    /**
     * Returns the image path corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the image path corresponding to the given key.
     */
    public String getImagePath(String key)
    {
        return imageResources.get(key);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path)
    {
        if(skinPack!=null)
        {
            if(skinPack.getClass().getClassLoader().getResource(path)!=null)
            {
                return skinPack.getClass().getClassLoader().getResource(path);
            }
        }

        return imagePack.getClass().getClassLoader().getResource(path);
    }

    // Language pack methods
    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales()
    {
        return languagePack.getAvailableLocales();
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key)
    {
        return getI18NString(key, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
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
     * @param key The identifier of the string.
     * @param params the parameters to pass to the localized string
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params)
    {
        return getI18NString(key, params, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties
     * file.
     * @param params the parameters to pass to the localized string
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale locale)
    {
        Map<String, String> stringResources;
        if ((locale != null) && locale.equals(languageLocale))
        {
            stringResources = languageResources;
        }
        else
        {
            stringResources = languagePack.getResources(locale);
        }

        String resourceString = stringResources.get(key);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return '!' + key + '!';
        }

        int mnemonicIndex = resourceString.indexOf('&');

        if (mnemonicIndex == 0
            || (mnemonicIndex > 0
                && resourceString.charAt(mnemonicIndex - 1) != '\\'))
        {
            String firstPart = resourceString.substring(0, mnemonicIndex);
            String secondPart = resourceString.substring(mnemonicIndex + 1);

            resourceString = firstPart.concat(secondPart);
        }

        if (resourceString.indexOf('\\') > -1)
        {
            resourceString = resourceString.replaceAll("\\\\", "");
        }

        if(params != null)
             resourceString
                = MessageFormat.format(resourceString, (Object[])params);

        return resourceString;
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key)
    {
        return getI18nMnemonic(key, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @param locale The locale that we'd like to receive the result in.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key, Locale locale)
    {
        Map<String, String> stringResources;
        if ((locale != null) && locale.equals(languageLocale))
        {
            stringResources = languageResources;
        }
        else
        {
            stringResources = languagePack.getResources(locale);
        }

        String resourceString = stringResources.get(key);

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
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public String getSettingsString(String key)
    {
        return settingsResources.get(key);
    }

    /**
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public int getSettingsInt(String key)
    {
        String resourceString = settingsResources.get(key);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }

        return Integer.parseInt(resourceString);
    }

    /**
     * Returns an <tt>URL</tt> from a given identifier.
     *
     * @param urlKey The identifier of the url.
     * @return The url for the given identifier.
     */
    public URL getSettingsURL(String urlKey)
    {
        String path = settingsResources.get(urlKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return settingsPack.getClass().getClassLoader().getResource(path);
    }

    /**
     * Returns a stream from a given identifier.
     *
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(String streamKey)
    {
        String path = settingsResources.get(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return settingsPack.getClass()
            .getClassLoader().getResourceAsStream(path);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     *
     * @return the <tt>URL</tt> of the sound corresponding to the given
     * property key.
     */
    public URL getSoundURL(String urlKey)
    {
        String path = soundResources.get(urlKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + urlKey);
            return null;
        }
        return getSoundURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the sound corresponding to the given path.
     *
     * @param path the path, for which we're looking for a sound URL
     * @return the <tt>URL</tt> of the sound corresponding to the given path.
     */
    public URL getSoundURLForPath(String path)
    {
        return soundPack.getClass().getClassLoader().getResource(path);
    }

    /**
     * Returns the path of the sound corresponding to the given
     * property key.
     *
     * @param soundKey the key, for the sound path
     * @return the path of the sound corresponding to the given
     * property key.
     */
    public String getSoundPath(String soundKey)
    {
        return soundResources.get(soundKey);
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
        URL imageURL = getImageURL(imageID);
        if (imageURL == null)
        {
            return null;
        }
        return new ImageIcon(imageURL);
    }

    /**
     * Builds a new skin bundle from the zip file content.
     *
     * @param zipFile Zip file with skin information.
     * @return <tt>File</tt> for the bundle.
     * @throws Exception When something goes wrong.
     */
    public File prepareSkinBundleFromZip(File zipFile)
        throws Exception
    {
        return SkinJarBuilder.createBundleFromZip(zipFile);
    }
}
