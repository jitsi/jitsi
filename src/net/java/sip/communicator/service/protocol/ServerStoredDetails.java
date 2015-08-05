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
package net.java.sip.communicator.service.protocol;

import java.net.*;
import java.util.*;

import org.jitsi.util.*;

/**
 * The ServerStoredDetails class contains a relatively large set of details that
 * various protocols may support storing online. Each detail is represented by
 * its own inner class that could be instantiated by either the protocol
 * provider implementation (for representing details returned by the server) or
 * by the using service (e.g. the UIService, when representing details that the
 * local user would like to set for the current account).
 * <p>
 * All detail classes inherit from the GenericDetail class, extending it to
 * represent more and more concrete details. The WorkAddressDetail for example,
 * meant to represent a postal work address, extends the AddressDetailClass
 * which is meant for representing all kinds of postal addresses. The
 * AddressDetailClass on its turn extends the StringDetail which itself inherits
 * from the GenericDetailClass.
 * <p>
 * When creating details that do not exist here (which you'll probably have to
 * do at one point or another) you are encouraged to extend from the most
 * concrete address possible so that your detail could be meaningfully handled
 * by the User Interface.
 * <p>
 * Let's assume for example that we'd like to add a BirthPlaceAddressDetail,
 * indicating place of birth. The BirthPlaceAddress detail class should extend
 * the AddressDetail class so that the GUI could understand that this is an
 * address and visualize it appropriately. The same goes for variations of an
 * EmailAddressDetail or any other detail having anything to do with a detail
 * represented in this class.
 * <p>
 * All details have a detailValue and a displayName as well as get methods that
 * would give you (read-only) access to them. Most classes extending the
 * GenericDetail to something more meaningful would provide additional accessors
 * allowing you to retrieve the value casted to its native class.
 * <p>
 * Detail names may be used when visualizing the detail (however, keep in mind
 * that you should leave space for internationalization when doing so).
 * <p>
 * This class is meant for usage with the  OperationSetServerStoredAccountInfo
 * and OperationSetServerStoredContactInfo operation sets.
 *
 * @author Emil Ivov
 */
public class ServerStoredDetails
{
    /**
     * A generic detail used as the root of all other server stored details.
     * This class should be extended or instantiated by implementors with the
     * purpose of representing details not defined here.
     */
    public static class GenericDetail
    {
        protected Object value = null;
        protected String detailDisplayName  = null;

        /**
         * Instantiates this detail setting its value and display name
         * accordingly.
         * @param detailDisplayName a display name that may be used as a
         * description when visualizing the value of the detail
         * @param value the value of the detail.
         */
        public GenericDetail(String detailDisplayName, Object value)
        {
            this.value = value;
            this.detailDisplayName  = detailDisplayName;
        }

        /**
         * Returns the value of the detail.
         * @return the value of the detail.
         */
        public Object getDetailValue()
        {
            return value;
        }

        /**
         * Returns a display name that may be used as a description when
         * visualizing the value of the detail (make sure you don't use this
         * string in internationalized versions).
         * @return the detail's display (descriptive) name.
         */
        public String getDetailDisplayName()
        {
            return detailDisplayName;
        }

        /**
         * Returns a String representation of the detail using both its value
         * and display name.
         * @return a String representation of the detail using both its value
         * and display name.
         */
        @Override
        public String toString()
        {
            return value == null ? "": value.toString();
        }

        /**
         * Compares two GenericDetails according
         * their DetailDisplayName and Value
         *
         * @param obj Object expected GenericDetail otherwise return false
         * @return <tt>true</tt> if this object has the same display name and
         * value as <tt>obj</tt> and false otherwise
         */
        @Override
        public boolean equals(Object obj)
        {
            if(!(obj instanceof GenericDetail))
                return false;

            if(this == obj)
            {
                return true;
            }

            GenericDetail other = (GenericDetail)obj;

            if(this.detailDisplayName != null // equals DisplayName
               && other.getDetailDisplayName() != null
               && this.detailDisplayName.equals(other.getDetailDisplayName()) &&

               // equals not null values
               ((this.value != null
               && other.getDetailValue() != null
               && this.value.equals(other.getDetailValue()))
               ||
               // or both values are null / empty
               (StringUtils.isNullOrEmpty((String)this.value)
                && StringUtils.isNullOrEmpty((String)other.getDetailValue()))))
                return true;
            else
                return false;
        }
    }

