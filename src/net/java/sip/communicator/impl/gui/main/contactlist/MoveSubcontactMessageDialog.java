/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;

public class MoveSubcontactMessageDialog
    extends JDialog
{
    private SIPCommMsgTextArea infoArea 
        = new SIPCommMsgTextArea(Messages.getString("moveSubcontactMsg"));
    
    private JLabel infoTitleLabel 
        = new JLabel(Messages.getString("moveSubcontact"));
    
    private JLabel iconLabel = new JLabel(
            new ImageIcon(ImageLoader.getImage(ImageLoader.WARNING_ICON)));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    
    public MoveSubcontactMessageDialog(MainFrame mainFrame)
    {
        super(mainFrame);
        
        this.setLocationRelativeTo(mainFrame);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoArea);
        
        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.mainPanel.add(labelsPanel, BorderLayout.CENTER);
        this.mainPanel.add(iconLabel, BorderLayout.WEST);
        
        this.getContentPane().add(mainPanel);
        this.pack();
    }
}
