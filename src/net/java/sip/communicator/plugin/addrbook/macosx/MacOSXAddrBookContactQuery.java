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
package net.java.sip.communicator.plugin.addrbook.macosx;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactsource.ContactDetail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactQuery
    extends AbstractAddrBookContactQuery<MacOSXAddrBookContactSourceService>
{
    /**
     * The <tt>Logger</tt> used by the <tt>MacOSXAddrBookContactQuery</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MacOSXAddrBookContactQuery.class);

    /**
     * The properties of <tt>ABPerson</tt> which are to be queried by the
     * <tt>MacOSXAddrBookContactQuery</tt> instances.
     */
    public static final long[] ABPERSON_PROPERTIES
        = new long[]
        {
            kABAIMInstantProperty(),
            kABEmailProperty(),
            kABFirstNameProperty(),
            kABFirstNamePhoneticProperty(),
            kABICQInstantProperty(),
            kABJabberInstantProperty(),
            kABLastNameProperty(),
            kABLastNamePhoneticProperty(),
            kABMiddleNameProperty(),
            kABMiddleNamePhoneticProperty(),
            kABMSNInstantProperty(),
            kABNicknameProperty(),
            kABPhoneProperty(),
            kABYahooInstantProperty(),
            kABPersonFlags(),
            kABOrganizationProperty(),
            kABMaidenNameProperty(),
            kABBirthdayProperty(),
            kABJobTitleProperty(),
            kABHomePageProperty(),
            kABURLsProperty(),
            kABCalendarURIsProperty(),
            kABAddressProperty(),
            kABOtherDatesProperty(),
            kABRelatedNamesProperty(),
            kABDepartmentProperty(),
            kABNoteProperty(),
            kABTitleProperty(),
            kABSuffixProperty()
        };

    /**
     * The index of the <tt>kABAIMInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABAIMInstantProperty = 0;

    /**
     * The index of the <tt>kABEmailProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABEmailProperty = 1;

    /**
     * The index of the <tt>kABFirstNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABFirstNameProperty = 2;

    /**
     * The index of the <tt>kABFirstNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABFirstNamePhoneticProperty = 3;

    /**
     * The index of the <tt>kABICQInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABICQInstantProperty = 4;

    /**
     * The index of the <tt>kABJabberInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABJabberInstantProperty = 5;

    /**
     * The index of the <tt>kABLastNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABLastNameProperty = 6;

    /**
     * The index of the <tt>kABLastNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABLastNamePhoneticProperty = 7;

    /**
     * The index of the <tt>kABMiddleNameProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMiddleNameProperty = 8;

    /**
     * The index of the <tt>kABMiddleNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMiddleNamePhoneticProperty = 9;

    /**
     * The index of the <tt>kABMSNInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMSNInstantProperty = 10;

    /**
     * The index of the <tt>kABNicknameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABNicknameProperty = 11;

    /**
     * The index of the <tt>kABOrganizationProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABOrganizationProperty = 15;

    /**
     * The index of the <tt>kABPersonFlags</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABPersonFlags = 14;

    /**
     * The index of the <tt>kABPhoneProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABPhoneProperty = 12;

    /**
     * The flag which indicates that an <tt>ABRecord</tt> is to be displayed as
     * a company.
     */
    public static final long kABShowAsCompany = 1;

    /**
     * The mask which extracts the <tt>kABShowAsXXX</tt> flag from the
     * <tt>personFlags</tt> of an <tt>ABPerson</tt>.
     */
    public static final long kABShowAsMask = 7;

    /**
     * The index of the <tt>kABYahooInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABYahooInstantProperty = 13;

    /**
     * The index of the <tt>kABMaidenNameProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABMaidenNameProperty = 16;

    /**
     * The index of the <tt>kABBirthdayProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABBirthdayProperty = 17;

    /**
     * The index of the <tt>kABJobTitleProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABJobTitleProperty = 18;

    /**
     * The index of the <tt>kABHomePageProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABHomePageProperty = 19;

    /**
     * The index of the <tt>kABURLsProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABURLsProperty = 20;

    /**
     * The index of the <tt>kABCalendarURIsProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABCalendarURIsProperty = 21;

    /**
     * The index of the <tt>kABAddressProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABAddressProperty = 22;

    /**
     * The index of the <tt>kABOtherDatesProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABOtherDatesProperty = 23;

    /**
     * The index of the <tt>kABRelatedNamesProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABRelatedNamesProperty = 24;

    /**
     * The index of the <tt>kABDepartmentProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABDepartmentProperty = 25;

    /**
     * The index of the <tt>kABNoteProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABNoteProperty = 26;

    /**
     * The index of the <tt>kABTitleProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABTitleProperty = 27;

    /**
     * The index of the <tt>kABSuffixProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    public static final int kABSuffixProperty = 28;

    /**
     * The regex which matches the superfluous parts of an <tt>ABMultiValue</tt>
     * label.
     */
    private static final Pattern LABEL_PATTERN
        = Pattern.compile(
            "kAB|Email|Phone|Label|(\\p{Punct}*)",
            Pattern.CASE_INSENSITIVE);

    static
    {
        System.loadLibrary("jmacosxaddrbook");
    }

    /**
     * Initializes a new <tt>MacOSXAddrBookContactQuery</tt> which is to perform
     * a specific <tt>query</tt> in the Address Book of Mac OS X on behalf of a
     * specific <tt>MacOSXAddrBookContactSourceService</tt>.
     *
     * @param contactSource the <tt>MacOSXAddrBookContactSourceService</tt>
     * which is to perform the new <tt>ContactQuery</tt> instance
     * @param query the <tt>Pattern</tt> for which <tt>contactSource</tt> i.e.
     * the Address Book of Mac OS X is being queried
     */
    public MacOSXAddrBookContactQuery(
            MacOSXAddrBookContactSourceService contactSource,
            Pattern query)
    {
        super(contactSource, query);
    }

    /**
     * Gets the <tt>imageData</tt> of a specific <tt>ABPerson</tt> instance.
     *
     * @param person the pointer to the <tt>ABPerson</tt> instance to get the
     * <tt>imageData</tt> of
     * @return the <tt>imageData</tt> of the specified <tt>ABPerson</tt>
     * instance
     */
    public static native byte[] ABPerson_imageData(long person);

    /**
     * Gets the values of a specific set of <tt>ABRecord</tt> properties for a
     * specific <tt>ABRecord</tt> instance.
     *
     * @param record the pointer to the <tt>ABRecord</tt> to get the property
     * values of
     * @param properties the set of <tt>ABRecord</tt> properties to get the
     * values of
     * @return the values of the specified set of <tt>ABRecord</tt> properties
     * for the specified <tt>ABRecord</tt> instance
     */
    public static native Object[] ABRecord_valuesForProperties(
            long record,
            long[] properties);

    /**
     * Returns the unique id of a record.
     * @param record the record which id is retrieved.
     * @return the record id.
     */
    public static native String ABRecord_uniqueId(long record);

    /**
     * Sets property for the supplied person id.
     * @param id the person id
     * @param property the property to use.
     * @param subPropety any sub property if available.
     * @param value the value to set.
     * @return whether the result was successfully added.
     */
    public static native boolean setProperty(
        String id, long property, String subPropety, Object value);

    /**
     * Remove a property.
     * @param id the person id.
     * @param property the property.
     * @return whether the result was successfully removed.
     */
    public static native boolean removeProperty(String id, long property);

    /**
     * Removes a contact from the address book.
     *
     * @param id the person id.
     *
     * @return whether the contact was successfully removed.
     */
    public static native boolean deleteContact(String id);

    /**
     * Creates a new address book contact.
     *
     * @return The identifier of the created contact. null if failed.
     */
    public static native String createContact();

    /**
     * Gets the pointer of the given contact.
     *
     * @param id the person id.
     *
     * @return The pointer of the given contact. Null if failed.
     */
    public static native long getContactPointer(String id);

    /**
     * Initializes a new <tt>ContactDetail</tt> instance which is to reperesent
     * a specific contact address that is the value of a specific
     * <tt>ABPerson</tt> property and, optionally, has a specific label.
     *
     * @param property the index in {@link #ABPERSON_PROPERTIES} of the
     * <tt>ABPerson</tt> property to be represented by <tt>ContactDetail</tt>
     * @param contactAddress the contact address to be represented by the new
     * <tt>ContactDetail</tt> instance
     * @param label an optional label to be added to the set of labels, if any,
     * determined by <tt>property</tt>
     * @param id The id of the detail.
     *
     * @return a new <tt>ContactDetail</tt> instance which represents the
     * specified <tt>contactAddress</tt>
     */
    private ContactDetail createContactDetail(
            int property,
            String contactAddress,
            Object label,
            String additionalProperty,
            String id)
    {
        Category c;
        SubCategory sc = null;

        switch (property)
        {
        case kABEmailProperty:
            c = Category.Email;
            break;
        case kABPhoneProperty:
            c = Category.Phone;
            break;
        case kABAIMInstantProperty:
            sc = SubCategory.AIM;
            c = Category.InstantMessaging;
            break;
        case kABICQInstantProperty:
            sc = SubCategory.ICQ;
            c = Category.InstantMessaging;
            break;
        case kABJabberInstantProperty:
            sc = SubCategory.Jabber;
            c = Category.InstantMessaging;
            break;
        case kABMSNInstantProperty:
            sc = SubCategory.MSN;
            c = Category.InstantMessaging;
            break;
        case kABYahooInstantProperty:
            sc = SubCategory.Yahoo;
            c = Category.InstantMessaging;
            break;
        case kABMaidenNameProperty:
        case kABFirstNameProperty:
            sc = SubCategory.Name;
            c = Category.Personal;
            break;
        case kABFirstNamePhoneticProperty:
            sc = SubCategory.Name;
            c = Category.Personal;
            break;
        case kABLastNameProperty:
            sc = SubCategory.LastName;
            c = Category.Personal;
            break;
        case kABLastNamePhoneticProperty:
            sc = SubCategory.LastName;
            c = Category.Personal;
            break;
        case kABMiddleNameProperty:
        case kABMiddleNamePhoneticProperty:
        case kABNicknameProperty:
            sc = SubCategory.Nickname;
            c = Category.Personal;
            break;
        case kABBirthdayProperty:
        case kABURLsProperty:
        case kABHomePageProperty:
            sc = SubCategory.HomePage;
            c = Category.Personal;
            break;
        case kABOtherDatesProperty:
        case kABRelatedNamesProperty:
        case kABNoteProperty:
        case kABTitleProperty:
        case kABSuffixProperty:
            c = Category.Personal;
            break;
        case kABOrganizationProperty:
        case kABJobTitleProperty:
            sc = SubCategory.JobTitle;
            c = Category.Organization;
            break;
        case kABDepartmentProperty:
            c = Category.Organization;
            sc = SubCategory.Name;
            break;
        case kABAddressProperty:
            c = Category.Address;
            break;
        default:
            c = null;
            break;
        }

        if (sc == null)
        {
            if (label == null)
                sc = null;
            else
            {
                sc = getSubCategoryFromLabel(label);
            }
        }

        SubCategory[] subCategories;
        SubCategory additionalSubCategory = null;

        if(additionalProperty != null)
            additionalSubCategory = getSubCategoryFromLabel(additionalProperty);

        if(additionalSubCategory != null)
            subCategories = new SubCategory[]
                { sc, additionalSubCategory };
        else
            subCategories = new SubCategory[]{ sc };

        return new MacOSXAddrBookContactDetail(
            property,
            contactAddress,
            c,
            subCategories,
            additionalProperty,
            id);
    }

    /**
     * Returns the SubCategory corresponding to the given label.
     *
     * @param label the label to match to a <tt>SubDirectory</tt>
     * @return the <tt>SubDirectory</tt> corresponding to the
     * given label
     */
    private SubCategory getSubCategoryFromLabel(Object label)
    {
        String labelString
            = LABEL_PATTERN.matcher((String) label).replaceAll("").trim();

        if (labelString.length() < 1)
            return null;

        SubCategory subCategory = null;

        if (labelString.equalsIgnoreCase("home"))
            subCategory = SubCategory.Home;
        else if (labelString.equalsIgnoreCase("work"))
            subCategory = SubCategory.Work;
        else if (labelString.equalsIgnoreCase("other"))
            subCategory = SubCategory.Other;
        else if (labelString.equalsIgnoreCase("mobile"))
            subCategory = SubCategory.Mobile;
        else if (labelString.equalsIgnoreCase("homepage"))
            subCategory = SubCategory.HomePage;
        else if (labelString.equalsIgnoreCase("street"))
            subCategory = SubCategory.Street;
        else if (labelString.equalsIgnoreCase("state"))
            subCategory = SubCategory.State;
        else if (labelString.equalsIgnoreCase("ZIP"))
            subCategory = SubCategory.PostalCode;
        else if (labelString.equalsIgnoreCase("country"))
            subCategory = SubCategory.Country;
        else if (labelString.equalsIgnoreCase("city"))
            subCategory = SubCategory.City;
        else if (labelString.equalsIgnoreCase("InstantMessageUsername"))
            subCategory = SubCategory.Nickname;
        else if (labelString.equalsIgnoreCase("workfax"))
            subCategory = SubCategory.Fax;
        else if (labelString.equalsIgnoreCase("fax"))
            subCategory = SubCategory.Fax;

        return subCategory;
    }

    /**
     * Calls back to a specific <tt>PtrCallback</tt> for each <tt>ABPerson</tt>
     * found in the Address Book of Mac OS X which matches a specific
     * <tt>String</tt> query.
     *
     * @param query the <tt>String</tt> for which the Address Book of Mac OS X
     * is to be queried. <b>Warning</b>: Ignored at the time of this writing.
     * @param callback the <tt>PtrCallback</tt> to be notified about the
     * matching <tt>ABPerson</tt>s
     */
    private static native void foreachPerson(
            String query,
            PtrCallback callback);

    /**
     * Gets the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent an <tt>ABPerson</tt> specified by the values of its
     * {@link #ABPERSON_PROPERTIES}.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to get the <tt>contactDetails</tt> of
     * @param id The id of the detail.
     *
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent the <tt>ABPerson</tt> specified by <tt>values</tt>
     */
    private List<ContactDetail> getContactDetails(Object[] values, String id)
    {
        List<ContactDetail> contactDetails = new LinkedList<ContactDetail>();

        for (int i = 0; i < ABPERSON_PROPERTIES.length; i++)
        {
            int property = i;
            Object value = values[property];

            if (value instanceof String)
            {
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                {
                    if (kABPhoneProperty == property)
                        stringValue
                            = AddrBookActivator.getPhoneNumberI18nService()
                                .normalize(stringValue);

                    contactDetails.add(
                            setCapabilities(
                                    createContactDetail(
                                            property,
                                            stringValue,
                                            null,
                                            null,
                                            id),
                                    property));
                }
            }
            else if (value instanceof Object[])
            {
                parseMultiDetails(contactDetails,
                                  (Object[]) value,
                                  property,
                                  null,
                                  id);
            }
        }
        return contactDetails;
    }

    /**
     * Parses the multi value data resulting it in contact details.
     * @param contactDetails the result list
     * @param multiValue the values to parse.
     * @param property the current property being parsed.
     * @param id The id of the detail.
     */
    private void parseMultiDetails(
        List<ContactDetail> contactDetails,
        Object[] multiValue,
        int property,
        String label,
        String id)
    {
        if(multiValue == null)
            return;

        for (int multiValueIndex = 0;
                multiValueIndex < multiValue.length;
                multiValueIndex += 2)
        {
            Object subValue = multiValue[multiValueIndex];

            if (subValue instanceof String)
            {
                String stringSubValue = (String) subValue;

                if (stringSubValue.length() != 0)
                {
                    if (kABPhoneProperty == property)
                    {
                        stringSubValue
                            = AddrBookActivator.getPhoneNumberI18nService()
                                .normalize(stringSubValue);
                    }

                    Object l = multiValue[multiValueIndex + 1];

                    contactDetails.add(
                            setCapabilities(
                                    createContactDetail(
                                        property,
                                        stringSubValue,
                                        l,
                                        label,
                                        id),
                                    property));
                }
            }
            else if (subValue instanceof Object[])
            {
                String l = null;

                Object lObject = multiValue[multiValueIndex + 1];
                if(lObject instanceof String)
                    l = (String)lObject;

                parseMultiDetails(contactDetails,
                                  (Object[]) subValue,
                                  property,
                                  l,
                                  id);
            }
        }
    }

    /**
     * Gets the <tt>displayName</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent an <tt>ABPerson</tt> specified by the values of its
     * {@link #ABPERSON_PROPERTIES}.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to get the <tt>displayName</tt> of
     * @return the <tt>displayName</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent the <tt>ABPerson</tt> specified by <tt>values</tt>
     */
    private static String getDisplayName(Object[] values)
    {
        long personFlags
            = (values[kABPersonFlags] instanceof Long)
                ? ((Long) values[kABPersonFlags]).longValue()
                : 0;
        String displayName;

        if ((personFlags & kABShowAsMask) == kABShowAsCompany)
        {
            displayName
                = (values[kABOrganizationProperty] instanceof String)
                    ? (String) values[kABOrganizationProperty]
                    : "";
            if (displayName.length() != 0)
                return displayName;
        }

        displayName
            = (values[kABNicknameProperty] instanceof String)
                ? (String) values[kABNicknameProperty]
                : "";
        if (displayName.length() != 0)
            return displayName;

        String firstName
            = (values[kABFirstNameProperty] instanceof String)
                ? (String) values[kABFirstNameProperty]
                : "";

        if ((firstName.length() == 0)
                && (values[kABFirstNamePhoneticProperty] instanceof String))
        {
            firstName = (String) values[kABFirstNamePhoneticProperty];
        }

        String lastName
            = (values[kABLastNameProperty] instanceof String)
                ? (String) values[kABLastNameProperty]
                : "";

        if ((lastName.length() == 0)
                && (values[kABLastNamePhoneticProperty] instanceof String))
            lastName = (String) values[kABLastNamePhoneticProperty];
        if ((lastName.length() == 0)
                && (values[kABMiddleNameProperty] instanceof String))
            lastName = (String) values[kABMiddleNameProperty];
        if ((lastName.length() == 0)
                && (values[kABMiddleNamePhoneticProperty] instanceof String))
            lastName = (String) values[kABMiddleNamePhoneticProperty];

        if (firstName.length() == 0)
            displayName = lastName;
        else
        {
            displayName
                = (lastName.length() == 0)
                    ? firstName
                    : (firstName + " " + lastName);
        }
        if (displayName.length() != 0)
            return displayName;

        for (int i = 0; i < ABPERSON_PROPERTIES.length; i++)
        {
            Object value = values[i];

            if (value instanceof String)
            {
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                {
                    displayName = stringValue;
                    break;
                }
            }
            else if (value instanceof Object[])
            {
                Object[] multiValue = (Object[]) value;

                for (int multiValueIndex = 0;
                        multiValueIndex < multiValue.length;
                        multiValueIndex += 2)
                {
                    Object subValue = multiValue[multiValueIndex];

                    if (subValue instanceof String)
                    {
                        String stringSubValue = (String) subValue;

                        if (stringSubValue.length() != 0)
                        {
                            displayName = stringSubValue;
                            break;
                        }
                    }
                }
            }
        }
        return displayName;
    }

    /**
     * Gets the organization name to be set on a <tt>SourceContact</tt>.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to get the organization name of.
     *
     * @return The organization name to be set on a <tt>SourceContact</tt>.
     */
    private static String getOrganization(Object[] values)
    {
        String organization = "";
        long personFlags
            = (values[kABPersonFlags] instanceof Long)
                ? ((Long) values[kABPersonFlags]).longValue()
                : 0;

        if ((personFlags & kABShowAsMask) != kABShowAsCompany)
        {
            organization = (values[kABOrganizationProperty] instanceof String)
                ? (String) values[kABOrganizationProperty]
                : "";
        }

        return organization;
    }

    /**
     * Gets the value of the <tt>kABAIMInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABAIMInstantProperty</tt> constant
     */
    public static native long kABAIMInstantProperty();

    /**
     * Gets the value of the <tt>kABEmailProperty</tt> constant.
     *
     * @return the value of the <tt>kABEmailProperty</tt> constant
     */
    public static native long kABEmailProperty();

    /**
     * Gets the value of the <tt>kABFirstNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNameProperty</tt> constant
     */
    public static native long kABFirstNameProperty();

    /**
     * Gets the value of the <tt>kABFirstNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNamePhoneticProperty</tt> constant
     */
    public static native long kABFirstNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABICQInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABICQInstantProperty</tt> constant
     */
    public static native long kABICQInstantProperty();

    /**
     * Gets the value of the <tt>kABJabberInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABJabberInstantProperty</tt> constant
     */
    public static native long kABJabberInstantProperty();

    /**
     * Gets the value of the <tt>kABLastNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNameProperty</tt> constant
     */
    public static native long kABLastNameProperty();

    /**
     * Gets the value of the <tt>kABLastNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNamePhoneticProperty</tt> constant
     */
    public static native long kABLastNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMiddleNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNameProperty</tt> constant
     */
    public static native long kABMiddleNameProperty();

    /**
     * Gets the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant
     */
    public static native long kABMiddleNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMSNInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABMSNInstantProperty</tt> constant
     */
    public static native long kABMSNInstantProperty();

    /**
     * Gets the value of the <tt>kABNicknameProperty</tt> constant.
     *
     * @return the value of the <tt>kABNicknameProperty</tt> constant
     */
    public static native long kABNicknameProperty();

    /**
     * Gets the value of the <tt>kABOrganizationProperty</tt> constant.
     *
     * @return the value of the <tt>kABOrganizationProperty</tt> constant
     */
    public static native long kABOrganizationProperty();

    /**
     * Gets the value of the <tt>kABPersonFlags</tt> constant.
     *
     * @return the value of the <tt>kABPersonFlags</tt> constant
     */
    public static native long kABPersonFlags();

    /**
     * Gets the value of the <tt>kABPhoneProperty</tt> constant.
     *
     * @return the value of the <tt>kABPhoneProperty</tt> constant
     */
    public static native long kABPhoneProperty();

    /**
     * Gets the value of the <tt>kABYahooInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABYahooInstantProperty</tt> constant
     */
    public static native long kABYahooInstantProperty();

    /**
     * Gets the value of the <tt>kABMaidenNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABMaidenNameProperty</tt> constant
     */
    public static native long kABMaidenNameProperty();

    /**
     * Gets the value of the <tt>kABBirthdayProperty</tt> constant.
     *
     * @return the value of the <tt>kABBirthdayProperty</tt> constant
     */
    public static native long kABBirthdayProperty();

    /**
     * Gets the value of the <tt>kABJobTitleProperty</tt> constant.
     *
     * @return the value of the <tt>kABJobTitleProperty</tt> constant
     */
    public static native long kABJobTitleProperty();

    /**
     * Gets the value of the <tt>kABHomePageProperty</tt> constant.
     *
     * @return the value of the <tt>kABHomePageProperty</tt> constant
     */
    public static native long kABHomePageProperty();

    /**
     * Gets the value of the <tt>kABURLsProperty</tt> constant.
     *
     * @return the value of the <tt>kABURLsProperty</tt> constant
     */
    public static native long kABURLsProperty();

    /**
     * Gets the value of the <tt>kABCalendarURIsProperty</tt> constant.
     *
     * @return the value of the <tt>kABCalendarURIsProperty</tt> constant
     */
    public static native long kABCalendarURIsProperty();

    /**
     * Gets the value of the <tt>kABAddressProperty</tt> constant.
     *
     * @return the value of the <tt>kABAddressProperty</tt> constant
     */
    public static native long kABAddressProperty();

    /**
     * Gets the value of the <tt>kABOtherDatesProperty</tt> constant.
     *
     * @return the value of the <tt>kABOtherDatesProperty</tt> constant
     */
    public static native long kABOtherDatesProperty();

    /**
     * Gets the value of the <tt>kABRelatedNamesProperty</tt> constant.
     *
     * @return the value of the <tt>kABRelatedNamesProperty</tt> constant
     */
    public static native long kABRelatedNamesProperty();

    /**
     * Gets the value of the <tt>kABDepartmentProperty</tt> constant.
     *
     * @return the value of the <tt>kABDepartmentProperty</tt> constant
     */
    public static native long kABDepartmentProperty();

    /**
     * Gets the value of the <tt>kABInstantMessageProperty</tt> constant.
     *
     * @return the value of the <tt>kABInstantMessageProperty</tt> constant
     */
    public static native long kABInstantMessageProperty();

    /**
     * Gets the value of the <tt>kABNoteProperty</tt> constant.
     *
     * @return the value of the <tt>kABNoteProperty</tt> constant
     */
    public static native long kABNoteProperty();

    /**
     * Gets the value of the <tt>kABTitleProperty</tt> constant.
     *
     * @return the value of the <tt>kABTitleProperty</tt> constant
     */
    public static native long kABTitleProperty();

    /**
     * Gets the value of the <tt>kABSuffixProperty</tt> constant.
     *
     * @return the value of the <tt>kABSuffixProperty</tt> constant
     */
    public static native long kABSuffixProperty();

    public static native String kABEmailWorkLabel();
    public static native String kABEmailHomeLabel();
    public static native String kABAddressHomeLabel();
    public static native String kABAddressWorkLabel();
    public static native String kABPhoneWorkLabel();
    public static native String kABPhoneHomeLabel();
    public static native String kABPhoneMobileLabel();
    public static native String kABPhoneMainLabel();
    public static native String kABPhoneWorkFAXLabel();
    public static native String kABHomeLabel();
    public static native String kABWorkLabel();
    public static native String kABOtherLabel();
    public static native String kABAddressStreetKey();
    public static native String kABAddressCityKey();
    public static native String kABAddressStateKey();
    public static native String kABAddressZIPKey();
    public static native String kABAddressCountryKey();


    /**
     * Determines whether a specific <tt>ABPerson</tt> property with a specific
     * <tt>value</tt> matches the {@link #query} of this
     * <tt>AsyncContactQuery</tt>.
     *
     * @param property the <tt>ABPerson</tt> property to check
     * @param value the value of the <tt>property</tt> to check
     * @return <tt>true</tt> if the specified <tt>value</tt> of the specified
     * <tt>property</tt> matches the <tt>query</tt> of this
     * <tt>AsyncContactQuery</tt>; otherwise, <tt>false</tt>
     */
    private boolean matches(int property, String value)
    {
        return
            query.matcher(value).find()
                || ((kABPhoneProperty == property) && phoneNumberMatches(value));
    }

    /**
     * Determines whether an <tt>ABPerson</tt> represented by the values of its
     * {@link #ABPERSON_PROPERTIES} matches {@link #query}.
     *
     * @param values the values of the <tt>ABPERSON_PROPERTIES</tt> which
     * represent the <tt>ABPerson</tt> to be determined whether it matches
     * <tt>query</tt>
     * @return <tt>true</tt> if the <tt>ABPerson</tt> represented by the
     * specified <tt>values</tt> matches <tt>query</tt>; otherwise,
     * <tt>false</tt>
     */
    private boolean matches(Object[] values)
    {
        int property = 0;

        for (Object value : values)
        {
            if (value instanceof String)
            {
                if (matches(property, (String) value))
                    return true;
            }
            else if (value instanceof Object[])
            {
                Object[] multiValue = (Object[]) value;

                for (int multiValueIndex = 0;
                        multiValueIndex < multiValue.length;
                        multiValueIndex += 2)
                {
                    Object subValue = multiValue[multiValueIndex];
                    if ((subValue instanceof String)
                            && matches(property, (String) subValue))
                        return true;
                }
            }
            property++;
        }
        return false;
    }

    /**
     * Notifies this <tt>MacOSXAddrBookContactQuery</tt> about a specific
     * <tt>ABPerson</tt>.
     *
     * @param person a pointer to the <tt>ABPerson</tt> instance to notify about
     * @return <tt>true</tt> if this <tt>MacOSXAddrBookContactQuery</tt> is to
     * continue being called; otherwise, <tt>false</tt>
     */
    private boolean onPerson(long person)
    {
        Object[] values
            = ABRecord_valuesForProperties(person, ABPERSON_PROPERTIES);
        final String id = ABRecord_uniqueId(person);

        String displayName = getDisplayName(values);
        if ((displayName.length() != 0)
            && (query.matcher(displayName).find() || matches(values)))
        {
            List<ContactDetail> contactDetails = getContactDetails(values, id);

            if (!contactDetails.isEmpty())
            {
                final MacOSXAddrBookSourceContact sourceContact
                    = new MacOSXAddrBookSourceContact(
                            getContactSource(),
                            displayName,
                            contactDetails);
                sourceContact.setData(SourceContact.DATA_ID, id);
                sourceContact.setDisplayDetails(getOrganization(values));

                try
                {
                    byte[] image = ABPerson_imageData(person);

                    if (image != null)
                        sourceContact.setImage(image);
                }
                catch (OutOfMemoryError oome)
                {
                    // Ignore it, the image is not vital.
                }

                addQueryResult(sourceContact);
            }
        }
        return (getStatus() == QUERY_IN_PROGRESS);
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    @Override
    protected void run()
    {
        foreachPerson(
            query.toString(),
            new PtrCallback()
            {
                @Override
                public boolean callback(long person)
                {
                    return onPerson(person);
                }
            });
    }

    /**
     * Sets the capabilities of a specific <tt>ContactDetail</tt> (e.g.
     * <tt>supportedOpSets</tt>) depending on the <tt>ABPerson</tt> property
     * that it stands for.
     *
     * @param contactDetail the <tt>ContactDetail</tt> to set the capabilities
     * of
     * @param property the index in {@link #ABPERSON_PROPERTIES} of the
     * <tt>ABPerson</tt> property represented by <tt>ContactDetail</tt>
     * @return <tt>contactDetail</tt>
     */
    private ContactDetail setCapabilities(
            ContactDetail contactDetail,
            int property)
    {
        List<Class<? extends OperationSet>> supportedOpSets
            = new LinkedList<Class<? extends OperationSet>>();
        Map<Class<? extends OperationSet>, String> preferredProtocols
            = new HashMap<Class<? extends OperationSet>, String>();

        // can be added as contacts
        supportedOpSets.add(OperationSetPersistentPresence.class);

        switch (property)
        {
        case kABAIMInstantProperty:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.AIM);
            break;
        case kABEmailProperty:
            supportedOpSets.add(OperationSetBasicTelephony.class);
            break;
        case kABICQInstantProperty:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.ICQ);
            break;
        case kABJabberInstantProperty:
            supportedOpSets.add(OperationSetBasicInstantMessaging.class);
            preferredProtocols.put(
                    OperationSetBasicInstantMessaging.class,
                    ProtocolNames.JABBER);
            supportedOpSets.add(OperationSetBasicTelephony.class);
            preferredProtocols.put(
                    OperationSetBasicTelephony.class,
                    ProtocolNames.JABBER);
            break;
        case kABPhoneProperty:
            supportedOpSets.add(OperationSetBasicTelephony.class);
            break;
        case kABYahooInstantProperty:
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
     * Callback method when receiving notifications for inserted items.
     */
    public void inserted(long person)
    {
        onPerson(person);
    }

    /**
     * Callback method when receiving notifications for updated items.
     */
    public void updated(long person)
    {
        SourceContact sourceContact =
            findSourceContactByID(ABRecord_uniqueId(person));
        if(sourceContact != null
            && sourceContact instanceof MacOSXAddrBookSourceContact)
        {
            // let's update the the details
            Object[] values
                = ABRecord_valuesForProperties(person, ABPERSON_PROPERTIES);
            String displayName = getDisplayName(values);
            final String id = ABRecord_uniqueId(person);

            MacOSXAddrBookSourceContact editableSourceContact
                = (MacOSXAddrBookSourceContact)sourceContact;

            editableSourceContact.setDisplayName(displayName);
            editableSourceContact.setDisplayDetails(getOrganization(values));

            List<ContactDetail> contactDetails = getContactDetails(values, id);
            editableSourceContact.setDetails(contactDetails);

            fireContactChanged(sourceContact);
        }
    }

    /**
     * Callback method when receiving notifications for deleted items.
     */
    public void deleted(String id)
    {
        SourceContact sourceContact = findSourceContactByID(id);

        if(sourceContact != null)
            fireContactRemoved(sourceContact);
    }

    /**
     * Find the property from category and subcategories.
     *
     * @param category
     * @param subCategories
     * @return
     */
    public static int getProperty(
        Category category,
        Collection<SubCategory> subCategories)
    {
        switch(category)
        {
            case Personal:
                if(subCategories.contains(SubCategory.Name))
                    return kABFirstNameProperty;
                else if(subCategories.contains(SubCategory.LastName))
                    return kABLastNameProperty;
                else if(subCategories.contains(SubCategory.Nickname))
                    return kABNicknameProperty;
                else if(subCategories.contains(SubCategory.HomePage))
                    return kABHomePageProperty;
                break;
            case Organization:
                if(subCategories.contains(SubCategory.JobTitle))
                    return kABJobTitleProperty;
                else
                    return kABDepartmentProperty;
            case Email:
                return kABEmailProperty;
            case InstantMessaging:
                if(subCategories.contains(SubCategory.AIM))
                    return kABAIMInstantProperty;
                else if(subCategories.contains(SubCategory.ICQ))
                    return kABICQInstantProperty;
                else if(subCategories.contains(SubCategory.MSN))
                    return kABMSNInstantProperty;
                else if(subCategories.contains(SubCategory.Jabber))
                    return kABJabberInstantProperty;
                else if(subCategories.contains(SubCategory.Yahoo))
                    return kABYahooInstantProperty;
                break;
            case Phone:
                return kABPhoneProperty;
            case Address:
                return kABAddressProperty;
            default: return -1;
        }

        return -1;
    }

    /**
     * Finds the label from category and sub categories.
     * @param subCategory
     * @return
     */
    public static String getLabel(
        int property,
        SubCategory subCategory,
        String subProperty)
    {
        switch(property)
        {
            case kABEmailProperty:
                if(subCategory == SubCategory.Home)
                    return kABEmailHomeLabel();
                if(subCategory == SubCategory.Work)
                    return kABEmailWorkLabel();
                break;
            case kABICQInstantProperty:
            case kABAIMInstantProperty:
            case kABYahooInstantProperty:
            case kABMSNInstantProperty:
            case kABJabberInstantProperty:
                return subProperty;
            case kABPhoneProperty:
                if(subCategory == SubCategory.Home)
                    return kABPhoneHomeLabel();
                if(subCategory == SubCategory.Work)
                    return kABPhoneWorkLabel();
                if(subCategory == SubCategory.Fax)
                    return kABPhoneWorkFAXLabel();
                if(subCategory == SubCategory.Mobile)
                    return kABPhoneMobileLabel();
                if(subCategory == SubCategory.Other)
                    return "other";
                break;
            case kABAddressProperty:
                if(subCategory == SubCategory.Street)
                    return kABAddressStreetKey();
                if(subCategory == SubCategory.City)
                    return kABAddressCityKey();
                if(subCategory == SubCategory.State)
                    return kABAddressStateKey();
                if(subCategory == SubCategory.Country)
                    return kABAddressCountryKey();
                if(subCategory == SubCategory.PostalCode)
                    return kABAddressZIPKey();
                break;
            default: return null;
        }

        return null;
    }

    /**
     * Adds a new empty contact, which will be filled in later.
     *
     * @param id The ID of the contact to add.
     */
    public void addEmptyContact(String id)
    {
        if(id != null)
        {
            final MacOSXAddrBookSourceContact sourceContact
                = new MacOSXAddrBookSourceContact(
                        getContactSource(),
                        null,
                        new LinkedList<ContactDetail>());
            sourceContact.setData(SourceContact.DATA_ID, id);
            addQueryResult(sourceContact);
        }
    }

    /**
     * Fires a contact changed event for the given contact.
     *
     * @param sourceContact The contact which has changed.
     */
    public void contactChanged(SourceContact sourceContact)
    {
        fireContactChanged(sourceContact);
    }
}
