/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AddContactDialog</tt> is the dialog containing the form for adding
 * a contact. It is different from the "Add Contact" wizard opened from the
 * quick menu button. The <tt>AddContactDialog</tt> is used when a new contact
 * is added to an already existing meta contact or when adding a meta contact
 * to an already existing meta contact group.
 * 
 * @author Yana Stamcheva
 */
public class AddContactDialog extends JDialog
    implements ActionListener {

    private AddContactPanel addContactPanel = new AddContactPanel();
    
    private JButton addButton = new JButton(Messages.getString("add"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MetaContact metaContact;
    
    private MetaContactGroup group;
    
    private ProtocolProviderService pps;
    
    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     * 
     * @param clist The <tt>MetaContactListService</tt>.
     * @param metaContact The <tt>MetaContact</tt> that would contain the
     * newly created contact.
     * @param pps The <tt>ProtocolProviderService</tt>.
     */
    public AddContactDialog(MetaContactListService clist,
            MetaContact metaContact,
            ProtocolProviderService pps) {
        
        this.clist = clist;
        this.metaContact = metaContact;
        this.pps = pps;
        
        this.init();
    }
    
    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new meta contact to an already existing meta contact group.
     * 
     * @param clist The <tt>MetaContactListService</tt>.
     * @param group The <tt>MetaContactGroup</tt> that would contain the
     * newly created meta contact.
     * @param pps The <tt>ProtocolProviderService</tt>.
     */
    public AddContactDialog(MetaContactListService clist,
            MetaContactGroup group,
            ProtocolProviderService pps) {
        
        this.clist = clist;
        this.group = group;
        this.pps = pps;
        
        this.init();
    }

    /**
     * Initializes the dialog.
     */
    private void init() {
        this.setTitle(Messages.getString("addContact"));
        
        this.setSize(520, 250);
        
        this.setModal(true);
        
        this.addButton.setName("add");
        this.cancelButton.setName("cancel");
        
        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(addContactPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("add")) {
            if (metaContact != null) {
                this.clist.addNewContactToMetaContact(pps, metaContact,
                    addContactPanel.getUIN());
            }
            else if (group != null) {
                this.clist.createMetaContact(
                        pps, group, addContactPanel.getUIN());
            }
            this.dispose();
        }
        else {
            this.dispose();
        }
    }
}
