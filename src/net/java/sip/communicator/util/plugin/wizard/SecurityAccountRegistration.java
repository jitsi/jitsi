/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.plugin.wizard;

import java.util.*;

/**
 * The <tt>SecurityAccountRegistration</tt> is used to determine security
 * options for different registration protocol (Jabber, SIP). Useful fot the
 * SecurityPanel.
 *
 * @author Vincent Lucas
 */
public interface SecurityAccountRegistration
{
    /**
     * If default call encryption is enabled
     *
     * @return If default call encryption is enabled
     */
    public boolean isDefaultEncryption();

    /**
     * Sets default call encryption
     *
     * @param defaultEncryption if we want to set call encryption on as default
     */
    public void setDefaultEncryption(boolean defaultEncryption);

    /**
     * Check if to include the ZRTP attribute to SIP/SDP
     *
     * @return include the ZRTP attribute to SIP/SDP
     */
    public boolean isSipZrtpAttribute();

    /**
     * Sets SIP ZRTP attribute support
     *
     * @param sipZrtpAttribute include the ZRTP attribute to SIP/SDP
     */
    public void setSipZrtpAttribute(boolean sipZrtpAttribute);

    /**
     * Tells if SDES is enabled for this SIP account.
     *
     * @return True if SDES is enabled. False, otherwise.
     */
    public boolean isSDesEnabled();

    /**
     * Enables or disables SDES for this SIP account.
     *
     * @param sdesEnabled True to enable SDES. False, otherwise.
     */
    public void setSDesEnabled(boolean sdesEnabled);

    /**
     * Returns the list of cipher suites enabled for SDES.
     *
     * @return The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
     */
    public String getSDesCipherSuites();

    /**
     * Sets the list of cipher suites enabled for SDES.
     *
     * @param The list of cipher suites enabled for SDES. Null if no cipher
     * suite is enabled.
     */
    public void setSDesCipherSuites(String cipherSuites);

    /**
     * Sets the method used for RTP/SAVP indication.
     */
    public void setSavpOption(int savpOption);

    /**
     * Returns the list of the enabled or disabled encryption protocols in the
     * priority order.
     *
     * @param enabled If true this function will return the enabled encryption
     * protocol list. Otherwise, it will return the disabled list.
     *
     * @return the list of the enabled or disabled encryption protocols in the
     * priority order.
     */
    public List<String> getEncryptionProtocols(boolean enabled);

    /**
     * Sets the list of the enabled and disabled encryption protocols in the
     * priority order.
     *
     * @param enabledEncrpytionProtools The list of the enabled encryption
     * protocols in the priority order.
     * @param disabledEncrpytionProtools The list of the disabled encryption
     * protocols in the priority order.
     */
    public void setEncryptionProtocols(
            List<String> enabledEncryptionProtocols,
            List<String> disabledEncryptionProtocols);
}
