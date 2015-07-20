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
package net.java.sip.communicator.plugin.thunderbird;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.jitsi.util.StringUtils;

import mork.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactsource.ContactDetail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Queries a Thunderbird address book for contacts matching the given pattern.
 *
 * @author Ingo Bauersachs
 */
public class ThunderbirdContactQuery
    extends AsyncContactQuery<ThunderbirdContactSourceService>
{
    /** Class logger */
    private final static Logger logger = Logger
        .getLogger(ThunderbirdContactQuery.class);

    /**
     * Creates a new instance of this class.
     *
     * @param owner The contact source that created this query.
     * @param query The pattern to match against the contacts database.
     */
    public ThunderbirdContactQuery(ThunderbirdContactSourceService owner,
        Pattern query)
    {
        super(owner, query);
    }

    /**
     * Starts the query against the address book database.
     */
    @Override
    protected void run()
    {
        String filename = super.getContactSource().getFilename();
        File file = new File(filename);
        try
        {
            if (file.lastModified() > getContactSource().lastDatabaseFileChange)
            {
                // parse the Thunderbird Mork database
                InputStreamReader sr =
                    new InputStreamReader(new FileInputStream(filename));
                MorkDocument md = new MorkDocument(sr);
                sr.close();

                // We now have rows in their tables and additional rows at
                // transaction level. Put the to a better format:
                // DB -> Tables -> Rows
                Map<String, Map<String, Row>> db =
                    new HashMap<String, Map<String, Row>>();
                for (Table t : md.getTables())
                {
                    String tableId = t.getTableId() + "/" + t.getScopeName();
                    Map<String, Row> table = db.get(tableId);
                    if (table == null)
                    {
                        table = new HashMap<String, Row>();
                        db.put(tableId, table);
                    }

                    for (Row r : t.getRows())
                    {
                        String scope = r.getScopeName();
                        if (scope == null)
                        {
                            scope = t.getScopeName();
                        }

                        table.put(r.getRowId() + "/" + scope, r);
                    }
                }

                // The additional rows at the root-level update/replace the ones
                // in the tables. There's usually neither a table nor a scope
                // defined, so lets just use the default.
                String defaultScope = md.getDicts().get(0).dereference("^80");
                for (Row r : md.getRows())
                {
                    String scope = r.getScopeName();
                    if (scope == null)
                    {
                        scope = defaultScope;
                    }

                    String tableId = "1/" + scope;
                    Map<String, Row> table = db.get(tableId);
                    if (table == null)
                    {
                        table = new HashMap<String, Row>();
                        db.put(tableId, table);
                    }

                    String rowId = r.getRowId() + "/" + scope;
                    if (rowId.startsWith("-"))
                    {
                        rowId = rowId.substring(1);
                    }

                    table.put(rowId, r);
                }

                super.getContactSource().database = db;
                super.getContactSource().defaultScope = defaultScope;
                super.getContactSource().lastDatabaseFileChange =
                    file.lastModified();
            }

            // okay, "transactions" are applied, now perform the search
            for (Entry<String, Map<String, Row>> table
                : super.getContactSource().database.entrySet())
            {
                for (Map.Entry<String, Row> e : table.getValue().entrySet())
                {
                    if (e.getKey().endsWith(getContactSource().defaultScope))
                    {
                        readEntry(e.getValue());
                    }
                }
            }

            super.stopped(true);
        }
        catch (FileNotFoundException e)
        {
            logger.warn("Could not open address book", e);
        }
        catch (Exception e)
        {
            logger.warn("Could not parse " + file, e);
        }
    }

    /**
     * Processes a database row by matching it against the query and adding it
     * to the result set if it matched.
     *
     * @param r The database row representing a contact.
     */
    private void readEntry(Row r)
    {
        // match the pattern against this contact
        boolean hadMatch = false;
        for (Alias value : r.getAliases().values())
        {
            if (value != null
                && (super.query.matcher(value.getValue()).find() || super
                    .phoneNumberMatches(value.getValue())))
            {
                hadMatch = true;
                break;
            }
        }

        // nope, didn't match, ignore
        if (!hadMatch)
        {
            return;
        }

        List<ContactDetail> details = new LinkedList<ContactDetail>();

        // e-mail(s)
        for (String email : getPropertySet(r, "PrimaryEmail", "SecondEmail",
            "DefaultEmail"))
        {
            ContactDetail detail = new ContactDetail(email, Category.Email);
            detail.addSupportedOpSet(OperationSetPersistentPresence.class);
            details.add(detail);
        }

        // phone number(s)
        this.addPhoneDetail(details, r, "HomePhone", SubCategory.Home);
        this.addPhoneDetail(details, r, "WorkPhone", SubCategory.Work);
        this.addPhoneDetail(details, r, "CellularNumber", SubCategory.Mobile);

        // and the dispaly name
        String displayName = r.getValue("DisplayName");
        if (StringUtils.isNullOrEmpty(displayName, true))
        {
            displayName = r.getValue("LastName");
            if (displayName != null)
            {
                displayName = displayName.trim();
            }

            String firstName = r.getValue("FirstName");
            if (!StringUtils.isNullOrEmpty(firstName, true))
            {
                displayName = firstName + " " + displayName;
            }
        }

        // create the contact and add it to the results
        GenericSourceContact sc =
            new GenericSourceContact(super.getContactSource(), displayName,
                details);
        addQueryResult(sc);
    }

    /**
     * Adds a "Phone" {@link ContactDetail} to a query contact.
     *
     * @param details The {@link List} of {@link ContactDetail}s to which the
     *            details is added.
     * @param r The source database row of the contact.
     * @param property The source database property name to add as a detail.
     * @param category The Phone-{@link SubCategory} for the phone number to
     *            add.
     */
    private void addPhoneDetail(List<ContactDetail> details, Row r,
        String property, SubCategory category)
    {
        String phone = r.getValue(property);
        if (StringUtils.isNullOrEmpty(phone, true))
        {
            return;
        }

        phone
            = ThunderbirdActivator.getPhoneNumberI18nService().normalize(phone);
        ContactDetail detail =
            new ContactDetail(phone, ContactDetail.Category.Phone,
                new ContactDetail.SubCategory[]
                { category });

        detail.addSupportedOpSet(OperationSetBasicTelephony.class);
        detail.addSupportedOpSet(OperationSetPersistentPresence.class);
        details.add(detail);
    }

    /**
     * Gets a set of non-empty properties from the source database row.
     *
     * @param r The source database row to process.
     * @param properties The property-names to extract.
     * @return A set of non-empty properties from the source database row.
     */
    private Set<String> getPropertySet(Row r, String... properties)
    {
        Set<String> validValues = new HashSet<String>(properties.length);
        for (String prop : properties)
        {
            String value = r.getValue(prop);
            if (!StringUtils.isNullOrEmpty(value, true))
            {
                validValues.add(value);
            }
        }

        return validValues;
    }
}
