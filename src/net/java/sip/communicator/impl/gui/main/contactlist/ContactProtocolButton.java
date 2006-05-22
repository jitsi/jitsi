package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class ContactProtocolButton extends SIPCommButton {

    private Contact protocolContact;
    
    public ContactProtocolButton(Image bgImage, Image rolloverImage){
        super(bgImage, rolloverImage);
    }

    public Contact getProtocolContact() {
        return protocolContact;
    }

    public void setProtocolContact(Contact protocolContact) {
        this.protocolContact = protocolContact;
    }
}
