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
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * The photo label corresponding to the current chat.
 */
public class ContactPhotoPanel extends JLayeredPane
{
    private final JLabel photoLabel = new JLabel();

    private final JLabel addContactButton = new JLabel(
        new ImageIcon(ImageLoader.getImage(
            ImageLoader.ADD_CONTACT_CHAT_ICON)));

    private ImageIcon tooltipIcon;

    private ChatSession chatSession;

    public ContactPhotoPanel()
    {
        this.setLayout(null);

        this.setPreferredSize(
            new Dimension(  ChatContact.AVATAR_ICON_WIDTH + 10,
                            ChatContact.AVATAR_ICON_HEIGHT));

        this.add(photoLabel, 1);

        this.photoLabel.setBounds(5, 0,
            ChatContact.AVATAR_ICON_WIDTH,
            ChatContact.AVATAR_ICON_HEIGHT);

        addContactButton.setBounds(
            ChatContact.AVATAR_ICON_WIDTH - 6,
            ChatContact.AVATAR_ICON_HEIGHT - 16,
            16, 16);

        this.addContactButton.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                if(chatSession != null)
                {
                    AddContactDialog dialog
                        = new AddContactDialog(
                                GuiActivator.getUIService().getMainFrame());

                    dialog.setSelectedAccount(
                        chatSession.getCurrentChatTransport()
                            .getProtocolProvider());

                    // this is the current contact address we want to add
                    dialog.setContactAddress(
                        chatSession.getCurrentChatTransport().getName());

                    dialog.setVisible(true);
                }
            }
        });
    }

    /**
     * Sets the given <tt>chatSession</tt> parameters to this contact
     * photo label.
     * 
     * @param chatSession The <tt>ChatSession</tt> to set.
     */
    public void setChatSession(ChatSession chatSession)
    {
        this.chatSession = chatSession;

        byte[] chatAvatar = chatSession.getChatAvatar();

        if (chatAvatar != null && chatAvatar.length > 0)
        {
            this.tooltipIcon = new ImageIcon(chatAvatar);

            ImageIcon contactPhotoIcon
                = ImageUtils.getScaledRoundedIcon(chatAvatar,
                    ChatContact.AVATAR_ICON_WIDTH ,
                    ChatContact.AVATAR_ICON_HEIGHT);

            if (contactPhotoIcon != null)
                this.photoLabel.setIcon(contactPhotoIcon);
        }
        else
        {
            // Even if we don't have the icon of the current contact we
            // should remove the one of the previously selected contact.
            this.photoLabel.setIcon(null);
            this.tooltipIcon = null;
        }

        // Need to set the tooltip in order to have createToolTip called
        // from the TooltipManager.
        this.setToolTipText("");

        // if its multichat don't show addContactButton, cause
        // it sa mutlichat room which
        // cannot be saved with add contact dialog
        if (!chatSession.isDescriptorPersistent()
            && !(chatSession instanceof ConferenceChatSession))
            this.add(addContactButton, 0);
        else
            this.remove(addContactButton);

        this.revalidate();
        this.repaint();
    }

    /**
     * Creates a tooltip.
     * @return the created tool tip
     */
    public JToolTip createToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        if (tooltipIcon != null)
            tip.setImage(tooltipIcon);

        tip.setTitle(chatSession.getChatName());

        Iterator<ChatTransport> transports = chatSession.getChatTransports();

        while (transports.hasNext())
        {
            ChatTransport transport = transports.next();

            ImageIcon protocolStatusIcon;
            if (transport.getStatus() != null)
            {
                protocolStatusIcon
                    = new ImageIcon(transport.getStatus().getStatusIcon());
            }
            else
                protocolStatusIcon = new ImageIcon();

            String transportAddress = transport.getName();

            tip.addLine( protocolStatusIcon,
                                    transportAddress);
        }

        tip.setComponent(this);

        return tip;
    }

    /**
     * Returns the string to be used as the tooltip for <i>event</i>. We
     * don't really use this string, but we need to return different string
     * each time in order to make the TooltipManager change the tooltip over
     * the different cells in the JList.
     * 
     * @param event the <tt>MouseEvent</tt>
     * @return the string to be used as the tooltip for <i>event</i>.
     */
    public String getToolTipText(MouseEvent event)
    {
        return chatSession.getChatName();
    }
}