/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

import java.util.*;


/**
 * Utility class that is obtained per metacontact. Used to check is a telephony
 * service, video calls and desktop sharing are enabled per contact from the
 * metacontact, or globally for the metacontct.
 * @author Damian Minkov
 */
public class ContactPhoneUtil
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallManager</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ContactPhoneUtil.class);

    /**
     * The metacontcat we are working on.
     */
    private MetaContact metaContact;

    /**
     * The phones that have been discovered for metacontact child contacts.
     */
    private Hashtable<Contact,List<String>> phones =
        new Hashtable<Contact, List<String>>();

    /**
     * Response listeners, when set and there is no currently available (cached)
     * information for the phones, we request such information and discontinue
     * current invocation and when result is available inform the listeners.
     */
    private Hashtable<Contact,DetailsResponseListener> responseListeners =
                new Hashtable<Contact, DetailsResponseListener>();

    /**
     * True if there is any phone found for the metacontact.
     */
    private boolean hasPhones = false;

    /**
     * True if there is any video phone found for the metacontact.
     */
    private boolean hasVideoDetail = false;

    /**
     * Is routing for video enabled for any of the contacts of the metacontact.
     */
    private boolean routingForVideoEnabled = false;

    /**
     * Is routing for desktop enabled for any of the contacts of the metacontact.
     */
    private boolean routingForDesktopEnabled = false;


    /**
     * Obtains the util for <tt>metaContact</tt>
     * @param metaContact the metaconctact.
     * @return ContactPhoneUtil for the <tt>metaContact</tt>.
     */
    public static ContactPhoneUtil getPhoneUtil(MetaContact metaContact)
    {
        return new ContactPhoneUtil(metaContact);
    }

    /**
     * Creates utility instance for <tt>metaContact</tt>.
     * @param metaContact the metacontact checked in the utility.
     */
    private ContactPhoneUtil(MetaContact metaContact)
    {
        this.metaContact = metaContact;
    }

    /**
     * Returns the metaContact we work on.
     * @return the metaContact we work on.
     */
    public MetaContact getMetaContact()
    {
        return metaContact;
    }

    /**
     * Adds response listener that will be informed when data is available.
     * This in case there is no currently cached data.
     * @param contact the contact which details will be checked.
     * @param listener the listener.
     */
    public void addDetailsResponseListener(
        Contact contact, DetailsResponseListener listener)
    {
        responseListeners.put(contact, listener);
    }

    /**
     * Returns localized addition phones list for contact, if any.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact
     * @return localized addition phones list for contact, if any.
     */
    public List<String> getPhones(Contact contact)
    {
        return getPhones(contact, true);
    }

    /**
     * Returns list of video phones for <tt>contact</tt>, localized.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check for video phones.
     * @return list of video phones for <tt>contact</tt>, localized.
     */
    public List<String> getVideoPhones(Contact contact)
    {
        if(!this.metaContact.containsContact(contact))
        {
            return new ArrayList<String>();
        }

        List<String> phonesList =  getPhonesFromOpSet(contact, true, true);

        if(phonesList == null)
            return null;

        return phonesList;
    }

    /**
     * List of phones for contact, localized if <tt>localized</tt> is
     * <tt>true</tt>, and not otherwise.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check for video phones.
     * @param localized whether to localize the phones, put a description text.
     * @return list of phones for contact.
     */
    public List<String> getPhones(Contact contact, boolean localized)
    {
        if(!this.metaContact.containsContact(contact))
        {
            return new ArrayList<String>();
        }

        if(phones.containsKey(contact))
        {
            return phones.get(contact);
        }

        List<String> phonesList = getPhonesFromOpSet(contact, false, localized);

        if(phonesList == null)
            return null;

        phones.put(contact, phonesList);

        return phonesList;
    }

    /**
     * Searches for phones for the contact.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check.
     * @param onlyVideo whether to include only video phones.
     * @param localized whether to localize phones.
     * @return list of phones, or null if we will use the listeners
     * for the result.
     */
    private List<String> getPhonesFromOpSet(
        Contact contact,
        boolean onlyVideo,
        boolean localized)
    {
        OperationSetServerStoredContactInfo infoOpSet =
            contact.getProtocolProvider().getOperationSet(
                OperationSetServerStoredContactInfo.class);
        Iterator<GenericDetail> details;
        ArrayList<String> phonesList = new ArrayList<String>();

        if(infoOpSet != null)
        {
            try
            {
                DetailsResponseListener listener
                    = responseListeners.get(contact);

                if(listener != null)
                {
                    details = infoOpSet.requestAllDetailsForContact(
                        contact, listener);

                    if(details == null)
                        return null;
                }
                else
                {
                    details = infoOpSet.getAllDetailsForContact(contact);
                }

                ArrayList<String> phoneNumbers = new ArrayList<String>();
                while(details.hasNext())
                {
                    GenericDetail d = details.next();

                    if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
                    {
                        PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                        String number = pnd.getNumber();
                        if(number != null &&
                            number.length() > 0)
                        {
                            hasPhones = true;
                            if(d instanceof VideoDetail)
                                hasVideoDetail = true;
                            else if(onlyVideo)
                                continue;

                            // skip duplicate numbers
                            if(phoneNumbers.contains(number))
                                continue;
                            phoneNumbers.add(number);

                            if(!localized)
                            {
                                phonesList.add(number);
                                continue;
                            }

                            phonesList.add(number
                                + " (" + getLocalizedPhoneNumber(d) + ")");
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                logger.error("Error obtaining server stored contact info");
            }
        }

        return phonesList;
    }

    /**
     * Is video called is enabled for metaContact. If any of the child
     * contacts has video enabled.
     * @return is video called is enabled for metaContact.
     */
    public boolean isVideoCallEnabled()
    {
        // make sure children are checked
        if(!checkMetaContactPhones())
            return false;

        return metaContact.getDefaultContact(
                    OperationSetVideoTelephony.class) != null
               || routingForVideoEnabled
               || hasVideoDetail;
    }

    /**
     * Is video call enabled for contact.
     * @param contact to check for video capabilities.
     * @return is video call enabled for contact.
     */
    public boolean isVideoCallEnabled(Contact contact)
    {
        if(!this.metaContact.containsContact(contact))
            return false;

        // make sure we have checked everything for the contact
        // before continue
        if(!checkContactPhones(contact))
            return false;

        routingForVideoEnabled =
            ConfigurationUtils
                .isRouteVideoAndDesktopUsingPhoneNumberEnabled()
            && phones.contains(contact)
            && phones.get(contact).size() > 0
            && AccountUtils.getOpSetRegisteredProviders(
                OperationSetVideoTelephony.class,
                null,
                null).size() > 0;

        return contact.getProtocolProvider().getOperationSet(
            OperationSetVideoTelephony.class) != null
                && hasContactCapabilities(contact,
                        OperationSetVideoTelephony.class)
                || routingForVideoEnabled;
    }

    /**
     * Is desktop sharing enabled for metaContact. If any of the child
     * contacts has desktop sharing enabled.
     * @return is desktop share is enabled for metaContact.
     */
    public boolean isDesktopSharingEnabled()
    {
        // make sure children are checked
        if(!checkMetaContactPhones())
            return false;

        return metaContact.getDefaultContact(
            OperationSetDesktopSharingServer.class) != null
            || routingForDesktopEnabled
            || hasVideoDetail;
    }

    /**
     * Is desktop sharing enabled for contact.
     * @param contact to check for desktop sharing capabilities.
     * @return is desktop sharing enabled for contact.
     */
    public boolean isDesktopSharingEnabled(Contact contact)
    {
        if(!this.metaContact.containsContact(contact))
            return false;

        // make sure we have checked everything for the contact
        // before continue
        if(!checkContactPhones(contact))
            return false;

        routingForDesktopEnabled =
            ConfigurationUtils
                .isRouteVideoAndDesktopUsingPhoneNumberEnabled()
            && phones.contains(contact)
            && phones.get(contact).size() > 0
            && AccountUtils.getOpSetRegisteredProviders(
                    OperationSetDesktopSharingServer.class,
                    null,
                    null).size() > 0;
        return contact.getProtocolProvider().getOperationSet(
            OperationSetDesktopSharingServer.class) != null
               && hasContactCapabilities(contact,
                        OperationSetDesktopSharingServer.class)
               || routingForDesktopEnabled;
    }

    /**
     * Is call enabled for metaContact. If any of the child
     * contacts has call enabled.
     * @return is call enabled for metaContact.
     */
    public boolean isCallEnabled()
    {
        // make sure children are checked
        if(!checkMetaContactPhones())
            return false;

        return metaContact.getDefaultContact(
                    OperationSetBasicTelephony.class) != null
               || (hasPhones
                    && CallManager.getTelephonyProviders().size() > 0);
    }

    /**
     * Is call enabled for contact.
     * @param contact to check for call capabilities.
     * @return is call enabled for contact.
     */
    public boolean isCallEnabled(Contact contact)
    {
        if(!checkContactPhones(contact))
            return false;

        return contact.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class) != null
               && hasContactCapabilities(contact,
                        OperationSetBasicTelephony.class);
    }

    /**
     * Checking all contacts for the metacontact.
     * Return <tt>false</tt> if there are listeners added for a contact
     * and we need to stop executions cause listener will be used to be informed
     * for result.
     *
     * @return whether to continue or listeners present and will be informed
     * for result.
     */
    private boolean checkMetaContactPhones()
    {
        Iterator<Contact> contactIterator = metaContact.getContacts();
        while(contactIterator.hasNext())
        {
            Contact contact = contactIterator.next();
            if(phones.containsKey(contact))
                continue;

            List<String> phones = getPhones(contact);
            if(phones == null)
                return false;
        }

        return true;
    }

    /**
     * Checking contact for phones.
     * Return <tt>false</tt> if there are listeners added for the contact
     * and we need to stop executions cause listener will be used to be informed
     * for result.
     *
     * @return whether to continue or listeners present and will be informed
     * for result.
     */
    private boolean checkContactPhones(Contact contact)
    {
        if(!phones.containsKey(contact))
        {
            List<String> phones = getPhones(contact);
            if(phones == null)
                return false;

            // to check for routingForVideoEnabled prop
            isVideoCallEnabled(contact);
            // to check for routingForDesktopEnabled prop
            isDesktopSharingEnabled(contact);
        }

        return true;
    }

    /**
     * Returns <tt>true</tt> if <tt>Contact</tt> supports the specified
     * <tt>OperationSet</tt>, <tt>false</tt> otherwise.
     *
     * @param contact contact to check
     * @param opSet <tt>OperationSet</tt> to search for
     * @return Returns <tt>true</tt> if <tt>Contact</tt> supports the specified
     * <tt>OperationSet</tt>, <tt>false</tt> otherwise.
     */
    private boolean hasContactCapabilities(
            Contact contact, Class<? extends OperationSet> opSet)
    {
        OperationSetContactCapabilities capOpSet =
            contact.getProtocolProvider().
                getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet == null)
        {
            // assume contact has OpSet capabilities
            return true;
        }
        else
        {
            if(capOpSet.getOperationSet(contact, opSet) != null)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns localized phone number.
     * @param d the detail.
     * @return the localized phone number.
     */
    private String getLocalizedPhoneNumber(GenericDetail d)
    {
        if(d instanceof WorkPhoneDetail)
        {
            return GuiActivator.getResources().
                getI18NString(
                    "service.gui.WORK_PHONE");
        }
        else if(d instanceof MobilePhoneDetail)
        {
            return GuiActivator.getResources().
                getI18NString(
                    "service.gui.MOBILE_PHONE");
        }
        else if(d instanceof VideoDetail)
        {
            return GuiActivator.getResources().
                getI18NString(
                    "service.gui.VIDEO_PHONE");
        }
        else
        {
            return GuiActivator.getResources().
                getI18NString(
                    "service.gui.HOME");
        }
    }

    /**
     * Searches for additional phone numbers found in contact information
     *
     * @return additional phone numbers found in contact information;
     */
    public List<UIContactDetail> getAdditionalNumbers()
    {
        List<UIContactDetail> telephonyContacts
            = new ArrayList<UIContactDetail>();

        Iterator<Contact> contacts = metaContact.getContacts();

        while(contacts.hasNext())
        {
            Contact contact = contacts.next();
            OperationSetServerStoredContactInfo infoOpSet =
                contact.getProtocolProvider().getOperationSet(
                    OperationSetServerStoredContactInfo.class);
            Iterator<GenericDetail> details;
            ArrayList<String> phones = new ArrayList<String>();

            if(infoOpSet != null)
            {
                details = infoOpSet.getAllDetailsForContact(contact);

                while(details.hasNext())
                {
                    GenericDetail d = details.next();
                    if(d instanceof PhoneNumberDetail &&
                        !(d instanceof PagerDetail) &&
                        !(d instanceof FaxDetail))
                    {
                        PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                        if(pnd.getNumber() != null &&
                            pnd.getNumber().length() > 0)
                        {
                            // skip phones which were already added
                            if(phones.contains(pnd.getNumber()))
                                continue;

                            phones.add(pnd.getNumber());

                            UIContactDetail cd =
                                new UIContactDetailImpl(
                                    pnd.getNumber(),
                                    pnd.getNumber() +
                                    " (" + getLocalizedPhoneNumber(d) + ")",
                                    null,
                                    new ArrayList<String>(),
                                    null,
                                    null,
                                    null,
                                    pnd)
                            {
                                public PresenceStatus getPresenceStatus()
                                {
                                    return null;
                                }
                            };
                            telephonyContacts.add(cd);
                        }
                    }
                }
            }
        }

        return telephonyContacts;
    }
}
