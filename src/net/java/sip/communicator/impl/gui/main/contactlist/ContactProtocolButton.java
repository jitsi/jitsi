package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class ContactProtocolButton extends SIPCommButton {

    private ProtocolProviderService protocolProvider;
    
    public ContactProtocolButton(Image bgImage, Image rolloverImage){
        super(bgImage, rolloverImage);
    }

    public ProtocolProviderService getProtocolProvider() {
        return protocolProvider;
    }

    public void setProtocolProvider(ProtocolProviderService protocolProvider) {
        this.protocolProvider = protocolProvider;
    }
}
