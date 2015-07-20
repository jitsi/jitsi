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
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import org.jitsi.util.*;

/**
 * The <tt>PhoneNumberContactQuery</tt> is a query over the
 * <tt>PhoneNumberContactSource</tt>.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class PhoneNumberContactQuery
    extends AsyncContactQuery<PhoneNumberContactSource>
    implements ContactPresenceStatusListener
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * The contact count.
     */
    private int contactCount;

    /**
     * The operations sets we use to listen for the presence that will be used
     * for SourceContacts.
     */
    private List<OperationSetPersistentPresence> operationSetPersistentPresences
        = Collections.synchronizedList(
            new LinkedList<OperationSetPersistentPresence>());

    /**
     * Is the query searching for phone number.
     */
    private final boolean isQueryPhoneNumber;

    /**
     * Creates an instance of <tt>PhoneNumberContactQuery</tt> by specifying
     * the parent contact source, the query string to match and the maximum
     * result contacts to return.
     *
     * @param contactSource the parent contact source
     * @param queryString the query string to match
     * @param contactCount the maximum result contact count
     */
    public PhoneNumberContactQuery( PhoneNumberContactSource contactSource,
                                    String queryString,
                                    int contactCount)
    {
        super(contactSource,
            Pattern.compile(queryString, Pattern.CASE_INSENSITIVE
                            | Pattern.LITERAL), true);

        this.queryString = queryString;
        this.contactCount = contactCount;

        this.isQueryPhoneNumber
            = PNContactSourceActivator.getPhoneNumberI18nService()
                .isPhoneNumber(queryString);
    }

    /**
     * Do all the work in different thread.
     */
    @Override
    public void run()
    {
        Iterator<ProtocolProviderService> providers
            = PNContactSourceActivator
                .getPhoneNumberProviders().iterator();

        while (providers.hasNext())
        {
            if(contactCount > 0 && getQueryResultCount() > contactCount)
                break;

            ProtocolProviderService provider = providers.next();

            OperationSetPersistentPresence persPresOpSet
                = provider.getOperationSet(
                        OperationSetPersistentPresence.class);

            // If there's no presence operation set continue to the
            // next protocol provider.
            if (persPresOpSet == null)
                continue;

            if(!operationSetPersistentPresences.contains(persPresOpSet))
                operationSetPersistentPresences.add(persPresOpSet);

            persPresOpSet.addContactPresenceStatusListener(this);

            ContactGroup rootGroup
                = persPresOpSet.getServerStoredContactListRoot();

            addResultContactsForGroup(rootGroup);

            Iterator<ContactGroup> subgroups = rootGroup.subgroups();

            while (subgroups.hasNext())
            {
                ContactGroup group = subgroups.next();

                addResultContactsForGroup(group);
            }
        }

        if (getStatus() != QUERY_CANCELED)
            setStatus(QUERY_COMPLETED);
    }

    /**
     * Adss the result contacts for the given group.
     *
     * @param group the <tt>ContactGroup</tt> to check for matching contacts
     */
    private void addResultContactsForGroup(ContactGroup group)
    {
        Iterator<Contact> contacts = group.contacts();
        while (contacts.hasNext())
        {
            if(contactCount > 0 && getQueryResultCount() > contactCount)
                break;

            Contact contact = contacts.next();

            addAdditionalNumbers(contact);
        }
    }

    /**
     * Returns all additional phone numbers corresponding to the given
     * contact.
     *
     * @param contact the <tt>contact</tt>, which phone details we're
     * looking for
     * @return a list of all additional phone numbers corresponding to the
     * given contact
     */
    private void addAdditionalNumbers(Contact contact)
    {
        OperationSetServerStoredContactInfo infoOpSet
            = contact.getProtocolProvider().getOperationSet(
                OperationSetServerStoredContactInfo.class);

        Iterator<GenericDetail> details;

        if(infoOpSet != null)
        {
            details = infoOpSet.getAllDetailsForContact(contact);

            while(details.hasNext())
            {
                if(contactCount > 0 && getQueryResultCount() > contactCount)
                    break;

                GenericDetail d = details.next();
                if(d instanceof PhoneNumberDetail &&
                    !(d instanceof PagerDetail) &&
                    !(d instanceof FaxDetail))
                {
                    PhoneNumberDetail pnd = (PhoneNumberDetail)d;
                    if(pnd.getNumber() != null &&
                        pnd.getNumber().length() > 0)
                    {
                        String localizedType = null;

                        if(d instanceof WorkPhoneDetail)
                        {
                            localizedType =
                                PNContactSourceActivator.getResources()
                                .getI18NString("service.gui.WORK_PHONE");
                        }
                        else if(d instanceof MobilePhoneDetail)
                        {
                            localizedType =
                                PNContactSourceActivator.getResources()
                                .getI18NString("service.gui.MOBILE_PHONE");
                        }
                        else if(d instanceof VideoDetail)
                        {
                            localizedType =
                                PNContactSourceActivator.getResources().
                                    getI18NString(
                                        "service.gui.VIDEO_PHONE");
                        }
                        else
                        {
                            localizedType =
                                PNContactSourceActivator.getResources()
                                .getI18NString("service.gui.HOME");
                        }

                        String contactName = contact.getDisplayName();
                        String contactAddress = contact.getAddress();
                        String numberString = pnd.getNumber();

                        if(StringUtils.isNullOrEmpty(queryString)
                            || query.matcher(numberString).find()
                            || query.matcher(contactName).find()
                            || query.matcher(contactAddress).find()
                            || (isQueryPhoneNumber
                                && PNContactSourceActivator
                                    .getPhoneNumberI18nService()
                                    .phoneNumbersMatch(
                                        queryString, numberString))
                            )
                        {
                            ArrayList<ContactDetail> contactDetails
                                = new ArrayList<ContactDetail>();

                            String detailDisplayName
                                = pnd.getNumber() + "(" + localizedType + ")";
                            ContactDetail detail
                                = new ContactDetail(pnd.getNumber(),
                                                    detailDisplayName);

                            ArrayList<Class<? extends OperationSet>>
                                supportedOpSets
                                = new ArrayList<Class<? extends OperationSet>>();
                            supportedOpSets
                                .add(OperationSetBasicTelephony.class);
                            detail.setSupportedOpSets(supportedOpSets);

                            contactDetails.add(detail);

                            PhoneNumberSourceContact numberSourceContact
                                = new PhoneNumberSourceContact(
                                    this,
                                    getContactSource(),
                                    contact,
                                    contactDetails,
                                    detailDisplayName);

                            addQueryResult(numberSourceContact);
                        }
                    }
                }
            }
        }
    }

    protected String normalizePhoneNumber(String phoneNumber)
    {
        return null;
    }

    @Override
    protected boolean phoneNumberMatches(String phoneNumber)
    {
        return false;
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    public void cancel()
    {
        clearListeners();

        super.cancel();
    }

    /**
     * Clears any listener we used.
     */
    private void clearListeners()
    {
        for(OperationSetPersistentPresence opSetPresence
                : operationSetPersistentPresences)
        {
            opSetPresence.removeContactPresenceStatusListener(this);
        }
        operationSetPersistentPresences.clear();
    }

    /**
     * Listens for contact status changes and updates it and inform for the
     * change.
     * @param evt the ContactPresenceStatusChangeEvent describing the status
     */
    public void contactPresenceStatusChanged(
        ContactPresenceStatusChangeEvent evt)
    {
        for(SourceContact sc : getQueryResults())
        {
            if(!(sc instanceof PhoneNumberSourceContact))
                continue;

            Contact contact = ((PhoneNumberSourceContact)sc).getContact();

            if(contact.equals(evt.getSource()))
            {
                ((PhoneNumberSourceContact) sc).setPresenceStatus(
                    evt.getNewStatus());
                fireContactChanged(sc);
            }
        }
    }

    /**
     * If query has status changed to cancel, let's clear listeners.
     * @param status {@link ContactQuery#QUERY_CANCELED},
     * {@link ContactQuery#QUERY_COMPLETED}
     */
    public void setStatus(int status)
    {
        if(status == QUERY_CANCELED)
            clearListeners();

        super.setStatus(status);
    }
}
