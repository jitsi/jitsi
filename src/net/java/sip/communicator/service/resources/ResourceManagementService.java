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
 */
public interface ResourceManagementService
{
    // Color pack methods
    /**
     * Loads an Color value for the given key
     * 
     * @param key The identifier of the color.
     * @return The color value as int.
     */
    public int getColor(String key);
    
    /**
     * Loads an Color value for the given key
     * 
     * @param key The identifier of the color.
     * @return The color value as String.
     */
    public String getColorString(String key);

    /**
     * Loads a stream from a given path.
     * 
     * @param path The path of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStreamForPath(String path);
    
    /**
     * Loads a stream from a given identifier.
     * 
     * @param streamKey The identifier of the stream.
     * @return The stream for the given identifier.
     */
    public InputStream getImageInputStream(String streamKey);
    
    /**
     * Loads an url from a given identifier.
     * 
     * @param urlKey The identifier of the url.
     * @return The url for the given identifier.
     */
    public URL getImageURL(String urlKey);
    
    /**
     * Loads an url from a given path.
     * 
     * @param path The path for the url.
     * @return The url for the given identifier.
     */
    public URL getImageURLForPath(String path);
    
    /**
     * Returns the value of the given key for image resources.
     * 
     * @param key The key.
     * @return String value for the given image key.
     */
    public String getImagePath(String key);
    
    // Language pack methods
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key);
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @param l The locale.
     * @return An internationalized string corresponding to the given key and
     * given locale.
     */
    public String getI18NString(String key, Locale l);
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @param params params to be replaced in the returned string
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params);
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string
     * @param params params to be replaced in the returned string.
     * @param l The locale.
     * @return An internationalized string corresponding to the given key.
     */
    public String getI18NString(String key, String[] params, Locale l);
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public char getI18nMnemonic(String key);
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
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
     * Returns a String for the setting corresponding to the given key.
     * 
     * @param key The key of the setting.
     * @return String to the corresponding resource.
     */
    public String getSettingsString(String key);
    
    /**
     * Returns an int for the setting corresponding to the given key.
     * 
     * @param key The key of the setting.
     * @return int to the corresponding resource.
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
     * Returns all color keys that can be obtained from this service.
     * 
     * @return Iterator to all color keys.
     */
    public Iterator getCurrentColors();
    
    /**
     * Returns all image keys that can be obtained from this service.
     * 
     * @return Iterator to all image keys.
     */
    public Iterator getCurrentImages();
    
    /**
     * Returns all color settings that can be obtained from this service.
     * 
     * @return Iterator to all settings keys.
     */
    public Iterator getCurrentSettings();
    
    /**
     * Returns all color sounds that can be obtained from this service.
     * 
     * @return Iterator to all sounds keys.
     */
    public Iterator getCurrentSounds();
    
    /**
     * Returns all available locales for the translated texts.
     * 
     * @return Iterator to all locales.
     */
    public Iterator getAvailableLocales();
    
    /**
     * Returns all string keys that can be obtained from 
     * this service for the given locale.
     * 
     * @return Iterator to all string keys.
     */
    public Iterator getI18nStringsByLocale(Locale l);
    
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
}
