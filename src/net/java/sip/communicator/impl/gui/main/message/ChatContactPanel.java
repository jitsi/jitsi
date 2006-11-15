/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ChatContactPanel</tt> is the panel that appears on the right of the
 * chat conversation area. It contains the name, status and other informations
 * for a <tt>MetaContact</tt> engaged in a chat conversation. 
 * <p>
 * Fast access to some operations with this <tt>MetaContact</tt> is provided
 * by buttons added above the contact name. At this moment there are three
 * a Call button, an Info button and a Send file button. When clicked the Call
 * button makes a call. The Info button shows the Information window for this
 * <tt>MetaContact</tt> and the Send file button sends a file to this contact.
 * <p>
 * Note that all buttons are now disabled, because the functionality they should
 * provide is not yet implemented.  
 *  
 * @author Yana Stamcheva
 */
public class ChatContactPanel
    extends JPanel
    implements ActionListener
{

    private SIPCommButton callButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CHAT_CONTACT_CALL_BUTTON), ImageLoader
            .getImage(ImageLoader.CHAT_CONTACT_CALL_ROLLOVER_BUTTON));

    private SIPCommButton infoButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CHAT_CONTACT_INFO_BUTTON), ImageLoader
            .getImage(ImageLoader.CHAT_CONTACT_INFO_ROLLOVER_BUTTON));

    private SIPCommButton sendFileButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CHAT_CONTACT_SEND_FILE_BUTTON), ImageLoader
            .getImage(ImageLoader.CHAT_SEND_FILE_ROLLOVER_BUTTON));

    private JLabel personPhotoLabel = new JLabel();

    private JLabel personNameLabel = new JLabel();

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private JPanel contactNamePanel = new JPanel(new FlowLayout(
            FlowLayout.CENTER, 0, 0));

    private MetaContact contactItem;

    private PresenceStatus status;
    
    private ChatPanel chatPanel;

    /**
     * Creates an instance of the <tt>ChatContactPanel</tt>.
     * 
     * @param contactItem The <tt>MetaContact</tt>.
     */
    public ChatContactPanel(ChatPanel chatPanel, MetaContact contactItem) {
        this(chatPanel, contactItem, null);
    }

    /**
     * Creates an instance of <tt>ChatContactPanel</tt>.
     * 
     * @param contactItem The <tt>MetaContact</tt>.
     * @param status The contact status.
     */
    public ChatContactPanel(ChatPanel chatPanel,
            MetaContact contactItem, PresenceStatus status) {

        super(new BorderLayout());

        this.setPreferredSize(new Dimension(100, 60));

        this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        this.contactItem = contactItem;
        this.status = status;
        this.chatPanel = chatPanel;

        this.setOpaque(false);
        this.mainPanel.setOpaque(false);
        this.contactNamePanel.setOpaque(false);
        this.buttonsPanel.setOpaque(false);

        this.init();
    }

    /**
     * Constructs the <tt>ChatContactPanel</tt>.
     */
    private void init() {
        this.personNameLabel.setText(contactItem.getDisplayName());
        this.personNameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        this.personNameLabel.setIcon(new ImageIcon(Constants
                .getStatusIcon(status)));
        // this.personPhotoLabel.setIcon(new ImageIcon(contactItem.getPhoto()));

        this.callButton.setToolTipText(Messages.getString("call"));
        this.infoButton.setToolTipText(Messages.getString("userInfo"));
        this.sendFileButton.setToolTipText(Messages.getString("sendFile"));

        this.callButton.setName("call");
        this.infoButton.setName("info");
        
        this.callButton.addActionListener(this);
        this.infoButton.addActionListener(this);
        
        this.buttonsPanel.add(infoButton);
        this.buttonsPanel.add(callButton);
        this.buttonsPanel.add(sendFileButton);

        this.contactNamePanel.add(personNameLabel);

        this.mainPanel.add(buttonsPanel, BorderLayout.NORTH);
        this.mainPanel.add(contactNamePanel, BorderLayout.CENTER);

        this.add(personPhotoLabel, BorderLayout.WEST);
        this.add(mainPanel, BorderLayout.CENTER);

        // Disabled all unused buttons.
        this.callButton.setEnabled(false);
        this.sendFileButton.setEnabled(false);
        
        this.updateProtocolContact(chatPanel.getProtocolContact());
    }

    /**
     * Overrides the <code>javax.swing.JComponent.paintComponent()</code> in
     * order to paint a gradient background.
     * 
     * @param g The Graphics object.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        GradientPaint p = new GradientPaint(this.getWidth() / 2, 0,
                Constants.MOVER_START_COLOR, this.getWidth() / 2,
                Constants.GRADIENT_SIZE,
                Constants.MOVER_END_COLOR);

        GradientPaint p1 = new GradientPaint(this.getWidth() / 2, this
                .getHeight()
                - Constants.GRADIENT_SIZE,
                Constants.MOVER_END_COLOR, this.getWidth() / 2,
                this.getHeight(), Constants.MOVER_START_COLOR);

        g2.setPaint(p);
        g2.fillRect(0, 0, this.getWidth(),
                        Constants.GRADIENT_SIZE);

        g2.setColor(Constants.MOVER_END_COLOR);
        g2.fillRect(0, Constants.GRADIENT_SIZE, this.getWidth(),
                this.getHeight() - Constants.GRADIENT_SIZE);

        g2.setPaint(p1);
        g2.fillRect(0, this.getHeight() - Constants.GRADIENT_SIZE
                - 1, this.getWidth(), this.getHeight() - 1);
    }

    /**
     * Changes the status icon left to the contact name when the status changes.
     * 
     * @param newStatus The new status.
     */
    public void setStatusIcon(PresenceStatus newStatus) {
        this.personNameLabel.setIcon(new ImageIcon(Constants
                .getStatusIcon(newStatus)));
    }
    
    /**
     * Disables or enables the contact info button depending on the selected 
     * protocol contact.
     * 
     * @param protocolContact the selected protocol contact 
     */
    public void updateProtocolContact(Contact protocolContact)
    {   
        MainFrame mainFrame = chatPanel.getChatWindow().getMainFrame();
        
        ProtocolProviderService pps
            = protocolContact.getProtocolProvider();

        OperationSetWebContactInfo wContactInfo
            = mainFrame.getWebContactInfo(pps);
        
        if(wContactInfo == null)
            infoButton.setEnabled(false);
        else
            infoButton.setEnabled(true);
    }
    
    /**
     * 
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        
        MainFrame mainFrame = chatPanel.getChatWindow().getMainFrame();
        
        if(button.getName().equals("call")) {
            
        }
        else if(button.getName().equals("info")){            
            
            if(contactItem != null) {
                
                Contact protocolContact = chatPanel.getProtocolContact();

                ProtocolProviderService pps
                    = protocolContact.getProtocolProvider();

                OperationSetWebContactInfo wContactInfo
                    = mainFrame.getWebContactInfo(pps);

                if(wContactInfo != null) {
                    CrossPlatformBrowserLauncher.openURL(
                        wContactInfo.getWebContactInfo(protocolContact)
                            .toString());
                }
            }
        }
    }
}
