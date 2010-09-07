/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

/**
 * The Resource Management Service gives easy access to
 * common resources for the application including texts, images, sounds and
 * some configurations.
 *
 * @author Damian Minkov
 * @author Adam Netocny
 */
public interface ResourceManagementService
{
    // Color pack methods
    /**
     * Returns the int representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the int representation of the color corresponding to the
     * given key.
     */
    public int getColor(String key);

    /**
     * Returns the string representation of the color corresponding to the
     * given key.
     *
     * @param key The key of the color in the colors properties file.
     * @return the string representation of the color corresponding to the
     * given key.
     */
    public String getColorString(String key);

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     *
     * @param path The path to the image file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * path.
     */
    public InputStream getImageInputStreamForPath(String path);

    /**
     * Returns the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     *
     * @param streamKey The identifier of the image in the resource properties
     * file.
     * @return the <tt>InputStream</tt> of the image corresponding to the given
     * key.
     */
    public InputStream getImageInputStream(String streamKey);

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given key.
     *
     * @param urlKey The identifier of the image in the resource properties file.
     * @return the <tt>URL</tt> of the image corresponding to the given key
     */
    public URL getImageURL(String urlKey);

    /**
     * Returns the <tt>URL</tt> of the image corresponding to the given path.
     *
     * @param path The path to the given image file.
     * @return the <tt>URL</tt> of the image corresponding to the given path.
     */
    public URL getImageURLForPath(String path);

    /**
     * Returns the image path corresponding to the given key.
     *
     * @param key The identifier of the image in the resource properties file.
     * @return the image path corresponding to the given key.
     */
    public String getImagePath(String key);

    // Language pack methods
    /**
     * Default Locale config string.
     */
    public final static String DEFAULT_LOCALE_CONFIG =
            "net.java.sip.communicator.service.resources.DefaultLocale";

    /**
     * All the locales in the language pack.
     * @return all the locales this Language pack contains.
     */
    public Iterator<Locale> getAvailableLocales();

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, Locale locale);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @param params An array of parameters to be replaced in the returned
     * string.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, String[] params);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @param params An array of parameters to be replaced in the returned
     * string.
     * @param locale The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale locale);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @param l The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key, Locale l);

    // Settings pack methods
    /**
     * Returns an url for the setting corresponding to the given key.
     * Used when the setting is an actual file.
     *
     * @param urlKey The key of the setting.
     * @return Url to the corresponding resource.
     */
    public URL getSettingsURL(String urlKey);

    /**
     * Returns an InputStream for the setting corresponding to the given key.
     * Used when the setting is an actual file.
     *
     * @param streamKey The key of the setting.
     * @return InputStream to the corresponding resource.
     */
    public InputStream getSettingsInputStream(String streamKey);

    /**
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public String getSettingsString(String key);

    /**
     * Returns the int value of the corresponding configuration key.
     *
     * @param key The identifier of the string in the resources properties file.
     * @return the int value of the corresponding configuration key.
     */
    public int getSettingsInt(String key);

    // Sound pack methods
    /**
     * Returns an url for the sound resource corresponding to the given key.
     *
     * @param urlKey The key of the setting.
     * @return Url to the corresponding resource.
     */
    public URL getSoundURL(String urlKey);

    /**
     * Returns an url for the sound resource corresponding to the given path.
     *
     * @param path The path to the sound resource.
     * @return Url to the corresponding resource.
     */
    public URL getSoundURLForPath(String path);

    /**
     * Returns the path of the sound corresponding to the given
     * property key.
     *
     * @param soundKey The key of the sound.
     * @return the path of the sound corresponding to the given
     * property key.
     */
    public String getSoundPath(String soundKey);

    /**
     * Constructs an <tt>ImageIcon</tt> from the specified image ID and returns
     * it.
     *
     * @param imageID The identifier of the image.
     * @return An <tt>ImageIcon</tt> containing the image with the given
     * identifier.
     */
    public ImageIcon getImage(String imageID);

    /**
     * Loads the image with the specified ID and returns a byte array
     * containing it.
     *
     * @param imageID The identifier of the image.
     * @return A byte array containing the image with the given identifier.
     */
    public byte[] getImageInBytes(String imageID);

    /**
     * Builds a new skin bundle from the zip file content.
     * @param zipFile Zip file with skin information.
     * @return <tt>File</tt> for the bundle.
     * @throws Exception When something goes wrong.
     */
    public File prepareSkinBundleFromZip(File zipFile) throws Exception;
}
