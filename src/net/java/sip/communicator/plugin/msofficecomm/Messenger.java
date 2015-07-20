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
package net.java.sip.communicator.plugin.msofficecomm;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.service.protocol.yahooconstants.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.xml.*;
import org.osgi.framework.*;
import org.w3c.dom.*;

/**
 * Represents the Java counterpart of a native <tt>IMessenger</tt>
 * implementation.
 *
 * @author Lyubomir Marinov
 */
public class Messenger
{
    /**
     * The <tt>Logger</tt> used by the <tt>Messenger</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(Messenger.class);

    static final int CONVERSATION_TYPE_AUDIO = 8;

    static final int CONVERSATION_TYPE_IM = 1;

    static final int CONVERSATION_TYPE_LIVEMEETING = 4;

    static final int CONVERSATION_TYPE_PHONE = 2;

    static final int CONVERSATION_TYPE_PSTN = 32;

    static final int CONVERSATION_TYPE_VIDEO = 16;

    static final int MISTATUS_AWAY = 0x0022;

    static final int MISTATUS_MAY_BE_AVAILABLE = 0x00A2;

    static final int MISTATUS_OFFLINE = 0x0001;

    /**
     * The <tt>MISTATUS</tt> value which indicates that the local or remote
     * client user is on the phone.
     */
    static final int MISTATUS_ON_THE_PHONE = 0x0032;
    
    static final int MISTATUS_IN_A_MEETING = 0x0052;

    static final int MISTATUS_ONLINE = 0x0002;

    static final int MISTATUS_UNKNOWN = 0x0000;

    static final int MPHONE_TYPE_CUSTOM = 3;

    /**
     * The <tt>MPHONE_TYPE</tt> value which indicates a home phone number.
     */
    static final int MPHONE_TYPE_HOME = 0;

    /**
     * The <tt>MPHONE_TYPE</tt> value which indicates a mobile phone number.
     */
    static final int MPHONE_TYPE_MOBILE = 2;

    /**
     * The <tt>MPHONE_TYPE</tt> value which indicates a work phone number.
     */
    static final int MPHONE_TYPE_WORK = 1;

    /**
     * The name of the boolean <tt>ConfigurationService</tt> property which
     * indicates whether {@link #startConversation(int, String[], String)} is
     * to invoke {@link UIService#createCall(String[])} with a phone number
     * associated with a specific <tt>IMessengerContact</tt> instead of the
     * <tt>Contact</tt> address.
     */
    private static final String PNAME_CREATE_CALL_BY_PHONE_NUMBER
        = "net.java.sip.communicator.plugin.msofficecomm."
            + "CREATE_CALL_BY_PHONE_NUMBER";

    /**
     * The name of the <tt>String</tt> <tt>ConfigurationService</tt> property
     * which specifies the sort order of the <tt>MPHONE_TYPE_*</tt> enumerated
     * type values. The default value orders them as follows:
     * {@link #MPHONE_TYPE_WORK}, {@link #MPHONE_TYPE_HOME},
     * {@link #MPHONE_TYPE_MOBILE}, {@link #MPHONE_TYPE_CUSTOM}.
     */
    private static final String PNAME_MPHONE_TYPE_SORT_ORDER
        = "net.java.sip.communicator.plugin.msofficecomm."
            + "MPHONE_TYPE_SORT_ORDER";

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
     * The <tt>MPHONE_TYPE_*</tt> enumerated type values indexed by their sort
     * order position.
     */
    private static final int[] MPHONE_TYPE_SORT_ORDER
        = new int[]
                {
                    MPHONE_TYPE_WORK,
                    MPHONE_TYPE_HOME,
                    MPHONE_TYPE_MOBILE,
                    MPHONE_TYPE_CUSTOM
                };

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
        String lib = "jmsofficecomm";