    /**
     * A generic detail that should be used (extended) when representing details
     * with a String content.
     */
    public static class StringDetail extends GenericDetail
    {
        public StringDetail(String detailDisplayName, String value)
        {
            super(detailDisplayName, value);
        }

        public String getString()
        {
            return (String)value;
        }
    }

//---------------------- physical addresses  -----------------------------------
    /**
     * A detail representing an address (street and street/house number).
     */
    public static class AddressDetail extends StringDetail
    {
        public AddressDetail(String address)
        {
            super("Address", address);

        }
        public String getAddress()
        {
            return getString();
        }
    }

    /**
     * A detail representing a street name and number associated with a work
     * address.
     */
    public static class WorkAddressDetail extends AddressDetail
    {
        public WorkAddressDetail(String address)
        {
            super(address);
        }
    }

    /**
     * A City name associated with a (home) address.
     */
    public static class CityDetail extends StringDetail
    {
        public CityDetail(String cityName)
        {
            super("City", cityName);
        }

        public String getCity()
        {
            return getString();
        }
    }

    /**
     * A City name associated with a work address.
     */
    public static class WorkCityDetail extends CityDetail
    {
        public WorkCityDetail(String cityName)
        {
            super(cityName);
        }
    }

    /**
     * The name of a state/province/region associated with a (home) address.
     */
    public static class ProvinceDetail extends StringDetail
    {
        public ProvinceDetail(String province)
        {
            super("Region/Province/State", province);
        }

        public String getProvince()
        {
            return getString();
        }
    }

    /**
     * The name of a state/province/region associated with a work address.
     */
    public static class WorkProvinceDetail extends ProvinceDetail
    {
        public WorkProvinceDetail(String workProvince)
        {
            super(workProvince);
        }
    }

    /**
     * A postal or ZIP code associated with a (home) address.
     */
    public static class PostalCodeDetail extends StringDetail
    {
        public PostalCodeDetail(String postalCode)
        {
            super("Postal/Zip Code", postalCode);
        }

        public String getPostalCode()
        {
            return getString();
        }
    }

    /**
     * A postal or ZIP code associated with a work address.
     */
    public static class WorkPostalCodeDetail extends PostalCodeDetail
    {
        public WorkPostalCodeDetail(String postalCode)
        {
            super(postalCode);
        }
    }


//------------------------------ LOCALE DETAILS --------------------------------
    /**
     * A generic detail that should be used (extended) when representing details
     * that have anything to do with locales (countries, languages, etc). Most
     * of the locales field could be ignored when extending the class. When
     * representing a country for example we'd only be using the fields
     * concerning the country.
     */
    public static class LocaleDetail extends GenericDetail
    {
        public LocaleDetail(String detailDisplayName, Locale locale)
        {
            super(detailDisplayName, locale);
        }

        public Locale getLocale()
        {
            return (Locale)getDetailValue();
        }
    }

    /**
     * A detail representing a country of residence for the corresponding
     * subject.
     */
    public static class CountryDetail extends LocaleDetail
    {
        public CountryDetail(Locale locale)
        {
            super("Country", locale);
        }

        public CountryDetail(String country)
        {
            super("Country", null);
            value = country;
        }

        public Locale getLocale()
        {
            if(value instanceof Locale)
                return (Locale)value;
            else
                return null;
        }
    }

    /**
     * The name of a country associated with a work address.
     */
    public static class WorkCountryDetail extends CountryDetail
    {
        public WorkCountryDetail(Locale locale)
        {
            super(locale);
        }

