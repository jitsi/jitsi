/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ldap;

import java.util.*;
import java.util.regex.*;
import javax.swing.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.plugin.ldap.configform.*;

/**
 * Implements <tt>ContactQuery</tt> for LDAP.
 *
 * @author Sebastien Vincent
 */
public class LdapContactQuery
    extends AsyncContactQuery<LdapContactSourceService>
{
    /**
     * Maximum results for LDAP query.
     */
    public static final int LDAP_MAX_RESULTS = 40;

    /**
     * Maximum number of results for this instance.
     */
    private final int count;

    /**
     * LDAP query.
     */
    private LdapQuery ldapQuery = null;

    /**
     * Object lock.
     */
    private final Object objLock = new Object();

    /**
     * Initializes a new <tt>LdapContactQuery</tt> instance which is to perform
     * a specific <tt>query</tt> on behalf of a specific <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is to
     * perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> is
     * being queried
     * @param count maximum number of results
     */
    protected LdapContactQuery(LdapContactSourceService
            contactSource, Pattern query, int count)
    {
        super(contactSource, query);
        this.count = count;
    }

    /**
     * Normalizes a <tt>String</tt> phone number by converting alpha characters
     * to their respective digits on a keypad and then stripping non-digit
     * characters.
     *
     * @param phoneNumber a <tt>String</tt> which represents a phone number to
     * normalize
     * @return a <tt>String</tt> which is a normalized form of the specified
     * <tt>phoneNumber</tt>
     */
    protected String normalizePhoneNumber(String phoneNumber)
    {
        PhoneNumberI18nService phoneNumberI18nService
            = LdapActivator.getPhoneNumberI18nService();

        return
            (phoneNumberI18nService == null)
                ? phoneNumber
                : phoneNumberI18nService.normalize(phoneNumber);
    }

    /**
     * Determines whether a specific <tt>String</tt> phone number matches the
     * {@link #query} of this <tt>AsyncContactQuery</tt>.
     *
     * @param phoneNumber the <tt>String</tt> which represents the phone number
     * to match to the <tt>query</tt> of this <tt>AsyncContactQuery</tt>
     * @return <tt>true</tt> if the specified <tt>phoneNumber</tt> matches the
     * <tt>query</tt> of this <tt>AsyncContactQuery</tt>; otherwise,
     * <tt>false</tt>
     */
    protected boolean phoneNumberMatches(String phoneNumber)
    {
        /*
         * PhoneNumberI18nService implements functionality to aid the parsing,
         * formatting and validation of international phone numbers so attempt to
         * use it to determine whether the specified phoneNumber matches the
         * query. For example, check whether the normalized phoneNumber matches
         * the query.
         */

        PhoneNumberI18nService phoneNumberI18nService
            = LdapActivator.getPhoneNumberI18nService();
        boolean phoneNumberMatches = false;

        if (phoneNumberI18nService != null)
        {
            if (query
                    .matcher(phoneNumberI18nService.normalize(phoneNumber))
                        .find())
            {
                phoneNumberMatches = true;
            }
            else
            {
                /*
                 * The fact that the normalized form of the phoneNumber doesn't
                 * match the query doesn't mean that, for example, it doesn't
                 * match the normalized form of the query. The latter, though,
                 * requires the query to look like a phone number as well. In
                 * order to not accidentally start matching all queries to phone
                 * numbers, it seems justified to normalize the query only when
                 * it is a phone number, not whenever it looks like a piece of a
                 * phone number.
                 */

                String phoneNumberQuery = getPhoneNumberQuery();

                if ((phoneNumberQuery != null)
                        && (phoneNumberQuery.length() != 0))
                {
                    try
                    {
                        phoneNumberMatches
                            = phoneNumberI18nService.phoneNumbersMatch(
                                    phoneNumberQuery,
                                    phoneNumber);
                    }
                    catch (IllegalArgumentException iaex)
                    {
                        /*
                         * Ignore it, phoneNumberMatches will remain equal to
                         * false.
                         */
                    }
                }
            }
        }
        return phoneNumberMatches;
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    @Override
    protected void run()
    {
        /* query we get is delimited by \Q and \E
         * and we should not query LDAP server with a too small number of
         * characters
         */
        String queryStr = query.toString();
        if(queryStr.length() < (4))
        {
            return;
        }

        /* remove \Q and \E from the Pattern */
        String queryString = queryStr.substring(2, queryStr.length() - 2);
        LdapService ldapService = LdapActivator.getLdapService();
        LdapFactory factory = ldapService.getFactory();

        ldapQuery = factory.createQuery(queryString);
        LdapSearchSettings settings = factory.createSearchSettings();
        settings.setDelay(250);
        settings.setMaxResults(count);

        LdapListener caller = new LdapListener()
        {
            public void ldapEventReceived(LdapEvent evt)
            {
                processLdapResponse(evt);
            }
        };

        LdapDirectory ldapDir = getContactSource().getLdapDirectory();
        if(ldapDir == null)
        {
            return;
        }

        ldapDir.searchPerson(ldapQuery, caller, settings);

        synchronized(objLock)
        {
            try
            {
                objLock.wait();
            }
            catch(InterruptedException e)
            {
            }
        }
    }

    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>.
     *
     * @param person LDAP person
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     */
    private List<ContactDetail> getContactDetails(LdapPersonFound person)
    {
        List<ContactDetail> ret = new LinkedList<ContactDetail>();
        Set<String> mailAddresses = person.getMail();
        Set<String> mobilePhones = person.getMobilePhone();
        Set<String> homePhones = person.getHomePhone();
        Set<String> workPhones = person.getWorkPhone();
        ContactDetail detail = null;

        for(String mail : mailAddresses)
        {
            detail = new ContactDetail(mail, ContactDetail.CATEGORY_EMAIL,
                    null);
            ret.add(detail);
        }

        for(String homePhone : homePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            homePhone = normalizePhoneNumber(homePhone);
            detail = new ContactDetail(homePhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_HOME});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String workPhone : workPhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            workPhone = normalizePhoneNumber(workPhone);
            detail = new ContactDetail(workPhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_WORK});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }

        for(String mobilePhone : mobilePhones)
        {
            List<Class<? extends OperationSet>> supportedOpSets
                = new ArrayList<Class<? extends OperationSet>>(1);

            supportedOpSets.add(OperationSetBasicTelephony.class);
            mobilePhone = normalizePhoneNumber(mobilePhone);
            detail = new ContactDetail(mobilePhone,
                    ContactDetail.CATEGORY_PHONE,
                    new String[]{ContactDetail.LABEL_MOBILE});
            detail.setSupportedOpSets(supportedOpSets);
            ret.add(detail);
        }
        return ret;
    }

    /**
     * Process LDAP event.
     *
     * @param evt LDAP event
     */
    private void processLdapResponse(LdapEvent evt)
    {
        if(evt.getCause() == LdapEvent.LdapEventCause.SEARCH_ACHIEVED ||
                evt.getCause() == LdapEvent.LdapEventCause.SEARCH_CANCELLED)
        {
            synchronized(objLock)
            {
                objLock.notify();
            }
        }

        if(evt.getCause() == LdapEvent.LdapEventCause.NEW_SEARCH_RESULT)
        {
            LdapPersonFound person = (LdapPersonFound) evt.getContent();
            String displayName = null;

            if(person == null)
            {
                return;
            }

            if(person.getDisplayName() != null)
            {
                displayName = person.getDisplayName();
            }
            else
            {
                displayName = person.getFirstName() + " " + person.getSurname();
            }

            List<ContactDetail> contactDetails = getContactDetails(person);

            if (!contactDetails.isEmpty())
            {
                GenericSourceContact sourceContact
                    = new GenericSourceContact(
                            getContactSource(),
                            displayName,
                            contactDetails);

                try
                {
                    sourceContact.setImage(person.fetchPhoto());
                }
                catch (OutOfMemoryError oome)
                {
                    // Ignore it, the image is not vital.
                }

                addQueryResult(sourceContact);
            }
        }
        else if(evt.getCause() == LdapEvent.LdapEventCause.SEARCH_AUTH_ERROR)
        {
            synchronized(objLock)
            {
                objLock.notify();
            }

            /* show settings form */
            new Thread()
            {
                public void run()
                {
                    DirectorySettingsForm settingsForm =
                        new DirectorySettingsForm();
                    LdapDirectorySettings ldapSettings =
                        getContactSource().getLdapDirectory().getSettings();

                    settingsForm.setModal(true);

                    settingsForm.loadData(ldapSettings);

                    settingsForm.setNameFieldEnabled(false);
                    settingsForm.setHostnameFieldEnabled(false);
                    settingsForm.setBaseDNFieldEnabled(false);
                    settingsForm.setPortFieldEnabled(false);

                    JOptionPane.showMessageDialog(
                            null,
                            Resources.getString(
                                "impl.ldap.WRONG_CREDENTIALS",
                                    new String[]{ldapSettings.getName()}),
                            Resources.getString(
                                    "impl.ldap.WRONG_CREDENTIALS",
                                    new String[]{ldapSettings.getName()}),
                            JOptionPane.WARNING_MESSAGE);

                    int ret = settingsForm.showDialog();

                    if(ret == 1)
                    {
                        LdapService ldapService =
                            LdapActivator.getLdapService();
                        LdapFactory factory = ldapService.getFactory();
                        LdapDirectory ldapDir =
                            getContactSource().getLdapDirectory();
                        LdapDirectorySettings settings =
                            ldapDir.getSettings();

                        LdapActivator.disableContactSource(ldapDir);
                        ldapService.getServerSet().removeServerWithName(
                                settings.getName());

                        ldapDir = factory.createServer(
                                settingsForm.getSettings());
                        ldapService.getServerSet().addServer(ldapDir);

                        LdapActivator.enableContactSource(ldapDir);
                    }
                }
            }.start();
        }
    }

    /**
     * Notifies this <tt>LdapContactQuery</tt> that it has stopped performing
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
        if(ldapQuery != null)
        {
            ldapQuery.setState(LdapQuery.State.CANCELLED);
        }

        synchronized(objLock)
        {
            objLock.notify();
        }
        super.cancel();
    }
}
