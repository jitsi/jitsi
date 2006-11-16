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
 * The <tt>RequestAuthorisationDialog</tt> is a <tt>JDialog</tt> that is
 * shown when user is trying to add a contact, which requires authorization.
 * 
 * @author Yana Stamcheva
 */
public class RequestAuthorizationDialog
    extends SIPCommDialog
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
    
    private JPanel northPanel = new JPanel(new BorderLayout());
    
    private JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    
    private JLabel titleLabel = new JLabel();
    
    private String title = Messages.getString("requestAuthorization");
    
    private AuthorizationRequest request;
    
    private int returnCode;
    
    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     * 
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public RequestAuthorizationDialog(MainFrame mainFrame, Contact contact,
            AuthorizationRequest request)
    {
        super(mainFrame);
        
        this.setModal(true);
        
        this.setTitle(title);
    
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        titleLabel.setText(title);
        
        this.mainPanel.setPreferredSize(new Dimension(350, 150));
        
        this.request = request;
        
        infoTextArea.setText(Messages.getString("requestAuthorizationInfo", 
                contact.getDisplayName()));
        
        this.requestScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));
        
        this.requestScrollPane.getViewport().add(requestPane);
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setEditable(false);
        
        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);
        
        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);
        
        this.requestButton.setName("request");
        this.cancelButton.setName("cancel");
        
        this.requestButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(requestButton);
        this.buttonsPanel.add(cancelButton);
        
        this.getRootPane().setDefaultButton(requestButton);
        this.requestButton.setMnemonic(
                Messages.getString("mnemonic.requestButton").charAt(0));
        this.cancelButton.setMnemonic(
                Messages.getString("mnemonic.cancel").charAt(0));
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);
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

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }   
}
