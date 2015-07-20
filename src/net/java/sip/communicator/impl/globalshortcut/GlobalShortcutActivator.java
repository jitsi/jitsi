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
package net.java.sip.communicator.impl.globalshortcut;

import java.util.*;

import net.java.sip.communicator.service.globalshortcut.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * OSGi Activator for global shortcut.
 *
 * @author Sebastien Vincent
 */
public class GlobalShortcutActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>GlobalShortcutActivator</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GlobalShortcutActivator.class);

    /**
     * The <tt>GlobalShortcutServiceImpl</tt>.
     */
    protected static GlobalShortcutServiceImpl globalShortcutService = null;

    /**
     * OSGi bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Keybindings service reference.
     */
    private static KeybindingsService keybindingsService = null;

    /**
     * UI service reference.
     */
    private static UIService uiService = null;

    /**
     * Returns the <tt>KeybindingsService</tt> obtained from the bundle context.
     *
     * @return the <tt>KeybindingsService</tt> obtained from the bundle context
     */
    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            keybindingsService
                = ServiceUtils.getService(
                        bundleContext,
                        KeybindingsService.class);
        }
        return keybindingsService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService
                = ServiceUtils.getService(
                        bundleContext,
                        UIService.class);
        }
        return uiService;
    }

    /**
     * Starts the execution of this service bundle in the specified context.
     *
     * @param bundleContext the context in which the service bundle is to
     * start executing
     * @throws Exception if an error occurs while starting the execution of the
     * service bundle in the specified context
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        GlobalShortcutActivator.bundleContext = bundleContext;

        globalShortcutService = new GlobalShortcutServiceImpl();
        globalShortcutService.start();
        bundleContext.registerService(
                GlobalShortcutService.class,
                globalShortcutService,
                null);

        globalShortcutService.reloadGlobalShortcuts();

        registerListenerWithProtocolProviderService();

        bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                GlobalShortcutActivator.this.serviceChanged(serviceEvent);
            }
        });
        if (logger.isDebugEnabled())
            logger.debug("GlobalShortcut Service ... [REGISTERED]");
    }

    /**
     * Stops the execution of this service bundle in the specified context.
     *
     * @param bundleContext the context in which this service bundle is to
     * stop executing
     * @throws Exception if an error occurs while stopping the execution of the
     * service bundle in the specified context
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        globalShortcutService.stop();

        GlobalShortcutActivator.bundleContext = null;
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    private void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService) service);
            break;
        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved((ProtocolProviderService) service);
            break;
        }
    }

    /**
     * Get all registered <tt>ProtocolProviderService</tt> and set our listener.
     */
    public void registerListenerWithProtocolProviderService()
    {
        Collection<ServiceReference<ProtocolProviderService>> ppsRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderService.class);

        if(!ppsRefs.isEmpty())
        {
            for(ServiceReference<ProtocolProviderService> ppsRef : ppsRefs)
            {
                ProtocolProviderService pps = bundleContext.getService(ppsRef);
                OperationSetBasicTelephony<?> opSet
                    = pps.getOperationSet(OperationSetBasicTelephony.class);

                if(opSet != null)
                {
                    opSet.addCallListener(
                            globalShortcutService.getCallShortcut());
                }
            }
        }
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been registered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * registered as a service.
     */
    private void handleProviderAdded(final ProtocolProviderService provider)
    {
        OperationSetBasicTelephony<?> opSet =
            provider.getOperationSet(OperationSetBasicTelephony.class);
        if(opSet != null)
            opSet.addCallListener(globalShortcutService.getCallShortcut());
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been unregistered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * unregistered as a service.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        OperationSetBasicTelephony<?> opSet =
            provider.getOperationSet(OperationSetBasicTelephony.class);
        if(opSet != null)
            opSet.removeCallListener(globalShortcutService.getCallShortcut());
    }
}
