/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The AccountID is an account identifier that, uniquely represents a specific
 * user account over a specific protocol. The class needs to be extended by
 * every protocol implementation because of its protected
 * constructor. The reason why this constructor is protected is mostly avoiding
 * confusion and letting people (using the protocol provider service) believe
 * that they are the ones who are supposed to instantiate the accountid class.
 * <p>
 * Every instance of the <tt>ProtocolProviderService</tt>, created through the
 * ProtocolProviderFactory is assigned an AccountID instance, that uniquely
 * represents it and whose string representation (obtained through the
 * getAccountUID() method) can be used for identification of persistently stored
 * account details.
 * <p>
 * Account id's are guaranteed to be different for different accounts and in the
 * same time are bound to be equal for multiple installations of the same
 * account.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public abstract class AccountID
{
    private static final Logger logger = Logger.getLogger(AccountID.class);

    /**
     * Allows a specific set of account properties to override a given default
     * protocol name (e.g. account registration wizards which want to present a
     * well-known protocol name associated with the account that is different
     * from the name of the effective protocol).
     * <p>
     * Note: The logic of the SIP protocol implementation at the time of this
     * writing modifies <tt>accountProperties</tt> to contain the default
     * protocol name if an override hasn't been defined. Since the desire is to
     * enable all account registration wizards to override the protocol name,
     * the current implementation places the specified
     * <tt>defaultProtocolName</tt> in a similar fashion.
     * </p>
     *
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param defaultProtocolName the protocol name to be used in case
     * <tt>accountProperties</tt> doesn't provide an overriding value
     * @return
     */
    private static final String getOverriddenProtocolName(
            Map<String, String> accountProperties, String defaultProtocolName)
    {
        String key = ProtocolProviderFactory.PROTOCOL;
        String protocolName = accountProperties.get(key);
        if ((protocolName == null) && (defaultProtocolName != null))
        {
            protocolName = defaultProtocolName;
            accountProperties.put(key, protocolName);
        }
        return protocolName;
    }

    /**
     * Contains all implementation specific properties that define the account.
     * The exact names of the keys are protocol (and sometimes implementation)
     * specific.
     * Currently, only String property keys and values will get properly stored.
     * If you need something else, please consider converting it through custom
     * accessors (get/set) in your implementation.
     */
    protected Map<String, String> accountProperties = null;

    /**
     * A String uniquely identifying the user for this particular account.
     */
    private final String userID;

    /**
     * A String uniquely identifying this account, that can also be used for
     * storing and unambiguously retrieving details concerning it.
     */
    private final String accountUID;

    /**
     * The name of the service that defines the context for this account.
     */
    private final String serviceName;

    /**
     * Creates an account id for the specified provider userid and
     * accountProperties.
     * @param userID a String that uniquely identifies the user.
     * @param accountProperties a Map containing any other protocol and
     * implementation specific account initialization properties
     * @param protocolName the name of the protocol implemented by the provider
     * that this id is meant for.
     * @param serviceName the name of the service (e.g. iptel.org, jabber.org,
     * icq.com) that this account is registered with.
     */
    protected AccountID( String userID,
                         Map<String, String> accountProperties,
                         String protocolName,
                         String serviceName)
    {

        /*
         * Allow account registration wizards to override the default protocol
         * name through accountProperties for the purposes of presenting a
         * well-known protocol name associated with the account that is
         * different from the name of the effective protocol.
         */
        protocolName
            = getOverriddenProtocolName(accountProperties, protocolName);

        this.userID = userID;
        this.accountProperties
            = new Hashtable<String, String>(accountProperties);
        this.serviceName = serviceName;

        //create a unique identifier string
        this.accountUID
            = protocolName
                + ":"
                + userID
                + "@"
                + ((serviceName == null) ? "" : serviceName);
    }

    /**
     * Returns the user id associated with this account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Returns a name that can be displayed to the user when referring to this
     * account.
     *
     * @return A String identifying the user inside this particular service.
     */
    public String getDisplayName()
    {
        String returnValue = getUserID();
        String protocolName =
            getAccountPropertyString(ProtocolProviderFactory.PROTOCOL);

        if (protocolName != null && protocolName.trim().length() > 0)
            returnValue += " (" + protocolName + ")";

        return returnValue;
    }

    /**
     * Returns a String uniquely identifying this account, guaranteed to remain
     * the same across multiple installations of the same account and to always
     * be unique for differing accounts.
     * @return String
     */
    public String getAccountUniqueID()
    {
        return accountUID;
    }

    /**
     * Returns a Map containing protocol and implementation account
     * initialization properties.
     * @return a Map containing protocol and implementation account
     * initialization properties.
     */
    public Map<String, String> getAccountProperties()
    {
        return new Hashtable<String, String>(accountProperties);
    }

    public Object getAccountProperty(Object key)
    {
        return accountProperties.get(key);
    }

    public boolean getAccountPropertyBoolean(Object key, boolean defaultValue)
    {
        String value = getAccountPropertyString(key);
        return (value == null) ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * Gets the value of a specific property as a signed decimal integer. If the
     * specified property key is associated with a value in this
     * <tt>AccountID</tt>, the string representation of the value is parsed into
     * a signed decimal integer according to the rules of
     * {@link Integer#parseInt(String)} . If parsing the value as a signed
     * decimal integer fails or there is no value associated with the specified
     * property key, <tt>defaultValue</tt> is returned.
     *
     * @param key the key of the property to get the value of as a
     * signed decimal integer
     * @param defaultValue the value to be returned if parsing the value of the
     * specified property key as a signed decimal integer fails or there is no
     * value associated with the specified property key in this
     * <tt>AccountID</tt>
     * @return the value of the property with the specified key in this
     * <tt>AccountID</tt> as a signed decimal integer; <tt>defaultValue</tt> if
     * parsing the value of the specified property key fails or no value is
     * associated in this <tt>AccountID</tt> with the specified property name
     */
    public int getAccountPropertyInt(Object key, int defaultValue)
    {
        String stringValue = getAccountPropertyString(key);
        int intValue = defaultValue;

        if ((stringValue != null) && (stringValue.length() > 0))
        {
            try
            {
                intValue = Integer.parseInt(stringValue);
            }
            catch (NumberFormatException ex)
            {
                logger.error("Failed to parse account property " + key
                    + " value " + stringValue + " as an integer", ex);
            }
        }
        return intValue;
    }

    public String getAccountPropertyString(Object key)
    {
        Object value = getAccountProperty(key);
        return (value == null) ? null : value.toString();
    }

    /**
     * Adds a property to the map of properties for this account identifier.
     *
     * @param key the key of the property
     * @param value the property value
     */
    public void putAccountProperty(String key, String value)
    {
        accountProperties.put(key, value);
    }

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     * @return  a hash code value for this object.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable
     */
    public int hashCode()
    {
        return (accountUID == null)? 0 : accountUID.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this account id.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     * @see     #hashCode()
     * @see     java.util.Hashtable
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;

        return (obj != null)
            && getClass().isInstance(obj)
            && userID.equals(((AccountID)obj).userID);
    }

    /**
     * Returns a string representation of this account id (same as calling
     * getAccountUniqueID()).
     *
     * @return  a string representation of this account id.
     */
    public String toString()
    {
        return getAccountUniqueID();
    }

    /**
     * Returns the name of the service that defines the context for this
     * account. Often this name would be an sqdn or even an ipaddress but this
     * would not always be the case (e.g. p2p providers may return a name that
     * does not directly correspond to an IP address or host name).
     * <p>
     * @return the name of the service that defines the context for this
     * account.
     */
    public String getService()
    {
        return this.serviceName;
    }

    /**
     * Returns a string that could be directly used (or easily converted to) an
     * address that other users of the protocol can use to communicate with us.
     * By default this string is set to userid@servicename. Protocol
     * implementors should override it if they'd need it to respect a different
     * syntax.
     *
     * @return a String in the form of userid@service that other protocol users
     * should be able to parse into a meaningful address and use it to
     * communicate with us.
     */
    public String getAccountAddress()
    {
        String userID = getUserID();
        return (userID.indexOf('@') > 0) ? userID
            : (userID + "@" + getService());
    }

    /**
     * Set the account properties.
     *
     * @param accountProperties the properties of the account
     */
    public void setAccountProperties(Map<String, String> accountProperties)
    {
        this.accountProperties = accountProperties;
    }
}
