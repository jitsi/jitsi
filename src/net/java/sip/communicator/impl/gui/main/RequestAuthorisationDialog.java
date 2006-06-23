/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.Contact;

public class RequestAuthorisationDialog extends JDialog
    implements ActionListener {

    private JLabel infoLabel = new JLabel();
    
    private JEditorPane requestPane = new JEditorPane();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JButton requestButton = new JButton(Messages.getString("request"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private AuthorizationRequest request;
    
    public RequestAuthorisationDialog(Contact contact,
            AuthorizationRequest request) {
        
        this.setModal(true);
        
        this.setTitle(Messages.getString("requestAuthorization"));
    
        this.mainPanel.setPreferredSize(new Dimension(300, 200));
        
        this.request = request;
        
        infoLabel.setText(Messages.getString("requestAuthorizationInfo", 
                contact.getDisplayName()));
        
        this.requestPane.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("requestAuthReason")));
        
        this.requestScrollPane.getViewport().add(requestPane);
        
        this.requestButton.setName("request");
        this.cancelButton.setName("cancel");
        
        this.buttonsPanel.add(requestButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.add(infoLabel, BorderLayout.NORTH);
        this.mainPanel.add(requestScrollPane, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }

    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if(name.equals("request")) {
            request.setReason(requestPane.getText());
        }
        else if(name.equals("cancel")) {
            request = null;
        }
    }
}
