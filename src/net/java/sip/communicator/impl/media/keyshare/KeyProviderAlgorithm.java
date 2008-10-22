/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.keyshare;

/**
 * KeyProvider interface defines the currently available provider types,
 * and a method to obtain the set type for a provider.
 * (Originally the interface contained setter and getter methods for keys
 *  and other cryptographic parameters but this were removed due to
 *  redundancy regarding the fact these are partial provided already inside
 *  directly in the SRTPTransformEngine class.
 *  It might still be a viable option.)
 *
 * @author Emanuel Onica (eonica@info.uaic.ro)
 */
public interface KeyProviderAlgorithm
{
    public enum ProviderType
    {
        DUMMY_PROVIDER,
        ZRTP_PROVIDER
    };

    /**
    * Obtains the current provider type for the class implementing the interface
    *
    * @return the provider type
    */
    public ProviderType getProviderType();

    /**
    * Gets this algorithm's priority of usage in handling the key management
    *
    * @return the priority of usage in handling the key management
    */
    public int getPriority();

    /**
    * Sets this algorithm's priority of usage in handling the key management
    *
    * @param priority the priority of usage in handling the key management
    */
    public void setPriority(int priority);

}
