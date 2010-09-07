/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

/**
 * Default Skin Pack interface.
 * @author Adam Netocny
 */
public interface SkinPack
    extends ResourcePack
{
    /**
     * Default resource name.
     */
    public String RESOURCE_NAME_DEFAULT_VALUE = "SkinPack";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for image
     * resource pack.
     */
    public Map<String, String> getImageResources();

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for style
     * resource pack.
     */
    public Map<String, String> getStyleResources();

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for color
     * resource pack.
     */
    public Map<String, String> getColorResources();
}
