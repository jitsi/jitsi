/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class Account {
    
    private String uin;
    
    private ProtocolProviderService protocolProvider;
    
    private String protocolName;
    
    public Account( String uin, 
                    ProtocolProviderService protocolProvider){
        
        this.uin = uin;
        this.protocolProvider = protocolProvider;
        this.protocolName = protocolProvider.getProtocolName();
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolProvider(ProtocolProviderService protocolProvider) {
        this.protocolProvider = protocolProvider;
    }

    public ProtocolProviderService getProtocolProvider() {
        return this.protocolProvider;
    }
    
    public String getUin() {
        return uin;
    }

    public void setUin(String uin) {
        this.uin = uin;
    }
}
