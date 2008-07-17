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

/**
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
    public URL getSettingsURL(String urlKey);
    public InputStream getSettingsInputStream(String streamKey);
    public String getSettingsString(String key);
    public int getSettingsInt(String key);
    
    // Sound pack methods
    public URL getSoundURL(String urlKey);
    public URL getSoundURLForPath(String path);
    
    public Iterator getCurrentColors();
    
    public Iterator getCurrentImages();
    
    public Iterator getCurrentSettings();
    
    public Iterator getCurrentSounds();
    
    public Iterator getAvailableLocales();
    
    public Iterator getI18nStringsByLocale(Locale l);
}
