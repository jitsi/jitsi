/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msofficecomm;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Represents the Java counterpart of a native <tt>IMessenger</tt>
 * implementation.
 *
 * @author Lyubomir Marinov
 */
public class Messenger
{
    static final int CONVERSATION_TYPE_AUDIO = 8;

    static final int CONVERSATION_TYPE_IM = 1;

    static final int CONVERSATION_TYPE_LIVEMEETING = 4;

    static final int CONVERSATION_TYPE_PHONE = 2;

    static final int CONVERSATION_TYPE_PSTN = 32;

    static final int CONVERSATION_TYPE_VIDEO = 16;

    static final int MISTATUS_AWAY = 0x0022;

    static final int MISTATUS_MAY_BE_AVAILABLE = 0x00A2;

    static final int MISTATUS_OFFLINE = 0x0001;

    static final int MISTATUS_ONLINE = 0x0002;

    static final int MISTATUS_UNKNOWN = 0x0000;

    /**
     * The <tt>BundleContext</tt> in which the <tt>msofficecomm</tt> bundle has
     * been started.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>MetaContactListService</tt> which the <tt>Messenger</tt> class
     * looks through in order to locate <tt>Contact</tt>s associated with a
     * specific sign-in name.
     */
    private static MetaContactListService metaContactListService;

    /**
     * The list of (local) accounts by sign-in name which correspond to
     * <tt>IMessengerContact</tt> implementations having <tt>true</tt> as the
     * value of their <tt>self</tt> boolean property.
     */
    private static final Map<String, Self> selves = new HashMap<String, Self>();

    /**
     * The <tt>ServiceListener</tt> which listens to the <tt>BundleContext</tt>
     * in which the <tt>msofficecomm</tt> bundle has been started for service
     * changes.
     */
    private static final ServiceListener serviceListener
        = new ServiceListener()
        {
            public void serviceChanged(ServiceEvent event)
            {
                Messenger.serviceChanged(event);
            }
        };

    static
    {
        System.loadLibrary("jmsofficecomm");
    }

    private static synchronized void addSelf(
            String signinName,
            ProtocolProviderService pps,
            OperationSetPresence presenceOpSet)
    {
        Self self = selves.get(signinName);

        if (self == null)
        {
            self = new Self(signinName);
            selves.put(signinName, self);
        }
        self.addProtocolProviderService(pps, presenceOpSet);
    }

    /**
     * Finds the <tt>Contact</tt> instances which are associated with a specific
     * <tt>IMessengerContact</tt> sign-in name and which originate from a
     * specific <tt>ProtocolProviderService</tt> instance.
     *
     * @param pps the <tt>ProtocolProviderService</tt> from which possibly
     * found <tt>Contact</tt> instances are to originate
     * @param presenceOpSet the <tt>OperationSetPresence</tt> associated with
     * the specified <tt>pps</tt>
     * @param signinName the <tt>IMessengerContact</tt> sign-in name for which
     * the associated <tt>Contact</tt> instances are to be found
     * @return a list of <tt>Contact</tt> instances which are associated with
     * the specified <tt>signinName</tt> and which originate from the specified
     * <tt>pps</tt> if such <tt>Contact</tt> instances have been found;
     * otherwise, an empty list
     */
    private static List<Contact> findContactsBySigninName(
            ProtocolProviderService pps,
            OperationSetPresence presenceOpSet,
            String signinName)
    {
        List<Contact> contacts = new ArrayList<Contact>();

        for (Iterator<MetaContact> metaContactIt
                    = metaContactListService.findAllMetaContactsForProvider(
                            pps);
                metaContactIt.hasNext();)
        {
            MetaContact metaContact = metaContactIt.next();

            for (Iterator<Contact> contactIt = metaContact.getContacts();
                    contactIt.hasNext();)
            {
                Contact contact = contactIt.next();

                if (signinName.equalsIgnoreCase(getSigninName(contact, pps)))
                {
                    /*
                     * Prefer matches by Contact address over
                     * EmailAddressDetail.
                     */
                    contacts.add(0, contact);
                    continue;
                }

                OperationSetServerStoredContactInfo serverStoredContactInfoOpSet
                    = pps.getOperationSet(
                            OperationSetServerStoredContactInfo.class);

                if (serverStoredContactInfoOpSet != null)
                {
                    for (Iterator<ServerStoredDetails.EmailAddressDetail>
                                emailAddressDetailIt
                                    =  serverStoredContactInfoOpSet
                                        .getDetailsAndDescendants(
                                                contact,
                                                ServerStoredDetails
                                                    .EmailAddressDetail.class);
                            emailAddressDetailIt.hasNext();)
                    {
                        ServerStoredDetails.EmailAddressDetail emailAddressDetail
                            = emailAddressDetailIt.next();

                        if (signinName.equalsIgnoreCase(
                                emailAddressDetail.getEMailAddress()))
                        {
                            contacts.add(contact);
                            break;
                        }
                    }
                }
            }
        }
        return contacts;
    }

