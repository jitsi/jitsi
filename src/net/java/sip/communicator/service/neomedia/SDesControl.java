package net.java.sip.communicator.service.neomedia;

import ch.imvs.sdes4j.srtp.SrtpCryptoAttribute;

public interface SDesControl
    extends SrtpControl
{
    public String[] getInitiatorCryptoAttributes();
    public String responderSelectAttribute(String[] peerAttributes);
    public void initiatorSelectAttribute(String[] peerAttributes);
    public SrtpCryptoAttribute getInAttribute();
    public SrtpCryptoAttribute getOutAttribute();
}
