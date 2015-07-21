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
package net.java.sip.communicator.impl.protocol.jabber;

import java.beans.*;
import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.argdelegation.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The jabber implementation of the URI handler. This class handles xmpp URIs by
 * trying to establish a chat with them or add you to a chatroom.
 *
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class UriHandlerJabberImpl
    implements UriHandler, ServiceListener, AccountManagerListener
{
    /**
     * The logger of this class.
     */
    private static final Logger logger =
        Logger.getLogger(UriHandlerJabberImpl.class);

    /**
     * The protocol provider factory that created us.
     */
    private final ProtocolProviderFactory protoFactory;

    /**
     * A reference to the OSGi registration we create with this handler.
     */
    private ServiceRegistration ourServiceRegistration = null;

    /**
     * The object that we are using to synchronize our service registration.
     */
    private final Object registrationLock = new Object();

    /**
     * The <code>AccountManager</code> which loads the stored accounts of
     * {@link #protoFactory} and to be monitored when the mentioned loading is
     * complete so that any pending {@link #uris} can be handled
     */
    private AccountManager accountManager;

    /**
     * The indicator (and its synchronization lock) which determines whether the
     * stored accounts of {@link #protoFactory} have already been loaded.
     * <p>
     * Before the loading of the stored accounts (even if there're none) of the
     * <code>protoFactory</code> is complete, no handling of URIs is to be
     * performed because there's neither information which account to handle the
     * URI in case there're stored accounts available nor ground for warning the
     * user a registered account is necessary to handle URIs at all in case
     * there're no stored accounts.
     * </p>
     */
    private final boolean[] storedAccountsAreLoaded = new boolean[1];

    /**
     * The list of URIs which have received requests for handling before the
     * stored accounts of the {@link #protoFactory} have been loaded. They will
     * be handled as soon as the mentioned loading completes.
     */
    private List<String> uris;

    /**
     * Marks network fails in order to avoid endless loops.
     */
    private boolean networkFailReceived = false;

    /**
     * Creates an instance of this uri handler, so that it would start handling
     * URIs by passing them to the providers registered by <tt>protoFactory</tt>
     * .
     *
     * @param protoFactory the provider that created us.
     *
     * @throws NullPointerException if <tt>protoFactory</tt> is <tt>null</tt>.
     */
    public UriHandlerJabberImpl(ProtocolProviderFactory protoFactory)
        throws NullPointerException
    {
        if (protoFactory == null)
        {
            throw new NullPointerException(
                "The ProtocolProviderFactory that a UriHandler is created with "
                    + " cannot be null.");
        }

        this.protoFactory = protoFactory;

        hookStoredAccounts();

        this.protoFactory.getBundleContext().addServiceListener(this);
        /*
         * Registering the UriHandler isn't strictly necessary if the
         * requirement to register the protoFactory after creating this instance
         * is met.
         */
        registerHandlerService();
    }

    /**
     * Disposes of this <code>UriHandler</code> by, for example, removing the
     * listeners it has added in its constructor (in order to prevent memory
     * leaks, for one).
     */
    public void dispose()
    {
        protoFactory.getBundleContext().removeServiceListener(this);
        unregisterHandlerService();

        unhookStoredAccounts();
    }

    /**
     * Sets up (if not set up already) listening for the loading of the stored
     * accounts of {@link #protoFactory} in order to make it possible to
     * discover when the prerequisites for handling URIs are met.
     */
    private void hookStoredAccounts()
    {
        if (accountManager == null)
        {
            BundleContext bundleContext = protoFactory.getBundleContext();

            accountManager =
                (AccountManager) bundleContext.getService(bundleContext
                    .getServiceReference(AccountManager.class.getName()));
            accountManager.addListener(this);
        }
    }

    /**
     * Reverts (if not reverted already) the setup performed by a previous chat
     * to {@link #hookStoredAccounts()}.
     */
    private void unhookStoredAccounts()
    {
        if (accountManager != null)
        {
            accountManager.removeListener(this);
            accountManager = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.java.sip.communicator.service.protocol.event.AccountManagerListener
     * #handleAccountManagerEvent
     * (net.java.sip.communicator.service.protocol.event.AccountManagerEvent)
     */
    public void handleAccountManagerEvent(AccountManagerEvent event)
    {

        /*
         * When the loading of the stored accounts of protoFactory is complete,
         * the prerequisites for handling URIs have been met so it's time to
         * load any handling requests which have come before the loading and
         * were thus delayed in uris.
         */
        if ((AccountManagerEvent.STORED_ACCOUNTS_LOADED == event.getType())
            && (protoFactory == event.getFactory()))
        {
            List<String> uris = null;

            synchronized (storedAccountsAreLoaded)
            {
                storedAccountsAreLoaded[0] = true;

                if (this.uris != null)
                {
                    uris = this.uris;
                    this.uris = null;
                }
            }

            unhookStoredAccounts();

            if (uris != null)
            {
                for (Iterator<String> uriIter = uris.iterator(); uriIter
                    .hasNext();)
                {
                    handleUri(uriIter.next());
                }
            }
        }
    }

    /**
     * Registers this UriHandler with the bundle context so that it could start
     * handling URIs
     */
    public void registerHandlerService()
    {
        synchronized (registrationLock)
        {
            if (ourServiceRegistration != null)
            {
                // ... we are already registered (this is probably
                // happening during startup)
                return;
            }

            Hashtable<String, String> registrationProperties =
                new Hashtable<String, String>();

            for (String protocol : getProtocol())
            {
                registrationProperties.put(UriHandler.PROTOCOL_PROPERTY,
                    protocol);
            }

            ourServiceRegistration =
                JabberActivator.bundleContext.registerService(UriHandler.class
                    .getName(), this, registrationProperties);
        }

    }

    /**
     * Unregisters this UriHandler from the bundle context.
     */
    public void unregisterHandlerService()
    {
        synchronized (registrationLock)
        {
            if (ourServiceRegistration != null)
            {
                ourServiceRegistration.unregister();
                ourServiceRegistration = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getProtocol()
    {
        return new String[]
        { "xmpp" };
    }

    /**
     * Parses the specified URI and creates a chat with the currently active
     * im operation set.
     *
     * @param uri the xmpp URI that we have to handle.
     */
    public void handleUri(String uri)
    {
        /*
         * TODO If the requirement to register the factory service after
         * creating this instance is broken, we'll end up not handling the URIs.
         */
        synchronized (storedAccountsAreLoaded)
        {
            if (!storedAccountsAreLoaded[0])
            {
                if (uris == null)
                {
                    uris = new LinkedList<String>();
                }
                uris.add(uri);
                return;
            }
        }

        ProtocolProviderService provider;
        try
        {
            provider = selectHandlingProvider(uri);
        }
        catch (OperationFailedException exc)
        {
            // The operation has been canceled by the user. Bail out.
            if (logger.isTraceEnabled())
                logger.trace("User canceled handling of uri " + uri);
            return;
        }

        // if provider is null then we need to tell the user to create an
        // account
        if (provider == null)
        {
            showErrorMessage(
                "You need to configure at least one XMPP account \n"
                    + "to be able to call " + uri, null);
            return;
        }

        if(!uri.contains("?"))
        {
            OperationSetPersistentPresence presenceOpSet
                = provider
                    .getOperationSet(OperationSetPersistentPresence.class);

            String contactId = uri.substring(uri.indexOf(':') + 1);

            //todo check url!!
            //Set the email pattern string
            Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
            if(!p.matcher(contactId).matches())
            {
                showErrorMessage(
                "Wrong contact id : " + uri, null);
                return;
            }

            Contact contact = presenceOpSet.findContactByID(contactId);
            if(contact == null)
            {
                Object result =
                    JabberActivator.getUIService().getPopupDialog()
                            .showConfirmPopupDialog(
                                    "Do you want to add the contact : "
                                        + contactId + " ?",
                                    "Add contact",
                                    PopupDialog.YES_NO_OPTION);

                if(result.equals(PopupDialog.YES_OPTION))
                {
                    ExportedWindow ex = JabberActivator.getUIService().
                            getExportedWindow(ExportedWindow.ADD_CONTACT_WINDOW,
                                new String[]{contactId});
                    ex.setVisible(true);
                }

                return;
            }

            JabberActivator.getUIService().
                    getChat(contact).setChatVisible(true);
        }
        else
        {
            String croom = uri.replaceFirst(getProtocol() + ":", "");
            int ix = croom.indexOf("?");
            String param = croom.substring(ix + 1, croom.length());
            croom = croom.substring(0, ix);

            if(param.equalsIgnoreCase("join"))
            {
                OperationSetMultiUserChat mchatOpSet
                    = provider
                        .getOperationSet(OperationSetMultiUserChat.class);

                try
                {
                    ChatRoom room = mchatOpSet.findRoom(croom);

                    if(room != null)
                    {
                        room.join();
                    }
                }
                catch (OperationFailedException exc)
                {
                    // if we are not online we get this error
                    // will wait for it and then will try to handle once again
                    if((exc.getErrorCode()
                                == OperationFailedException.NETWORK_FAILURE)
                            && !networkFailReceived)
                    {
                        networkFailReceived = true;
                        OperationSetPresence presenceOpSet
                            = provider
                                .getOperationSet(OperationSetPresence.class);
                        presenceOpSet.addProviderPresenceStatusListener(
                                new ProviderStatusListener(uri, presenceOpSet));
                    }
                    else
                        showErrorMessage("Error joining to  " + croom, exc);
                }
                catch (OperationNotSupportedException exc)
                {
                    showErrorMessage(
                            "Join to " + croom + ", not supported!",
                            exc);
                }
            }
            else
                showErrorMessage(
                "Unknown param : " + param, null);
        }
    }

    /**
     * Informs the user that they need to be registered before chatting and
     * asks them whether they would like us to do it for them.
     *
     * @param uri the uri that the user would like us to chat with after
     * registering.
     * @param provider the provider that we may have to reregister.
     */
    private void promptForRegistration(
            String uri,
            ProtocolProviderService provider)
    {
        int answer
            = JabberActivator
                .getUIService()
                    .getPopupDialog()
                        .showConfirmPopupDialog(
                                "You need to be online in order to chat and"
                                    + " your account is currently offline. Do"
                                    + " you want to connect now?",
                                "Account is currently offline",
                                PopupDialog.YES_NO_OPTION);

        if (answer == PopupDialog.YES_OPTION)
            new ProtocolRegistrationThread(uri, provider).start();
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
        Object sourceService =
            JabberActivator.bundleContext.
                getService(event.getServiceReference());

        // ignore anything but our protocol factory.
        if (sourceService != protoFactory)
        {
            return;
        }

        switch (event.getType())
        {
            case ServiceEvent.REGISTERED:
                // our factory has just been registered as a service ...
                registerHandlerService();
                break;
            case ServiceEvent.UNREGISTERING:
                // our factory just died - seppuku.
                unregisterHandlerService();
                break;
            default:
                // we don't care.
                break;
        }
    }

    /**
     * Uses the <tt>UIService</tt> to show an error <tt>message</tt> and log and
     * <tt>exception</tt>.
     *
     * @param message the message that we'd like to show to the user.
     * @param exc the exception that we'd like to log
     */
    private void showErrorMessage(String message, Exception exc)
    {
        JabberActivator.getUIService().getPopupDialog().showMessagePopupDialog(
            message, "Failed to create chat!", PopupDialog.ERROR_MESSAGE);
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
         * The URI that we'd need to chat.
         */
        private String uri = null;

        /**
         * Configures this thread register our parent provider and re-attempt
         * connection to the specified <tt>uri</tt>.
         *
         * @param uri the uri that we need to handle.
         * @param handlerProvider the provider that we are going to make
         *            register and that we are going to use to handle the
         *            <tt>uri</tt>.
         */
        public ProtocolRegistrationThread(String uri,
            ProtocolProviderService handlerProvider)
        {
            super("UriHandlerProviderRegistrationThread:uri=" + uri);
            this.uri = uri;
            this.handlerProvider = handlerProvider;
        }

        /**
         * Starts the registration process, ads this class as a registration
         * listener and then tries to rehandle the uri this thread was initiated
         * with.
         */
        @Override
        public void run()
        {
            handlerProvider.addRegistrationStateChangeListener(this);

            try
            {
                handlerProvider.register(JabberActivator.getUIService()
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
         * @param evt the <tt>RegistrationStateChangeEvent</tt> that this thread
         *            was initiated with.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                Thread uriRehandleThread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        handleUri(uri);
                    }
                };

                uriRehandleThread.setName("UriRehandleThread:uri=" + uri);
                uriRehandleThread.start();
            }

            // we're only interested in a single event so we stop listening
            // (unless this was a REGISTERING notification)
            if (evt.getNewState() == RegistrationState.REGISTERING)
                return;

            handlerProvider.removeRegistrationStateChangeListener(this);
        }
    }

    /**
     * Returns the default provider that we are supposed to handle URIs through
     * or null if there aren't any. Depending on the implementation this method
     * may require user intervention so make sure you don't rely on a quick
     * outcome when chatting it.
     *
     * @param uri the uri that we'd like to handle with the provider that we are
     *            about to select.
     *
     * @return the provider that we should handle URIs through.
     *
     * @throws OperationFailedException with code <tt>OPERATION_CANCELED</tt> if
     *             the users.
     */
    public ProtocolProviderService selectHandlingProvider(String uri)
        throws OperationFailedException
    {
        ArrayList<AccountID> registeredAccounts =
            protoFactory.getRegisteredAccounts();

        // if we don't have any providers - return null.
        if (registeredAccounts.size() == 0)
        {
            return null;
        }

        // if we only have one provider - select it
        if (registeredAccounts.size() == 1)
        {
            ServiceReference providerReference =
                protoFactory.getProviderForAccount(registeredAccounts.get(0));

            ProtocolProviderService provider =
                (ProtocolProviderService) JabberActivator.bundleContext
                    .getService(providerReference);

            return provider;
        }

        // otherwise - ask the user.
        ArrayList<ProviderComboBoxEntry> providers =
            new ArrayList<ProviderComboBoxEntry>();
        for (AccountID accountID : registeredAccounts)
        {
            ServiceReference providerReference =
                protoFactory.getProviderForAccount(accountID);

            ProtocolProviderService provider =
                (ProtocolProviderService) JabberActivator.bundleContext
                    .getService(providerReference);

            providers.add(new ProviderComboBoxEntry(provider));
        }

        Object result
            = JabberActivator.getUIService().getPopupDialog()
                    .showInputPopupDialog(
                            "Please select the account that you would like \n"
                                + "to use to chat with " + uri,
                            "Account Selection",
                            PopupDialog.OK_CANCEL_OPTION,
                            providers.toArray(),
                            providers.get(0));

        if (result == null)
        {
            throw new OperationFailedException("Operation cancelled",
                OperationFailedException.OPERATION_CANCELED);
        }

        return ((ProviderComboBoxEntry) result).provider;
    }

    /**
     * A class that we use to wrap providers before showing them to the user
     * through a selection popup dialog from the UIService.
     */
    private static class ProviderComboBoxEntry
    {
        public final ProtocolProviderService provider;

        public ProviderComboBoxEntry(ProtocolProviderService provider)
        {
            this.provider = provider;
        }

        /**
         * Returns a human readable <tt>String</tt> representing the provider
         * encapsulated by this class.
         *
         * @return a human readable string representing the provider.
         */
        @Override
        public String toString()
        {
            return provider.getAccountID().getAccountAddress();
        }
    }

    /**
     * Waiting for the provider to become online and then handle the uri.
     */
    private class ProviderStatusListener
        implements ProviderPresenceStatusListener
    {
        private final String uri;
        private final OperationSetPresence parentOpSet;

        public ProviderStatusListener(
                String uri,
                OperationSetPresence parentOpSet)
        {
            this.uri = uri;
            this.parentOpSet = parentOpSet;
        }

        public void providerStatusChanged(ProviderPresenceStatusChangeEvent ev)
        {
            if(ev.getNewStatus().isOnline())
            {
                parentOpSet.removeProviderPresenceStatusListener(this);
                handleUri(uri);
            }
        }

        public void providerStatusMessageChanged(PropertyChangeEvent ev) {}
    }
}
