/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.demuxcontactsource;

import net.java.sip.communicator.service.contactsource.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the demux contact source plug-in.
 *
 * @author Yana Stamcheva
 */
public class DemuxContactSourceActivator
    implements BundleActivator
{
    private ServiceRegistration demuxServiceRegistration;

    /**
     * Starts the demux contact source plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the demux
     * contact source plug-in is to be started
     * @throws Exception if anything goes wrong while starting the demux
     * contact source plug-in
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        // Registers the service implementation provided by this plugin.
        demuxServiceRegistration = bundleContext.registerService(
            DemuxContactSourceService.class.getName(),
            new DemuxContactSourceServiceImpl(),
            null);
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
    public void stop(BundleContext bundleContext) throws Exception
    {
        if (demuxServiceRegistration != null)
        {
            demuxServiceRegistration.unregister();
            demuxServiceRegistration = null;
        }
    }
}
