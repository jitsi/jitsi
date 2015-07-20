/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.SwingWorker;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.replacement.directimage.*;
import net.java.sip.communicator.util.*;

public class ShowPreviewDialog
    extends SIPCommDialog
    implements ActionListener,
               ChatLinkClickedListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The <tt>Logger</tt> used by the <tt>ShowPreviewDialog</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ShowPreviewDialog.class);

    /**
     * The Ok button.
     */
    private final JButton okButton;

    /**
     * The cancel button.
     */
    private final JButton cancelButton;

    /**
     * Checkbox that indicates whether or not to show this dialog next time.
     */
    private final JCheckBox configureReplacement;

    /**
     * The <tt>ChatConversationPanel</tt> that this dialog is associated with.
     */
    private final ChatConversationPanel chatPanel;

    /**
     * Mapping between messageID and the string representation of the chat
     * message.
     */
    private Map<String, String> msgIDToChatString
        = new ConcurrentHashMap<String, String>();

    /**
     * Mapping between the pair (messageID, link position) and the actual link
     * in the string representation of the chat message.
     */
    private Map<String, String> msgIDandPositionToLink
        = new ConcurrentHashMap<String, String>();

    /**
     * Mapping between link and replacement for this link that is acquired
     * from it's corresponding <tt>ReplacementService</tt>.
     */
    private Map<String, String> linkToReplacement
        = new ConcurrentHashMap<String, String>();

    /**
     * The id of the message that is currently associated with this dialog.
     */
    private String currentMessageID = "";

    /**
     * The position of the link in the current message.
     */
    private String currentLinkPosition = "";

    /**
     * Creates an instance of <tt>ShowPreviewDialog</tt>
     * @param chatPanel The <tt>ChatConversationPanel</tt> that is associated
     * with this dialog.
     */
    ShowPreviewDialog(final ChatConversationPanel chatPanel)
    {
        this.chatPanel = chatPanel;

        this.setTitle(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHOW_PREVIEW_DIALOG_TITLE"));
        okButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.OK"));
        cancelButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        //mainPanel.setPreferredSize(new Dimension(200, 150));
        this.getContentPane().add(mainPanel);

        JTextPane descriptionMsg = new JTextPane();
        descriptionMsg.setEditable(false);
        descriptionMsg.setOpaque(false);
        descriptionMsg.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.SHOW_PREVIEW_WARNING_DESCRIPTION"));

        Icon warningIcon = null;
        try
        {
            warningIcon =
                new ImageIcon(
                    ImageIO.read(GuiActivator.getResources().getImageURL(
                        "service.gui.icons.WARNING_ICON")));
        }
        catch (IOException e)
        {
            logger.debug("failed to load the warning icon");
        }
        JLabel warningSign = new JLabel(warningIcon);

        JPanel warningPanel = new TransparentPanel();
        warningPanel.setLayout(new BoxLayout(warningPanel, BoxLayout.X_AXIS));
        warningPanel.add(warningSign);
        warningPanel.add(Box.createHorizontalStrut(10));
        warningPanel.add(descriptionMsg);

        configureReplacement
            = new SIPCommCheckBox(
                GuiActivator.getResources().getI18NString(
                    "plugin.chatconfig.replacement.CONFIGURE_REPLACEMENT"));

        JPanel checkBoxPanel = new TransparentPanel(
            new FlowLayout(FlowLayout.CENTER));
        checkBoxPanel.add(configureReplacement);

        JPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        mainPanel.add(warningPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(checkBoxPanel);
        mainPanel.add(buttonsPanel);

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        this.setPreferredSize(new Dimension(390, 230));
    }

    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        if (arg0.getSource().equals(okButton))
        {
            SwingWorker worker = new SwingWorker()
            {
                /**
                 * Called on the event dispatching thread
                 * (not on the worker thread) after the 
                 * <code>construct</code> method has returned.
                 */
                @Override
                public void finished()
                {
                    String newChatString = (String)get();

                    if (newChatString != null)
                    {
                        try
                        {
                            Element elem =
                                chatPanel.document.getElement(currentMessageID);
                            chatPanel.document.setOuterHTML(
                                elem, newChatString);
                            msgIDToChatString.put(
                                currentMessageID, newChatString);
                        }
                        catch (BadLocationException ex)
                        {
                            logger.error("Could not replace chat message", ex);
                        }
                        catch (IOException ex)
                        {
                            logger.error("Could not replace chat message", ex);
                        }
                    }
                }

                @Override
                protected Object construct() throws Exception
                {
                    String newChatString
                        = msgIDToChatString.get(currentMessageID);
                    try
                    {
                        String originalLink = msgIDandPositionToLink.get
                            (currentMessageID + "#" + currentLinkPosition);
                        String replacementLink
                            = linkToReplacement.get(originalLink);
                        String replacement;
                        DirectImageReplacementService source
                            = GuiActivator.getDirectImageReplacementSource();
                        if (originalLink.equals(replacementLink) &&
                            (!source.isDirectImage(originalLink) ||
                                source.getImageSize(originalLink) == -1))
                        {
                            replacement = originalLink;
                        }
                        else
                        {
                            replacement =
                                "<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\""
                                + replacementLink + "\" BORDER=\"0\" ALT=\""
                                + originalLink + "\"></IMG>";
                        }

                        String old = originalLink + "</A> <A href=\"jitsi://"
                            + ShowPreviewDialog.this.getClass().getName()
                            + "/SHOWPREVIEW?" + currentMessageID + "#"
                            + currentLinkPosition + "\">"
                            + GuiActivator.getResources().
                            getI18NString("service.gui.SHOW_PREVIEW");

                        newChatString = newChatString.replace(old, replacement);
                    }
                    catch (Exception ex)
                    {
                        logger.error("Could not replace chat message", ex);
                    }
                    return newChatString;
                }
            };
            worker.start();
            this.setVisible(false);
        }
        else if (arg0.getSource().equals(cancelButton))
        {
            this.setVisible(false);
        }

        // Shows chat config panel
        if(configureReplacement.isSelected())
        {
            ConfigurationContainer configContainer
                = GuiActivator.getUIService().getConfigurationContainer();

            ConfigurationForm chatConfigForm =
                ChatConversationPanel.getChatConfigForm();

            if(chatConfigForm != null)
            {
                configContainer.setSelected(chatConfigForm);

                configContainer.setVisible(true);
            }

            // reset for next dialog appearance
            configureReplacement.setSelected(false);
        }
    }

    @Override
    public void chatLinkClicked(URI url)
    {
        String action = url.getPath();
        if (action.equals("/SHOWPREVIEW"))
        {
            currentMessageID = url.getQuery();
            currentLinkPosition = url.getFragment();

            this.setVisible(true);
            this.setLocationRelativeTo(chatPanel);
        }
    }

    /**
     * Returns mapping between messageID and the string representation of
     * the chat message.
     * @return mapping between messageID and chat string.
     */
    Map<String, String> getMsgIDToChatString()
    {
        return msgIDToChatString;
    }

    /**
     * Returns mapping between the pair (messageID, link position) and the
     * actual link in the string representation of the chat message.
     * @return mapping between (messageID, linkPosition) and link.
     */
    Map<String, String> getMsgIDandPositionToLink()
    {
        return msgIDandPositionToLink;
    }

    /**
     * Returns mapping between link and replacement for this link that was
     * acquired from it's corresponding <tt>ReplacementService</tt>.
     * @return mapping between link and it's corresponding replacement.
     */
    Map<String, String> getLinkToReplacement()
    {
        return linkToReplacement;
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key.
     *
     * @param escaped <tt>true</tt> if this frame has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escaped)
    {
        cancelButton.doClick();
    }
}
