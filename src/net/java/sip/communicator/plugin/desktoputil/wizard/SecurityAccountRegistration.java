/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil.wizard;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * The <tt>SecurityAccountRegistration</tt> is used to determine security
 * options for different registration protocol (Jabber, SIP). Useful fot the
 * SecurityPanel.
 *
 * @author Vincent Lucas
 */
public abstract class SecurityAccountRegistration
{
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
        this.encryptionProtocols.put(
                ProtocolProviderFactory.ENCRYPTION_PROTOCOL + ".ZRTP",
                0);
        this.encryptionProtocolStatus = new HashMap<String, Boolean>(1);
        this.encryptionProtocolStatus.put(
                ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS + ".ZRTP",
                true);
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
     * @param The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
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
     * @param encryptionProtools The map between the encryption protocols and
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
     * @param encryptionProtools The map between the encryption protocols and
     * their status.
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
    public void addEncryptionProtocolsToProperties(
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
    public void addEncryptionProtocolStatusToProperties(
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
}
