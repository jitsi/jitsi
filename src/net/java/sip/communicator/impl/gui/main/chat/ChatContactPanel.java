/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
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
    implements  ActionListener,
                MouseListener
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

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private ImageIcon contactPhotoIcon;

    private ChatContact chatContact;

    private ChatPanel chatPanel;
    
    /**
     * Creates an instance of the <tt>ChatContactPanel</tt>.
     *
     * @param chatPanel the <tt>ChatPanel</tt>, to which this
     * <tt>ChatContactPanel</tt> belongs to.
     * @param contact the chat contact
     */
    public ChatContactPanel(ChatPanel chatPanel, ChatContact contact)
    {
        super(new BorderLayout(10, 5));

        this.chatContact = contact;

        this.chatPanel = chatPanel;

        // Adds a mouse listener, which when notified will open a right button
        // menu.
        this.addMouseListener(this);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.setOpaque(false);
        this.mainPanel.setOpaque(false);
        this.buttonsPanel.setOpaque(false);

        this.personNameLabel.setText(chatContact.getName());
        this.personNameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        this.personNameLabel.setIcon(chatContact.getStatusIcon());

        this.callButton.setToolTipText(
            Messages.getI18NString("call").getText());
        this.infoButton.setToolTipText(
            Messages.getI18NString("userInfo").getText());
        this.sendFileButton.setToolTipText(
            Messages.getI18NString("sendFile").getText());

        this.callButton.setName("call");
        this.infoButton.setName("info");

        this.callButton.addActionListener(this);
        this.infoButton.addActionListener(this);

        this.buttonsPanel.add(infoButton);
        this.buttonsPanel.add(callButton);
        this.buttonsPanel.add(sendFileButton);
        
        this.buttonsPanel.setVisible(false);
        
        this.mainPanel.add(buttonsPanel, BorderLayout.NORTH);
        this.mainPanel.add(personNameLabel, BorderLayout.CENTER);

        this.add(personPhotoLabel, BorderLayout.WEST);
        this.add(mainPanel, BorderLayout.CENTER);

        // Disabled all unused buttons.

        ProtocolProviderService pps = chatContact.getProtocolProvider();

        if (chatContact.getSourceContact() instanceof Contact)
        {
            Contact c = (Contact) chatContact.getSourceContact();
            MetaContact m = chatPanel.getChatWindow().getMainFrame()
                    .getContactList().findMetaContactByContact(c);

            if (m.getDefaultContact(OperationSetBasicTelephony.class)
                    == null)
                this.callButton.setEnabled(false); 

            if (m.getDefaultContact(OperationSetFileTransfer.class)
                    == null)
                this.sendFileButton.setEnabled(false); 
        }
        else
        {
            if (pps.getOperationSet(OperationSetBasicTelephony.class) == null)
                this.callButton.setEnabled(false);

            if (pps.getOperationSet(OperationSetFileTransfer.class) == null)
                this.sendFileButton.setEnabled(false);
        }

        //Load the contact photo.
        new Thread()
        {
            public void run()
            {
                contactPhotoIcon = chatContact.getImage();

                SwingUtilities.invokeLater(new Runnable(){
                    public void run()
                    {
                        if(contactPhotoIcon != null)
                        {
                            personPhotoLabel.setBorder(
                                    new SIPCommBorders.BoldRoundBorder());
                            personPhotoLabel.setIcon(contactPhotoIcon);
                        }
                    }
                });

            }
        }.start();

        Object contactInfoOpSet
            = pps.getOperationSet(OperationSetWebContactInfo.class);

        if(contactInfoOpSet == null)
            infoButton.setEnabled(false);
        else
            infoButton.setEnabled(true);

        this.updateStatusIcon();
    }

    /**
     * Overrides the <code>javax.swing.JComponent.paintComponent()</code> in
     * order to paint a gradient background.
     *
     * @param g The Graphics object.
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        Graphics2D g2 = (Graphics2D) g;
        
        AntialiasingManager.activateAntialiasing(g2);
        
        if(chatContact.isSelected())
        {
            GradientPaint p = new GradientPaint(this.getWidth()/2, 0,
              Constants.SELECTED_END_COLOR,
              this.getWidth()/2, this.getHeight(),
              Constants.MOVER_END_COLOR);
                
            g2.setPaint(p);
            g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

            g2.setColor(Constants.BLUE_GRAY_BORDER_DARKER_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 0, this.getWidth() - 1, this.getHeight() - 1,
                    7, 7);
        }   
    }

    /**
     * Changes the status icon left to the contact name. This method is called
     * when the status changes.
     */
    public void updateStatusIcon()
    {
        this.personNameLabel.setIcon(chatContact.getStatusIcon());
    }

    /**
     * Opens a web page containing information of the currently selected user.
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();

        // first, see if the contact with which we chat supports telephony
        // and call that one. If he don't, we look for the default
        // telephony contact in its enclosing metacontact
        if(button.getName().equals("call"))
        {
            CallManager cm =
                    chatPanel.getChatWindow().getMainFrame().getCallManager();
            Object o = chatContact.getSourceContact();

            OperationSetBasicTelephony opSetBT
                    = (OperationSetBasicTelephony) chatContact.getProtocolProvider()
                    .getOperationSet(OperationSetBasicTelephony.class);

            if (opSetBT != null)
            {
                if (o instanceof Contact)
                {
                    Vector v = new Vector();
                    v.add((Contact)chatContact.getSourceContact());
                    cm.createCall(v);
                }
                else // hope an appropriate telephony will be used.
                    cm.createCall(((ChatRoomMember) o).getContactAddress());
            }
            else if (o instanceof Contact)
            {
                MetaContact m = chatPanel.getChatWindow().getMainFrame()
                        .getContactList().findMetaContactByContact((Contact)o);

                Vector v = new Vector();
                v.add(m.getDefaultContact(OperationSetBasicTelephony.class));
                cm.createCall(v);
            }
        }
        else if(button.getName().equals("info"))
        {
            ProtocolProviderService pps
                = chatContact.getProtocolProvider();

            Object contactInfoOpSet
                = pps.getOperationSet(OperationSetWebContactInfo.class);

            if(contactInfoOpSet != null) {
                GuiActivator.getBrowserLauncher().openURL(
                    ((OperationSetWebContactInfo)contactInfoOpSet)
                        .getWebContactInfo(chatContact.getAddress())
                        .toString());
            }
        }
    }

    /**
     * Renames the contact contained in this chat contact panel.
     * @param newName the new name
     */
    public void renameContact(String newName)
    {
        personNameLabel.setText(newName);
    }

    /**
     * Sets explicetly an image to the contact.
     * 
     * @param contactPhoto the image of the contact
     */
    public void setContactPhoto(ImageIcon contactPhoto)
    {
        this.contactPhotoIcon = contactPhoto;
        
        this.personPhotoLabel.setIcon(contactPhotoIcon);
    }
    
    /**
     * If isSelected is set to TRUE expands this panel, by adding the buttons
     * panel. The ButtonsPanel contains buttons that offer different
     * functionalities, as "Make a call", "See user info", etc. When the
     * isSelected is set to FALSE, this panel is reduced to its basic content.
     * 
     * @param isSelected indicates if this chat contact panel is selected
     */
    public void setSelected(boolean isSelected)
    {
        chatContact.setSelected(isSelected);
        
        if(isSelected)
        {
            this.buttonsPanel.setVisible(true);
            this.mainPanel.revalidate();
            this.mainPanel.repaint();
        }
        else
        {
            this.buttonsPanel.setVisible(false);
            this.mainPanel.revalidate();
            this.mainPanel.repaint();
        }
    }

    public void mouseClicked(MouseEvent e)
    {   
    }

    public void mouseEntered(MouseEvent e)
    {   
    }

    public void mouseExited(MouseEvent e)
    {   
    }

    public void mousePressed(MouseEvent e)
    {
        if(!(chatPanel instanceof ConferenceChatPanel))
            return;
        
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
        {
            ChatContactRightButtonMenu rightButtonMenu
                = new ChatContactRightButtonMenu(chatPanel, chatContact);
            
            rightButtonMenu.setInvoker(this);
            
            Point mousePoint = e.getPoint();
            
            SwingUtilities.convertPointToScreen(mousePoint, this);
            
            rightButtonMenu.setLocation((int) mousePoint.getX(),
                (int) mousePoint.getY());

            rightButtonMenu.setVisible(true);
        }
    }

    public void mouseReleased(MouseEvent e)
    {   
    }
}
