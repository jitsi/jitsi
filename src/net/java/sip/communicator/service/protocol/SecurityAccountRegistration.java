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

import net.java.sip.communicator.util.*;
import org.jitsi.service.neomedia.*;

import java.io.*;
import java.util.*;

/**
 * The <tt>SecurityAccountRegistration</tt> is used to determine security
 * options for different registration protocol (Jabber, SIP). Useful to the
 * SecurityPanel.
 *
 * @author Vincent Lucas
 * @author Pawel Domas
 * @author Lyubomir Marinov
 */
public abstract class SecurityAccountRegistration
    implements Serializable
{
    /**
     * The encryption protocols managed by this SecurityPanel.
     */
    public static final List<String> ENCRYPTION_PROTOCOLS
        = Collections.unmodifiableList(
                Arrays.asList(
                        SrtpControlType.ZRTP.toString(),
                        SrtpControlType.SDES.toString(),
                        SrtpControlType.DTLS_SRTP.toString()));

    /**
     * Enables support to encrypt calls.
     */
    private boolean defaultEncryption = true;

    /**
     * Enables ZRTP encryption.
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
        encryptionProtocols = new HashMap<String, Integer>(1);
        encryptionProtocols.put("ZRTP", 0);
        encryptionProtocolStatus = new HashMap<String, Boolean>(1);
        encryptionProtocolStatus.put("ZRTP", true);
        sdesCipherSuites
            = UtilActivator.getResources().getSettingsString(
                    SDesControl.SDES_CIPHER_SUITES);
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
        for (Map.Entry<String, Integer> e : getEncryptionProtocols().entrySet())
        {
            properties.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL
                        + "."
                        + e.getKey(),
                    e.getValue().toString());
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
        for (Map.Entry<String,Boolean> e
                : getEncryptionProtocolStatus().entrySet())
        {
            properties.put(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                        + "."
                        + e.getKey(),
                    e.getValue().toString());
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

        Map<String,Integer> srcEncryptionProtocols
            = accountID.getIntegerPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL,
                    true);
        Map<String,Boolean> srcEncryptionProtocolStatus
            = accountID.getBooleanPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS,
                    true,
                    false);
        // Load stored values.
        int prefixeLength
            = ProtocolProviderFactory.ENCRYPTION_PROTOCOL.length() + 1;

        for (Map.Entry<String,Integer> e : srcEncryptionProtocols.entrySet())
        {
            String name = e.getKey().substring(prefixeLength);
            if (isExistingEncryptionProtocol(name))
            {
                // Copy the priority
                encryptionProtocols.put(name, e.getValue());
                // Extract the status
                boolean enabled
                    = srcEncryptionProtocolStatus.get(
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
        int nbEncryptionProtocols = ENCRYPTION_PROTOCOLS.size();
        String[] encryptions = new String[nbEncryptionProtocols];
        boolean[] selectedEncryptions = new boolean[nbEncryptionProtocols];

        // Load stored values.
        for (Map.Entry<String,Integer> e : encryptionProtocols.entrySet())
        {
            int index = e.getValue();

            // If the property is set.
            if (index != -1)
            {
                String name = e.getKey();

                if (isExistingEncryptionProtocol(name))
                {
                    encryptions[index] = name;
                    selectedEncryptions[index]
                        = encryptionProtocolStatus.get(name);
                }
            }
        }

        // Load default values.
        int j = 0;
        for (String encryptionProtocol : ENCRYPTION_PROTOCOLS)
        {
            // Specify a default value only if there is no specific value set.
            if(!encryptionProtocols.containsKey(encryptionProtocol))
            {
                boolean set = false;
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

        return new Object[] { encryptions, selectedEncryptions};
    }

    /**
     * Checks if a specific <tt>protocol</tt> is on the list of supported
     * (encryption) protocols.
     *
     * @param protocol the protocol name
     * @return <tt>true</tt> if <tt>protocol</tt> is supported; <tt>false</tt>,
     * otherwise
     */
    private static boolean isExistingEncryptionProtocol(String protocol)
    {
        return ENCRYPTION_PROTOCOLS.contains(protocol);
    }
}
