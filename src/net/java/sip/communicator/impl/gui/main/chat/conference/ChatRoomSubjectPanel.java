/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The panel containing the subject of the chat room and the configuration
 * button.
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomSubjectPanel
    extends TransparentPanel
{
    /**
     * The object used for logging.
     */
    private Logger logger = Logger.getLogger(ChatRoomSubjectPanel.class);

    /**
     * The panel containing the subject of the chat room.
     */
    private JLabel subjectLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.SUBJECT") + ": ");

    /**
     * The field containing the subject of the chat room.
     */
    private JTextField subjectField = new JTextField();

    /**
     * The button that opens the configuration form of the chat room.
     */
    private JButton configButton = new JButton(new ImageIcon(
        ImageLoader.getImage(ImageLoader.CHAT_ROOM_CONFIG)));

    /**
     * The corresponding chat session.
     */
    private ConferenceChatSession chatSession;

    /**
     * The parent window.
     */
    private ChatWindow chatWindow;

    /**
     * Creates the panel containing the chat room subject.
     * 
     * @param chatWindow the chat window, where this panel is added
     * @param chatSession the chat session
     * chat room subject and the configuration information.
     */
    public ChatRoomSubjectPanel(ChatWindow chatWindow,
                                ConferenceChatSession chatSession)
    {
        super(new BorderLayout(5, 5));

        this.chatSession = chatSession;
        this.chatWindow = chatWindow;

        this.add(subjectLabel, BorderLayout.WEST);
        this.add(subjectField, BorderLayout.CENTER);
        this.add(configButton, BorderLayout.EAST);

        this.configButton.setPreferredSize(new Dimension(26, 26));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.configButton.addActionListener(new ConfigButtonActionListener());

        this.subjectField.setText(chatSession.getChatSubject());

        // The subject is set not editable until we implement this functionality.
        // TODO: Implement the editing of the chat room subject
        this.subjectField.setEditable(false);
    }

    /**
     * Sets the subject in the corresponding field.
     * 
     * @param subject the subject of the chat room
     */
    public void setSubject(String subject)
    {
        this.subjectField.setText(subject);
    }

    /**
     * Opens the configuration dialog when the configure buttons is pressed.
     */
    private class ConfigButtonActionListener
        implements ActionListener
    {
        /**
         * Obtains and opens the configuration form of the corresponding chat
         * room when user clicks on the configuration button.
         * @param evt the <tt>ActionEvent</tt> that notified us
         */
        public void actionPerformed(ActionEvent evt)
        {
            try
            {
                ChatRoomConfigurationForm configForm
                    = chatSession.getChatConfigurationForm();

                ChatRoomConfigurationWindow configWindow
                    = new ChatRoomConfigurationWindow(
                        chatSession.getChatName(), configForm);

                configWindow.pack();
                configWindow.setVisible(true);
            }
            catch (OperationFailedException e)
            {
                logger.error(
                    "Failed to obtain the chat room configuration form.", e);

                if(e.getErrorCode()
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    new ErrorDialog(
                        chatWindow,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.WARNING"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CHAT_ROOM_CONFIGURATION_FORBIDDEN",
                            new String[]{chatSession.getChatName()}),
                        ErrorDialog.WARNING)
                            .showDialog();
                }
                else
                {
                    new ErrorDialog(
                        chatWindow,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.ERROR"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CHAT_ROOM_CONFIGURATION_FAILED",
                            new String[]{
                            chatSession.getChatName()}),
                        e).showDialog();
                }
            }
        }
    }
}
