/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.macosx;

import java.util.*;

import net.java.sip.communicator.plugin.addrbook.*;
import net.java.sip.communicator.service.contactsource.*;

/**
 * Implements <tt>ContactQuery</tt> for the Address Book of Mac OS X.
 *
 * @author Lyubomir Marinov
 */
public class MacOSXAddrBookContactQuery
    extends AsyncContactQuery<MacOSXAddrBookContactSourceService>
{

    /**
     * The properties of <tt>ABPerson</tt> which are to be queried by the
     * <tt>MacOSXAddrBookContactQuery</tt> instances.
     */
    private static final long[] ABPERSON_PROPERTIES
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
            kABOrganizationProperty()
        };

    /**
     * The index of the <tt>kABAIMInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABAIMInstantProperty = 0;

    /**
     * The index of the <tt>kABEmailProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABEmailProperty = 1;

    /**
     * The index of the <tt>kABFirstNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABFirstNameProperty = 2;

    /**
     * The index of the <tt>kABFirstNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABFirstNamePhoneticProperty = 3;

    /**
     * The index of the <tt>kABICQInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABICQInstantProperty = 4;

    /**
     * The index of the <tt>kABJabberInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABJabberInstantProperty = 5;

    /**
     * The index of the <tt>kABLastNameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABLastNameProperty = 6;

    /**
     * The index of the <tt>kABLastNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABLastNamePhoneticProperty = 7;

    /**
     * The index of the <tt>kABMiddleNameProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABMiddleNameProperty = 8;

    /**
     * The index of the <tt>kABMiddleNamePhoneticProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABMiddleNamePhoneticProperty = 9;

    /**
     * The index of the <tt>kABMSNInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABMSNInstantProperty = 10;

    /**
     * The index of the <tt>kABNicknameProperty</tt> <tt>ABPerson</tt> property
     * in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABNicknameProperty = 11;

    /**
     * The index of the <tt>kABOrganizationProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABOrganizationProperty = 15;

    /**
     * The index of the <tt>kABPersonFlags</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABPersonFlags = 14;

    /**
     * The index of the <tt>kABPhoneProperty</tt> <tt>ABPerson</tt> property in
     * {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABPhoneProperty = 12;

    /**
     * The flag which indicates that an <tt>ABRecord</tt> is to be displayed as
     * a company.
     */
    private static final long kABShowAsCompany = 1;

    /**
     * The mask which extracts the <tt>kABShowAsXXX</tt> flag from the
     * <tt>personFlags</tt> of an <tt>ABPerson</tt>.
     */
    private static final long kABShowAsMask = 7;

    /**
     * The index of the <tt>kABYahooInstantProperty</tt> <tt>ABPerson</tt>
     * property in {@link #ABPERSON_PROPERTIES}.
     */
    private static final int kABYahooInstantProperty = 13;

    /**
     * The indexes in {@link #ABPERSON_PROPERTIES} of the properties which are
     * to be represented in <tt>SourceContact</tt> as <tt>ContactDetail</tt>s.
     */
    private static final int[] CONTACT_DETAIL_PROPERTY_INDEXES
        = new int[]
        {
            kABEmailProperty,

            kABPhoneProperty,

            kABAIMInstantProperty,
            kABICQInstantProperty,
            kABJabberInstantProperty,
            kABMSNInstantProperty,
            kABYahooInstantProperty
        };

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
     * @param query the <tt>String</tt> for which <tt>contactSource</tt> i.e.
     * the Address Book of Mac OS X is being queried
     */
    public MacOSXAddrBookContactQuery(
            MacOSXAddrBookContactSourceService contactSource,
            String query)
    {
        super(contactSource, query);
    }

    private static native byte[] ABPerson_imageData(long person);

    private static native Object[] ABRecord_valuesForProperties(
            long record,
            long[] properties);

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
     * @return the <tt>contactDetails</tt> to be set on a <tt>SourceContact</tt>
     * which is to represent the <tt>ABPerson</tt> specified by <tt>values</tt>
     */
    private List<ContactDetail> getContactDetails(Object[] values)
    {
        List<ContactDetail> contactDetails = new LinkedList<ContactDetail>();

        for (int i = 0; i < CONTACT_DETAIL_PROPERTY_INDEXES.length; i++)
        {
            Object value = values[CONTACT_DETAIL_PROPERTY_INDEXES[i]];

            if (value instanceof String)
            {
                String stringValue = (String) value;

                if (stringValue.length() != 0)
                    contactDetails.add(new ContactDetail(stringValue));
            }
            else if (value instanceof Object[])
            {
                for (Object subValue : (Object[]) value)
                {
                    if (subValue instanceof String)
                    {
                        String stringSubValue = (String) subValue;

                        if (stringSubValue.length() != 0)
                        {
                            contactDetails.add(
                                    new ContactDetail(stringSubValue));
                        }
                    }
                }
            }
        }
        return contactDetails;
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
    private String getDisplayName(Object[] values)
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

        for (int i = 0; i < CONTACT_DETAIL_PROPERTY_INDEXES.length; i++)
        {
            Object value = values[CONTACT_DETAIL_PROPERTY_INDEXES[i]];

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
                for (Object subValue : (Object[]) value)
                {
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
     * Gets the value of the <tt>kABAIMInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABAIMInstantProperty</tt> constant
     */
    private static native long kABAIMInstantProperty();

    /**
     * Gets the value of the <tt>kABEmailProperty</tt> constant.
     *
     * @return the value of the <tt>kABEmailProperty</tt> constant
     */
    private static native long kABEmailProperty();

    /**
     * Gets the value of the <tt>kABFirstNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNameProperty</tt> constant
     */
    private static native long kABFirstNameProperty();

    /**
     * Gets the value of the <tt>kABFirstNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABFirstNamePhoneticProperty</tt> constant
     */
    private static native long kABFirstNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABICQInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABICQInstantProperty</tt> constant
     */
    private static native long kABICQInstantProperty();

    /**
     * Gets the value of the <tt>kABJabberInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABJabberInstantProperty</tt> constant
     */
    private static native long kABJabberInstantProperty();

    /**
     * Gets the value of the <tt>kABLastNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNameProperty</tt> constant
     */
    private static native long kABLastNameProperty();

    /**
     * Gets the value of the <tt>kABLastNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABLastNamePhoneticProperty</tt> constant
     */
    private static native long kABLastNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMiddleNameProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNameProperty</tt> constant
     */
    private static native long kABMiddleNameProperty();

    /**
     * Gets the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant.
     *
     * @return the value of the <tt>kABMiddleNamePhoneticProperty</tt> constant
     */
    private static native long kABMiddleNamePhoneticProperty();

    /**
     * Gets the value of the <tt>kABMSNInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABMSNInstantProperty</tt> constant
     */
    private static native long kABMSNInstantProperty();

    /**
     * Gets the value of the <tt>kABNicknameProperty</tt> constant.
     *
     * @return the value of the <tt>kABNicknameProperty</tt> constant
     */
    private static native long kABNicknameProperty();

    /**
     * Gets the value of the <tt>kABOrganizationProperty</tt> constant.
     *
     * @return the value of the <tt>kABOrganizationProperty</tt> constant
     */
    private static native long kABOrganizationProperty();

    /**
     * Gets the value of the <tt>kABPersonFlags</tt> constant.
     *
     * @return the value of the <tt>kABPersonFlags</tt> constant
     */
    private static native long kABPersonFlags();

    /**
     * Gets the value of the <tt>kABPhoneProperty</tt> constant.
     *
     * @return the value of the <tt>kABPhoneProperty</tt> constant
     */
    private static native long kABPhoneProperty();

    /**
     * Gets the value of the <tt>kABYahooInstantProperty</tt> constant.
     *
     * @return the value of the <tt>kABYahooInstantProperty</tt> constant
     */
    private static native long kABYahooInstantProperty();

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
        for (Object value : values)
        {
            if (value instanceof String)
            {
                if (((String) value).toLowerCase().contains(query))
                    return true;
            }
            else if (value instanceof Object[])
            {
                for (Object subValue : (Object[]) value)
                {
                    if ((subValue instanceof String)
                            && ((String) subValue)
                                    .toLowerCase().contains(query))
                        return true;
                }
            }
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

        if (matches(values))
        {
            String displayName = getDisplayName(values);

            if (displayName.length() != 0)
            {
                List<ContactDetail> contactDetails = getContactDetails(values);

                if (!contactDetails.isEmpty())
                {
                    AddrBookSourceContact sourceContact
                        = new AddrBookSourceContact(
                                getContactSource(),
                                displayName,
                                contactDetails);

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
        }
        return (getStatus() == QUERY_IN_PROGRESS);
    }

    /**
     * Performs this <tt>AsyncContactQuery</tt> in a background <tt>Thread</tt>.
     *
     * @see AsyncContactQuery#run()
     */
    protected void run()
    {
        foreachPerson(
            query,
            new PtrCallback()
            {
                public boolean callback(long person)
                {
                    return onPerson(person);
                }
            });
    }
}
