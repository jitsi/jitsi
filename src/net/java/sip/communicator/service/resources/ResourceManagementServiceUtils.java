/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import java.util.*;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public final class ResourceManagementServiceUtils
{

    /**
     * Constructs a new <code>Locale</code> instance from a specific locale
     * identifier which can either be a two-letter language code or contain a
     * two-letter language code and a two-letter country code in the form
     * <code>&lt;language&gt;_&lt;country&gt;</code>.
     * 
     * @param localeId
     *            the locale identifier describing the new <code>Locale</code>
     *            instance to be created
     * @return a new <code>Locale</code> instance with language and country (if
     *         specified) matching the given locale identifier
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
     * Gets the <code>ResourceManagementService</code> instance registered in a
     * specific <code>BundleContext</code> (if any).
     * 
     * @param bundleContext
     *            the <code>BundleContext</code> to be checked for a registered
     *            <code>ResourceManagementService</code>
     * @return a <code>ResourceManagementService</code> instance registered in
     *         the specified <code>BundleContext</code> if any; otherwise,
     *         <tt>null</tt>
     */
    public static ResourceManagementService getService(
        BundleContext bundleContext)
    {
        ServiceReference ref
            = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

        return
            (ref == null)
                ? null
                : (ResourceManagementService) bundleContext.getService(ref);
    }

    /**
     * Prevents the creation of <code>ResourceManagementServiceUtils</code>
     * instances.
     */
    private ResourceManagementServiceUtils()
    {
    }
}
