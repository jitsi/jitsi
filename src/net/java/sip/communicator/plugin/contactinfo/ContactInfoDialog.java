/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A GUI plug-in for SIP Communicator that will allow cross protocol contact
 * information viewing and editing.
 * 
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoDialog
    extends SIPCommFrame
{
    /**
     * The right side of this frame that contains protocol specific contact
     * details.
     */
    protected ContactInfoDetailsPanel detailsPanel
        = new ContactInfoDetailsPanel();

    /**
     * The left side of this frame that contains a list of all sub-contacts
     * associated with the selected contact.
     */
    protected ContactInfoContactPanel contactPanel;

    /**
     * The contact that was right clicked on. The sub-contacts of contactItem
     * will be the ones selectable in contactPanel.
     */
    protected MetaContact metaContact;

    /**
     * Accepts a MetaContact and constructs a frame with ContactInfoSearchPanel
     * on the left and an information interface, ContactInfoDetailsPanel,
     * on the right.
     * @param metaContact the sub-contacts of this MetaContact that was right
     * clicked on will be the ones selectable in contactPanel.
     */
    public ContactInfoDialog(MetaContact metaContact)
    {
        this.metaContact = metaContact;

        this.setTitle(Resources.getString("plugin.contactinfo.TITLE")
                    + ": "
                    + metaContact.getDisplayName());

        Iterator<Contact> subContacts = metaContact.getContacts();

        this.contactPanel
            = new ContactInfoContactPanel(subContacts, this);

        Container contentPane = getContentPane();
        contentPane.add(contactPanel, BorderLayout.WEST);
        contentPane.add(detailsPanel, BorderLayout.CENTER);

        this.pack();
    }

    /**
     * Loads the details of the given contact.
     * 
     * @param contact the <tt>Contact</tt>, which details we load
     */
    public void loadContactDetails(Contact contact)
    {
        this.detailsPanel.loadContactDetails(contact);
    }

    protected void close(boolean isEscaped)
    {
    }
}
