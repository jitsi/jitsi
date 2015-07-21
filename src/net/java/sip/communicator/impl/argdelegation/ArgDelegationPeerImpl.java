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

import java.util.*;

import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.launchutils.*;

import org.osgi.framework.*;

/**
 * Implements the <tt>UriDelegationPeer</tt> interface from our argument handler
 * utility. We use this handler to relay arguments to URI handlers that have
 * been registered from other services such as the SIP provider for example.
 *
 * @author Emil Ivov
 */
public class ArgDelegationPeerImpl
    implements ArgDelegationPeer,
               ServiceListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ArgDelegationPeerImpl</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ArgDelegationPeerImpl.class);

    /**
     * The list of uriHandlers that we are currently aware of.
     */
    private final Map<String, UriHandler> uriHandlers
        = new Hashtable<String, UriHandler>();

    /**
     * Creates an instance of this peer and scans <tt>bundleContext</tt> for all
     * existing <tt>UriHandler</tt>
     *
     * @param bundleContext a reference to a currently valid instance of a
     * bundle context.
     */
    public ArgDelegationPeerImpl(BundleContext bundleContext)
    {
        Collection<ServiceReference<UriHandler>> uriHandlerRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    UriHandler.class);

        if (!uriHandlerRefs.isEmpty())
        {
            synchronized (uriHandlers)
            {
                for (ServiceReference<UriHandler> uriHandlerRef
                        : uriHandlerRefs)
                {
                    UriHandler uriHandler
                        = bundleContext.getService(uriHandlerRef);

                    for (String protocol : uriHandler.getProtocol())
                    {
                        uriHandlers.put(protocol, uriHandler);
                    }
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
        BundleContext bc
            = event.getServiceReference().getBundle().getBundleContext();

        /*
         * TODO When the Update button of the plug-in manager is invoked for the
         * IRC protocol provider plug-in, bc is of value null and thus causes a
         * NullPointerException. Determine whether it is a problem (in general)
         * to not process ServiceEvent.UNREGISTERING in such a case.
         */
        if (bc == null)
            return;

        Object service = bc.getService(event.getServiceReference());

        //we are only interested in UriHandler-s
        if (!(service instanceof UriHandler))
            return;

        UriHandler uriHandler = (UriHandler) service;

        synchronized (uriHandlers)
        {
            switch (event.getType())
            {
            case ServiceEvent.MODIFIED:
            case ServiceEvent.REGISTERED:
                for (String protocol : uriHandler.getProtocol())
                {
                    uriHandlers.put(protocol, uriHandler);
                }
                break;

            case ServiceEvent.UNREGISTERING:
                for (String protocol : uriHandler.getProtocol())
                {
                    if(uriHandlers.get(protocol) == uriHandler)
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
    public void handleUri(String uriArg)
    {
        if (logger.isTraceEnabled())
            logger.trace("Handling URI: " + uriArg);
        //first parse the uri and determine the scheme/protocol
        //the parsing is currently a bit oversimplified so we'd probably need
        //to revisit it at some point.
        int colonIndex = uriArg.indexOf(":");

        if( colonIndex == -1)
        {
            //no scheme, we don't know how to handle the URI
            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                        "Could not determine how to handle: " + uriArg
                            + ".\nNo protocol scheme found.",
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            return;
        }

        String scheme = uriArg.substring(0, colonIndex);

        UriHandler handler;
        synchronized (uriHandlers) {
            handler = uriHandlers.get(scheme);
        }

        //if handler is null we need to tell the user.
        if(handler == null)
        {
            if (logger.isTraceEnabled())
                logger.trace("Couldn't open " + uriArg
                         + "No handler found for protocol"+ scheme);
            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                        "\"" + scheme + "\" URIs are currently not supported.",
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            return;
        }

        //we're all set. let's do the handling now.
        try
        {
            handler.handleUri(uriArg);
        }
        //catch every possible exception
        catch(Throwable thr)
        {
            // ThreadDeath should always be re-thrown.
            if (thr instanceof ThreadDeath)
                throw (ThreadDeath) thr;

            ArgDelegationActivator.getUIService().getPopupDialog()
                .showMessagePopupDialog(
                        "Error handling " + uriArg,
                        "Error handling URI",
                        PopupDialog.ERROR_MESSAGE);
            logger.error("Failed to handle \""+ uriArg +"\"", thr);
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
        ArgDelegationActivator.getUIService().setVisible(true);
    }
}

