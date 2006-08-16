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
 * 
 * @author Yana Stamcheva
 */
public class AuthorizationRequestedDialog extends JDialog
    implements ActionListener {
    
    public static final int ACCEPT_CODE = 0;
    
    public static final int REJECT_CODE = 1;
    
    public static final int IGNORE_CODE = 2;
    
    public static final int ERROR_CODE = -1;
    
    private JTextArea infoTextArea = new JTextArea();
    
    private JEditorPane requestPane = new JEditorPane();
    
    private JEditorPane responsePane = new JEditorPane();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JButton acceptButton 
        = new JButton(Messages.getString("accept"));
    
    private JButton rejectButton
        = new JButton(Messages.getString("reject"));
    
    private JButton ignoreButton = new JButton(Messages.getString("ignore"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JScrollPane responseScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JSplitPane reasonsSplitPane
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    
    private int result;
    
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public AuthorizationRequestedDialog(Contact contact,
            AuthorizationRequest request) {
        
        this.setModal(true);
        
        this.setTitle(Messages.getString("authorizationRequested"));
                    
        infoTextArea.setText(Messages.getString("authorizationRequestedInfo", 
                contact.getDisplayName()));
        
        this.requestPane.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("requestAuthReason")));
        this.requestPane.setEditable(false);
        this.requestPane.setText(request.getReason());
                
        this.requestScrollPane.getViewport().add(requestPane);
        
        this.responsePane.setBorder(BorderFactory
            .createTitledBorder(Messages.getString("responseAuthReasonEnter")));
                
        this.responseScrollPane.getViewport().add(responsePane);
        
        this.reasonsSplitPane.setDividerLocation(170);
        this.reasonsSplitPane.add(requestScrollPane);
        this.reasonsSplitPane.add(responseScrollPane);
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        
        this.acceptButton.setName("accept");
        this.rejectButton.setName("reject");
        this.ignoreButton.setName("ignore");
        
        this.getRootPane().setDefaultButton(acceptButton);
        this.acceptButton.addActionListener(this);
        this.rejectButton.addActionListener(this);
        this.ignoreButton.addActionListener(this);
                
        this.buttonsPanel.add(acceptButton);
        this.buttonsPanel.add(rejectButton);
        this.buttonsPanel.add(ignoreButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(infoTextArea, BorderLayout.NORTH);
        this.mainPanel.add(reasonsSplitPane, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
        
        this.setSize(new Dimension(500, 400));
        
        this.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - this.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - this.getHeight()/2
                );
    }

    /**
     * Shows this modal dialog.
     * @return the result code, which shows what was the choice of the user
     */
    public int showDialog() {
        this.setVisible(true);
        
        return result;
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("accept")) {
            this.result = ACCEPT_CODE;
        }
        else if (name.equals("reject")) {
            this.result = REJECT_CODE;
        }
        else if (name.equals("ignore")) {
            this.result = IGNORE_CODE;
        }
        else {
            this.result = ERROR_CODE;
        }        
        this.dispose();
    }
    
    /**
     * Returns the response reason, which has been entered from the user to
     * explain it's response on the request.
     * @return the response reason of the user
     */
    public String getResponseReason() {
        return this.responsePane.getText();
    }
}
