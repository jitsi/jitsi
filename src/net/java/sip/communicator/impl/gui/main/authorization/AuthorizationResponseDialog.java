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

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

public class AuthorizationResponseDialog extends SIPCommDialog
    implements ActionListener {
    
    private JTextArea infoTextArea = new JTextArea();
    
    private JTextArea responseArea = new JTextArea();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
    private JButton okButton = new JButton(Messages.getString("ok"));
    
    private JScrollPane responseScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel northPanel = new JPanel(new BorderLayout());
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    
    private JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    
    private JLabel titleLabel = new JLabel();
    
    private String title = Messages.getString("authorizationResponse");
        
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
        
        this.setTitle(title);
        
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        titleLabel.setText(title);
        
        this.mainPanel.setPreferredSize(new Dimension(400, 250));
                
        AuthorizationResponse.AuthorizationResponseCode responseCode
            = response.getResponseCode();
        
        if(responseCode.equals(AuthorizationResponse.ACCEPT)) {
            infoTextArea.setText(contact.getDisplayName() + " "
                    + Messages.getString("authAccepted"));
        }
        else if(responseCode.equals(AuthorizationResponse.REJECT)) {
            infoTextArea.setText(contact.getDisplayName() + " "
                    + Messages.getString("authRejected"));
        }
        
        this.responseScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));
        
        this.responseArea.setText(response.getReason());
        this.responseArea.setLineWrap(true);
        this.responseArea.setWrapStyleWord(true);
        this.responseArea.setEditable(false);
        this.responseArea.setOpaque(false);
        
        this.responseScrollPane.getViewport().add(responseArea);
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setEditable(false);
        this.infoTextArea.setOpaque(false);
        
        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);
        
        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);
        
        this.okButton.requestFocus();
        this.okButton.setName("ok");
        this.getRootPane().setDefaultButton(okButton);
        
        this.okButton.addActionListener(this);
        
        this.buttonsPanel.add(okButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);
        this.mainPanel.add(responseScrollPane, BorderLayout.CENTER);
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

    protected void close()
    {
        this.dispose();
    }
}
