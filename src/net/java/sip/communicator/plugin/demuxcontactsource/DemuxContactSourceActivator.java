/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
