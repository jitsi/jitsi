/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.phonenumbercontactsource;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 * The <tt>PhoneNumberContactQuery</tt> is a query over the
 * <tt>PhoneNumberContactSource</tt>.
 * 
 * @author Yana Stamcheva
 */
public class PhoneNumberContactQuery
    extends AsyncContactQuery<PhoneNumberContactSource>
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
                            | Pattern.LITERAL));

        this.queryString = queryString;
        this.contactCount = contactCount;
    }

    /**
     * Do all the work in different thread.
     */
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
                        else
                        {
                            localizedType =
                                PNContactSourceActivator.getResources()
                                .getI18NString("service.gui.HOME");
                        }

                        String contactName = contact.getDisplayName();
                        String contactAddress = contact.getAddress();
                        String numberString = pnd.getNumber();

                        if(queryString == null
                            || (queryString != null
                                && (numberString.startsWith(
                                            queryString)
                                    || contactName.startsWith(queryString)
                                    || contactAddress.startsWith(queryString)
                                    )))
                        {
                            ArrayList<ContactDetail> contactDetails
                                = new ArrayList<ContactDetail>();

                            ContactDetail detail
                                = new ContactDetail(pnd.getNumber());
                            ArrayList<Class<? extends OperationSet>>
                                supportedOpSets
                                = new ArrayList<Class<? extends OperationSet>>();
                            supportedOpSets
                                .add(OperationSetBasicTelephony.class);
                            detail.setSupportedOpSets(supportedOpSets);

                            contactDetails.add(detail);

                            PhoneNumberSourceContact numberSourceContact
                                = new PhoneNumberSourceContact(
                                    getContactSource(),
                                    contact,
                                    contactDetails,
                                    pnd.getNumber()
                                    + "(" + localizedType + ")");

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

    protected boolean phoneNumberMatches(String phoneNumber)
    {
        return false;
    }
}
