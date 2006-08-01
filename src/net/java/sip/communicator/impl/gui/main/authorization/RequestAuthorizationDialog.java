/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.authorization;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.protocol.AuthorizationRequest;
import net.java.sip.communicator.service.protocol.Contact;
/**
 * The <tt>RequestAuthorisationDialog</tt> is a <tt>JDialog</tt> that is
 * shown when user is trying to add a contact, which requires authorization.
 * 
 * @author Yana Stamcheva
 */
public class RequestAuthorizationDialog extends JDialog
    implements ActionListener {

    private JTextArea infoTextArea = new JTextArea();
    
    private JEditorPane requestPane = new JEditorPane();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JButton requestButton = new JButton(Messages.getString("request"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private AuthorizationRequest request;
    
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public RequestAuthorizationDialog(Contact contact,
            AuthorizationRequest request) {
        
        this.setModal(true);
        
        this.setTitle(Messages.getString("requestAuthorization"));
    
        this.mainPanel.setPreferredSize(new Dimension(300, 200));
        
        this.request = request;
        
        infoTextArea.setText(Messages.getString("requestAuthorizationInfo", 
                contact.getDisplayName()));
        
        this.requestPane.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("requestAuthReasonEnter")));
        
        this.requestScrollPane.getViewport().add(requestPane);
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        
        this.requestButton.setName("request");
        this.cancelButton.setName("cancel");
        
        this.requestButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(requestButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(infoTextArea, BorderLayout.NORTH);
        this.mainPanel.add(requestScrollPane, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
        
        this.setSize(new Dimension(400, 300));
        
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
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if(name.equals("request")) {
            request.setReason(requestPane.getText());
        }
        else if(name.equals("cancel")) {
            request = null;
        }
        this.dispose();
    }
}
