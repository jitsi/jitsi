/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ibm.media.bean.multiplayer.ImageLabel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class RenameContactDialog extends JDialog
    implements ActionListener {

    private JPanel renameContactPanel = new JPanel(new BorderLayout());
    
    private JLabel nameLabel = new JLabel(Messages.getString("newName"));
    
    private JTextField nameField = new JTextField();
    
    private JButton renameButton = new JButton(Messages.getString("rename"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    //private JLabel iconLabel = new JLabel(new ImageIcon(
    //        ImageLoader.getImage(ImageLoader.RENAME_DIALOG_ICON)));
    
    private MetaContactListService clist;
    
    private MetaContact metaContact;
        
    public RenameContactDialog(MetaContactListService clist,
            MetaContact metaContact) {
        
        this.clist = clist;
        this.metaContact = metaContact;
        
        this.init();
    }
    
    private void init() {
        this.setTitle(Messages.getString("renameContact"));
        
        this.setSize(400, 100);
        
        this.setModal(true);
        
        this.renameContactPanel.add(nameLabel, BorderLayout.WEST);
        this.renameContactPanel.add(nameField, BorderLayout.CENTER);
        
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");
        
        this.renameButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(renameButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(renameContactPanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        //this.getContentPane().add(iconLabel, BorderLayout.WEST);
    }
    
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("rename")) {
            if (metaContact != null) {
                this.clist.renameMetaContact(metaContact, nameField.getText());
            }
            this.dispose();
        }
        else {
            this.dispose();
        }
    }
}