    /**
     * Gets the <tt>Contact</tt> instances which are associated with a specific
     * <tt>IMessengerContact</tt> sign-in name and which support a specific
     * <tt>OperationSet</tt>.
     *
     * @param signinName the <tt>IMessengerContact</tt> sign-in name for which
     * the associated <tt>Contact</tt> instances are to be found
     * @param opSetClass the <tt>OperationSet</tt> class to be supported by the
     * possibly found <tt>Contact</tt> instances
     * @return a list of <tt>Contact</tt> instances which are associated with
     * the specified <tt>signinName</tt> and which support the specified
     * <tt>opSetClass</tt> if such <tt>Contact</tt> instances have been found;
     * otherwise, an empty list
     */
    private static List<Contact> findContactsBySigninName(
            String signinName,
            Class<? extends OperationSet> opSetClass)
    {
        List<Contact> contacts = new ArrayList<Contact>();

        for (Self self : selves.values())
            self.findContactsBySigninName(signinName, opSetClass, contacts);
        return contacts;
    }

    /**
     * Gets the (local) account associated with a specific sign-in name in the
     * form of a <tt>Self</tt> instance if the specified sign-in name is
     * associated with such a (local) account.
     *
     * @param signinName the sign-in name associated with the (local) account to
     * be retrieved
     * @return a <tt>Self</tt> instance describing a (local) account associated
     * with the specified <tt>signinName</tt> if such a <tt>Self</tt> instance
     * exists; otherwise, <tt>null</tt>
     */
    private static Self getSelf(String signinName)
    {
        Self self = selves.get(signinName);

        if (self == null)
        {
            for (Self aSelf : selves.values())
            {
                if (aSelf.isSelf(signinName))
                {
                    self = aSelf;
                    break;
                }
            }
        }
        return self;
    }

    /**
     * Gets the <tt>IMessengerContact</tt> sign-in name associated with a
     * specific <tt>Contact</tt> from a specific
     * <tt>ProtocolProviderService</tt>. If no <tt>Contact</tt> is specified,
     * gets the sign-in name associated with the <tt>AccountID</tt> of the
     * specified <tt>ProtocolProviderService</tt>.
     *
     * @param contact the <tt>Contact</tt> to retrieve the sign-in name of or
     * <tt>null</tt> to retrieve the sign-in name associated with the
     * <tt>AccountID</tt> of the specified <tt>pps</tt>
     * @param pps the <tt>ProtocolProviderService</tt> of <tt>contact</tt> if
     * <tt>contact</tt> is other than <tt>null</tt> or of the <tt>AccountID</tt>
     * to get the sign-in name of if <tt>contact</tt> is <tt>null</tt>
     * @return the sign-in name associated with the specified <tt>contact</tt>
     * from the specified <tt>pps</tt> if <tt>contact</tt> is other than
     * <tt>null</tt> or with the <tt>AccountID</tt> of the specified
     * <tt>pps</tt> if <tt>contact</tt> is <tt>null</tt>
     */
    private static String getSigninName(
            Contact contact,
            ProtocolProviderService pps)
    {
        String address
            = (contact == null)
                ? pps.getAccountID().getAccountAddress()
                : contact.getAddress();
        String signinName;

        if (address.contains("@"))
        {
            String protocol
                = ((pps == null) ? contact.getProtocolProvider() : pps)
                        .getProtocolName()
                    + ":";

            if (address.toLowerCase().startsWith(protocol.toLowerCase()))
                signinName = address.substring(protocol.length());
            else
                signinName = address;
        }
        else
            signinName = null;
        return signinName;
    }

