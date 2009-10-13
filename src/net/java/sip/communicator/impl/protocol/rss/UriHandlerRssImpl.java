/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The RSS implementation of the URI handler. This class handles RSS feeds by
 * adding them to your contact list.
 *
 * @author Emil Ivov
 */
public class UriHandlerRssImpl
    implements UriHandler,
               ServiceListener

{
    private static final Logger logger =
        Logger.getLogger(UriHandlerRssImpl.class);

    /**
     * A reference to the OSGi registration we create with this handler.
     */
    private ServiceRegistration ourServiceRegistration = null;

    /**
     * The object that we are using to synchronize our service registration.
     */
    private final Object registrationLock = new Object();

    /**
     * Creates an instance of this uri handler, so that it would start handling
     * URIs by passing them to the RSS providers.
     */
    protected UriHandlerRssImpl()
        throws NullPointerException
    {
    }

    /**
     * Registers this UriHandler with the bundle context so that it could
     * start handling URIs
     */
    public void registerHandlerService()
    {
        synchronized(registrationLock)
        {
            if (ourServiceRegistration != null)
            {
                // ... we are already registered (this is probably
                // happening during startup)
                return;
            }

            Hashtable<String, String> registrationProperties
                = new Hashtable<String, String>();

            registrationProperties
                .put(UriHandler.PROTOCOL_PROPERTY, getProtocol());

            ourServiceRegistration
                = RssActivator
                    .bundleContext
                        .registerService(
                            UriHandler.class.getName(),
                            this,
                            registrationProperties);
        }
    }

    /**
     * Unregisters this UriHandler from the bundle context.
     */
    public void unregisterHandlerService()
    {
        synchronized(registrationLock)
        {
            ourServiceRegistration.unregister();
            ourServiceRegistration = null;
        }
    }

    /**
     * Returns the protocol that this handler is responsible for. In this case
     * this would be the "feed" protocol.
     *
     * @return the protocol that this handler is responsible for.
     */
    public String getProtocol()
    {
        return "feed";
    }

    /**
     * Parses the specified URI and creates a call with the currently active
     * telephony operation set.
     *
     * @param uri the feed: URI that we have to call.
     */
    public void handleUri(String uri)
    {
        byte[] icon = RssActivator.getResources()
            .getImageInBytes("pageImageRss");
        int answer = RssActivator.getUIService().getPopupDialog()
            .showConfirmPopupDialog(
                        "Would you like to add the following feed to "
                        +"your list of contacts?\n"
                        +uri,
                        "Add RSS feed?",
                        PopupDialog.YES_NO_OPTION,
                        PopupDialog.QUESTION_MESSAGE,
                        icon);

        if (answer != PopupDialog.YES_OPTION)
        {
            return;
        }

        ProtocolProviderService provider;
        try
        {
            provider = getRssProvider();
        }
        catch (OperationFailedException exc)
        {
            // The operation has been canceled by the user. Bail out.
            logger.trace("User canceled handling of uri " + uri);
            return;
        }

        //if provider is null then we need to tell the user to create an account
        if(provider == null)
        {
            showErrorMessage(
                "You need to configure at least one "
                + "RSS" +" account to be able to add the feed\n"
                + uri,
                null);
            return;
        }

        OperationSetPresence presenceOpSet
            = provider.getOperationSet(OperationSetPresence.class);

        try
        {
            presenceOpSet.subscribe(uri);
        }
        catch (OperationFailedException exc)
        {
            showErrorMessage("Failed to subscribe to the following feed\n"
                            + uri,
                            exc);
        }
    }

    /**
     * The point of implementing a service listener here is so that we would
     * only register our own uri handling service and thus only handle URIs
     * while the factory is available as an OSGi service. We remove ourselves
     * when our factory unregisters its service reference.
     *
     * @param event the OSGi <tt>ServiceEvent</tt>
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sourceService
            = RssActivator
                .bundleContext.getService(event.getServiceReference());

        //ignore anything but our protocol factory.
        if (!(sourceService instanceof ProtocolProviderFactoryRssImpl))
            return;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            //our factory has just been registered as a service ...
            registerHandlerService();
            break;

        case ServiceEvent.UNREGISTERING:
            //our factory just died - seppuku.
            unregisterHandlerService();
            break;

        case ServiceEvent.MODIFIED:
        default:
            //we don't care.
            break;
        }
    }

    /**
     * Uses the <tt>UIService</tt> to show an error <tt>message</tt> and log
     * and <tt>exception</tt>.
     *
     * @param message the message that we'd like to show to the user.
     * @param exc the exception that we'd like to log
     */
    private void showErrorMessage(String message, Exception exc)
    {
        RssActivator.getUIService().getPopupDialog().showMessagePopupDialog(
                        message,
                        "Failed to create call!",
                        PopupDialog.ERROR_MESSAGE);
        logger.error(message, exc);
    }

    /**
     * Returns the default provider that we are supposed to add feeds to.
     *
     * @return the provider that we should add the  URIs to or null if no
     * provider was found.
     */
    public ProtocolProviderService getRssProvider()
        throws OperationFailedException
    {
        //get a reference to the provider
        ServiceReference[] serRefs = null;
        String osgiFilter = "(" + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.RSS + ")";

        try {
            serRefs = RssActivator.bundleContext.getServiceReferences(
                ProtocolProviderService.class.getName(), osgiFilter);
        } catch (InvalidSyntaxException ise)
        {
            //shouldn't happen as the filter is static (typos maybe? :D)
            return null;
        }

        if(serRefs == null || serRefs.length == 0)
            return null;

        return (ProtocolProviderService)RssActivator.getBundleContext()
            .getService(serRefs[0]);
    }
}
