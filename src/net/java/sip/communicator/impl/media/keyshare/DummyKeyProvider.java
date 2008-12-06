/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.keyshare;

/**
 * DummyProvider class implements KeyProvider interface.
 * Used only for testing - activates the hardcoded keys behaviour for SRTP traffic.
 * 
 * @author Emanuel Onica (eonica@info.uaic.ro)
 *
 */
public class DummyKeyProvider 
    implements KeyProviderAlgorithm 
{
	
	/**
	 * The constant provider type of this class 
	 */
	private static final KeyProviderAlgorithm.ProviderType providerType = 
		KeyProviderAlgorithm.ProviderType.DUMMY_PROVIDER;	
	
	private int priority;
	
	public DummyKeyProvider(int priority)
	{
	    this.priority = priority;
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.java.sip.communicator.impl.media.keyshare.KeyProvider#getProviderType()
	 */
	public KeyProviderAlgorithm.ProviderType getProviderType()
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
