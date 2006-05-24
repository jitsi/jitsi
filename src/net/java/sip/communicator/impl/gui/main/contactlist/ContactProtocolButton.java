package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Image;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.service.protocol.Contact;

/**
 * The ContactProtocolButton is a button behind a "meta contact" in the
 * contactlist, which corresponds to a specific protocol contact. This
 * button allows opening a chat with a given "meta contact" by specifing
 * the exact protocol contact to use for the chat.
 *  
 * @author Yana Stamcheva
 */
public class ContactProtocolButton extends SIPCommButton {

    private Contact protocolContact;

    /**
     * Creates an instance of ContactProtocolButton.
     * @param bgImage The background image of the button.
     * @param rolloverImage The rollover image of the button.
     */
    public ContactProtocolButton(Image bgImage, Image rolloverImage) {
        super(bgImage, rolloverImage);
    }

    /**
     * Returns the specific protocol contact corresponding to this button.
     * @return The specific protocol contact corresponding to this button.
     */
    public Contact getProtocolContact() {
        return protocolContact;
    }

    /**
     * Sets the specific protocol contact corresponding to this button.
     * @param protocolContact The specific protocol contact.
     */
    public void setProtocolContact(Contact protocolContact) {
        this.protocolContact = protocolContact;
    }
}
