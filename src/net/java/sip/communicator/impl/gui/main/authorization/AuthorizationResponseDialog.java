/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.authorization;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

public class AuthorizationResponseDialog extends JDialog
    implements ActionListener {
    
    private JTextArea infoTextArea = new JTextArea();
    
    private JTextArea responseArea = new JTextArea();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
    private JButton okButton = new JButton(Messages.getString("ok"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param response The <tt>AuthorizationResponse</tt> that has been
     * received.
     */
    public AuthorizationResponseDialog(Contact contact,
            AuthorizationResponse response) {
        
        this.setModal(true);
        
        this.setTitle(Messages.getString("authorizationResponse"));
    
        this.mainPanel.setPreferredSize(new Dimension(300, 200));
                
        AuthorizationResponse.AuthorizationResponseCode responseCode
            = response.getResponseCode();
        
        if(responseCode.equals(AuthorizationResponse.ACCEPT)) {
            infoTextArea.setText(Messages.getString("authAccepted", 
                contact.getDisplayName()));
        }
        else if(responseCode.equals(AuthorizationResponse.REJECT)) {
            infoTextArea.setText(Messages.getString("authRejected", 
                    contact.getDisplayName()));
        }
        
        this.responseArea.setBorder(BorderFactory
            .createTitledBorder(Messages.getString("authorizationResponse")));
        
        this.responseArea.setText(response.getReason());
        this.responseArea.setLineWrap(true);
        this.responseArea.setWrapStyleWord(true);
        
        this.requestScrollPane.getViewport().add(responseArea);
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
                
        this.okButton.setName("ok");
                
        this.okButton.addActionListener(this);
        
        this.buttonsPanel.add(okButton);
                
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(infoTextArea, BorderLayout.NORTH);
        this.mainPanel.add(requestScrollPane, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
        
        this.setSize(new Dimension(400, 200));
        
        this.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - this.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - this.getHeight()/2
                );
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     */
    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }
}
