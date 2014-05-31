/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ciscodirectory;

import org.jitsi.service.configuration.*;

/**
 * This object contains the configuration for one Cisco directory. Changes to
 * this object are also applied to the Jitsi configuration.
 *
 * @author Fabien Cortina <fabien.cortina@gmail.com>
 */
final class DirectorySettings
{
    private final static String PROP_ENABLED = "ENABLED";
    private final static String PROP_DIRECTORY_URL = "DIRECTORY_URL";
    private final static String PROP_PHONE_PREFIX = "PHONE_PREFIX";

    private final ConfigurationService config;
    private final String base;

    private boolean enabled;
    private String directoryUrl;
    private String prefix;

    /**
     * Creates a setting wrapper for a given configuration service and root
     * property.
     *
     * @param config the configuration service used to read/write the settings.
     * @param root the property under which other properties are stored.
     */
    DirectorySettings(ConfigurationService config, String root)
    {
        this.config = config;
        this.base = root;

        setEnabled(config.getBoolean(root + "." + PROP_ENABLED, false));
        setDirectoryUrl(config.getString(root + "." + PROP_DIRECTORY_URL));
        setPrefix(config.getString(root + "." + PROP_PHONE_PREFIX));
    }

    /**
     * @return true if the contact source is enabled.
     */
    boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Enables or disabled the contact source.
     *
     * @param enabled whether the source should be enabled or disabled.
     */
    void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        this.config.setProperty(base + "." + PROP_ENABLED, enabled);
    }

    /**
     * @return the URL of the Cisco directory service.
     */
    String getDirectoryUrl()
    {
        return directoryUrl;
    }

    /**
     * Sets the URL of the Cisco directory service.
     *
     * @param directoryUrl A typical value would be 
     * "http://SERVER:8080/ccmcip/xmldirectorylist.jsp"
     */
    void setDirectoryUrl(String directoryUrl)
    {
        this.directoryUrl = directoryUrl;
        this.config.setProperty(base + "." + PROP_DIRECTORY_URL, directoryUrl);
    }

    /**
     * @return the prefix to add before phone numbers returned by the source.
     */
    String getPrefix()
    {
        return prefix;
    }

    /**
     * Sets the prefix to add before phone numbers returned by the source.
     *
     * @param prefix the prefix to add before phone numbers.
     */
    void setPrefix(String prefix)
    {
        this.prefix = prefix;
        this.config.setProperty(base + "." + PROP_PHONE_PREFIX, prefix);
    }
}
