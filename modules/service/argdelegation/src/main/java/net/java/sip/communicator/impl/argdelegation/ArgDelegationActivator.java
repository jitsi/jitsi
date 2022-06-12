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
package net.java.sip.communicator.impl.argdelegation;

import java.awt.*;
import java.awt.Desktop.*;
import net.java.sip.communicator.launchutils.*;
import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.osgi.*;
import org.osgi.framework.*;

/**
 * Activates the <tt>ArgDelegationService</tt> and registers a URI delegation
 * peer with the util package arg manager so that we would be notified when the
 * application receives uri arguments.
 *
 * @author Emil Ivov
 */
public class ArgDelegationActivator
    extends DependentActivator
{
    /**
     * A reference to the delegation peer implementation that is currently
     * handling uri arguments.
     */
    private ArgDelegationPeerImpl delegationPeer = null;

    /**
     * A reference to the <tt>UIService</tt> currently in use in
     * SIP Communicator.
     */
    private static UIService uiService = null;

    public ArgDelegationActivator()
    {
        super(UIService.class);
    }

    /**
     * Starts the arg delegation bundle and registers the delegationPeer with
     * the util package URI manager.
     */
    public void startWithServices(BundleContext bundleContext)
    {
        uiService = getService(UIService.class);
        delegationPeer = new ArgDelegationPeerImpl(bundleContext);
        bundleContext.addServiceListener(delegationPeer);

        //register our instance of delegation peer.
        LaunchArgHandler.getInstance().setDelegationPeer(delegationPeer);

        if (Desktop.isDesktopSupported())
        {
            var desktop = Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(Action.APP_OPEN_URI))
            {
                try
                {
                    desktop.setOpenURIHandler(evt ->
                        delegationPeer.handleUri(evt.getURI().toString()));
                }
                catch (Exception ex)
                {
                    // ignore
                }
            }
        }
    }

    /**
     * Unsets the delegation peer instance that we set when we start this
     * bundle.
     *
     * @param bc an instance of the currently valid bundle context.
     * @throws Exception if unsetting the delegation peer instance that we set
     * when we start this bundle fails
     */
    public void stop(BundleContext bc) throws Exception
    {
        uiService = null;
        if (delegationPeer != null)
        {
            bc.removeServiceListener(delegationPeer);
            delegationPeer = null;
        }

        LaunchArgHandler.getInstance().setDelegationPeer(null);
    }

    /**
     * Returns a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        return uiService;
    }
}
