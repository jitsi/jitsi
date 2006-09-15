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
/**
 * The <tt>RequestAuthorisationDialog</tt> is a <tt>JDialog</tt> that is
 * shown when user is trying to add a contact, which requires authorization.
 * 
 * @author Yana Stamcheva
 */
public class RequestAuthorizationDialog
    extends JDialog
    implements ActionListener
{
    public static final int OK_RETURN_CODE = 1;
    
    public static final int CANCEL_RETURN_CODE = 0;
    
    private JTextArea infoTextArea = new JTextArea();
    
    private JEditorPane requestPane = new JEditorPane();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JButton requestButton = new JButton(Messages.getString("request"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private AuthorizationRequest request;
    
    private int returnCode;
    
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public RequestAuthorizationDialog(Contact contact,
            AuthorizationRequest request)
    {   
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
     * Shows this modal dialog and returns the result of the user choice.
     * @return if the "Request" button was pressed returns OK_RETURN_CODE,
     * otherwise CANCEL_RETURN_CODE is returned
     */
    public int showDialog()
    {
        this.setVisible(true);
        
        return returnCode;
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if(name.equals("request")) {
            returnCode = OK_RETURN_CODE;
        }
        else if(name.equals("cancel")) {
            returnCode = CANCEL_RETURN_CODE;
        }
        this.dispose();
    }
    
    /**
     * The text entered as a resuest reason.
     * @return the text entered as a resuest reason
     */
    public String getRequestReason()
    {
        return requestPane.getText();
    }   
}
