/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addgroup;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>CreateGroupPanel</tt> is the form for creating a group.
 *  
 * @author Yana Stamcheva
 */
public class CreateGroupPanel
    extends TransparentPanel
{

    private JLabel uinLabel = new JLabel(
        Messages.getI18NString("groupName").getText());

    private JTextField textField = new JTextField();

    private TransparentPanel dataPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
            Messages.getI18NString("createGroupName").getText());

    private JLabel infoTitleLabel = new JLabel(
        Messages.getI18NString("createGroup").getText());

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_GROUP_ICON)));

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout());

    private JLabel errorLabel = new JLabel();

    /**
     * Creates and initializes the <tt>CreateGroupPanel</tt>.
     */
    public CreateGroupPanel()
    {
        super(new BorderLayout());
        
        this.setPreferredSize(new Dimension(650, 300));
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        
        this.infoLabel.setEditable(false);

        this.dataPanel.add(uinLabel, BorderLayout.WEST);
        
        this.dataPanel.add(textField, BorderLayout.CENTER);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(dataPanel);
        this.labelsPanel.add(errorLabel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        errorLabel.setForeground(Color.red);
        errorLabel.setVisible(false);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    }
    
    /**
     * Returns the string identifier entered by user.
     * @return the string identifier entered by user
     */
    public String getGroupName()
    {
        return textField.getText();
    }
    
    public void requestFocusInField()
    {
        this.textField.requestFocus();
    }
    
    /**
     * Shows an error message below the create group text field.
     * 
     * @param msg the message to show
     */
    public void showErrorMessage(String msg)
    {
        errorLabel.setText("*" + msg);
        
        errorLabel.setVisible(true);
    }    
}
