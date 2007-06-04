/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

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
    implements  ActionListener
{

    private Logger logger = Logger.getLogger(ChatContactPanel.class);

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

    private PresenceStatus status;

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

        this.setPreferredSize(new Dimension(100, 60));

        this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        this.status = chatContact.getPresenceStatus();
        this.chatPanel = chatPanel;

        this.setOpaque(false);
        this.mainPanel.setOpaque(false);
        this.buttonsPanel.setOpaque(false);

        this.personNameLabel.setText(chatContact.getName());
        this.personNameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
        this.personNameLabel.setIcon(new ImageIcon(Constants
                .getStatusIcon(status)));

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

        this.mainPanel.add(buttonsPanel, BorderLayout.NORTH);
        this.mainPanel.add(personNameLabel, BorderLayout.CENTER);

        this.add(personPhotoLabel, BorderLayout.WEST);
        this.add(mainPanel, BorderLayout.CENTER);

        // Disabled all unused buttons.
        this.callButton.setEnabled(false);
        this.sendFileButton.setEnabled(false);

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

        ProtocolProviderService pps
            = chatContact.getProtocolProvider();

        Object contactInfoOpSet
            = pps.getOperationSet(OperationSetWebContactInfo.class);

        if(contactInfoOpSet == null)
            infoButton.setEnabled(false);
        else
            infoButton.setEnabled(true);

        this.setStatusIcon(chatContact.getPresenceStatus());
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
     * Opens a web page containing information of the currently selected user.
     *
     * @param evt the action event that has just occurred.
     */
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();

        if(button.getName().equals("call"))
        {
            //TODO: Implement the call functionality
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
     * Sets the given <tt>ImageIcon</tt> to be the photo shown on the left of
     * the contact name.
     *
     * @param contactPhoto the image to show as a contact photo
     */
    public void setContactPhoto(ImageIcon contactPhoto)
    {
        contactPhotoIcon = contactPhoto;

        personPhotoLabel.setBorder(
                new SIPCommBorders.BoldRoundBorder());
        personPhotoLabel.setIcon(contactPhotoIcon);
    }
}
