/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.exampleplugin;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.swing.*;

/**
 * A plugin dialog that is open through the right button menu over a contact and
 * shows the contact name.
 * 
 * @author Yana Stamcheva
 */
public class PluginDialog
    extends SIPCommDialog
{
    private JTextArea infoTextArea = new JTextArea();
    
    private JPanel mainPanel = new TransparentPanel();
    
    private JLabel contactLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    
    public PluginDialog(MetaContact metaContact)
    {
        this.setTitle("Example plugin");
        
        this.infoTextArea.setPreferredSize(new Dimension(250, 70));
        
        this.infoTextArea.setText("This is an example plugin that shows the "
            + "currently selected contact"
            + " in a separate window.");
        
        this.nameLabel.setText("The name of the selected contact is:");
        this.contactLabel.setText(metaContact.getDisplayName());
        
        this.mainPanel.add(infoTextArea);
        this.mainPanel.add(nameLabel);
        this.mainPanel.add(contactLabel);
        
        this.getContentPane().add(mainPanel);
        
        this.setStyles();
        
        this.setResizable(false);
        this.pack();
    }
    
    private void setStyles()
    {
        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.infoTextArea.setEditable(false);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setAlignmentX(JTextArea.CENTER_ALIGNMENT);
        
        this.nameLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        this.contactLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        this.contactLabel.setAlignmentY(JLabel.TOP_ALIGNMENT);
        this.contactLabel.setFont(contactLabel.getFont().deriveFont(Font.BOLD));        
    }

    protected void close(boolean isEscaped)
    {
    }
}
