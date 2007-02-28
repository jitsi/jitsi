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
    
    private JLabel requestLabel = new JLabel(
        Messages.getI18NString("typeYourRequest").getText() + ": ");
    
    private JTextField requestField = new JTextField();
    
    private JPanel requestPanel = new JPanel(new BorderLayout());
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private I18NString requestString = Messages.getI18NString("request");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton requestButton = new JButton(requestString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    private JPanel northPanel = new JPanel(new BorderLayout());
    
    private JPanel titlePanel = new JPanel(new GridLayout(0, 1));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    
    private JLabel titleLabel = new JLabel();
    
    private String title
        = Messages.getI18NString("requestAuthorization").getText();
    
    private AuthorizationRequest request;
    
    private Object lock = new Object();
    
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
        
        this.setModal(false);
        
        this.setTitle(title);
    
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18f));
        titleLabel.setText(title);
        
        this.mainPanel.setPreferredSize(new Dimension(400, 230));
        
        this.request = request;
        
        infoTextArea.setText(Messages.getI18NString("requestAuthorizationInfo", 
                new String[]{contact.getDisplayName()}).getText());
        
        this.infoTextArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12f));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setEditable(false);
                
        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);
        
        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);
        
        this.requestPanel.add(requestLabel, BorderLayout.WEST);
        this.requestPanel.add(requestField, BorderLayout.CENTER);
        
        this.requestButton.setName("request");
        this.cancelButton.setName("cancel");
        
        this.requestButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(requestButton);
        this.buttonsPanel.add(cancelButton);
        
        this.getRootPane().setDefaultButton(requestButton);
        this.requestButton.setMnemonic(requestString.getMnemonic());
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);
        this.mainPanel.add(requestPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);                   
    }

    /**
     * Shows this modal dialog and returns the result of the user choice.
     * @return if the "Request" button was pressed returns OK_RETURN_CODE,
     * otherwise CANCEL_RETURN_CODE is returned
     */
    public int showDialog()
    {
        this.setVisible(true);
        
        this.requestField.requestFocus();
        
        synchronized (lock) {
            try {                    
                lock.wait();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
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
        
        synchronized (lock) {
            lock.notify();
        }
        
        this.dispose();
    }
    
    /**
     * The text entered as a resuest reason.
     * @return the text entered as a resuest reason
     */
    public String getRequestReason()
    {
        return requestField.getText();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }   
}
