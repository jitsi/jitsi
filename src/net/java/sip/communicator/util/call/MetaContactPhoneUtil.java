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
package net.java.sip.communicator.util.call;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo.DetailsResponseListener;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MobilePhoneDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.VideoDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkPhoneDetail;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * Utility class used to check if there is a telephony service, video calls and
 * desktop sharing enabled for a protocol specific <tt>MetaContact</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class MetaContactPhoneUtil
{
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
     * The video phones that have been discovered
     * for metacontact child contacts.
     */
    private Hashtable<Contact,List<String>> videoPhones =
        new Hashtable<Contact, List<String>>();

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
    public static MetaContactPhoneUtil getPhoneUtil(MetaContact metaContact)
    {
        return new MetaContactPhoneUtil(metaContact);
    }

    /**
     * Creates utility instance for <tt>metaContact</tt>.
     * @param metaContact the metacontact checked in the utility.
     */
    protected MetaContactPhoneUtil(MetaContact metaContact)
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
     * Returns localized addition phones list for contact, if any.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact
     * @return localized addition phones list for contact, if any.
     */
    public List<String> getPhones(Contact contact)
    {
        return getPhones(contact, null, true);
    }

    /**
     * Returns list of video phones for <tt>contact</tt>, localized.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check for video phones.
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @return list of video phones for <tt>contact</tt>, localized.
     */
    public List<String> getVideoPhones( Contact contact,
                                        DetailsResponseListener listener)
    {
        if(!this.metaContact.containsContact(contact))
        {
            return new ArrayList<String>();
        }

        if(videoPhones.containsKey(contact))
        {
            return videoPhones.get(contact);
        }

        List<String> phonesList = ContactPhoneUtil.getContactAdditionalPhones(
                contact, listener, true, true);

        if(phonesList == null)
            return null;
        else if (phonesList.size() > 0)
            hasVideoDetail = true;

        videoPhones.put(contact, phonesList);

        // to check for routingForVideoEnabled prop
        isVideoCallEnabled(contact);
        // to check for routingForDesktopEnabled prop
        isDesktopSharingEnabled(contact);

        return phonesList;
    }

    /**
     * List of phones for contact, localized if <tt>localized</tt> is
     * <tt>true</tt>, and not otherwise.
     * Return null if we have stopped searching and a listener is available
     * and will be used to inform for results.
     * @param contact the contact to check for video phones.
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @param localized whether to localize the phones, put a description text.
     * @return list of phones for contact.
     */
    public List<String> getPhones(  Contact contact,
                                    DetailsResponseListener listener,
                                    boolean localized)
    {
        if(!this.metaContact.containsContact(contact))
        {
            return new ArrayList<String>();
        }

        if(phones.containsKey(contact))
        {
            return phones.get(contact);
        }

        List<String> phonesList
            = ContactPhoneUtil.getContactAdditionalPhones(
                contact, listener, false, localized);

        if(phonesList == null)
            return null;
        else if (phonesList.size() > 0)
            hasPhones = true;

        phones.put(contact, phonesList);

        return phonesList;
    }

    /**
     * Is video called is enabled for metaContact. If any of the child
     * contacts has video enabled.
     *
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @return is video called is enabled for metaContact.
     */
    public boolean isVideoCallEnabled(DetailsResponseListener listener)
    {
        // make sure children are checked
        if(!checkMetaContactVideoPhones(listener))
            return false;

        return metaContact.getDefaultContact(
                    OperationSetVideoTelephony.class) != null
               || routingForVideoEnabled
               || hasVideoDetail;
    }

    /**
     * Is video called is enabled for metaContact. If any of the child
     * contacts has video enabled.
     *
     * @return is video called is enabled for metaContact.
     */
    public boolean isVideoCallEnabled()
    {
        return isVideoCallEnabled((DetailsResponseListener) null);
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
            && phones.containsKey(contact)
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
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @return is desktop share is enabled for metaContact.
     */
    public boolean isDesktopSharingEnabled(DetailsResponseListener listener)
    {
        // make sure children are checked
        if(!checkMetaContactVideoPhones(listener))
            return false;

        return metaContact.getDefaultContact(
            OperationSetDesktopSharingServer.class) != null
            || routingForDesktopEnabled
            || hasVideoDetail;
    }

    /**
     * Is desktop sharing enabled for metaContact. If any of the child
     * contacts has desktop sharing enabled.
     * @return is desktop share is enabled for metaContact.
     */
    public boolean isDesktopSharingEnabled()
    {
        return isDesktopSharingEnabled((DetailsResponseListener) null);
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
            && phones.containsKey(contact)
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
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @return is call enabled for metaContact.
     */
    public boolean isCallEnabled(DetailsResponseListener listener)
    {
        return isCallEnabled(listener, true);
    }

    /**
     * Is call enabled for metaContact. If any of the child
     * contacts has call enabled.
     * @param listener the <tt>DetailsResponseListener</tt> to listen for result
     * details
     * @param checkForTelephonyOpSet whether we should check for registered
     * telephony operation sets that can be used to dial out, can be used
     * in plugins dialing out using methods outside the provider.
     * @return is call enabled for metaContact.
     */
    public boolean isCallEnabled(DetailsResponseListener listener,
                                 boolean checkForTelephonyOpSet)
    {
        // make sure children are checked
        if(!checkMetaContactPhones(listener))
            return false;

         boolean hasPhoneCheck = hasPhones;

         if(checkForTelephonyOpSet)
             hasPhoneCheck =
                 hasPhones && AccountUtils.getRegisteredProviders(
                     OperationSetBasicTelephony.class).size() > 0;

        return metaContact.getDefaultContact(
                    OperationSetBasicTelephony.class) != null
               || hasPhoneCheck;
    }

    /**
     * Is call enabled for metaContact. If any of the child
     * contacts has call enabled.
     * @return is call enabled for metaContact.
     */
    public boolean isCallEnabled()
    {
        return isCallEnabled(null, true);
    }

    /**
     * Is call enabled for metaContact. If any of the child
     * contacts has call enabled.
     * @param checkForTelephonyOpSet whether we should check for registered
     * telephony operation sets that can be used to dial out, can be used
     * in plugins dialing out using methods outside the provider.
     * @return is call enabled for metaContact.
     */
    public boolean isCallEnabled(boolean checkForTelephonyOpSet)
    {
        return isCallEnabled(null,
            checkForTelephonyOpSet);
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
        return checkMetaContactPhones(null);
    }

    /**
     * Checking all contacts for the metacontact.
     * Return <tt>false</tt> if there are listeners added for a contact
     * and we need to stop executions cause listener will be used to be informed
     * for result.
     *
     * @param l the <tt>DetailsResponseListener</tt> to listen for further
     * details
     * @return whether to continue or listeners present and will be informed
     * for result.
     */
    private boolean checkMetaContactPhones(DetailsResponseListener l)
    {
        Iterator<Contact> contactIterator = metaContact.getContacts();
        while(contactIterator.hasNext())
        {
            Contact contact = contactIterator.next();
            if(phones.containsKey(contact))
                continue;

            List<String> phones = getPhones(contact, l, false);
            if(phones == null)
                return false;
        }

        return true;
    }

    /**
     * Checking all contacts for the metacontact.
     * Return <tt>false</tt> if there are listeners added for a contact
     * and we need to stop executions cause listener will be used to be informed
     * for result.
     *
     * @param l the <tt>DetailsResponseListener</tt> to listen for further
     * details
     * @return whether to continue or listeners present and will be informed
     * for result.
     */
    private boolean checkMetaContactVideoPhones(DetailsResponseListener l)
    {
        Iterator<Contact> contactIterator = metaContact.getContacts();
        while(contactIterator.hasNext())
        {
            Contact contact = contactIterator.next();
            if(videoPhones.containsKey(contact))
                continue;

            List<String> phones = getVideoPhones(contact, l);
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
     *
     * @param d the detail.
     * @return the localized phone number.
     */
    protected String getLocalizedPhoneNumber(GenericDetail d)
    {
        if(d instanceof WorkPhoneDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.WORK_PHONE");
        }
        else if(d instanceof MobilePhoneDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.MOBILE_PHONE");
        }
        else if(d instanceof VideoDetail)
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.VIDEO_PHONE");
        }
        else
        {
            return UtilActivator.getResources().
                getI18NString(
                    "service.gui.HOME");
        }
    }
}