    /**
     * Gets the connection/presence status of the contact associated with a
     * specific <tt>MessengerContact</tt> instance in the form of a
     * <tt>MISTATUS</tt> value.
     *
     * @param messengerContact a <tt>MessengerContact</tt> instance which
     * specifies the contact for which the connection/presence status is to be
     * retrieved
     * @return a <tt>MISTATUS</tt> value which represents the
     * connection/presence status of the contact associated with the specified
     * <tt>messengerContact</tt>
     */
    static int getStatus(MessengerContact messengerContact)
    {
        String signinName = messengerContact.signinName;
        int presenceStatus;

        if (signinName == null)
            presenceStatus = Integer.MIN_VALUE;
        else
        {
            Self self = getSelf(signinName);

            if (self == null)
            {
                presenceStatus = Integer.MIN_VALUE;
                for (Self aSelf : selves.values())
                {
                    int aPresenceStatus = aSelf.getPresenceStatus(signinName);

                    if (presenceStatus < aPresenceStatus)
                    {
                        presenceStatus = aPresenceStatus;
                        if (presenceStatus >= PresenceStatus.MAX_STATUS_VALUE)
                            break;
                    }
                }
            }
            else
                presenceStatus = self.getPresenceStatus();
        }

        return presenceStatusToMISTATUS(presenceStatus);
    }

    /**
     * Gets the indicator which determines whether a specific
     * <tt>MessengerContact</tt> is the same user as the current client user.
     *
     * @param messengerContact the <tt>MessengerContact</tt> which is to be
     * determined whether it is the same user as the current client user
     * @return <tt>true</tt> if the specified <tt>messengerContact</tt> is the
     * same user as the current client user; otherwise, <tt>false</tt>
     */
    static boolean isSelf(MessengerContact messengerContact)
    {
        String signinName = messengerContact.signinName;

        return (signinName == null) ? false : (getSelf(signinName) != null);
    }

    private static native void onContactStatusChange(
            String signinName,
            int status);

    private static int presenceStatusToMISTATUS(int presenceStatus)
    {
        int mistatus;

        if (presenceStatus == Integer.MIN_VALUE)
            mistatus = MISTATUS_UNKNOWN;
        else
        {
            if (presenceStatus < PresenceStatus.ONLINE_THRESHOLD)
                mistatus = MISTATUS_OFFLINE;
            else if (presenceStatus < PresenceStatus.AWAY_THRESHOLD)
                mistatus = MISTATUS_MAY_BE_AVAILABLE;
            else if (presenceStatus < PresenceStatus.AVAILABLE_THRESHOLD)
                mistatus = MISTATUS_AWAY;
            else
                mistatus = MISTATUS_ONLINE;
        }
        return mistatus;
    }

    private static synchronized void removeSelf(
            String signinName,
            ProtocolProviderService pps)
    {
        Self self = selves.get(signinName);

        if ((self != null) && (self.removeProtocolProviderService(pps) < 1))
        {
            for (Iterator<Self> it = selves.values().iterator(); it.hasNext();)
            {
                if (it.next() == self)
                    it.remove();
            }
            self.dispose();
        }
    }

