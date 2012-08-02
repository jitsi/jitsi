/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

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
}