        try
        {
            System.loadLibrary(lib);
        }
        catch (Throwable t)
        {
            logger.error(
                    "Failed to load native library " + lib + ": "
                        + t.getMessage());
            RegistryHandler.checkRegistryKeys();
            throw new RuntimeException(t);
        }
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
                    for (Iterator<EmailAddressDetail> emailAddressDetailIt
                                =  serverStoredContactInfoOpSet
                                    .getDetailsAndDescendants(
                                            contact,
                                            EmailAddressDetail.class);
                            emailAddressDetailIt.hasNext();)
                    {
                        EmailAddressDetail emailAddressDetail
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
     * @param limit the maximum number of found <tt>Contact</tt>s at which the
     * search should stop or {@link Integer#MAX_VALUE} if the search is to be
     * unbound with respect to the number of found <tt>Contact</tt>s
     * @return a list of <tt>Contact</tt> instances which are associated with
     * the specified <tt>signinName</tt> and which support the specified
     * <tt>opSetClass</tt> if such <tt>Contact</tt> instances have been found;
     * otherwise, an empty list
     */
    private static List<Contact> findContactsBySigninName(
            String signinName,
            Class<? extends OperationSet> opSetClass,
            int limit)
    {
        List<Contact> contacts = new ArrayList<Contact>();

        for (Self self : selves.values())
        {
            self.findContactsBySigninName(
                    signinName, opSetClass, limit,
                    contacts);
            /* Obey the specified limit of the number of found Contacts. */
            if (contacts.size() >= limit)
                break;
        }
        return contacts;
    }

    /**
     * Gets the <tt>PhoneNumberDetail</tt> instances which are associated with a
     * specific <tt>IMessengerContact</tt> sign-in name.
     *
     * @param signinName the <tt>IMessengerContact</tt> sign-in name for which
     * the associated <tt>PhoneNumberDetail</tt> instances are to be found
     * @param limit the maximum number of found <tt>PhoneNumberDetail</tt>s at
     * which the search should stop or {@link Integer#MAX_VALUE} if the search
     * is to be unbound with respect to the number of found
     * <tt>PhoneNumberDetail</tt>s
     * @return a list of <tt>PhoneNumberDetail</tt> instances which are
     * associated with the specified <tt>signinName</tt> if such
     * <tt>PhoneNumberDetail</tt> instances have been found; otherwise, an empty
     * list
     */
    private static Set<PhoneNumberDetail> findPhoneNumbersBySigninName(
            String signinName,
            int limit)
    {
        /*
         * XXX The limit is not being obeyed at this time because the
         * PhoneNumberDetails are ordered by MPHONE_TYPE.
         */

        Set<PhoneNumberDetail> phoneNumbers
            = new TreeSet<PhoneNumberDetail>(
                    new Comparator<PhoneNumberDetail>()
                    {
                        public int compare(
                                PhoneNumberDetail pn1,
                                PhoneNumberDetail pn2)
                        {
                            int so1
                                = getMPHONE_TYPESortOrder(getMPHONE_TYPE(pn1));
                            int so2
                                = getMPHONE_TYPESortOrder(getMPHONE_TYPE(pn2));

                            return
                                (so1 == so2)
                                    ? pn1.getNumber().compareTo(
                                            pn2.getNumber())
                                    : (so1 - so2);
                        }
                    });

        for (Self self : selves.values())
        {
            self.findPhoneNumbersBySigninName(
                    signinName,
                    Integer.MAX_VALUE,
                    phoneNumbers);
        }

        return phoneNumbers;
    }

    /**
     * Gets an <tt>MPHONE_TYPE</tt> enumerated type value which indicates the
     * phone number type of a specific <tt>PhoneNumberDetail</tt>.
     *
     * @param phoneNumber the <tt>PhoneNumberDetail</tt> for which a matching
     * <tt>MPHONE_TYPE</tt> enumerated type value is to be retrieved
     * @return an <tt>MPHONE_TYPE</tt> enumerated type value which indicates the
     * phone number type of the speciifed <tt>phoneNumber</tt>
     */
    private static int getMPHONE_TYPE(PhoneNumberDetail phoneNumber)
    {
        if (phoneNumber.getClass().equals(PhoneNumberDetail.class))
            return MPHONE_TYPE_HOME;
        else if (phoneNumber instanceof MobilePhoneDetail)
            return MPHONE_TYPE_MOBILE;
        else if (phoneNumber instanceof WorkPhoneDetail)
            return MPHONE_TYPE_WORK;
        else
            return MPHONE_TYPE_CUSTOM;
    }

    /**
     * Gets an <tt>int</tt> value which specifies the sort order position of a
     * specific <tt>MPHONE_TYPE</tt> enumerated type value which can be used
     * with a <tt>Comparator</tt> implementation.
     *
     * @param mphonetype the <tt>MPHONE_TYPE</tt> enumerated type value for
     * which the sort order position is to be retrieved
     * @return an <tt>int</tt> value which specifies the sort order position of
     * the specified <tt>MPHONE_TYPE</tt> enumerated type value which can be
     * used with a <tt>Comparator</tt> implementation
     */
    private static int getMPHONE_TYPESortOrder(int mphonetype)
    {
        for (int i = 0; i < MPHONE_TYPE_SORT_ORDER.length; i++)
            if (MPHONE_TYPE_SORT_ORDER[i] == mphonetype)
                return i;
        return MPHONE_TYPE_SORT_ORDER.length;
    }

    /**
     * Gets the phone number information of the contact associated with a
     * specific <tt>MessengerContact</tt> instance.
     *
     * @param messengerContact a <tt>MessengerContact</tt> instance which
     * specifies the contact for which the phone number information is to be
     * retrieved
     * @param type member of the <tt>MPHONE_TYPE</tt> enumerated type which
     * specifies the type of the phone number information to be retrieved
     * @return the phone number information of the contact associated with the
     * specified <tt>messengerContact</tt>
     */
    static String getPhoneNumber(MessengerContact messengerContact, int type)
    {
        Set<PhoneNumberDetail> phoneNumbers
            = findPhoneNumbersBySigninName(
                    messengerContact.signinName,
                    Integer.MAX_VALUE);

        for (PhoneNumberDetail phoneNumber : phoneNumbers)
            if (getMPHONE_TYPE(phoneNumber) == type)
                return phoneNumber.getNumber();
        return null;
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
        ProtocolPresenceStatus presenceStatus;

        if(logger.isTraceEnabled())
            logger.trace("Got getStatus for " + signinName);

        if (signinName == null)
            presenceStatus = null;
        else
        {
            Self self = getSelf(signinName);

            if (self == null)
            {
                presenceStatus = null;
                for (Self aSelf : selves.values())
                {
                    ProtocolPresenceStatus aPresenceStatus
                        = aSelf.getPresenceStatus(signinName);

                    if (aPresenceStatus != null)
                    {
                        if (presenceStatus == null)
                            presenceStatus = aPresenceStatus;
                        else if (presenceStatus.compareTo(aPresenceStatus) < 0)
                            presenceStatus = aPresenceStatus;
                        if (presenceStatus.toInt()
                                >= PresenceStatus.MAX_STATUS_VALUE)
                            break;
                    }
                }
            }
            else
                presenceStatus = self.getPresenceStatus();
        }

        return ProtocolPresenceStatus.toMISTATUS(presenceStatus);
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

        ServiceReference[] serviceReferences
            = bundleContext.getServiceReferences(
                    ProtocolProviderService.class.getName(),
                    null);

        if ((serviceReferences != null) && (serviceReferences.length != 0))
        {
            for (ServiceReference serviceReference : serviceReferences)
            {
                serviceListener.serviceChanged(
                        new ServiceEvent(
                                ServiceEvent.REGISTERED,
                                serviceReference));
            }
        }

        if (logger.isInfoEnabled())
            logger.info("Messenger [REGISTERED] as service listener.");
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
     * @param conversationData an XML BLOB specifying the phone numbers to be
     * dialed in order to start the conversation
     */
    public void startConversation(
            final int conversationType,
            String[] participants,
            String conversationData)
    {
        if(logger.isTraceEnabled())
            logger.trace("Got startConversation participants:"
                + participants == null? "" : Arrays.asList(participants)
                + ", conversationData=" + conversationData);

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

            if ((conversationData != null) && (conversationData.length() != 0))
            {
                if (logger.isTraceEnabled())
                {
                    logger.trace(
                            "conversationData = \"" + conversationData + "\"");
                }

                // According to MSDN, vConversationData could be an XML BLOB.
                if (conversationData.startsWith("<"))
                {
                    try
                    {
                        Document document
                            = XMLUtils.createDocument(conversationData);
                        Element documentElement = document.getDocumentElement();

                        if ("TelURIs".equalsIgnoreCase(
                                documentElement.getTagName()))
                        {
                            NodeList childNodes
                                = documentElement.getChildNodes();

                            if (childNodes != null)
                            {
                                int childNodeCount = childNodes.getLength();
                                List<String> phoneNumbers
                                    = new ArrayList<String>(childNodeCount);

                                for (int childNodeIndex = 0;
                                        childNodeIndex < childNodeCount;
                                        childNodeIndex++)
                                {
                                    Node childNode
                                        = childNodes.item(childNodeIndex);

                                    if (childNode.getNodeType()
                                            == Node.ELEMENT_NODE)
                                    {
                                        phoneNumbers.add(
                                                childNode.getTextContent());
                                    }
                                }

                                int count = participants.length;

                                if (phoneNumbers.size() == count)
                                {
                                    for (int i = 0; i < count; i++)
                                    {
                                        String phoneNumber
                                            = phoneNumbers.get(i);

                                        if ((phoneNumber != null)
                                                && (phoneNumber.length() != 0))
                                        {
                                            if (phoneNumber
                                                    .toLowerCase()
                                                        .startsWith("tel:"))
                                            {
                                                phoneNumber
                                                    = phoneNumber.substring(4);
                                            }
                                            participants[i] = phoneNumber;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error(
                                "Failed to parse"
                                    + " IMessengerAdvanced::StartConversation"
                                    + " vConversationData: "
                                    + conversationData,
                                e);
                    }
                }
                else
                {
                    /*
                     * Practice/testing shows that vConversationData is the
                     * phone number in the case of a single participant.
                     */
                    if (participants.length == 1)
                        participants[0] = conversationData;
                }
            }

            break;
        case CONVERSATION_TYPE_IM:
            opSetClass = OperationSetBasicInstantMessaging.class;
            break;

        default:
            throw new UnsupportedOperationException();
        }

        List<String> contactList = new ArrayList<String>();
        ConfigurationService cfg
            = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        boolean createCallByPhoneNumber
            = (cfg != null)
                && cfg.getBoolean(PNAME_CREATE_CALL_BY_PHONE_NUMBER, false);

        for (String participant : participants)
        {
            List<Contact> participantContacts
                = findContactsBySigninName(participant, opSetClass, 1);

            if (participantContacts.size() > 0)
            {
                contactList.add(
                        getSigninName(participantContacts.get(0), null));
            }
            else if (opSetClass.equals(OperationSetBasicTelephony.class))
            {
                /*
                 * The boolean ConfigurationService property
                 * PNAME_CREATE_CALL_BY_PHONE_NUMBER enables instructing whether
                 * a sign-in name which does not resolve to a Contact with
                 * support for OperationSetBasicTelephony is to be resolved to
                 * a phone number (via an associated vCard).
                 */
                if (createCallByPhoneNumber)
                {
                    Set<PhoneNumberDetail> participantPhoneNumbers
                        = findPhoneNumbersBySigninName(participant, 1);

                    if (participantPhoneNumbers.size() > 0)
                    {
                        contactList.add(
                                participantPhoneNumbers.iterator().next()
                                        .getNumber());
                    }
                }
                else
                {
                    /*
                     * There is no Contact for the specified participant which
                     * supports OperationSetBasicTelephony. Try without the
                     * support restriction.
                     */
                    participantContacts
                        = findContactsBySigninName(participant, null, 1);
                    if (participantContacts.size() > 0)
                    {
                        contactList.add(
                                getSigninName(
                                        participantContacts.get(0),
                                        null));
                    }
                    else
                    {
                        /*
                         * Well, just try to start a conversation with the
                         * unresolved contact.
                         */
                        contactList.add(participant);
                    }
                }
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
     * Represents a presence status reported by a specific protocol. Allows
     * distinguishing statuses more specific than the ranges defined by the
     * <tt>PresenceStatus</tt> class.
     */
    private static class ProtocolPresenceStatus
    {
        /**
         * The <tt>PresenceStatus</tt> instance represented by this instance.
         */
        private PresenceStatus presenceStatus;

        /**
         * The name of the protocol from which {@link #presenceStatus} has
         * originated. Allows translating <tt>presenceStatus</tt> to a
         * <tt>MISTATUS</tt> value which is equivalent to a protocol-specific
         * status not defined in the generic <tt>PresenceStatus</tt> class.
         */
        private String protocolName;

        /**
         * Initializes a new <tt>ProtocolPresenceStatus</tt> instance which is
         * to represent a specific <tt>PresenceStatus</tt> originating from a
         * specific protocol.
         *
         * @param protocolName the name of the protocol from which the specified
         * <tt>PresenceStatus</tt> has originated
         * @param presenceStatus the <tt>PresenceStatus</tt> to be represented
         * by the new instance
         */
        public ProtocolPresenceStatus(
                String protocolName,
                PresenceStatus presenceStatus)
        {
            setPresenceStatus(protocolName, presenceStatus);
        }

        /**
         * Returns <tt>-1</tt>, <tt>0</tt> or <tt>1</tt> if the
         * <tt>PresenceStatus</tt> represented by this instance is,
         * respectively, less than, equal to or greater than a specific
         * <tt>PresenceStatus</tt>.
         *
         * @param presenceStatus the <tt>PresenceStatus</tt> this instance is to
         * be compared to
         * @return <tt>-1</tt>, <tt>0</tt> or <tt>1</tt> if the
         * <tt>PresenceStatus</tt> represented by this instance is, respectively,
         * less than, equal to or greater than the specified
         * <tt>presenceStatus</tt>
         */
        public int compareTo(PresenceStatus presenceStatus)
        {
            return this.presenceStatus.compareTo(presenceStatus);
        }

        /**
         * Returns <tt>-1</tt>, <tt>0</tt> or <tt>1</tt> if the
         * <tt>PresenceStatus</tt> represented by this instance is,
         * respectively, less than, equal to or greater than the
         * <tt>PresenceStatus</tt> represented by a specific
         * <tt>ProtocolPresenceStatus</tt> instance.
         *
         * @param protocolPresenceStatus the <tt>ProtocolPresenceStatus</tt>
         * this instance is to be compared to
         * @return <tt>-1</tt>, <tt>0</tt> or <tt>1</tt> if the
         * <tt>PresenceStatus</tt> represented by this instance is, respectively,
         * less than, equal to or greater than the specified
         * <tt>protocolPresenceStatus</tt>
         */
        public int compareTo(ProtocolPresenceStatus protocolPresenceStatus)
        {
            return compareTo(protocolPresenceStatus.presenceStatus);
        }

        /**
         * Sets the <tt>PresenceStatus</tt> to be represented by this instance.
         *
         * @param protocolName the name of the protocol from which the
         * <tt>PresenceStatus</tt> to be set on this instance has originated
         * @param presenceStatus the <tt>PresenceStatus</tt> to be represented
         * by this instance
         */
        public void setPresenceStatus(
                String protocolName,
                PresenceStatus presenceStatus)
        {
            this.protocolName = protocolName;
            this.presenceStatus = presenceStatus;
        }

        /**
         * Gets an <tt>int</tt> value in the terms of <tt>PresenceStatus</tt>
         * which is equivalent to the <tt>PresenceStatus</tt> represented by
         * this instance.
         *
         * @return an <tt>int</tt> value in the terms of <tt>PresenceStatus</tt>
         * which is equivalent to the <tt>PresenceStatus</tt> represented by
         * this instance
         */
        public int toInt()
        {
            return presenceStatus.getStatus();
        }

        /**
         * Gets a <tt>MISTATUS</tt> value which is equivalent to the
         * <tt>PresenceStatus</tt> represented by this instance.
         *
         * @return a <tt>MISTATUS</tt> value which is equivalent to the
         * <tt>PresenceStatus</tt> represented by this instance
         */
        public int toMISTATUS()
        {
            return toMISTATUS(protocolName, presenceStatus);
        }

        /**
         * Gets a <tt>MISTATUS</tt> value which is equivalent to the
         * <tt>PresenceStatus</tt> represented by a specific
         * <tt>ProtocolPresenceStatus</tt> instance.
         *
         * @param protocolPresenceStatus the <tt>ProtocolPresenceStatus</tt> to
         * get an equivalent <tt>MISTATUS</tt> value for
         * @return a <tt>MISTATUS</tt> value which is equivalent to the
         * <tt>PresenceStatus</tt> represented by the specified
         * <tt>protocolPresenceStatus</tt>
         */
        public static int toMISTATUS(
                ProtocolPresenceStatus protocolPresenceStatus)
        {
            return
                (protocolPresenceStatus == null)
                    ? MISTATUS_UNKNOWN
                    : protocolPresenceStatus.toMISTATUS();
        }

        /**
         * Gets a <tt>MISTATUS</tt> value which is equivalent to a specific
         * <tt>PresenceStatus</tt> which has originated from a protocol with a
         * specific name.
         *
         * @param protocolName the name of the protocol from which the specified
         * <tt>PresenceStatus</tt> has originated
         * @param presenceStatus the <tt>PresenceStatus</tt> for which an
         * equivalent <tt>MISTATUS</tt> value is to be retrieved
         * @return a <tt>MISTATUS</tt> value which is equivalent to the
         * specified <tt>presenceStatus</tt> in the context of the protocol with
         * the specified <tt>protocolName</tt>
         */
        public static int toMISTATUS(
                String protocolName,
                PresenceStatus presenceStatus)
        {
            int i
                = (presenceStatus == null)
                    ? Integer.MIN_VALUE
                    : presenceStatus.getStatus();
            int mistatus;

            if (i == Integer.MIN_VALUE)
                mistatus = MISTATUS_UNKNOWN;
            else
            {
                if ((i == 31 /* FIXME */)
                        && ProtocolNames.JABBER.equalsIgnoreCase(protocolName)
                        && JabberStatusEnum.ON_THE_PHONE.equalsIgnoreCase(
                                presenceStatus.getStatusName()))
                {
                    mistatus = MISTATUS_ON_THE_PHONE;
                }
                else if (ProtocolNames.YAHOO.equalsIgnoreCase(protocolName)
                        && YahooStatusEnum.ON_THE_PHONE.equals(presenceStatus))
                {
                    mistatus = MISTATUS_ON_THE_PHONE;
                }
                else if ((i == 32 /* FIXME */)
                    && ProtocolNames.JABBER.equalsIgnoreCase(protocolName)
                    && JabberStatusEnum.IN_A_MEETING.equalsIgnoreCase(
                            presenceStatus.getStatusName()))
                {
                    mistatus = MISTATUS_IN_A_MEETING;
                }
                else if (i < PresenceStatus.ONLINE_THRESHOLD)
                    mistatus = MISTATUS_OFFLINE;
                else if (i < PresenceStatus.AWAY_THRESHOLD)
                    mistatus = MISTATUS_MAY_BE_AVAILABLE;
                else if (i < PresenceStatus.AVAILABLE_THRESHOLD)
                    mistatus = MISTATUS_AWAY;
                else
                    mistatus = MISTATUS_ONLINE;
            }
            return mistatus;
        }
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

        /**
         * The <tt>PresenceStatus</tt> of this (local) account and the name of
         * the protocol from which it has originated.
         */
        private ProtocolPresenceStatus presenceStatus;

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
                String oldProtocolName
                    = event.getSourceProvider().getProtocolName();
                PresenceStatus oldStatus = event.getOldStatus();

                Messenger.onContactStatusChange(
                        signinName,
                        ProtocolPresenceStatus.toMISTATUS(
                                oldProtocolName,
                                oldStatus));
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
         * the possibly found <tt>Contact</tt> instances or <tt>null</tt> if no
         * specific <tt>OperationSet</tt> class is required of the possibly
         * found <tt>Contact</tt> instances
         * @param limit the maximum number of found <tt>Contact</tt>s at which
         * the search should stop or {@link Integer#MAX_VALUE} if the search is
         * to be unbound with respect to the number of found <tt>Contact</tt>s
         * @param contacts a list with <tt>Contact</tt> element type which is to
         * receive the possibly found <tt>Contact</tt> instances
         */
        void findContactsBySigninName(
                String signinName,
                Class<? extends OperationSet> opSetClass,
                int limit,
                List<Contact> contacts)
        {
            for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                    : ppss.entrySet())
            {
                ProtocolProviderService pps = e.getKey();
                OperationSetContactCapabilities contactCapabilitiesOpSet
                    = (opSetClass == null)
                        ? null
                        : pps.getOperationSet(
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
                    {
                        contacts.add(contact);
                        /*
                         * Obey the specified limit of the number of found
                         * Contacts.
                         */
                        if (contacts.size() >= limit)
                            break;
                    }
                }
                /* Obey the specified limit of the number of found Contacts. */
                if (contacts.size() >= limit)
                    break;
            }
        }

        /**
         * Gets the <tt>PhoneNumberDetail</tt> instances which are associated
         * with a specific <tt>IMessengerContact</tt> sign-in name.
         *
         * @param signinName the <tt>IMessengerContact</tt> sign-in name for
         * which the associated <tt>PhoneNumberDetail</tt> instances are to be
         * found
         * @param limit the maximum number of found <tt>PhoneNumberDetail</tt>s
         * at which the search should stop or {@link Integer#MAX_VALUE} if the
         * search is to be unbound with respect to the number of found
         * <tt>PhoneNumberDetail</tt>s
         * @param phoneNumbers a list with <tt>PhoneNumberDetail</tt> element
         * type which is to receive the possibly found
         * <tt>PhoneNumberDetail</tt> instances
         */
        void findPhoneNumbersBySigninName(
                String signinName,
                int limit,
                Set<PhoneNumberDetail> phoneNumbers)
        {
            for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                    : ppss.entrySet())
            {
                ProtocolProviderService pps = e.getKey();
                OperationSetServerStoredContactInfo serverStoredContactInfoOpSet
                    = pps.getOperationSet(
                            OperationSetServerStoredContactInfo.class);

                if (serverStoredContactInfoOpSet == null)
                    continue;

                for (Contact contact
                        : Messenger.findContactsBySigninName(
                                pps,
                                e.getValue(),
                                signinName))
                {
                    Iterator<PhoneNumberDetail> iter
                        = serverStoredContactInfoOpSet.getDetailsAndDescendants(
                                contact,
                                PhoneNumberDetail.class);

                    if (iter == null)
                        continue;

                    while (iter.hasNext())
                    {
                        PhoneNumberDetail phoneNumber = iter.next();

                        if (getMPHONE_TYPE(phoneNumber) != MPHONE_TYPE_CUSTOM)
                        {
                            phoneNumbers.add(phoneNumber);
                            /*
                             * Obey the specified limit of the number of found
                             * PhoneNumberDetails.
                             */
                            if (phoneNumbers.size() >= limit)
                                break;
                        }
                    }
                }
                /*
                 * Obey the specified limit of the number of found
                 * PhoneNumberDetails.
                 */
                if (phoneNumbers.size() >= limit)
                    break;
            }
        }

        /**
         * Gets the <tt>PresenceStatus</tt> of this instance and the name of the
         * protocol from which it has originated.
         *
         * @return the <tt>PresenceStatus</tt> of this instance and the name of
         * the protocol from which it has originated
         */
        ProtocolPresenceStatus getPresenceStatus()
        {
            return presenceStatus;
        }

        /**
         * Gets the <tt>PresenceStatus</tt> of a <tt>Contact</tt> known to this
         * instance to be associated with a specific <tt>IMessengerContact</tt>
         * sign-in name.
         *
         * @param signinName the sign-in name associated with the
         * <tt>IMessengerContact</tt> whose <tt>PresenceStatus</tt> is to be
         * retrieved
         * @return the <tt>PresenceStatus</tt> and the name of the protocol from
         * which it has originated of a <tt>Contact</tt> known to this instance
         * to be associated with the specified <tt>signinName</tt> or
         * <tt>null</tt> if no such association is known to this instance
         */
        ProtocolPresenceStatus getPresenceStatus(String signinName)
        {
            ProtocolPresenceStatus presenceStatus;

            if (this.signinName.equalsIgnoreCase(signinName))
                presenceStatus = getPresenceStatus();
            else
            {
                presenceStatus = null;
                for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                        : ppss.entrySet())
                {
                    try
                    {
                        ProtocolProviderService pps = e.getKey();
                        Iterable<Contact> contacts
                            = Messenger.findContactsBySigninName(
                                    pps,
                                    e.getValue(),
                                    signinName);
                        String protocolName = pps.getProtocolName();

                        for (Contact contact : contacts)
                        {
                            PresenceStatus contactPresenceStatus
                                = contact.getPresenceStatus();

                            if (contactPresenceStatus != null)
                            {
                                if (presenceStatus == null)
                                {
                                    presenceStatus
                                        = new ProtocolPresenceStatus(
                                                protocolName,
                                                contactPresenceStatus);
                                }
                                else if (presenceStatus.compareTo(
                                            contactPresenceStatus)
                                        < 0)
                                {
                                    presenceStatus.setPresenceStatus(
                                            protocolName,
                                            contactPresenceStatus);
                                }
                                if (presenceStatus.toInt()
                                        >= PresenceStatus.MAX_STATUS_VALUE)
                                    break;
                            }
                        }
                        if ((presenceStatus != null)
                                && (presenceStatus.toInt()
                                        >= PresenceStatus.MAX_STATUS_VALUE))
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
                    if(!pps.isRegistered())
                    {
                        continue;
                    }

                    OperationSetServerStoredAccountInfo
                        serverStoredAccountInfoOpSet
                            = pps.getOperationSet(
                                    OperationSetServerStoredAccountInfo.class);

                    if (serverStoredAccountInfoOpSet != null)
                    {
                        for (Iterator<EmailAddressDetail> emailAddressDetailIt
                                    = serverStoredAccountInfoOpSet
                                        .getDetailsAndDescendants(
                                                EmailAddressDetail.class);
                                emailAddressDetailIt.hasNext();)
                        {
                            EmailAddressDetail emailAddressDetail
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
            ProtocolPresenceStatus protocolPresenceStatus = null;

            for (Map.Entry<ProtocolProviderService, OperationSetPresence> e
                    : ppss.entrySet())
            {
                OperationSetPresence presenceOpSet = e.getValue();
                PresenceStatus presenceOpSetStatus
                    = presenceOpSet.getPresenceStatus();

                if (presenceOpSetStatus != null)
                {
                    if (protocolPresenceStatus == null)
                    {
                        protocolPresenceStatus
                            = new ProtocolPresenceStatus(
                                    e.getKey().getProtocolName(),
                                    presenceOpSetStatus);
                    }
                    else if (protocolPresenceStatus.compareTo(
                                presenceOpSetStatus)
                            < 0)
                    {
                        protocolPresenceStatus.setPresenceStatus(
                                e.getKey().getProtocolName(),
                                presenceOpSetStatus);
                    }
                    if (protocolPresenceStatus.toInt()
                            >= PresenceStatus.MAX_STATUS_VALUE)
                        break;
                }
            }

            setPresenceStatus(protocolPresenceStatus);
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

        /**
         * Sets the <tt>PresenceStatus</tt> of this instance.
         *
         * @param presenceStatus the <tt>PresenceStatus</tt> and the name of the
         * protocol from which it has originated
         */
        private void setPresenceStatus(ProtocolPresenceStatus presenceStatus)
        {
            int oldMISTATUS
                = ProtocolPresenceStatus.toMISTATUS(this.presenceStatus);
            int newMISTATUS = ProtocolPresenceStatus.toMISTATUS(presenceStatus);

            if (oldMISTATUS != newMISTATUS)
            {
                this.presenceStatus = presenceStatus;

                Messenger.onContactStatusChange(
                        signinName,
                        oldMISTATUS);
            }
        }
    }
}
