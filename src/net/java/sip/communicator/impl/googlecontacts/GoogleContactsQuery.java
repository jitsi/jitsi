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
package net.java.sip.communicator.impl.googlecontacts;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements <tt>ContactQuery</tt> for Google Contacts.
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsQuery
    extends AsyncContactQuery<GoogleContactsSourceService>
{
    /**
     * Maximum results for Google Contacts query.
     */
    public static final int GOOGLECONTACTS_MAX_RESULTS = 20;

    /**
     * Maximum number of results for this instance.
     */
    private final int count;

    /**
     * The Google query.
     */
    private GoogleQuery gQuery = null;

    /**
     * Initializes a new <tt>GoogleContactsQuery</tt> instance which is to
     * perform a specific <tt>query</tt> on behalf of a specific
     * <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     * @param count maximum number of results
     */
    protected GoogleContactsQuery(GoogleContactsSourceService
            contactSource, Pattern query, int count)
    {
        super(contactSource, query);
        this.count = count;
    }

    /**
     * Create a <tt>SourceContact</tt> from a <tt>GoogleContactsEntry</tt>.
     *
     * @param entry <tt>GoogleContactsEntry</tt>
     */
    private void onGoogleContactsEntry(GoogleContactsEntry entry)
    {
        String displayName = entry.getFullName();
        if(displayName == null || displayName.length() == 0)
        {
            if((entry.getGivenName() == null ||
                    entry.getGivenName().length() == 0) &&
                    (entry.getFamilyName() == null ||
                            entry.getFamilyName().length() == 0))
            {
                return;
            }

            displayName = entry.getGivenName() + " " +
                entry.getFamilyName();
        }

        List<ContactDetail> contactDetails = getContactDetails(entry);

        if (!contactDetails.isEmpty())
        {
            GenericSourceContact sourceContact
                = new GenericSourceContact(
                        getContactSource(),
                        displayName,
                        contactDetails);

            try
            {
                byte img[] = GoogleContactsServiceImpl.downloadPhoto(
                        ((GoogleContactsEntryImpl)entry).getPhotoLink(),
                        getContactSource().getConnection().
                        getGoogleService());
                sourceContact.setImage(img);
            }
            catch (OutOfMemoryError oome)
            {
                // Ignore it, the image is not vital.
            }

            addQueryResult(sourceContact);
        }
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    @Override
    protected void run()
    {
        GoogleContactsServiceImpl service =
            GoogleContactsActivator.getGoogleContactsService();
        gQuery = new GoogleQuery(query);

        GoogleContactsConnection cnx = getContactSource().getConnection();

        if(cnx == null)
        {
            return;
        }

        service.searchContact(
                cnx,
                gQuery,
                count,
                new GoogleEntryCallback()
                {
                    public void callback(GoogleContactsEntry entry)
                    {
                        onGoogleContactsEntry(entry);
                    }
                });
    }

    @Override
    public synchronized void start()
    {
        boolean hasStarted = false;

        try
        {
            super.start();
            hasStarted = true;
        }
        finally
        {
            if (!hasStarted)
            {
                getContactSource().removeQuery(this);
            }
        }
    }
    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>.
     *
     * @param entry <tt>GoogleContactsEntry</tt>
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     */
    private List<ContactDetail> getContactDetails(GoogleContactsEntry entry)
    {
        List<ContactDetail> ret = new LinkedList<ContactDetail>();
        List<String> homeMails = entry.getHomeMails();
        List<String> workMails = entry.getWorkMails();
        List<String> mobilePhones = entry.getMobilePhones();
        List<String> homePhones = entry.getHomePhones();
        List<String> workPhones = entry.getWorkPhones();
        Map<String, GoogleContactsEntry.IMProtocol> ims =
            entry.getIMAddresses();
        ContactDetail detail = null;

        for(String mail : homeMails)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                            = new ArrayList<Class<? extends OperationSet>>(1);
            // can be added as contacts
            supportedOpSets.add(OperationSetPersistentPresence.class);

            detail = new ContactDetail(mail,
                    ContactDetail.Category.Email,
                    new ContactDetail.SubCategory[]{
                        ContactDetail.SubCategory.Home});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }
        for(String mail : workMails)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                            = new ArrayList<Class<? extends OperationSet>>(1);
            // can be added as contacts
            supportedOpSets.add(OperationSetPersistentPresence.class);

            detail = new ContactDetail(mail,
                    ContactDetail.Category.Email,
                    new ContactDetail.SubCategory[]{
                        ContactDetail.SubCategory.Work});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String homePhone : homePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(2);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            // can be added as contacts
            supportedOpSets.add(OperationSetPersistentPresence.class);
            homePhone = GoogleContactsActivator.getPhoneNumberI18nService()
                .normalize(homePhone);
            detail = new ContactDetail(homePhone,
                    ContactDetail.Category.Phone,
                    new ContactDetail.SubCategory[]{
                        ContactDetail.SubCategory.Home});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String workPhone : workPhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(2);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            // can be added as contacts
            supportedOpSets.add(OperationSetPersistentPresence.class);
            workPhone = GoogleContactsActivator.getPhoneNumberI18nService()
                .normalize(workPhone);
            detail = new ContactDetail(workPhone,
                ContactDetail.Category.Phone,
                new ContactDetail.SubCategory[]{
                    ContactDetail.SubCategory.Work});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String mobilePhone : mobilePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(2);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            // can be added as contacts
            supportedOpSets.add(OperationSetPersistentPresence.class);
            mobilePhone = GoogleContactsActivator.getPhoneNumberI18nService()
                .normalize(mobilePhone);
            detail = new ContactDetail(mobilePhone,
                ContactDetail.Category.Phone,
                new ContactDetail.SubCategory[]{
                    ContactDetail.SubCategory.Mobile});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(Map.Entry<String, GoogleContactsEntry.IMProtocol> im
                : ims.entrySet())
        {
            if(im.getValue() != GoogleContactsEntry.IMProtocol.OTHER)
            {
                ContactDetail.SubCategory imSubCat;
                switch(im.getValue())
                {
                case AIM:
                    imSubCat = ContactDetail.SubCategory.AIM;
                    break;
                case ICQ:
                    imSubCat = ContactDetail.SubCategory.ICQ;
                    break;
                case YAHOO:
                    imSubCat = ContactDetail.SubCategory.Yahoo;
                    break;
                case JABBER:
                    imSubCat = ContactDetail.SubCategory.Jabber;
                    break;
                case MSN:
                    imSubCat = ContactDetail.SubCategory.MSN;
                    break;
                case GOOGLETALK:
                    imSubCat = ContactDetail.SubCategory.GoogleTalk;
                    break;
                default:
                    imSubCat = null;
                    break;
                }

                detail
                    = new ContactDetail(
                            im.getKey(),
                            ContactDetail.Category.InstantMessaging,
                            new ContactDetail.SubCategory[] { imSubCat });

                setIMCapabilities(detail, im.getValue());

                // can be added as contacts
                detail.getSupportedOperationSets().add(
                        OperationSetPersistentPresence.class);

                ret.add(detail);
            }
        }

        return ret;
    }

    /**
     * Sets the IM capabilities of a specific <tt>ContactDetail</tt> (e.g.
     * <tt>supportedOpSets</tt>).
     *
     * @param contactDetail the <tt>ContactDetail</tt> to set the capabilities
     * of
     * @param protocol protocol
     * @return <tt>contactDetail</tt>
     */
    private ContactDetail setIMCapabilities(
            ContactDetail contactDetail,
            GoogleContactsEntry.IMProtocol protocol)
    {
        List<Class<? extends OperationSet>> supportedOpSets
            = new LinkedList<Class<? extends OperationSet>>();
        Map<Class<? extends OperationSet>, String> preferredProtocols
            = new HashMap<Class<? extends OperationSet>, String>();

        switch (protocol)
        {
        case GOOGLETALK:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.AIM);
            break;
        case ICQ:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.ICQ);
            break;
        case JABBER:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.JABBER);
            supportedOpSets.add(OperationSetBasicTelephony.class);
            preferredProtocols.put(
                    OperationSetBasicTelephony.class,
                    ProtocolNames.JABBER);
            break;
        case YAHOO:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.YAHOO);
            break;
        default:
            break;
        }
        contactDetail.setSupportedOpSets(supportedOpSets);

        if (!preferredProtocols.isEmpty())
            contactDetail.setPreferredProtocols(preferredProtocols);

        return contactDetail;
    }

    /**
     * Notifies this <tt>GoogleContactsQuery</tt> that it has stopped performing
     * in the associated background <tt>Thread</tt>.
     *
     * @param completed <tt>true</tt> if this <tt>ContactQuery</tt> has
     * successfully completed, <tt>false</tt> if an error has been encountered
     * during its execution
     * @see AsyncContactQuery#stopped(boolean)
     */
    @Override
    protected void stopped(boolean completed)
    {
        try
        {
            super.stopped(completed);
        }
        finally
        {
            getContactSource().stopped(this);
        }
    }

    /**
     * Cancels this <tt>ContactQuery</tt>.
     *
     * @see ContactQuery#cancel()
     */
    @Override
    public void cancel()
    {
        if(gQuery != null)
        {
            gQuery.cancel();
        }
        super.cancel();
    }
}
