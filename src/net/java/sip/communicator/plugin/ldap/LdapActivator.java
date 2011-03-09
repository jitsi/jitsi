/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ldap;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the LDAP plug-in which provides
 * support for LDAP contact sources.
 *
 * @author Sebastien Vincent
 */
public class LdapActivator implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> in which the LDAP plug-in is started.
     */
    private static BundleContext bundleContext = null;

    /**
     * LDAP service.
     */
    private static LdapService ldapService = null;

    /**
     * The <tt>ContactSourceService</tt> implementation for the OS-specific
     * Address Book.
     */
    private LdapContactSourceService css = null;

    /**
     * The <tt>ServiceRegistration</tt> of {@link #css} in the
     * <tt>BundleContext</tt> in which this <tt>LdapActivator</tt> has been
     * started.
     */
    private ServiceRegistration cssServiceRegistration = null;

    /**
     * The cached reference to the <tt>PhoneNumberI18nService</tt> instance used
     * by the functionality of the LDAP plug-in and fetched from its
     * <tt>BundleContext</tt>.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * Starts the LDAP plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the LDAP
     * plug-in is to be started
     * @throws Exception if anything goes wrong while starting the LDAP
     * plug-in
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        LdapActivator.bundleContext = bundleContext;

        css = new LdapContactSourceService();

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
                css.stop();
                css = null;
            }
        }
    }

    /**
     * Get LDAP service.
     *
     * @return LDAP service
     */
    public static LdapService getLdapService()
    {
        if(ldapService == null)
        {
            ldapService = ServiceUtils.getService(bundleContext,
                    LdapService.class);
        }

        return ldapService;
    }

    /**
     * Stops the LDAP plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the LDAP
     * plug-in is to be stopped
     * @throws Exception if anything goes wrong while stopping the LDAP
     * plug-in
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext bundleContext) throws Exception
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
                css.stop();
            }
        }
    }

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
}
