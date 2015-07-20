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
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import java.util.*;

/**
 * OSGi Activator for the Busy Lamp Field Plugin.
 *
 * @author Damian Minkov
 */
public class BLFActivator
    implements BundleActivator,
               ServiceListener,
               BLFStatusListener
{
    /**
     * The bundle context.
     */
    static BundleContext bundleContext;

    /**
     * Property to enable/disable this plugin.
     */
    private static final String BLF_PLUGIN_ENABLED =
        "net.java.sip.communicator.plugin.busylampfield.ENABLED";

    /**
     * The name of default group name if none is present.
     */
    private static final String BLF_DEFAULT_GROUP_NAME = "Monitored Lines";

    /**
     * List of currently registered contact source services for the
     * corresponding group.
     */
    private Map<String, ServiceRegistration<ContactSourceService>>
        currentBLFGroups = new LinkedHashMap
            <String, ServiceRegistration<ContactSourceService>>();

    /**
     * Starts implementation.
     *
     * @param bundleContext
     * @throws Exception
     */
    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        ConfigurationService config = ServiceUtils.getService(
            bundleContext, ConfigurationService.class);

        if(!config.getBoolean(BLF_PLUGIN_ENABLED, false))
            return;

        BLFActivator.bundleContext = bundleContext;

        bundleContext.addServiceListener(this);

        initProviders();

        bundleContext.registerService(
            CustomContactActionsService.class.getName(),
            new CustomActionsBLFSourceContact(),
            null
        );
    }

    /**
     * Stops.
     * @param bundleContext
     * @throws Exception
     */
    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {}

    /**
     * Listens for registered providers.
     * @param serviceEvent
     */
    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference<?> serviceRef = serviceEvent.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if(serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        Object service = bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if(!(service instanceof ProtocolProviderService))
            return;

        switch(serviceEvent.getType())
        {
            case ServiceEvent.REGISTERED:
                handleProviderAdded((ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                handleProviderRemoved((ProtocolProviderService) service);
                break;
        }
    }

    /**
     * Initializes all currently registered protocol providers that contain
     * the desired operation set.
     */
    private void initProviders()
    {
        for(ServiceReference<ProtocolProviderService> serRef :
                ServiceUtils.getServiceReferences(
                    bundleContext, ProtocolProviderService.class))
        {
            handleProviderAdded(bundleContext.getService(serRef));
        }
    }

    /**
     * Adds a protocol provider, when such provider is registered.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to add
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider)
    {
        OperationSetTelephonyBLF opset =
            protocolProvider.getOperationSet(OperationSetTelephonyBLF.class);

        if(opset == null)
            return;

        opset.addStatusListener(this);

        for(OperationSetTelephonyBLF.Line line
            : opset.getCurrentlyMonitoredLines())
        {
            String groupName = line.getGroup();
            if(groupName == null)
                groupName = BLF_DEFAULT_GROUP_NAME;

            ServiceRegistration<ContactSourceService> serviceReg
                = currentBLFGroups.get(groupName);

            if(serviceReg != null)
            {
                BLFContactSourceService css
                    = (BLFContactSourceService)bundleContext.getService(
                        serviceReg.getReference());
                css.addLine(line);

                continue;
            }
            else
            {
                BLFContactSourceService css
                    = new BLFContactSourceService(
                    groupName, currentBLFGroups.size() + 1);

                serviceReg = (ServiceRegistration<ContactSourceService>)
                    bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        css,
                        null);
                currentBLFGroups.put(groupName, serviceReg);

                css.addLine(line);
            }
        }
    }

    /**
     * Removes a protocol provider from the list when unregistered.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to remove
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider)
    {
        OperationSetTelephonyBLF opset =
            protocolProvider.getOperationSet(OperationSetTelephonyBLF.class);

        if(opset == null)
            return;

        opset.removeStatusListener(this);

        for(OperationSetTelephonyBLF.Line line
            : opset.getCurrentlyMonitoredLines())
        {
            String groupName = line.getGroup();
            if(groupName == null)
                groupName = BLF_DEFAULT_GROUP_NAME;

            ServiceRegistration<ContactSourceService> serviceReg
                = currentBLFGroups.remove(groupName);

            if(serviceReg == null)
                continue;

            serviceReg.unregister();
        }
    }

    /**
     * Called whenever a change occurs in the BLFStatus of one of the
     * monitored lines that we have subscribed for.
     * @param event the BLFStatusEvent describing the status change.
     */
    @Override
    public void blfStatusChanged(BLFStatusEvent event)
    {
        if(!(event.getSource() instanceof OperationSetTelephonyBLF.Line))
            return;

        OperationSetTelephonyBLF.Line line
            = (OperationSetTelephonyBLF.Line)event.getSource();

        String gr = line.getGroup();
        if(gr == null)
            gr = BLF_DEFAULT_GROUP_NAME;

        ServiceRegistration<ContactSourceService> serviceReg
            = currentBLFGroups.get(gr);

        if(serviceReg == null)
            return;

        BLFContactSourceService css
            = (BLFContactSourceService)bundleContext.getService(
                serviceReg.getReference());
        css.updateLineStatus(line, event.getType());
    }
}
