/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import net.java.sip.communicator.service.contactsource.*;
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

        css = (ContactSourceService) Class.forName(cssClassName).newInstance();
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
