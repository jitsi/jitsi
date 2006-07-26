/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.contactlist.addcontact.RenameContactPanel;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

/**
 * The <tt>RenameContactDialog</tt> is the dialog containing the form for
 * renaming a contact. 
 * 
 * @author Yana Stamcheva
 */
public class RenameContactDialog extends JDialog
    implements ActionListener {

    private RenameContactPanel renameContactPanel 
        = new RenameContactPanel();
    
    private JButton renameButton = new JButton(Messages.getString("rename"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MetaContact metaContact;
        
    /**
     * Creates an instance of <tt>RenameContactDialog</tt>.
     * 
     * @param clist The <tt>MetaContactListService</tt>.
     * @param metaContact The <tt>MetaContact</tt> to rename.
     */
    public RenameContactDialog(MetaContactListService clist,
            MetaContact metaContact) {
        
        this.setSize(new Dimension(520, 270));
        
        this.clist = clist;
        this.metaContact = metaContact;
        
        this.init();
    }
    
    /**
     * Initializes the <tt>RenameContactDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init() {
        this.setTitle(Messages.getString("renameContact"));
        
        this.setModal(true);
        
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");
        
        this.renameButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(renameButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(renameContactPanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. In order to rename the contact invokes
     * the <code>renameMetaContact</code> method of the current
     * <tt>MetaContactListService</tt>.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("rename")) {
            if (metaContact != null) {
                this.clist.renameMetaContact(
                    metaContact, renameContactPanel.getNewName());
            }
            this.dispose();
        }
        else {
            this.dispose();
        }
    }
}
