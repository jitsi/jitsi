/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The interface contains the names of a number of details that may be stored
 * on a server or a p2p network depending on the protocol. The interface and its
 * static fields are meant for usage with the getXxx() methods of operation sets
 * OperationSetServerStoredAccountInfo and OperationSetServerStoredUserInfo.
 * <p>
 * The names enumerated in this interface do not in anyway represent an
 * exhaustive set covering all possible details (the number of which is next
 * to infinite).
 * <p>
 * Nevertheless, if a detail, supported by an Operation Set is enumerated here,
 * the implementors are strongly encouraged to use the name defined below.
 * <p>
 * @author Emil Ivov
 */
public interface ServerStoredDetails
{
    /**
     * A street name and number associated with a home address.
     */
    public static final String HOME_ADDRESS         = "HomeAddress";

    /**
     * A City name associated with a home address.
     */
    public static final String HOME_CITY            = "HomeCity";

    /**
     * The name of a state/province/region associated with a home address.
     */
    public static final String HOME_STATE_OR_PROVINCE = "HomeStateOrProvince";

    /**
     * The name of a country associated with a home address.
     */
    public static final String HOME_COUNTRY         = "HomeCountry";

    /**
     * The name of a zip or postal code associated with a home address.
     */
    public static final String HOME_ZIP_OR_POSTAL_CODE = "HomeZipOrPostalCode";

    /**
     * A personal web page.
     */
    public static final String WEB_PAGE             = "WebPage";


    /**
     * A street name and number associated with a work address.
     */
    public static final String WORK_ADDRESS         = "WorkAddress";

    /**
     * A City name associated with a work address.
     */
    public static final String WORK_CITY            = "WorkCity";

    /**
     * The name of a state/province/region associated with a work address.
     */
    public static final String WORK_STATE_OR_PROVINCE="WorkStateOrProvince";

    /**
     * The name of a country associated with a work address.
     */
    public static final String WORK_COUNTRY         = "WorkCountry";

    /**
     * The name of a zip or postal code associated with a work address.
     */
    public static final String WORK_ZIP_OR_POSTAL_CODE  = "WorkZipOrPostalCode";

    /**
     * The address of a business web page.
     */
    public static final String WORK_WEB_PAGE        = "WorkWebPage";

    /**
     * The name of the organization (company, ngo, university, hospital or other)
     * employing the information author..
     */
    public static final String ORGANIZATION_NAME         = "OrganizationName";


    /**
     * The number of a private mobile.
     */
    public static final String PRIVATE_MOBILE       = "PrivateMobile";

    /**
     * The number of a business mobile.
     */
    public static final String WORK_MOBILE          = "WorkMobile";

    /**
     * A Fax number.
     */
    public static final String FAX                  = "Fax";

    /**
     * A pager number.
     */
    public static final String PAGER                = "Pager";


    /**
     * A first, given name.
     */
    public static final String FIRST_NAME           = "FirstName";

    /**
     * A Middle (father's) name.
     */
    public static final String MIDDLE_NAME          = "MiddleName";

    /**
     * A family name (surname).
     */
    public static final String FAMILY_NAME          = "FamilyName";

    /**
     * The name that should be displayed to identify the information author.
     */
    public static final String DISPLAY_NAME         = "DisplayName";

    /**
     * An informal name (nickname) used for referring to the subject.
     */
    public static final String NICKNAME             = "Nickname";
}