        public WorkCountryDetail(String country)
        {
            super(country);
        }
    }

//-------------------------------- Language ------------------------------------
    /**
     * A locale detail indicating a language spoken by the corresponding Contact.
     */
    public static class SpokenLanguageDetail extends LocaleDetail
    {
        public SpokenLanguageDetail(Locale language)
        {
            super("Language", language);
        }
    }

//------------------------- phones --------------------------------------------
    /**
     * A generic detail used for representing a (personal) phone number.
     */
    public static class PhoneNumberDetail extends StringDetail
    {
        public PhoneNumberDetail(String number)
        {
            super("Phone", number);
        }

        public String getNumber()
        {
            return getString();
        }
    }

    /**
     * A detail used for representing a work phone number.
     */
    public static class WorkPhoneDetail extends PhoneNumberDetail
    {
        public WorkPhoneDetail(String workPhone)
        {
            super(workPhone);
            super.detailDisplayName = "Work Phone";
        }
    }

    /**
     * A detail used for representing a (personal) mobile phone number.
     */
    public static class MobilePhoneDetail extends PhoneNumberDetail
    {
        public MobilePhoneDetail(String privateMobile)
        {
            super(privateMobile);
            super.detailDisplayName = "Mobile Phone";
        }
    }

    /**
     * A detail used for representing a work mobile phone number.
     */
    public static class WorkMobilePhoneDetail extends MobilePhoneDetail
    {
        public WorkMobilePhoneDetail(String workMobile)
        {
            super(workMobile);
        }
    }

    /**
     * A Fax number.
     */
    public static class FaxDetail extends PhoneNumberDetail
    {
        public FaxDetail(String number)
        {
            super(number);
            super.detailDisplayName = "Fax";
        }
    }

    /**
     * A detail used for representing a video phone number.
     */
    public static class VideoDetail extends PhoneNumberDetail
    {
        public VideoDetail(String number)
        {
            super(number);
            super.detailDisplayName = "Video";
        }
    }

    /**
     * A detail used for representing a work video phone number.
     */
    public static class WorkVideoDetail extends VideoDetail
    {
        public WorkVideoDetail(String number)
        {
            super(number);
        }
    }

    /**
     * A Pager number.
     */
    public static class PagerDetail extends PhoneNumberDetail
    {
        public PagerDetail(String number)
        {
            super(number);
            super.detailDisplayName = "Pager";
        }
    }


//----------------------------- web page ---------------------------------------

    /**
     * A generic detail representing any url
     */
    public static class URLDetail extends GenericDetail
    {
        public URLDetail(String name, URL url)
        {
            super(name, url);
        }

        public URL getURL()
        {
            return (URL)getDetailValue();
        }

        /**
         * Compares two URLDetails according their name
         * and URLs
         *
         * @param obj Object expected URLDetail otherwise return false
         * @return <tt>true</tt> if this object has the same name and
         * URL value as <tt>obj</tt> and false otherwise
         */
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof URLDetail))
                return false;

            if (this == obj)
            {
                return true;
            }

            URLDetail other = (URLDetail)obj;
            boolean equalsDisplayName =
                this.detailDisplayName != null
                && other.getDetailDisplayName() != null
                && this.detailDisplayName.equals(other.getDetailDisplayName());
            boolean equalValues =
                this.value != null
                && other.getDetailValue() != null
                && this.value.equals(other.getDetailValue());

            boolean bothNullValues =
                this.value == null && other.value == null;
            if (equalsDisplayName && (equalValues || bothNullValues))
                return true;
            else
                return false;
        }
    }

    /**
     * A personal web page.
     */
    public static class WebPageDetail extends URLDetail
    {
        public WebPageDetail(URL url)
        {
            super("Web Page", url);
        }
    }

    /**
     * A web page associated with the subject's principal occupation (work).
     */
    public static class WorkPageDetail extends WebPageDetail
    {
        public WorkPageDetail(URL url)
        {
            super(url);
        }
    }

