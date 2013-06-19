/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.util.*;
import org.jitsi.service.neomedia.*;

import java.io.*;
import java.util.*;

/**
 * The <tt>SecurityAccountRegistration</tt> is used to determine security
 * options for different registration protocol (Jabber, SIP). Useful fot the
 * SecurityPanel.
 *
 * @author Vincent Lucas
 * @author Pawel Domas
 */
public abstract class SecurityAccountRegistration
    implements Serializable
{
    /**
     * The encryption protocols managed by this SecurityPanel.
     */
    public static final String[] ENCRYPTION_PROTOCOLS = {"ZRTP", "SDES"};

    /**
     * Enables support to encrypt calls.
     */
    private boolean defaultEncryption = true;

    /**
     * Enqbles ZRTP encryption.
     */
    private boolean sipZrtpAttribute = true;

    /**
     * Tells if SDES is enabled for this account.
     */
    private boolean sdesEnabled = false;

    /**
     * The list of cipher suites enabled for SDES.
     */
    private String sdesCipherSuites = null;

    /**
     * The map between encryption protocols and their priority order.
     */
    private Map<String, Integer> encryptionProtocols;

    /**
     * The map between encryption protocols and their status (enabled or
     * disabled).
     */
    private Map<String, Boolean> encryptionProtocolStatus;

    /**
     * Initializes the security account registration properties with the default
     * values.
     */
    public SecurityAccountRegistration()
    {
        // Sets the default values.
        this.encryptionProtocols = new HashMap<String, Integer>(1);
        this.encryptionProtocols.put("ZRTP", 0);
        this.encryptionProtocolStatus = new HashMap<String, Boolean>(1);
        this.encryptionProtocolStatus.put("ZRTP", true);
        sdesCipherSuites
                = UtilActivator.getResources()
                        .getSettingsString(SDesControl.SDES_CIPHER_SUITES);
    }

    /**
     * If default call encryption is enabled
     *
     * @return If default call encryption is enabled
     */
    public boolean isDefaultEncryption()
    {
        return defaultEncryption;
    }

    /**
     * Sets default call encryption
     *
     * @param defaultEncryption if we want to set call encryption on as default
     */
    public void setDefaultEncryption(boolean defaultEncryption)
    {
        this.defaultEncryption = defaultEncryption;
    }

    /**
     * Check if to include the ZRTP attribute to SIP/SDP or to Jabber/IQ
     *
     * @return include the ZRTP attribute to SIP/SDP or to Jabber/IQ
     */
    public boolean isSipZrtpAttribute()
    {
        return sipZrtpAttribute;
    }

    /**
     * Sets ZRTP attribute support
     *
     * @param sipZrtpAttribute include the ZRTP attribute to SIP/SDP or to
     * Jabber/IQ
     */
    public void setSipZrtpAttribute(boolean sipZrtpAttribute)
    {
        this.sipZrtpAttribute = sipZrtpAttribute;
    }

    /**
     * Tells if SDES is enabled for this account.
     *
     * @return True if SDES is enabled. False, otherwise.
     */
    public boolean isSDesEnabled()
    {
        return sdesEnabled;
    }

    /**
     * Enables or disables SDES for this account.
     *
     * @param sdesEnabled True to enable SDES. False, otherwise.
     */
    public void setSDesEnabled(boolean sdesEnabled)
    {
        this.sdesEnabled = sdesEnabled;
    }

    /**
     * Returns the list of cipher suites enabled for SDES.
     *
     * @return The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
     */
    public String getSDesCipherSuites()
    {
        return sdesCipherSuites;
    }

    /**
     * Sets the list of cipher suites enabled for SDES.
     *
     * @param cipherSuites The list of cipher suites enabled for SDES.
     *                     Null if no cipher suite is enabled.
     */
    public void setSDesCipherSuites(String cipherSuites)
    {
        this.sdesCipherSuites = cipherSuites;
    }

    /**
     * Sets the method used for RTP/SAVP indication.
     */
    public abstract void setSavpOption(int savpOption);

    /**
     * Returns the method used for RTP/SAVP indication.
     * @return the method used for RTP/SAVP indication.
     */
    public abstract int getSavpOption();

    /**
     * Returns the map between the encryption protocols and their priority
     * order.
     *
     * @return The map between the encryption protocols and their priority
     * order.
     */
    public Map<String, Integer> getEncryptionProtocols()
    {
        return encryptionProtocols;
    }

    /**
     * Sets the map between the encryption protocols and their priority order.
     *
     * @param encryptionProtocols The map between the encryption protocols and
     * their priority order.
     */
    public void setEncryptionProtocols(
            Map<String, Integer> encryptionProtocols)
    {
        this.encryptionProtocols = encryptionProtocols;
    }

    /**
     * Returns the map between the encryption protocols and their status.
     *
     * @return The map between the encryption protocols and their status.
     */
    public Map<String, Boolean> getEncryptionProtocolStatus()
    {
        return encryptionProtocolStatus;
    }

    /**
     * Sets the map between the encryption protocols and their status.
     *
     * @param encryptionProtocolStatus The map between the encryption protocols
     *                                 and their status.
     */
    public void setEncryptionProtocolStatus(
            Map<String, Boolean> encryptionProtocolStatus)
    {
        this.encryptionProtocolStatus = encryptionProtocolStatus;
    }

    /**
     * Adds the ordered encryption protocol names to the property list given in
     * parameter.
     *
     * @param properties The property list to fill in.
     */
    private void addEncryptionProtocolsToProperties(
            Map<String, String> properties)
    {
        Map<String, Integer> encryptionProtocols
            = this.getEncryptionProtocols();
        Iterator<String> encryptionProtocolIterator
            = encryptionProtocols.keySet().iterator();
        String encryptionProtocol;
        while(encryptionProtocolIterator.hasNext())
        {
            encryptionProtocol = encryptionProtocolIterator.next();
            properties.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL
                        + "."
                        + encryptionProtocol,
                    encryptionProtocols.get(encryptionProtocol).toString());
        }
    }

    /**
     * Adds the encryption protocol status to the property list given in
     * parameter.
     *
     * @param properties The property list to fill in.
     */
    private void addEncryptionProtocolStatusToProperties(
            Map<String, String> properties)
    {
        Map<String, Boolean> encryptionProtocolStatus
            = this.getEncryptionProtocolStatus();
        Iterator<String> encryptionProtocolStatusIterator
            = encryptionProtocolStatus.keySet().iterator();
        String encryptionProtocol;
        while(encryptionProtocolStatusIterator.hasNext())
        {
            encryptionProtocol = encryptionProtocolStatusIterator.next();
            properties.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + "."
                        + encryptionProtocol,
                    encryptionProtocolStatus.get(encryptionProtocol)
                        .toString());
        }
    }

    /**
     * Stores security properties held by this registration object into given
     * properties map.
     * @param propertiesMap the map that will be used for storing security
     * properties held by this object.
     */
    public void storeProperties(Map<String, String> propertiesMap)
    {
        propertiesMap.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                          Boolean.toString(isDefaultEncryption()));

        // Sets the ordered list of encryption protocols.
        addEncryptionProtocolsToProperties(propertiesMap);

        // Sets the list of encryption protocol status.
        addEncryptionProtocolStatusToProperties(propertiesMap);

        propertiesMap.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                          Boolean.toString(isSipZrtpAttribute()));

        propertiesMap.put(ProtocolProviderFactory.SAVP_OPTION,
                          Integer.toString(getSavpOption()));

        propertiesMap.put(ProtocolProviderFactory.SDES_CIPHER_SUITES,
                          getSDesCipherSuites());
    }

    /**
     * Loads security properties from the account with the given identifier.
     * @param accountID the account identifier.
     */
    public void loadAccount(AccountID accountID)
    {
        setDefaultEncryption(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true));

        encryptionProtocols = new HashMap<String, Integer>();
        encryptionProtocolStatus = new HashMap<String, Boolean>();

        Map<String, Integer> srcEncryptionProtocols
                = accountID.getIntegerPropertiesByPrefix(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL, true);
        Map<String, Boolean> srcEncryptionProtocolStatus
                = accountID.getBooleanPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS,
                            true,
                            false);
        // Load stored values.
        int prefixeLength
                = ProtocolProviderFactory.ENCRYPTION_PROTOCOL.length() + 1;
        String name;
        boolean enabled;
        for(String protocolPropertyName : srcEncryptionProtocols.keySet())
        {
            name = protocolPropertyName.substring(prefixeLength);
            if (isExistingEncryptionProtocol(name))
            {
                // Copies the priority
                encryptionProtocols.put(
                        name,
                        srcEncryptionProtocols.get(protocolPropertyName));
                // Extracts the status
                enabled = srcEncryptionProtocolStatus.get(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                                + "."
                                + name);
                encryptionProtocolStatus.put(name, enabled);
            }
        }

        setSipZrtpAttribute(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                        true));

        setSavpOption(
                accountID.getAccountPropertyInt(
                        ProtocolProviderFactory.SAVP_OPTION,
                        ProtocolProviderFactory.SAVP_OFF));

        setSDesCipherSuites(
                accountID.getAccountPropertyString(
                        ProtocolProviderFactory.SDES_CIPHER_SUITES));
    }

    /**
     * Loads the list of enabled and disabled encryption protocols with their
     * priority into array of <tt>String</tt> and array of <tt>Boolean</tt>.
     * The protocols are positioned in the array by the priority and the
     * <tt>Boolean</tt> array holds the enabled flag on the corresponding index.
     *
     * @param encryptionProtocols The map of encryption protocols with their
     * priority available for this account.
     * @param encryptionProtocolStatus The map of encryption protocol statuses.
     * @return <tt>Object[]</tt> array holding:<br/>
     * - at [0] <tt>String[]</tt> the list of extracted protocol names<br/>
     * - at [1] <tt>boolean[]</tt> the list of of protocol status flags
     */
    public static Object[] loadEncryptionProtocols(
            Map<String, Integer> encryptionProtocols,
            Map<String, Boolean> encryptionProtocolStatus)
    {
        int nbEncryptionProtocols = ENCRYPTION_PROTOCOLS.length;
        String[] encryptions = new String[nbEncryptionProtocols];
        boolean[] selectedEncryptions = new boolean[nbEncryptionProtocols];

        // Load stored values.
        String name;
        int index;
        Iterator<String> encryptionProtocolNames
                = encryptionProtocols.keySet().iterator();
        while(encryptionProtocolNames.hasNext())
        {
            name = encryptionProtocolNames.next();
            index = encryptionProtocols.get(name);
            // If the property is set.
            if(index != -1)
            {
                if (isExistingEncryptionProtocol(name))
                {
                    encryptions[index] = name;
                    selectedEncryptions[index]
                            = encryptionProtocolStatus.get(name);
                }
            }
        }

        // Load default values.
        String encryptionProtocol;
        boolean set;
        int j = 0;
        for(int i = 0; i < ENCRYPTION_PROTOCOLS.length; ++i)
        {
            encryptionProtocol = ENCRYPTION_PROTOCOLS[i];
            // Specify a default value only if there is no specific value set.
            if(!encryptionProtocols.containsKey(encryptionProtocol))
            {
                set = false;
                // Search for the first empty element.
                while(j < encryptions.length && !set)
                {
                    if(encryptions[j] == null)
                    {
                        encryptions[j] = encryptionProtocol;
                        // By default only ZRTP is set to true.
                        selectedEncryptions[j]
                                = encryptionProtocol.equals("ZRTP");
                        set = true;
                    }
                    ++j;
                }

            }
        }

        return new Object[]{ encryptions, selectedEncryptions};
    }

    /**
     * Checks if given <tt>protocol</tt> is on supported protocols list.
     * @param protocol the protocol name
     * @return <tt>true</tt> if encryption protocol with given protocol name is
     * supported.
     */
    private static boolean isExistingEncryptionProtocol(String protocol)
    {
        for (String key : ENCRYPTION_PROTOCOLS)
        {
            if (key.equals(protocol))
            {
                return true;
            }
        }

        return false;
    }

}
