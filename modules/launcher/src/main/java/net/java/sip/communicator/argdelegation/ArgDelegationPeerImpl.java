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
package net.java.sip.communicator.argdelegation;

import java.net.*;
import java.util.*;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.launchutils.ArgDelegationPeer;
import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.osgi.ServiceUtils;
import org.apache.commons.lang3.*;
import org.osgi.framework.*;

/**
 * Implements the <tt>UriDelegationPeer</tt> interface from our argument handler
 * utility. We use this handler to relay arguments to URI handlers that have
 * been registered from other services such as the SIP provider for example.
 *
 * @author Emil Ivov
 */
@Slf4j
public class ArgDelegationPeerImpl
    implements ArgDelegationPeer, ServiceListener
{
    /**
     * The list of uriHandlers that we are currently aware of.
     */
    private final Map<String, UriHandler> uriHandlers = new Hashtable<>();

    private final List<URI> recordedArgs = new ArrayList<>();

    private final UIService uiService;

    private final BundleContext bundleContext;

    /**
     * Creates an instance of this peer and scans <tt>bundleContext</tt> for all
     * existing <tt>UriHandler</tt>
     *
     * @param bundleContext a reference to a currently valid instance of a
     * bundle context.
     */
    public ArgDelegationPeerImpl(UIService uiService, BundleContext bundleContext)
    {
        this.uiService = uiService;
        this.bundleContext = bundleContext;
        var uriHandlerRefs = ServiceUtils.getServiceReferences(bundleContext, UriHandler.class);
        {
            for (var uriHandlerRef : uriHandlerRefs)
            {
                var uriHandler = bundleContext.getService(uriHandlerRef);
                for (var protocol : uriHandler.getProtocols())
                {
                    uriHandlers.put(protocol, uriHandler);
                }
            }
        }
    }

    /**
     * Listens for <tt>UriHandlers</tt> that are registered in the bundle
     * context after we had started so that we could add them to the list
     * of currently known handlers.
     *
     * @param event the event containing the newly (un)registered service.
     */
    public void serviceChanged(ServiceEvent event)
    {
        var bc = event.getServiceReference().getBundle().getBundleContext();
        if (bc == null)
        {
            return;
        }

        var service = bc.getService(event.getServiceReference());
        //we are only interested in UriHandler-s
        if (!(service instanceof UriHandler))
        {
            return;
        }

        UriHandler uriHandler = (UriHandler) service;
        synchronized (uriHandlers)
        {
            switch (event.getType())
            {
            case ServiceEvent.MODIFIED:
            case ServiceEvent.REGISTERED:
                for (String protocol : uriHandler.getProtocols())
                {
                    uriHandlers.put(protocol, uriHandler);
                }

                // Dispatch any arguments that were held back
                for (var uri : new ArrayList<>(recordedArgs))
                {
                    handleUri(uri);
                }
                break;

            case ServiceEvent.UNREGISTERING:
                for (String protocol : uriHandler.getProtocols())
                {
                    uriHandlers.remove(protocol);
                }

                break;
            }
        }
    }

    /**
     * Relays <tt>uirArg</tt> to the corresponding handler or shows an error
     * message in case no handler has been registered for the corresponding
     * protocol.
     *
     * @param uriArg the uri that we've been passed and that we'd like to
     * delegate to the corresponding provider.
     */
    @Override
    public void handleUri(URI uriArg)
    {
        logger.trace("Handling URI: {}", uriArg);

        //first parse the uri and determine the scheme/protocol
        if (uriArg == null || StringUtils.isEmpty(uriArg.getScheme()))
        {
            //no scheme, we don't know how to handle the URI
            uiService.getPopupDialog()
                .showMessagePopupDialog(
                        "Could not determine how to handle: " + uriArg
                            + ".\nNo protocol scheme found.",
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            return;
        }

        var scheme = uriArg.getScheme();
        UriHandler handler;
        synchronized (uriHandlers)
        {
            handler = uriHandlers.get(scheme);
        }

        //if handler is null we need to tell the user.
        if (handler == null)
        {
            recordedArgs.remove(uriArg);
            if (Arrays.stream(bundleContext.getBundles()).allMatch(b -> b.getState() == Bundle.INSTALLED))
            {
                logger.warn("Couldn't open {}. No handler found for protocol {}", uriArg, scheme);
                uiService.getPopupDialog()
                    .showMessagePopupDialog(
                            "\"" + scheme + "\" URIs are currently not supported.",
                            "Error handling URI",
                            PopupDialog.ERROR_MESSAGE);
            }
            else
            {
                recordedArgs.add(uriArg);
            }

            return;
        }

        //we're all set. let's do the handling now.
        try
        {
            handler.handleUri(uriArg);
        }
        catch (Exception ex)
        {
            uiService.getPopupDialog()
                .showMessagePopupDialog(
                        "Error handling " + uriArg,
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            logger.error("Failed to handle {}", uriArg, ex);
        }
    }

    /**
     * This method would simply bring the application on focus as it is called
     * when the user has tried to launch a second instance of SIP Communicator
     * while a first one was already running.  Future implementations may also
     * show an error/information message to the user notifying them that a
     * second instance is not to be launched.
     */
    public void handleConcurrentInvocationRequest()
    {
        uiService.setVisible(true);
    }
}

