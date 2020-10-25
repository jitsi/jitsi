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
package net.java.sip.communicator.impl.resources;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The abstract class for ResourceManagementService. It listens for
 * {@link ResourcePack} that are registered and exposes them later for use by
 * subclasses. It implements default behaviour for most methods.
 */
public class ResourceManagementServiceImpl
    implements ResourceManagementService, ServiceListener
{
    /**
     * The logger
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourceManagementServiceImpl.class);

    private final ConfigurationService configService;

    /**
     * The OSGI BundleContext
     */
    private final BundleContext bundleContext;

    /**
     * Resources for currently loaded <tt>LanguagePack</tt>.
     */
    private Map<String, String> languageResources;

    /**
     * Currently loaded language pack.
     */
    private LanguagePack languagePack;

    /**
     * The {@link Locale} of <code>languageResources</code> so that the caching
     * of the latter can be used when a string with the same <code>Locale</code>
     * is requested.
     */
    private Locale languageLocale;

    /**
     * Currently loaded image pack.
     */
    private ImagePack imagePack;

    /**
     * Currently loaded color pack.
     */
    private ResourcePack colorPack;

    /**
     * Currently loaded sound pack.
     */
    private ResourcePack soundPack;

    /**
     * Currently loaded settings pack.
     */
    private ResourcePack settingsPack;

    /**
     * Creates an instance of <tt>AbstractResourcesService</tt>.
     *
     * @param bundleContext the OSGi bundle context
     */
    public ResourceManagementServiceImpl(BundleContext bundleContext,
        ConfigurationService configService)
    {
        this.bundleContext = bundleContext;
        this.configService = configService;

        colorPack
            = getDefaultResourcePack(
                    ColorPack.class,
                    ColorPack.RESOURCE_NAME_DEFAULT_VALUE);

        imagePack
            = getDefaultResourcePack(
                    ImagePack.class,
                    ImagePack.RESOURCE_NAME_DEFAULT_VALUE);

        soundPack
            = getDefaultResourcePack(
                    SoundPack.class,
                    SoundPack.RESOURCE_NAME_DEFAULT_VALUE);

        settingsPack
            = getDefaultResourcePack(
                    SettingsPack.class,
                    SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);

        // changes the default locale if set in the config
        String defaultLocale =
                (String) configService.getProperty(DEFAULT_LOCALE_CONFIG);
        if(defaultLocale != null)
            Locale.setDefault(LocaleUtil.getLocale(defaultLocale));

        languagePack
            = getDefaultResourcePack(
                    LanguagePack.class,
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);

        languageLocale = Locale.getDefault();
        if (languagePack != null)
        {
            languageResources = languagePack.getResources(languageLocale);
        }
    }

    /**
     * Handles all <tt>ServiceEvent</tt>s corresponding to <tt>ResourcePack</tt>
     * being registered or unregistered.
     *
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = bundleContext.getService(
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

            if(resourcePack instanceof ColorPack && colorPack == null)
            {
                colorPack = resourcePack;
            }
            else if(resourcePack instanceof ImagePack && imagePack == null)
            {
                imagePack = (ImagePack) resourcePack;
            }
            else if(resourcePack instanceof SoundPack && soundPack == null)
            {
                soundPack = resourcePack;
            }
            else if(resourcePack instanceof SettingsPack && settingsPack == null)
            {
                settingsPack = resourcePack;
            }
            else if(resourcePack instanceof LanguagePack
                && languagePack == null)
            {
                languagePack = (LanguagePack) resourcePack;
                languageLocale = Locale.getDefault();
                languageResources = resourcePack.getResources();
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            if(resourcePack instanceof ColorPack
                    && colorPack.equals(resourcePack))
            {
                colorPack
                    = getDefaultResourcePack(
                            ColorPack.class,
                            ColorPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof ImagePack
                    && imagePack.equals(resourcePack))
            {
                imagePack
                    = getDefaultResourcePack(
                            ImagePack.class,
                            ImagePack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof SoundPack
                    && soundPack.equals(resourcePack))
            {
                soundPack
                    = getDefaultResourcePack(
                            SoundPack.class,
                            SoundPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof SettingsPack
                && settingsPack.equals(resourcePack))
            {
                settingsPack
                    = getDefaultResourcePack(
                            SettingsPack.class,
                            SettingsPack.RESOURCE_NAME_DEFAULT_VALUE);
            }
            else if(resourcePack instanceof LanguagePack
                && languagePack.equals(resourcePack))
            {
                languagePack
                    = getDefaultResourcePack(
                    LanguagePack.class,
                    LanguagePack.RESOURCE_NAME_DEFAULT_VALUE);
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
        return Integer.parseInt(getColorString(key), 16);
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
        String res = colorPack.getResources().get(key);

        if(res == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Missing color resource for key: " + key);

            return "0xFFFFFF";
        }
        else
            return res;
    }

    /**
     * Searches for the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     *
     * @param clazz The name of the resource class.
     * @param typeName The name of the type we're looking for.
     * For example: RESOURCE_NAME_DEFAULT_VALUE
     * @return the <tt>ResourcePack</tt> corresponding to the given
     * <tt>className</tt> and <tt></tt>.
     */
    protected <T extends ResourcePack> T getDefaultResourcePack(
            Class<T> clazz,
            String typeName)
    {
        Collection<ServiceReference<T>> serRefs;
        String osgiFilter
            = "(" + ResourcePack.RESOURCE_NAME + "=" + typeName + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(clazz, osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("Could not obtain resource packs reference.", ex);
            return null;
        }

        if (!serRefs.isEmpty())
        {
            return bundleContext.getService(serRefs.iterator().next());
        }

        return null;
    }

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales()
    {
        return languagePack.getAvailableLocales();
    }

    /**
     * Returns the string for given <tt>key</tt> for specified <tt>locale</tt>.
     * It's the real process of retrieving string for specified locale.
     * The result is used in other methods that operate on localized strings.
     *
     * @param key the key name for the string
     * @param locale the Locale of the string
     * @return the resources string corresponding to the given <tt>key</tt> and
     * <tt>locale</tt>
     */
    protected String doGetI18String(String key, Locale locale)
    {
        Map<String, String> stringResources;
        if ((locale != null) && locale.equals(languageLocale))
        {
            stringResources = languageResources;
        }
        else
        {
            stringResources
                    = (languagePack == null)
                    ? null
                    : languagePack.getResources(locale);
        }

        return (stringResources == null) ? null : stringResources.get(key);
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key)
    {
        return getI18NString(key, null, Locale.getDefault());
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string.
     * @param params the parameters to pass to the localized string
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String... params)
    {
        return getI18NString(key, params, Locale.getDefault());
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
     * Does the additional processing on the resource string. It removes "&"
     * marks used for mnemonics and other characters.
     *
     * @param resourceString the resource string to be processed
     * @return the processed string
     */
    private String processI18NString(String resourceString)
    {
        if(resourceString == null)
            return null;

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

        if (resourceString.contains("''"))
        {
            resourceString = resourceString.replaceAll("''", "'");
        }

        return resourceString;
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
        String resourceString = doGetI18String(key, locale);
        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return '!' + key + '!';
        }

        if(params != null)
        {
            resourceString
                    = MessageFormat.format(resourceString, (Object[]) params);
        }

        return processI18NString(resourceString);
    }

    /**
     * Returns the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>.
     */
    public char getI18nMnemonic(String key)
    {
        return getI18nMnemonic(key, Locale.getDefault());
    }

    /**
     * Returns the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>
     *
     * @param key The identifier of the string in the resources properties file.
     * @param locale The locale that we'd like to receive the result in.
     * @return the character after the first '&' in the internationalized
     * string corresponding to <tt>key</tt>.
     */
    public char getI18nMnemonic(String key, Locale locale)
    {
        String resourceString = doGetI18String(key, locale);

        if (resourceString == null)
        {
            logger.warn("Missing resource for key: " + key);
            return 0;
        }

        int mnemonicIndex = resourceString.indexOf('&');
        if (mnemonicIndex > -1 && mnemonicIndex < resourceString.length() - 1)
        {
            return resourceString.charAt(mnemonicIndex + 1);
        }

        return 0;
    }

    /**
     * Returns the string value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the string of the corresponding configuration key.
     */
    public String getSettingsString(String key)
    {
        Object configValue = configService.getProperty(key);
        return configValue == null ? null : configValue.toString();
    }

    /**
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public int getSettingsInt(String key)
    {
        String resourceString = getSettingsString(key);

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
        String path = getSettingsString(urlKey);

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
        return getSettingsInputStream(streamKey, settingsPack.getClass());
    }

    /**
     * Returns a stream from a given identifier, obtained through the class
     * loader of the given resourceClass.
     *
     * @param streamKey The identifier of the stream.
     * @param resourceClass the resource class through which the resource would
     * be obtained
     * @return The stream for the given identifier.
     */
    public InputStream getSettingsInputStream(  String streamKey,
                                                Class<?> resourceClass)
    {
        String path = settingsPack.getResources().get(streamKey);

        if (path == null || path.length() == 0)
        {
            logger.warn("Missing resource for key: " + streamKey);
            return null;
        }

        return resourceClass.getClassLoader().getResourceAsStream(path);
    }

    /**
     * Returns the image path corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the image path corresponding to the given key.
     */
    public String getImagePath(String key)
    {
        return imagePack.getResources().get(key);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public byte[] getImageInBytes(String imageID)
    {
        try (InputStream in = getImageInputStream(imageID))
        {
            if(in == null)
            {
                return null;
            }

            byte[] image = new byte[in.available()];
            in.read(image);
            return image;
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
            return null;
        }
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
        if (path != null && imagePack != null)
            return imagePack.getClass().getClassLoader()
                .getResourceAsStream(path);

        return null;
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
        String path = getImagePath(streamKey);

        if (path == null || path.length() == 0)
        {
            if (logger.isDebugEnabled())
                logger.debug("Missing resource for key: " + streamKey);
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
        String path = getImagePath(urlKey);

        if (path == null || path.length() == 0)
        {
            if (logger.isDebugEnabled())
                logger.debug("Missing resource for key: " + urlKey);
            return null;
        }
        return getImageURLForPath(path);
    }

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path)
    {
        return imagePack.getClass().getClassLoader().getResource(path);
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

        return (imageURL == null) ? null : new ImageIcon(imageURL);
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
        return soundPack.getResources().get(soundKey);
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
        String path = getSoundPath(urlKey);

        if (path == null || path.length() == 0)
        {
            if (logger.isDebugEnabled())
                logger.debug("Missing resource for key: " + urlKey);
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
     * Not implemented.
     *
     * @param zipFile Zip file with skin information.
     * @return <tt>File</tt> for the bundle.
     * @throws UnsupportedOperationException always
     */
    public File prepareSkinBundleFromZip(File zipFile)
    {
        throw new UnsupportedOperationException("No skin support");
    }
}