//--------------------------- Binary -------------------------------------------
    /**
     * A generic detail used for representing binary content such as photos
     * logos, avatars ....
     */
    public static class BinaryDetail extends GenericDetail
    {
        public BinaryDetail(String displayDetailName, byte[] bytes)
        {
            super(displayDetailName, bytes);
        }

        public byte[] getBytes()
        {
            return (byte[])getDetailValue();
        }

        /**
         * Compares two BinaryDetails according their DetailDisplayName
         * and the result of invoking their getBytes() methods.
         *
         * @param obj Object expected BinaryDetail otherwise return false
         * @return <tt>true</tt> if this object has the same display name and
         * value as <tt>obj</tt> and false otherwise
         */
        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof BinaryDetail))
                return false;

            if (this == obj)
            {
                return true;
            }

            BinaryDetail other = (BinaryDetail)obj;
            boolean equalsDisplayName =
                this.detailDisplayName != null
                && other.getDetailDisplayName() != null
                && this.detailDisplayName.equals(other.getDetailDisplayName());
            boolean equalsNotNull =
                this.value != null
                && other.getDetailValue() != null
                && Arrays.equals(this.getBytes(), other.getBytes());
            boolean nullOrEmpty =
                (this.value == null || this.getBytes().length == 0)
                && (other.getDetailValue() == null
                    || other.getBytes().length == 0);
            if (equalsDisplayName && (equalsNotNull || nullOrEmpty))
                return true;
            else
                return false;
        }
    }

    /**
     * A detail containing any contact related images.
     */
    public static class ImageDetail extends BinaryDetail
    {
        public ImageDetail(String detailDisplayName, byte[] image)
        {
            super(detailDisplayName, image);
        }
    }

//-------------------------- Names ---------------------------------------------

    /**
     * A generic detail representing any kind of name.
     */
    public static class NameDetail extends StringDetail
    {
        public NameDetail(String detailDisplayName, String name)
        {
            super(detailDisplayName, name);
        }

        public String getName()
        {
            return getString();
        }
    }


    /**
     * The name of the organization (company, ngo, university, hospital or other)
     * employing the corresponding contact.
     */
    public static class WorkOrganizationNameDetail extends NameDetail
    {
        public WorkOrganizationNameDetail(String workOrganizationName)
        {
            super("Work Organization Name", workOrganizationName);
        }
    }

    /**
     * A first, given name.
     */
    public static class FirstNameDetail extends NameDetail
    {
        public FirstNameDetail(String firstName)
        {
            super("First Name", firstName);
        }
    }

    /**
     * A Middle (father's) name.
     */
    public static class MiddleNameDetail extends NameDetail
    {
        public MiddleNameDetail(String middleName)
        {
            super("Middle Name", middleName);
        }
    }

    /**
     * A last (family) name.
     */
    public static class LastNameDetail extends NameDetail
    {
        public LastNameDetail(String lastName)
        {
            super("Last Name", lastName);
        }
    }

    /**
     * The name that should be displayed to identify the information author.
     */
    public static class DisplayNameDetail extends NameDetail
    {
        public DisplayNameDetail(String name)
        {
            super("Display Name", name);
        }
    }

    /**
     * An informal name (nickname) used for referring to the subject.
     */
    public static class NicknameDetail extends NameDetail
    {
        public NicknameDetail(String name)
        {
            super("Nickname", name);
        }
    }


    /**
     * A bi-state detail indicating a gender. Constructor is private and the only
     * possible instances are GenderDetail.MALE and GenderDetail.FEMALE
     * construction.
     */
    public static class GenderDetail extends StringDetail
    {
        public static final GenderDetail MALE = new GenderDetail("Male");
        public static final GenderDetail FEMALE = new GenderDetail("Female");

        public GenderDetail(String gender)
        {
            super("Gender",  gender);
        }

        /**
         * Returns a "Male" or "Female" string.
         * @return a String with a "Male" or "Female" contents
         */
        public String getGender()
        {
            return getString();
        }
    }

