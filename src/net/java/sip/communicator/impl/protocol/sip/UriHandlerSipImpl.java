/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.text.*;
import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The sip implementation of the URI handler. This class handles sip URIs by
 * trying to establish a call to them.
 *
 * @author Emil Ivov
 */
public class UriHandlerSipImpl
    implements UriHandler,
               ServiceListener

{
    private static final Logger logger =
        Logger.getLogger(UriHandlerSipImpl.class);

    /**
     * The protocol provider factory that created us.
     */
    private ProtocolProviderFactory protoFactory = null;

    /**
     * A reference to the OSGi registration we create with this handler.
     */
    private ServiceRegistration ourServiceRegistration = null;

    /**
     * The object that we are using to synchronize our service registration.
     */
    private Object registrationLock = new Object();

    /**
     * Creates an instance of this uri handler, so that it would start handling
     * URIs by passing them to the providers registered by <tt>protoFactory</tt>.
     *
     * @param parentProvider the provider that created us.
     *
     * @throws NullPointerException if <tt>protoFactory</tt> is <tt>null</tt>.
     */
    protected UriHandlerSipImpl(ProtocolProviderFactory protoFactory)
        throws NullPointerException
    {
        if(protoFactory == null)
        {
            throw new NullPointerException(
             "The ProtocolProviderFactory that a UriHandler is created with "
             + " cannot be null.");
        }

        this.protoFactory = protoFactory;

        //we listen for service events so that we can disable ourselves in
        //case our protocol factory decides to leave.
        SipActivator.bundleContext.addServiceListener(this);

        registerHandlerService();
    }

    /**
     * Registers this UriHandler with the bundle context so that it could
     * start handling URIs
     */
    private void registerHandlerService()
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

            registrationProperties.put(UriHandler.PROTOCOL_PROPERTY,
                            getProtocol());

            ourServiceRegistration = SipActivator.bundleContext
                            .registerService(UriHandler.class.getName(), this,
                                            registrationProperties);
        }

    }

    /**
     * Unregisters this UriHandler from the bundle context.
     */
    private void unregisterHandlerService()
    {
        synchronized(registrationLock)
        {
            ourServiceRegistration.unregister();
            ourServiceRegistration = null;
        }
    }

    /**
     * Returns the protocol that this handler is responsible for or "sip" in
     * other words.
     *
     * @return the "sip" string to indicate that this handler is responsible
     * for handling "sip" uris.
     */
    public String getProtocol()
    {
        return "sip";
    }

    /**
     * Parses the specified URI and creates a call with the currently active
     * telephony operation set.
     *
     * @param uri the SIP URI that we have to call.
     */
    public void handleUri(String uri)
    {
        ProtocolProviderService provider;
        try
        {
            provider = selectHandlingProvider(uri);
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
                + "SIP" +" account \n"
                +"to be able to call " + uri,
                null);
            return;
        }

        OperationSetBasicTelephony telephonyOpSet
            = (OperationSetBasicTelephony) provider
                .getOperationSet(OperationSetBasicTelephony.class);

        try
        {
            telephonyOpSet.createCall(uri);
        }
        catch (OperationFailedException exc)
        {
            //make sure that we prompt for registration only if it is really
            //required by the provider.
            if(exc.getErrorCode()
                            == OperationFailedException.PROVIDER_NOT_REGISTERED)
            {
                promptForRegistration(uri, provider);
            }
            showErrorMessage("Failed to create a call to " + uri, exc);
        }
        catch (ParseException exc)
        {
            showErrorMessage(
                            uri + " does not appear to be a valid SIP address",
                            exc);
        }
    }

    /**
     * Informs the user that they need to be registered before placing calls
     * and asks them whether they would like us to do it for them.
     *
     * @param uri the uri that the user would like us to call after registering.
     * @param provider the provider that we may have to reregister.
     */
    private void promptForRegistration(String uri,
                                       ProtocolProviderService provider)
    {
        int answer = SipActivator.getUIService()
            .getPopupDialog().showConfirmPopupDialog(
                    "You need to be online in order to make a call and your "
                    + "account is currently offline. Do want to connect now?",
                    "Account is currently offline",
                    PopupDialog.YES_NO_OPTION);

        if(answer == PopupDialog.YES_OPTION)
        {
            new ProtocolRegistrationThread(uri, provider).start();
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
        Object sourceService = SipActivator.bundleContext
            .getService(event.getServiceReference());

        //ignore anything but our protocol factory.
        if( ! (sourceService instanceof ProtocolProviderFactorySipImpl)
            || (sourceService != protoFactory))
        {
            return;
        }

        if(event.getType() == ServiceEvent.REGISTERED)
        {
            //our factory has just been registered as a service ...
            registerHandlerService();
        }
        else if(event.getType() == ServiceEvent.UNREGISTERING)
        {
            //our factory just died - seppuku.
            unregisterHandlerService();
        }
        else if(event.getType() == ServiceEvent.MODIFIED)
        {
            //we don't care.
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
        SipActivator.getUIService().getPopupDialog().showMessagePopupDialog(
                        message,
                        "Failed to create call!",
                        PopupDialog.ERROR_MESSAGE);
        logger.error(message, exc);
    }

    /**
     * We use this class when launching a provider registration by ourselves in
     * order to track for provider registration states and retry uri handling,
     * once the provider is registered.
     *
     */
    private class ProtocolRegistrationThread
        extends Thread
        implements RegistrationStateChangeListener
    {

        private ProtocolProviderService handlerProvider = null;

        /**
         * The URI that we'd need to re-call.
         */
        private String uri = null;

        /**
         * Configures this thread register our parent provider and re-attempt
         * connection to the specified <tt>uri</tt>.
         *
         * @param uri the uri that we need to handle.
         * @param handlerProvider the provider that we are going to make
         * register and that we are going to use to handle the <tt>uri</tt>.
         */
        public ProtocolRegistrationThread(
                        String uri,
                        ProtocolProviderService handlerProvider)
        {
            super("UriHandlerProviderRegistrationThread:uri=" + uri);
            this.uri = uri;
            this.handlerProvider = handlerProvider;
        }

        /**
         * Starts the registration process, ads this class as a registration
         * listener and then tries to rehandle the uri this thread was initiaded
         * with.
         */
        @Override
        public void run()
        {
            handlerProvider.addRegistrationStateChangeListener(this);

            try
            {
                handlerProvider.register(SipActivator.getUIService()
                                .getDefaultSecurityAuthority(handlerProvider));
            }
            catch (OperationFailedException exc)
            {
                logger.error("Failed to manually register provider.");
                logger.warn(exc.getMessage(), exc);
            }
        }


        /**
         * If the parent provider passes into the registration state, the method
         * re-handles the URI that this thread was initiated with. The method
         * would only rehandle the uri if the event shows successful
         * registration. It would ignore intermediate states such as
         * REGISTERING. Disconnection and failure events would simply cause this
         * listener to remove itself from the list of registration listeners.
         *
         * @param evt the <tt>RegistrationStateChangeEvent</tt> that this
         * thread was initiated with.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                Thread uriRehandleThread = new Thread()
                {
                    public void run()
                    {
                        handleUri(uri);
                    }
                };

                uriRehandleThread.setName("UriRehandleThread:uri="+uri);
                uriRehandleThread.start();
            }

            //we're only interested in a single event so we stop listening
            //(unless this was a REGISTERING notification)
            if(evt.getNewState() == RegistrationState.REGISTERING)
                return;

            handlerProvider.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Returns the default provider that we are supposed to handle URIs through
     * or null if there aren't any. Depending on the implementation this
     * method may require user intervention so make sure you don't rely on
     * a quick outcome when calling it.
     *
     * @param uri the uri that we'd like to handle with the provider that we are
     * about to select.
     *
     * @return the provider that we should handle URIs through.
     *
     * @throws OperationFailedException with code <tt>OPERATION_CANCELED</tt>
     * if the users.
     */
    public ProtocolProviderService selectHandlingProvider(String uri)
        throws OperationFailedException
    {
        ArrayList<AccountID> registeredAccounts
            = protoFactory.getRegisteredAccounts();

        //if we don't have any providers - return null.
        if(registeredAccounts.size() == 0)
        {
            return null;
        }


        //if we only have one provider - select it
        if(registeredAccounts.size() == 1)
        {
            ServiceReference providerReference
                = protoFactory.getProviderForAccount(registeredAccounts.get(0));

            ProtocolProviderService provider = (ProtocolProviderService)
                SipActivator.getBundleContext().getService(providerReference);

            return provider;
        }

        //otherwise - ask the user.
        ArrayList<ProviderComboBoxEntry> providers
            = new ArrayList<ProviderComboBoxEntry>();
        for (AccountID accountID : registeredAccounts)
        {
            ServiceReference providerReference
                = protoFactory.getProviderForAccount(accountID);

            ProtocolProviderService provider = (ProtocolProviderService)
                SipActivator.getBundleContext().getService(providerReference);

            providers.add( new ProviderComboBoxEntry( provider ) );
        }

        Object result = SipActivator.getUIService().getPopupDialog()
            .showInputPopupDialog(
                "Please select the account that you would like \n"
                + "to use to call "
                + uri,
                "Account Selection",
                PopupDialog.OK_CANCEL_OPTION,
                providers.toArray(),
                providers.get(0));

        if( result == null)
        {
            throw new OperationFailedException(
                            "Operation cancelled",
                            OperationFailedException.OPERATION_CANCELED);
        }

        return ((ProviderComboBoxEntry)result).provider;
    }

    /**
     * A class that we use to wrap providers before showing them to the user
     * through a selection popup dialog from the UIService.
     */
    private class ProviderComboBoxEntry
    {
        public ProtocolProviderService provider;

        public ProviderComboBoxEntry(ProtocolProviderService provider)
        {
            this.provider = provider;
        }

        /**
         * Returns a human readable <tt>String</tt> representing the
         * provider encapsulated by this class.
         *
         * @return a human readable string representing the provider.
         */
        @Override
        public String toString()
        {
            return provider.getAccountID().getAccountAddress();
        }
    }
}