    /**
     * Notifies the <tt>Messenger</tt> class about a service change in the
     * <tt>BundleContext</tt> in which the <tt>msofficecomm</tt> bundle has been
     * started
     *
     * @param event a <tt>ServiceEvent</tt> describing the service change in the
     * <tt>BundleContext</tt> in which the <tt>msofficecomm</tt> bundle has been
     * started
     */
    private static void serviceChanged(ServiceEvent event)
    {
        Object service = bundleContext.getService(event.getServiceReference());

        if (service instanceof ProtocolProviderService)
        {
            ProtocolProviderService pps = (ProtocolProviderService) service;
            /*
             * The Messenger class implements an integration of Jitsi presence
             * into Microsoft Office so the only accounts of interest to it are
             * the ones which support presence.
             */
            OperationSetPresence presenceOpSet
                = pps.getOperationSet(OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                String signinName = getSigninName(null, pps);

                if (signinName != null)
                {
                    switch (event.getType())
                    {
                    case ServiceEvent.REGISTERED:
                        addSelf(signinName, pps, presenceOpSet);
                        break;
                    case ServiceEvent.UNREGISTERING:
                        removeSelf(signinName, pps);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Starts the <tt>Messenger</tt> class and instance functionality in a
     * specific <tt>BundleContext</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>Messenger</tt> class and instance functionality is to be started
     * @throws Exception if anything goes wrong while starting the
     * <tt>Messenger</tt> class and instance functionality in the specified
     * <tt>BundleContext</tt>
     */
    static synchronized void start(BundleContext bundleContext)
        throws Exception
    {
        Messenger.bundleContext = bundleContext;
        metaContactListService
            = ServiceUtils.getService(
                    bundleContext,
                    MetaContactListService.class);

        bundleContext.addServiceListener(serviceListener);
        for (ServiceReference reference
                : bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null))
        {
            serviceListener.serviceChanged(
                    new ServiceEvent(ServiceEvent.REGISTERED, reference));
        }
    }

    /**
     * Stops the <tt>Messenger</tt> class and instance functionality in a
     * specific <tt>BundleContext</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the
     * <tt>Messenger</tt> class and instance functionality is to be stopped
     * @throws Exception if anything goes wrong while stopping the
     * <tt>Messenger</tt> class and instance functionality in the specified
     * <tt>BundleContext</tt>
     */
    static synchronized void stop(BundleContext bundleContext)
        throws Exception
    {
        bundleContext.removeServiceListener(serviceListener);

        Messenger.bundleContext = null;
        metaContactListService = null;

        // selves
        for (Iterator<Self> it = selves.values().iterator(); it.hasNext();)
        {
            it.next().dispose();
            it.remove();
        }
    }

    /**
     * Initializes a new <tt>Messenger</tt> instance which is to represent the
     * Java counterpart of a native <tt>IMessenger</tt> implementation.
     */
    public Messenger()
    {
    }

    /**
     * Starts a conversation with one or more other users using text, voice,
     * video, or data.
     *
     * @param conversationType a <tt>CONVERSATION_TYPE</tt> value specifying the
     * type of the conversation to be started
     * @param participants an array of <tt>String</tt> values specifying the
     * other users to start a conversation with
     */
    public void startConversation(
            final int conversationType,
            String[] participants)
    {
        /*
         * Firstly, resolve the participants into Contacts which may include
         * looking up their vCards.
         */
        Class<? extends OperationSet> opSetClass;

        switch (conversationType)
        {
        case CONVERSATION_TYPE_AUDIO:
        case CONVERSATION_TYPE_PHONE:
        case CONVERSATION_TYPE_PSTN:
            opSetClass = OperationSetBasicTelephony.class;
            break;
        case CONVERSATION_TYPE_IM:
            opSetClass = OperationSetBasicInstantMessaging.class;
            break;
        default:
            throw new UnsupportedOperationException();
        }

        List<String> contactList = new ArrayList<String>();

        for (String participant : participants)
        {
            List<Contact> participantContacts
                = findContactsBySigninName(participant, opSetClass);

            if (participantContacts.size() > 0)
            {
                contactList.add(
                        getSigninName(participantContacts.get(0), null));
            }
        }

        final String[] contactArray
            = contactList.toArray(new String[contactList.size()]);

        /*
         * Secondly, start the conversation of the specified type with the
         * resolved Contacts.
         */
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        BundleContext bundleContext = Messenger.bundleContext;

                        if (bundleContext != null)
                        {
                            UIService uiService
                                = ServiceUtils.getService(
                                        bundleContext,
                                        UIService.class);

                            if (uiService != null)
                            {
                                switch (conversationType)
                                {
                                case CONVERSATION_TYPE_AUDIO:
                                case CONVERSATION_TYPE_PHONE:
                                case CONVERSATION_TYPE_PSTN:
                                    uiService.createCall(contactArray);
                                    break;
                                case CONVERSATION_TYPE_IM:
                                    uiService.startChat(contactArray);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    /**
     * Describes a (local) account which corresponds to an
     * <tt>IMessengerContact</tt> implementation having <tt>true</tt> as the
     * value of its <tt>self</tt> boolean property.
     */
    private static class Self
        implements ContactPresenceStatusListener,
                   ProviderPresenceStatusListener
    {
        private final Map<ProtocolProviderService, OperationSetPresence> ppss
            = new HashMap<ProtocolProviderService, OperationSetPresence>();

        private int presenceStatus = Integer.MIN_VALUE;

        /**
         * The sign-in name associated with this (local) account.
         */
        public final String signinName;

        /**
         * Initializes a new <tt>Self</tt> instance which is to describe a
         * (local) account associated with a specific sign-in name.
         *
         * @param signinName the sign-in name to be associated with the new
         * instance
         */
        public Self(String signinName)
        {
            this.signinName = signinName;
        }

        void addProtocolProviderService(
                ProtocolProviderService pps,
                OperationSetPresence presenceOpSet)
        {
            if (!ppss.containsKey(pps))
            {
                ppss.put(pps, presenceOpSet);

                presenceOpSet.addContactPresenceStatusListener(this);
                presenceOpSet.addProviderPresenceStatusListener(this);
                providerStatusChanged(null);
            }
        }

        public void contactPresenceStatusChanged(
                ContactPresenceStatusChangeEvent event)
        {
            String signinName
                = getSigninName(
                        event.getSourceContact(),
                        event.getSourceProvider());

            if (signinName != null)
            {
                PresenceStatus oldStatus = event.getOldStatus();

                Messenger.onContactStatusChange(
                        signinName,
                        presenceStatusToMISTATUS(
                                (oldStatus == null)
                                    ? Integer.MIN_VALUE
                                    : oldStatus.getStatus()));
            }
        }

        /**
         * Disposes this instance by releasing the resources it has acquired by
         * now. Removes this instance as a listener from the associated
         * <tt>OperationSetPresence</tt> instances.
         */
        void dispose()
        {
            Iterator<Map.Entry<ProtocolProviderService, OperationSetPresence>>
                it
                    = ppss.entrySet().iterator();

            while (it.hasNext())
            {
                Map.Entry<ProtocolProviderService, OperationSetPresence> e
                    = it.next();
                OperationSetPresence presenceOpSet = e.getValue();

                presenceOpSet.removeContactPresenceStatusListener(this);
                presenceOpSet.removeProviderPresenceStatusListener(this);
                it.remove();
            }
        }

        /**
         * Gets the <tt>Contact</tt> instances which are associated with a
         * specific <tt>IMessengerContact</tt> sign-in name and which support a
         * specific <tt>OperationSet</tt>.
         *
         * @param signinName the <tt>IMessengerContact</tt> sign-in name for
         * which the associated <tt>Contact</tt> instances are to be found
         * @param opSetClass the <tt>OperationSet</tt> class to be supported by
         * the possibly found <tt>Contact</tt> instances
         * @param contacts a list with <tt>Contact</tt> element type which is to
         * receive the possibly found <tt>Contact</tt> instances
         */
        void findContactsBySigninName(
                String signinName,
                Class<? extends OperationSet> opSetClass,
                List<Contact> contacts)
        {
            for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                    : ppss.entrySet())
            {
                ProtocolProviderService pps = e.getKey();
                OperationSetContactCapabilities contactCapabilitiesOpSet
                    = pps.getOperationSet(
                            OperationSetContactCapabilities.class);

                for (Contact contact
                        : Messenger.findContactsBySigninName(
                                pps,
                                e.getValue(),
                                signinName))
                {
                    if ((contactCapabilitiesOpSet == null)
                            || (contactCapabilitiesOpSet.getOperationSet(
                                        contact,
                                        opSetClass)
                                    != null))
                        contacts.add(contact);
                }
            }
        }

        int getPresenceStatus()
        {
            return presenceStatus;
        }

        int getPresenceStatus(String signinName)
        {
            int presenceStatus;

            if (this.signinName.equalsIgnoreCase(signinName))
                presenceStatus = getPresenceStatus();
            else
            {
                presenceStatus = Integer.MIN_VALUE;
                for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                        : ppss.entrySet())
                {
                    try
                    {
                        Iterable<Contact> contacts
                            = Messenger.findContactsBySigninName(
                                    e.getKey(),
                                    e.getValue(),
                                    signinName);

                        for (Contact contact : contacts)
                        {
                            PresenceStatus contactPresenceStatus
                                = contact.getPresenceStatus();

                            if (contactPresenceStatus != null)
                            {
                                int contactStatus
                                    = contactPresenceStatus.getStatus();

                                if (presenceStatus < contactStatus)
                                {
                                    presenceStatus = contactStatus;
                                    if (presenceStatus
                                            >= PresenceStatus.MAX_STATUS_VALUE)
                                        break;
                                }
                            }
                        }
                        if (presenceStatus
                                >= PresenceStatus.MAX_STATUS_VALUE)
                            break;
                    }
                    catch (Throwable t)
                    {
                        /*
                         * It does not sound like it makes a lot of sense to
                         * fail the getting of the presence status of the
                         * specified signinName just because one of the possibly
                         * many OperationSetPresence instances has failed.
                         * Additionally, the native counterpart will swallow any
                         * Java exception anyway.
                         */
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else
                            t.printStackTrace(System.err);
                    }
                }
            }
            return presenceStatus;
        }

        /**
         * Gets an indicator which determines whether the (local) account
         * described by this <tt>Self</tt> instance is associated with a
         * specific sign-in name.
         *
         * @param signinName the sign-in name to be determined whether it is
         * associated with this <tt>Self</tt> instance
         * @return <tt>true</tt> if the specified <tt>signinName</tt> is
         * associated with the (local) account described by this <tt>Self</tt>
         * instance
         */
        boolean isSelf(String signinName)
        {
            boolean self;

            if (this.signinName.equalsIgnoreCase(signinName))
                self = true;
            else
            {
                self = false;
                for (ProtocolProviderService pps : ppss.keySet())
                {
                    OperationSetServerStoredAccountInfo
                        serverStoredAccountInfoOpSet
                            = pps.getOperationSet(
                                    OperationSetServerStoredAccountInfo.class);

                    if (serverStoredAccountInfoOpSet != null)
                    {
                        for (Iterator<ServerStoredDetails.EmailAddressDetail>
                                    emailAddressDetailIt
                                        = serverStoredAccountInfoOpSet
                                            .getDetailsAndDescendants(
                                                    ServerStoredDetails
                                                        .EmailAddressDetail
                                                            .class);
                                emailAddressDetailIt.hasNext();)
                        {
                            ServerStoredDetails.EmailAddressDetail emailAddressDetail
                                = emailAddressDetailIt.next();

                            if (signinName.equalsIgnoreCase(
                                    emailAddressDetail.getEMailAddress()))
                            {
                                self = true;
                                break;
                            }
                        }
                        if (self)
                            break;
                    }
                }
            }
            return self;
        }

        public void providerStatusChanged(
                ProviderPresenceStatusChangeEvent event)
        {
            PresenceStatus presenceStatus = null;

            for (OperationSetPresence presenceOpSet : ppss.values())
            {
                PresenceStatus presenceOpSetStatus
                    = presenceOpSet.getPresenceStatus();

                if (presenceOpSetStatus != null)
                {
                    if ((presenceStatus == null)
                            || (presenceStatus.compareTo(presenceOpSetStatus)
                                    < 0))
                        presenceStatus = presenceOpSetStatus;
                }
            }

            setPresenceStatus(presenceStatus);
        }

        public void providerStatusMessageChanged(PropertyChangeEvent event) {}

        int removeProtocolProviderService(ProtocolProviderService pps)
        {
            OperationSetPresence presenceOpSet = ppss.get(pps);

            if (presenceOpSet != null)
            {
                presenceOpSet.removeContactPresenceStatusListener(this);
                presenceOpSet.removeProviderPresenceStatusListener(this);
                ppss.remove(pps);
                providerStatusChanged(null);
            }
            return ppss.size();
        }

        private void setPresenceStatus(PresenceStatus presenceStatus)
        {
            int status
                = (presenceStatus == null)
                    ? Integer.MIN_VALUE
                    : presenceStatus.getStatus();

            if (this.presenceStatus != status)
            {
                int oldValue = this.presenceStatus;

                this.presenceStatus = status;

                Messenger.onContactStatusChange(
                        signinName,
                        presenceStatusToMISTATUS(oldValue));
            }
        }
    }
}
