/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

/**
 * The <tt>ResourcePack</tt> service.
 *
 * @author Damian Minkov
 */
public interface ResourcePack
{
    public String RESOURCE_NAME = "ResourceName";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     *
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources();

    /**
     * Returns the name of this resource pack.
     *
     * @return the name of this resource pack.
     */
    public String getName();

    /**
     * Returns the description of this resource pack.
     *
     * @return the description of this resource pack.
     */
    public String getDescription();
}
