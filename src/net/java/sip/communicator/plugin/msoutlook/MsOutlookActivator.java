/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msoutlook;

import net.java.sip.communicator.service.contactsource.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the msoutlook plug-in which provides
 * support for Microsoft Outlook.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookActivator
    implements BundleActivator
{
    /**
     * The <tt>MsOutlookAddressBookContactSourceService</tt> which implements
     * <tt>ContactSourceService</tt> for the Address Book of Microsoft Outlook.
     */
    private MsOutlookAddressBookContactSourceService msoabcss;

    /**
     * The <tt>ServiceRegistration</tt> of {@link #msoabcss} in the
     * <tt>BundleContext</tt> in which this <tt>MsOutlookActivator</tt> has been
     * started.
     */
    private ServiceRegistration msoabcssServiceRegistration;

    /**
     * Starts the msoutlook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the msoutlook
     * plug-in is to be started
     * @throws Exception if anything goes wrong while starting the msoutlook
     * plug-in
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        msoabcss = new MsOutlookAddressBookContactSourceService();
        try
        {
            msoabcssServiceRegistration
                = bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        msoabcss,
                        null);
        }
        finally
        {
            if (msoabcssServiceRegistration == null)
            {
                msoabcss.stop();
                msoabcss = null;
            }
        }
    }

    /**
     * Stops the msoutlook plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the msoutlook
     * plug-in is to be stopped
     * @throws Exception if anything goes wrong while stopping the msoutlook
     * plug-in
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        try
        {
            if (msoabcssServiceRegistration != null)
            {
                msoabcssServiceRegistration.unregister();
                msoabcssServiceRegistration = null;
            }
        }
        finally
        {
            if (msoabcss != null)
            {
                msoabcss.stop();
                msoabcss = null;
            }
        }
    }
}
