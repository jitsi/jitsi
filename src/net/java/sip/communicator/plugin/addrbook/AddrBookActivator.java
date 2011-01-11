/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the addrbook plug-in which provides
 * support for OS-specific Address Book.
 *
 * @author Lyubomir Marinov
 */
public class AddrBookActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>AddrBookActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AddrBookActivator.class);

    /**
     * The <tt>BundleContext</tt> in which the addrbook plug-in is started.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ContactSourceService</tt> implementation for the OS-specific
     * Address Book.
     */
    private ContactSourceService css;

    /**
     * The <tt>ServiceRegistration</tt> of {@link #css} in the
     * <tt>BundleContext</tt> in which this <tt>AddrBookActivator</tt> has been
     * started.
     */
    private ServiceRegistration cssServiceRegistration;

    /**
     * The cached reference to the <tt>PhoneNumberI18nService</tt> instance used
     * by the functionality of the addrbook plug-in and fetched from its
     * <tt>BundleContext</tt>.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * Gets the <tt>PhoneNumberI18nService</tt> to be used by the functionality
     * of the addrbook plug-in.
     *
     * @return the <tt>PhoneNumberI18nService</tt> to be used by the
     * functionality of the addrbook plug-in
     */
    public static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if (phoneNumberI18nService == null)
        {
            phoneNumberI18nService
                = ServiceUtils.getService(
                        bundleContext,
                        PhoneNumberI18nService.class);
        }
        return phoneNumberI18nService;
    }

    /**
     * Starts the addrbook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the addrbook
     * plug-in is to be started
     * @throws Exception if anything goes wrong while starting the addrbook
     * plug-in
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        AddrBookActivator.bundleContext = bundleContext;

        bundleContext.registerService(
                PhoneNumberI18nService.class.getName(),
                new PhoneNumberI18nServiceImpl(),
                null);

        /* Register the ContactSourceService implementation (if any). */
        String cssClassName;

        if (OSUtils.IS_WINDOWS)
        {
            cssClassName
                = "net.java.sip.communicator.plugin.addrbook"
                    + ".msoutlook.MsOutlookAddrBookContactSourceService";
        }
        else if (OSUtils.IS_MAC)
        {
            cssClassName
                = "net.java.sip.communicator.plugin.addrbook"
                    + ".macosx.MacOSXAddrBookContactSourceService";
        }
        else
            return;

        try
        {
            css
                = (ContactSourceService)
                    Class.forName(cssClassName).newInstance();
        }
        catch (Exception ex)
        {
            logger.error("Failed to instantiate " + cssClassName, ex);
            return;
        }
        try
        {
            cssServiceRegistration
                = bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        css,
                        null);
        }
        finally
        {
            if (cssServiceRegistration == null)
            {
                if (css instanceof AsyncContactSourceService)
                    ((AsyncContactSourceService) css).stop();
                css = null;
            }
        }
    }

    /**
     * Stops the addrbook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the addrbook
     * plug-in is to be stopped
     * @throws Exception if anything goes wrong while stopping the addrbook
     * plug-in
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            if (cssServiceRegistration != null)
            {
                cssServiceRegistration.unregister();
                cssServiceRegistration = null;
            }
        }
        finally
        {
            if (css != null)
            {
                if (css instanceof AsyncContactSourceService)
                    ((AsyncContactSourceService) css).stop();
                css = null;
            }
        }
    }
}
