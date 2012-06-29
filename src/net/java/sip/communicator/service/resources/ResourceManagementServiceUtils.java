/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public final class ResourceManagementServiceUtils
{

    /**
     * Constructs a new <tt>Locale</tt> instance from a specific locale
     * identifier which can either be a two-letter language code or contain a
     * two-letter language code and a two-letter country code in the form
     * <tt>&lt;language&gt;_&lt;country&gt;</tt>.
     * 
     * @param localeId the locale identifier describing the new <tt>Locale</tt>
     * instance to be created
     * @return a new <tt>Locale</tt> instance with language and country (if
     * specified) matching the given locale identifier
     */
    public static Locale getLocale(String localeId)
    {
        int underscoreIndex = localeId.indexOf('_');
        String language;
        String country;

        if (underscoreIndex == -1)
        {
            language = localeId;
            country = "";
        }
        else
        {
            language = localeId.substring(0, underscoreIndex);
            country = localeId.substring(underscoreIndex + 1);
        }
        return new Locale(language, country);
    }

    /**
     * Gets the <tt>ResourceManagementService</tt> instance registered in a
     * specific <tt>BundleContext</tt> (if any).
     * 
     * @param bundleContext the <tt>BundleContext</tt> to be checked for a
     * registered <tt>ResourceManagementService</tt>
     * @return a <tt>ResourceManagementService</tt> instance registered in
     * the specified <tt>BundleContext</tt> if any; otherwise, <tt>null</tt>
     */
    public static ResourceManagementService getService(
        BundleContext bundleContext)
    {
        return
            ServiceUtils.getService(
                    bundleContext,
                    ResourceManagementService.class);
    }

    /**
     * Prevents the creation of <tt>ResourceManagementServiceUtils</tt>
     * instances.
     */
    private ResourceManagementServiceUtils()
    {
    }
}
