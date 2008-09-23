/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.keyshare;

/**
 * ZRTPProvider class implements KeyProvider interface.
 * Used to activate the ZRTPConnector creation. 
 * Also could be used to provide additional info.
 * (Originally the interface contained setter and getter methods for keys 
 *  and other cryptographic parameters but this were removed due to
 *  redundancy regarding the fact these are partial provided already inside
 *  directly in the SRTPTransformEngine class. 
 *  The ZRTPConnector originally implemented the interface to have direct 
 *  access to these as a ZRTPProvider.
 *  It might still be a viable option.)
 *    
 * @author Emanuel Onica (eonica@info.uaic.ro)
 *
 */
public class ZRTPKeyProvider implements KeyProviderAlgorithm {

	/**
	 * The constant provider type of this class 
	 */
	private static final KeyProviderAlgorithm.ProviderType providerType 
	    = KeyProviderAlgorithm.ProviderType.ZRTP_PROVIDER;
	
	private int priority;
	
	public ZRTPKeyProvider(int priority)
	{
	    this.priority = priority;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.java.sip.communicator.impl.media.keyshare.KeyProvider#getProviderType()
	 */
	public ProviderType getProviderType() 
	{
		return providerType;
	}
	
	public int getPriority()
    {
        return priority;
    }
    
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

}
