/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.defaultresourcepack;

import java.util.*;

import net.java.sip.communicator.service.resources.*;

/**
 * The default sound resource pack.
 * 
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class DefaultSoundPackImpl
    implements SoundPack
{

    private static final String DEFAULT_RESOURCE_PATH
        = "resources.sounds.sounds";

    /**
     * Returns a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     * 
     * @return a <tt>Map</tt>, containing all [key, value] pairs for this
     * resource pack.
     */
    public Map<String, String> getResources()
    {
        ResourceBundle resourceBundle
            = ResourceBundle.getBundle(DEFAULT_RESOURCE_PATH);

        Map<String, String> resources = new Hashtable<String, String>();

        this.initResources(resourceBundle, resources);

        return resources;
    }

    /**
     * Returns the name of this resource pack.
     * 
     * @return the name of this resource pack.
     */
    public String getName()
    {
        return "Default Sounds Resources";
    }

    /**
     * Returns the description of this resource pack.
     * 
     * @return the description of this resource pack.
     */
    public String getDescription()
    {
        return "Provide SIP Communicator default sounds resource pack.";
    }

    /**
     * Fills the given resource map with all (key,value) pairs obtained from the
     * given <tt>ResourceBundle</tt>. This method will look in the properties
     * files for references to other properties files and will include in the
     * final map data from all referenced files.
     * 
     * @param resourceBundle The initial <tt>ResourceBundle</tt>, corresponding
     * to the "main" properties file.
     * @param resources A <tt>Map</tt> that would store the data.
     */
    private void initResources( ResourceBundle resourceBundle,
                                Map<String, String> resources)
    {
        Enumeration soundKeys = resourceBundle.getKeys();

        while (soundKeys.hasMoreElements())
        {
            String key = (String) soundKeys.nextElement();
            String value = resourceBundle.getString(key);

            if (key.startsWith("$reference"))
            {
                ResourceBundle referenceBundle
                    = ResourceBundle.getBundle(value);

                initResources(referenceBundle, resources);
            }
            else
            {
                resources.put(key, value);
            }
        }
    }
}
