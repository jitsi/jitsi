package net.java.sip.communicator.service.neomedia;

import ch.imvs.sdes4j.srtp.SrtpCryptoAttribute;

/**
 * SDES based SRTP MediaStream encryption control.
 * 
 * @author Ingo Bauersachs
 */
public interface SDesControl
    extends SrtpControl
{
    /**
     * Name of the config setting that supplies the default enabled cipher
     * suites. Cipher suites are comma-separated.
     */
    public static final String SDES_CIPHER_SUITES =
        "net.java.sip.communicator.service.neomedia.SDES_CIPHER_SUITES";

    /**
     * Set the enabled SDES ciphers.
     * 
     * @param ciphers The list of enabled ciphers.
     */
    public void setEnabledCiphers(Iterable<String> ciphers);

    /**
     * Gets all supported cipher suites.
     * 
     * @return all supported cipher suites.
     */
    public Iterable<String> getSupportedCryptoSuites();

    /**
     * Gets the encoded SDES crypto-attributes for all enabled ciphers when the
     * control is used as the initiator.
     * 
     * @return the encoded SDES crypto-attributes for all enabled ciphers.
     */
    public String[] getInitiatorCryptoAttributes();

    /**
     * Chooses a supported crypto attribute from the peer's list of supplied
     * attributes and creates the local crypto attribute. Used when the control
     * is running in the role as responder.
     * 
     * @param peerAttributes The peer's crypto attribute offering.
     * @return The local crypto attribute for the answer of the offer or null if
     *         no matching cipher suite could be found.
     */
    public String responderSelectAttribute(Iterable<String> peerAttributes);

    /**
     * Select the local crypto attribute from the initial offering (@see
     * {@link #getInitiatorCryptoAttributes()}) based on the peer's first
     * matching cipher suite.
     * 
     * @param peerAttributes The peer's crypto offers.
     * @return True when a matching cipher suite was found, false otherwise.
     */
    public boolean initiatorSelectAttribute(Iterable<String> peerAttributes);

    /**
     * Gets the crypto attribute of the incoming MediaStream.
     * @return the crypto attribute of the incoming MediaStream.
     */
    public SrtpCryptoAttribute getInAttribute();

    /**
     * Gets the crypto attribute of the outgoing MediaStream.
     * @return the crypto attribute of the outgoing MediaStream.
     */
    public SrtpCryptoAttribute getOutAttribute();
}