//-------------------------------- Date & Time ---------------------------------
    /**
     * A generic detail meant to represent any date (calendar) associated details.
     * Protocols that support separate fields for  year, month, day and time, or
     * even age should try their best to convert to a date (setting to 0
     * all unknown details).
     */
    public static class CalendarDetail extends GenericDetail
    {
        public CalendarDetail(String detailDisplayName, Calendar date)
        {
            super(detailDisplayName, date);
        }

        public Calendar getCalendar()
        {
            return (Calendar)getDetailValue();
        }
    }

    /**
     * A complete birth date.
     */
    public static class BirthDateDetail extends CalendarDetail
    {
        public BirthDateDetail(Calendar date)
        {
            super("Birth Date", date);
        }

        /**
         * Compares two BirthDateDetails according to their
         * Calender's year, month and day.
         *
         * @param obj Object expected BirthDateDetail otherwise return false
         * @return <tt>true</tt> if this object has the same value as
         * <tt>obj</tt> and false otherwise
         */
        @Override
        public boolean equals(Object obj)
        {
            if(!(obj instanceof BirthDateDetail))
                return false;

            if(this == obj)
            {
                return true;
            }

            BirthDateDetail other = (BirthDateDetail)obj;

            // both null dates
            if (this.value == null && other.getDetailValue() == null)
                return true;

            if (this.value != null && other.getDetailValue() != null)
            {
                boolean yearEquals =
                    ((Calendar)this.value).get(Calendar.YEAR) ==
                    ((Calendar)other.value).get(Calendar.YEAR);
                boolean monthEquals =
                    ((Calendar)this.value).get(Calendar.MONTH) ==
                    ((Calendar)other.value).get(Calendar.MONTH);
                boolean dayEquals =
                    ((Calendar)this.value).get(Calendar.DAY_OF_MONTH) ==
                    ((Calendar)other.value).get(Calendar.DAY_OF_MONTH);
                return yearEquals && monthEquals && dayEquals;
            }
            else
                return false;
        }
    }

    /**
     * A generic detail meant to represent the time zone associated with the
     * corresponding contact and that could be extended to represent other
     * time zone related details.
     */
    public static class TimeZoneDetail extends GenericDetail
    {
        public TimeZoneDetail(String displayDetailName, TimeZone timeZone)
        {
            super(displayDetailName, timeZone);
        }

        public TimeZone getTimeZone()
        {
            return (TimeZone)getDetailValue();
        }
    }

//------------------------------- E-Mails  ------------------------------------
    /**
     * Represents a (personal) email address.
     */
    public static class EmailAddressDetail extends StringDetail
    {
        public EmailAddressDetail(String value)
        {
            this("e-mail", value);
        }

        protected EmailAddressDetail(String detailDisplayName, String value)
        {
            super(detailDisplayName, value);
        }

        public String getEMailAddress()
        {
            return getString();
        }
    }

    /**
     * Represents a (personal) email address.
     */
    public static class WorkEmailAddressDetail extends EmailAddressDetail
    {
        public WorkEmailAddressDetail(String value)
        {
            super("Work e-mail", value);
        }
    }
//----------------------------- Interests -------------------------------------

    /**
     * Represents a personal interest or hobby.
     */
    public static class InterestDetail extends StringDetail
    {
        public InterestDetail(String value)
        {
            super("Interest", value);
        }

        public String getInterest()
        {
            return getString();
        }
    }

//---------------------------- Numbers -----------------------------------------
    /**
     * A generic detail that should be used (extended) when representing any
     * numbers.
     */
    public static class NumberDetail extends GenericDetail
    {
        public NumberDetail(String detailName, java.math.BigDecimal value)
        {
            super(detailName, value);
        }

        public java.math.BigDecimal getNumber()
        {
            return (java.math.BigDecimal)getDetailValue();
        }
    }

//---------------------------- Numbers -----------------------------------------
    /**
     * A generic detail that should be used (extended) when representing any
     * boolean values.
     */
    public static class BooleanDetail
        extends GenericDetail
    {
        public BooleanDetail(String detailName, boolean value)
        {
            super(detailName, new Boolean(value));
        }

        public boolean getBoolean()
        {
            return ((Boolean)getDetailValue()).booleanValue();
        }
    }

//---------------------------- Others ------------------------------------------
    /**
     * A job title.
     */
    public static class JobTitleDetail extends StringDetail
    {
        public JobTitleDetail(String jobTitle)
        {
            super("Job Title", jobTitle);
        }
    }

    /**
     * Represents a (personal) "about me" short description.
     */
    public static class AboutMeDetail extends StringDetail
    {
        public AboutMeDetail(String description)
        {
            super("Description", description);
        }
    }
}
