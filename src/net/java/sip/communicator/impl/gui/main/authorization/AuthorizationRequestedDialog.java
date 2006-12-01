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
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class AuthorizationRequestedDialog
    extends SIPCommDialog
    implements ActionListener {
    
    public static final int ACCEPT_CODE = 0;
    
    public static final int REJECT_CODE = 1;
    
    public static final int IGNORE_CODE = 2;
    
    public static final int ERROR_CODE = -1;
    
    private JTextArea infoTextArea = new JTextArea();
    
    private JEditorPane requestPane = new JEditorPane();
    
    private JEditorPane responsePane = new JEditorPane();
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel northPanel = new JPanel(new BorderLayout());
    
    private JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    
    private JLabel titleLabel = new JLabel();
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    
    private JButton acceptButton 
        = new JButton(Messages.getString("accept"));
    
    private JButton rejectButton
        = new JButton(Messages.getString("reject"));
    
    private JButton ignoreButton = new JButton(Messages.getString("ignore"));
    
    private JScrollPane requestScrollPane = new JScrollPane();
    
    private JScrollPane responseScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel reasonsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
    
    private String title = Messages.getString("authorizationRequested");
    
    private Object lock = new Object();
    
    private int result;
    
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public AuthorizationRequestedDialog(MainFrame mainFrame, Contact contact,
            AuthorizationRequest request) {
        
        super(mainFrame);
        
        this.setModal(false);
        
        this.setTitle(title);

        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        titleLabel.setText(title);
        
        infoTextArea.setText(Messages.getString("authorizationRequestedInfo", 
                contact.getDisplayName()));
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setEditable(false);
        
        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);
        
        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);
        
        if(request.getReason() != null && !request.getReason().equals("")) {
            
            this.requestScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));
        
            this.requestPane.setEditable(false);
            this.requestPane.setOpaque(false);
            this.requestPane.setText(request.getReason());
                    
            this.requestScrollPane.getViewport().add(requestPane);
            
            this.reasonsPanel.add(requestScrollPane);
            
            this.mainPanel.setPreferredSize(new Dimension(550, 400));            
        }
        else {
            this.mainPanel.setPreferredSize(new Dimension(550, 300));
        }
        
        this.responseScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));
        
        this.responseScrollPane.getViewport().add(responsePane);
        
        this.reasonsPanel.add(responseScrollPane);
        
        this.acceptButton.setName("accept");
        this.rejectButton.setName("reject");
        this.ignoreButton.setName("ignore");
        
        this.getRootPane().setDefaultButton(acceptButton);
        this.acceptButton.addActionListener(this);
        this.rejectButton.addActionListener(this);
        this.ignoreButton.addActionListener(this);
                
        this.acceptButton.setMnemonic(
                Messages.getString("mnemonic.acceptButton").charAt(0));
        this.rejectButton.setMnemonic(
                Messages.getString("mnemonic.rejectButton").charAt(0));
        this.ignoreButton.setMnemonic(
                Messages.getString("mnemonic.ignoreButton").charAt(0));
        
        this.buttonsPanel.add(acceptButton);
        this.buttonsPanel.add(rejectButton);
        this.buttonsPanel.add(ignoreButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);        
        this.mainPanel.add(reasonsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows this modal dialog.
     * @return the result code, which shows what was the choice of the user
     */
    public int showDialog() {
        this.setVisible(true);
        
        synchronized (lock) {
            try {                    
                lock.wait();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
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
        
        synchronized (lock) {
            lock.notify();
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

    protected void close(boolean isEscaped)
    {
        this.ignoreButton.doClick();
    }
}
