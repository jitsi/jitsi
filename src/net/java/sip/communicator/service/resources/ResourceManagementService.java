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
    public int getColor(String key);
    public String getColorString(String key);
    
    // Image pack methods
    public InputStream getImageInputStreamForPath(String path);
    public InputStream getImageInputStream(String streamKey);
    public URL getImageURL(String urlKey);
    public URL getImageURLForPath(String path);
    
    // Language pack methods
    public String getI18NString(String key);
    public String getI18NString(String key, Locale l);
    public String getI18NString(String key, String[] params);
    public String getI18NString(String key, String[] params, Locale l);
    public char getI18nMnemonic(String key);
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
