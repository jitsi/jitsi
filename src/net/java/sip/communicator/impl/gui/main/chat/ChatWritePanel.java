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
import java.awt.Container;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

import org.apache.commons.lang3.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>ChatWritePanel</tt> is the panel, where user writes her messages.
 * It is located at the bottom of the split in the <tt>ChatPanel</tt> and it
 * contains an editor, where user writes the text.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ChatWritePanel
    extends TransparentPanel
    implements  ActionListener,
                KeyListener,
                MouseListener,
                UndoableEditListener,
                DocumentListener,
                PluginComponentListener,
                Skinnable,
                ChatSessionChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>ChatWritePanel</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ChatWritePanel.class);

    private final JEditorPane editorPane = new JEditorPane();

    private final UndoManager undo = new UndoManager();

    private final ChatPanel chatPanel;

    private final Timer stoppedTypingTimer = new Timer(2 * 1000, this);

    private final Timer typingTimer = new Timer(5 * 1000, this);

    private int typingState = OperationSetTypingNotifications.STATE_STOPPED;

    private WritePanelRightButtonMenu rightButtonMenu;

    private final ArrayList<ChatMenuListener> menuListeners
        = new ArrayList<ChatMenuListener>();

    private final SIPCommScrollPane scrollPane = new SIPCommScrollPane();

    private ChatTransportSelectorBox transportSelectorBox;

    private final Container centerPanel;

    private SIPCommToggleButton smsButton;

    private JLabel smsCharCountLabel;

    private JLabel smsNumberLabel;

    private int smsNumberCount = 1;

    private int smsCharCount = 160;

    private boolean smsMode = false;

    /**
     * Mode where we do not mix sending im and sms in one chat window.
     */
    private boolean disableMergedSmsMode = false;

    /**
     * Property to control merge sms mode.
     */
    private static final String MERGE_SMS_MODE_DISABLED_PROP
        = "net.java.sip.communicator.impl.gui.MERGE_SMS_MODE_DISABLED";

    /**
     * A timer used to reset the transport resource to the bare ID if there was
     * no activity from this resource since a bunch of time.
     */
    private java.util.Timer outdatedResourceTimer = null;

    /**
     * Tells if the current resource is outdated. A timer has already been
     * triggered, but when there is only a single resource there is no bare ID
     * available. Thus, flag this resource as outdated to switch to the bare ID
     * when available.
     */
    private boolean isOutdatedResource = true;

    /**
     * List of plugin components that are registered for updates.
     */
    private List<PluginComponent> pluginComponents = Collections
        .synchronizedList(new ArrayList<PluginComponent>());

    /**
     * Creates an instance of <tt>ChatWritePanel</tt>.
     *
     * @param panel The parent <tt>ChatPanel</tt>.
     */
    public ChatWritePanel(ChatPanel panel)
    {
        super(new BorderLayout());

        this.chatPanel = panel;

        centerPanel = createCenter();

        int chatAreaSize = ConfigurationUtils.getChatWriteAreaSize();
        Dimension writeMessagePanelDefaultSize
            = new Dimension(500, (chatAreaSize > 0) ? chatAreaSize : 28);
        Dimension writeMessagePanelMinSize = new Dimension(500, 28);
        Dimension writeMessagePanelMaxSize = new Dimension(500, 100);

        setMinimumSize(writeMessagePanelMinSize);
        setMaximumSize(writeMessagePanelMaxSize);
        setPreferredSize(writeMessagePanelDefaultSize);

        this.add(centerPanel, BorderLayout.CENTER);

        this.rightButtonMenu
            = new WritePanelRightButtonMenu(chatPanel.getChatContainer());

        this.typingTimer.setRepeats(true);

        // initialize send command to Ctrl+Enter
        ConfigurationService configService =
            GuiActivator.getConfigurationService();

        disableMergedSmsMode = configService.getBoolean(
            MERGE_SMS_MODE_DISABLED_PROP, disableMergedSmsMode);

        String messageCommandProperty =
            "service.gui.SEND_MESSAGE_COMMAND";
        String messageCommand = configService.getString(messageCommandProperty);

        if(messageCommand == null)
            messageCommand =
                GuiActivator.getResources().
                    getSettingsString(messageCommandProperty);

        this.changeSendCommand((messageCommand == null || messageCommand
            .equalsIgnoreCase("enter")));

        if(ConfigurationUtils.isFontSupportEnabled())
            initDefaultFontConfiguration();
    }

    /**
     * Initializes the default font configuration for this chat write area.
     */
    private void initDefaultFontConfiguration()
    {
        String fontFamily = ConfigurationUtils.getChatDefaultFontFamily();
        int fontSize = ConfigurationUtils.getChatDefaultFontSize();

        // Font family and size
        if (fontFamily != null && fontSize > 0)
            setFontFamilyAndSize(fontFamily, fontSize);

        // Font style
        setBoldStyleEnable(ConfigurationUtils.isChatFontBold());
        setItalicStyleEnable(ConfigurationUtils.isChatFontItalic());
        setUnderlineStyleEnable(ConfigurationUtils.isChatFontUnderline());

        // Font color
        Color fontColor = ConfigurationUtils.getChatDefaultFontColor();

        if (fontColor != null)
            setFontColor(fontColor);
    }

    /**
     * Creates the center panel.
     *
     * @return the created center panel
     */
    private Container createCenter()
    {
        JPanel centerPanel = new JPanel(new GridBagLayout());

        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 3));

        GridBagConstraints constraints = new GridBagConstraints();

        initSmsLabel(centerPanel);
        initTextArea(centerPanel);

        smsCharCountLabel = new JLabel(String.valueOf(smsCharCount));
        smsCharCountLabel.setForeground(Color.GRAY);
        smsCharCountLabel.setVisible(false);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 2, 0, 2);
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        centerPanel.add(smsCharCountLabel, constraints);

        smsNumberLabel = new JLabel(String.valueOf(smsNumberCount))
        {
            @Override
            public void paintComponent(Graphics g)
            {
                AntialiasingManager.activateAntialiasing(g);
                g.setColor(getBackground());
                g.fillOval(0, 0, getWidth(), getHeight());

                super.paintComponent(g);
            }
        };
        smsNumberLabel.setHorizontalAlignment(JLabel.CENTER);
        smsNumberLabel.setPreferredSize(new Dimension(18, 18));
        smsNumberLabel.setMinimumSize(new Dimension(18, 18));
        smsNumberLabel.setForeground(Color.WHITE);
        smsNumberLabel.setBackground(Color.GRAY);
        smsNumberLabel.setVisible(false);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 2, 0, 2);
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        centerPanel.add(smsNumberLabel, constraints);

        return centerPanel;
    }

    /**
     * Initializes the sms menu.
     *
     * @param centerPanel the parent panel
     */
    private void initSmsLabel(final JPanel centerPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 3, 0, 0);

        ImageID smsIcon =
            new ImageID("service.gui.icons.SEND_SMS");

        ImageID selectedIcon =
            new ImageID("service.gui.icons.SEND_SMS_SELECTED");

        smsButton = new SIPCommToggleButton(
                    ImageLoader.getImage(smsIcon),
                    ImageLoader.getImage(selectedIcon),
                    ImageLoader.getImage(smsIcon),
                    ImageLoader.getImage(selectedIcon));

        smsButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(smsMode && !isIMAllowed())
                {
                    return;
                }

                smsMode = smsButton.isSelected();

                Color bgColor;
                if (smsMode)
                {
                    bgColor = new Color(GuiActivator.getResources()
                        .getColor("service.gui.LIST_SELECTION_COLOR"));

                    smsCharCountLabel.setVisible(true);
                    smsNumberLabel.setVisible(true);
                }
                else
                {
                    bgColor = Color.WHITE;
                    smsCharCountLabel.setVisible(false);
                    smsNumberLabel.setVisible(false);
                }

                centerPanel.setBackground(bgColor);
                editorPane.setBackground(bgColor);
            }
        });


        // We hide the sms label until we know if the chat supports sms.
        smsButton.setVisible(false);

        centerPanel.add(smsButton, constraints);
    }

    private void initTextArea(JPanel centerPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();

        editorPane.setContentType("text/html");
        editorPane.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setCaretPosition(0);
        editorPane.setEditorKit(new SIPCommHTMLEditorKit(this));
        editorPane.getDocument().addUndoableEditListener(this);
        editorPane.getDocument().addDocumentListener(this);
        editorPane.addKeyListener(this);
        editorPane.addMouseListener(this);
        editorPane.setCursor(
            Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        editorPane.setDragEnabled(true);
        editorPane.setTransferHandler(new ChatTransferHandler(chatPanel));

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        scrollPane.setViewportView(editorPane);

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.weighty = 1f;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(scrollPane, constraints);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        /*
         * Stop the Timers because they're implicitly globally referenced and
         * thus don't let them retain this instance.
         */
        typingTimer.stop();
        typingTimer.removeActionListener(this);
        stoppedTypingTimer.stop();
        stoppedTypingTimer.removeActionListener(this);
        if (typingState != OperationSetTypingNotifications.STATE_STOPPED)
            stopTypingTimer();

        if(outdatedResourceTimer != null)
        {
            outdatedResourceTimer.cancel();
            outdatedResourceTimer.purge();
            outdatedResourceTimer = null;
        }

        editorPane.removeKeyListener(this);
        menuListeners.clear();

        if(rightButtonMenu != null)
        {
            rightButtonMenu.dispose();
            rightButtonMenu = null;
        }

        scrollPane.dispose();
    }

    /**
     * Returns the editor panel, contained in this <tt>ChatWritePanel</tt>.
     *
     * @return The editor panel, contained in this <tt>ChatWritePanel</tt>.
     */
    public JEditorPane getEditorPane()
    {
        return editorPane;
    }

    /**
     * Replaces the Ctrl+Enter send command with simple Enter.
     *
     * @param isEnter indicates if the new send command is enter or cmd-enter
     */
    public void changeSendCommand(boolean isEnter)
    {
        ActionMap actionMap = editorPane.getActionMap();
        actionMap.put("send", new SendMessageAction());
        actionMap.put("newLine", new NewLineAction());

        InputMap im = this.editorPane.getInputMap();

        if (isEnter)
        {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.SHIFT_DOWN_MASK), "newLine");

            this.setToolTipText(
                "<html>"
                    + GuiActivator.getResources()
                        .getI18NString("service.gui.SEND_MESSAGE")
                    + " - Enter <br> "
                    + "Use Ctrl-Enter or Shift-Enter to make a new line"
                    + "</html>");
        }
        else
        {
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newLine");

            this.setToolTipText(
                    GuiActivator.getResources()
                        .getI18NString("service.gui.SEND_MESSAGE")
                        + " Ctrl-Enter");
        }
    }

    /**
     * Enables/disables the sms mode.
     *
     * @param selected <tt>true</tt> to enable sms mode, <tt>false</tt> -
     * otherwise
     */
    public void setSmsSelected(boolean selected)
    {
        if(disableMergedSmsMode && isIMAllowed())
            return;

        if((selected && !smsButton.isSelected())
            || (!selected && smsButton.isSelected()))
        {
            smsButton.doClick();
        }
    }

    /**
     * Returns <tt>true</tt> if the sms mode is enabled, otherwise returns
     * <tt>false</tt>.
     * @return <tt>true</tt> if the sms mode is enabled, otherwise returns
     * <tt>false</tt>
     */
    public boolean isSmsSelected()
    {
        return smsMode;
    }

    /**
     * Checks if sending IM message is allowed. When in sms mode, it
     * can be the only method to send message. We will disable sms -> im
     * switching.
     * @return is IM allowed.
     */
    private boolean isIMAllowed()
    {
        // check are we allowed to change back to im mode
        Object descr = chatPanel.getChatSession().getDescriptor();

        if(descr instanceof MetaContact)
        {
            List<Contact> imContact
                = ((MetaContact)descr).getContactsForOperationSet(
                OperationSetBasicInstantMessaging.class);

            if(imContact == null || imContact.size() == 0)
                return false;
        }
        else if(descr instanceof SourceContact)
        {
            List<ContactDetail> imContact
                = ((SourceContact)descr).getContactDetails(
                OperationSetBasicInstantMessaging.class);

            if(imContact == null || imContact.size() == 0)
                return false;
        }

        return true;
    }

    /**
     * The <tt>SendMessageAction</tt> is an <tt>AbstractAction</tt> that
     * sends the text that is currently in the write message area.
     */
    private class SendMessageAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            // chatPanel.stopTypingNotifications();
            chatPanel.sendButtonDoClick();
        }
    }

    /**
     * The <tt>NewLineAction</tt> is an <tt>AbstractAction</tt> that types
     * an enter in the write message area.
     */
    private class NewLineAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            int caretPosition = editorPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) editorPane.getDocument();

            try
            {
                doc.insertString(caretPosition, "\n", null);
            }
            catch (BadLocationException e1)
            {
                logger.error("Could not insert <br> to the document.", e1);
            }

            editorPane.setCaretPosition(caretPosition + 1);
        }
    }

    /**
     * Handles the <tt>UndoableEditEvent</tt>, by adding the content edit to
     * the <tt>UndoManager</tt>.
     *
     * @param e The <tt>UndoableEditEvent</tt>.
     */
    public void undoableEditHappened(UndoableEditEvent e)
    {
        this.undo.addEdit(e.getEdit());
    }

    /**
     * Implements the undo operation.
     */
    private void undo()
    {
        try
        {
            undo.undo();
        }
        catch (CannotUndoException e)
        {
            logger.error("Unable to undo.", e);
        }
    }

    /**
     * Implements the redo operation.
     */
    private void redo()
    {
        try
        {
            undo.redo();
        }
        catch (CannotRedoException e)
        {
            logger.error("Unable to redo.", e);
        }
    }

    /**
     * Sends typing notifications when user types.
     *
     * @param e the event.
     */
    public void keyTyped(KeyEvent e)
    {
        if (ConfigurationUtils.isSendTypingNotifications() && !smsMode)
        {
            if (typingState != OperationSetTypingNotifications.STATE_TYPING)
            {
                stoppedTypingTimer.setDelay(2 * 1000);
                typingState = OperationSetTypingNotifications.STATE_TYPING;

                int result = chatPanel.getChatSession()
                    .getCurrentChatTransport()
                        .sendTypingNotification(typingState);

                if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                    typingTimer.start();
            }

            if (!stoppedTypingTimer.isRunning())
                stoppedTypingTimer.start();
            else
                stoppedTypingTimer.restart();
        }
    }

    /**
     * When CTRL+Z is pressed invokes the <code>ChatWritePanel.undo()</code>
     * method, when CTRL+R is pressed invokes the
     * <code>ChatWritePanel.redo()</code> method.
     *
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyPressed(KeyEvent e)
    {
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
            && (e.getKeyCode() == KeyEvent.VK_Z)
            // And not ALT(right ALT gives CTRL + ALT).
            && (e.getModifiers() & KeyEvent.ALT_MASK) != KeyEvent.ALT_MASK)
        {
            if (undo.canUndo())
                undo();
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK
            && (e.getKeyCode() == KeyEvent.VK_R)
            // And not ALT(right ALT gives CTRL + ALT).
            && (e.getModifiers() & KeyEvent.ALT_MASK) != KeyEvent.ALT_MASK)
        {
            if (undo.canRedo())
                redo();
        }
        else if(e.getKeyCode() == KeyEvent.VK_TAB)
        {
            if(!(chatPanel.getChatSession() instanceof ConferenceChatSession))
                return;

            e.consume();
            int index = ((JEditorPane)e.getSource()).getCaretPosition();

            StringBuffer message = new StringBuffer(chatPanel.getMessage());

            int position = index-1;

            while (position > 0 && (message.charAt(position) != ' '))
            {
                position--;
            }

            if(position != 0)
                position++;

            String sequence = message.substring(position, index);
            
            if (sequence.length() <= 0)
            {
                // Do not look for matching contacts if the matching pattern is
                // 0 chars long, since all contacts will match.
                return;
            }
            
            Iterator<ChatContact<?>> iter = chatPanel.getChatSession()
                                             .getParticipants();
            ArrayList<String> contacts = new ArrayList<String>();
            while(iter.hasNext())
            {
                ChatContact<?> c = iter.next();
                if(c.getName().length() >= (index-position) &&
                    c.getName().substring(0,index-position).equals(sequence))
                {
                    message.replace(position, index, c.getName()
                        .substring(0,index-position));
                    contacts.add(c.getName());
                }
            }

            if(contacts.size() > 1)
            {
                char key = contacts.get(0).charAt(index-position-1);
                int pos = index-position-1;
                boolean flag = true;

                while(flag)
                {
                    try
                    {
                        for(String name : contacts)
                        {
                            if(key != name.charAt(pos))
                            {
                                flag = false;
                            }
                        }

                        if(flag)
                        {
                            pos++;
                            key = contacts.get(0).charAt(pos);
                        }
                    }
                    catch(IndexOutOfBoundsException exp)
                    {
                        flag = false;
                    }
                }

                message.replace(position, index, contacts.get(0)
                    .substring(0,pos));

                Iterator<String> contactIter = contacts.iterator();
                String contactList = "<DIV align='left'><h5>";
                while(contactIter.hasNext())
                {
                    contactList += contactIter.next() + " ";
                }
                contactList += "</h5></DIV>";

                chatPanel.getChatConversationPanel()
                    .appendMessageToEnd(contactList,
                        ChatHtmlUtils.HTML_CONTENT_TYPE);
            }
            else if(contacts.size() == 1)
            {
                String limiter = (position == 0) ? ": " : "";
                message.replace(position, index, contacts.get(0) + limiter);
            }

            try
            {
                ((JEditorPane)e.getSource()).getDocument().remove(0,
                    ((JEditorPane)e.getSource()).getDocument().getLength());
                ((JEditorPane)e.getSource()).getDocument().insertString(0,
                    message.toString(), null);
            }
            catch (BadLocationException ex)
            {
                ex.printStackTrace();
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            // Only enters editing mode if the write panel is empty in
            // order not to lose the current message contents, if any.
            if (this.chatPanel.getLastSentMessageUID() != null
                    && this.chatPanel.isWriteAreaEmpty())
            {
                this.chatPanel.startLastMessageCorrection();
                e.consume();
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            if (chatPanel.isMessageCorrectionActive())
            {
                Document doc = editorPane.getDocument();
                if (editorPane.getCaretPosition() == doc.getLength())
                {
                    chatPanel.stopMessageCorrection();
                }
            }
        }
    }

    public void keyReleased(KeyEvent e) {}

    /**
     * Performs actions when typing timer has expired.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (typingTimer.equals(source))
        {
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                chatPanel.getChatSession().getCurrentChatTransport()
                    .sendTypingNotification(
                        OperationSetTypingNotifications.STATE_TYPING);
            }
        }
        else if (stoppedTypingTimer.equals(source))
        {
            typingTimer.stop();
            if (typingState == OperationSetTypingNotifications.STATE_TYPING)
            {
                try
                {
                    typingState = OperationSetTypingNotifications.STATE_PAUSED;

                    int result = chatPanel.getChatSession()
                        .getCurrentChatTransport().
                            sendTypingNotification(typingState);

                    if (result
                            == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
                        stoppedTypingTimer.setDelay(3 * 1000);
                }
                catch (Exception ex)
                {
                    logger.error("Failed to send typing notifications.", ex);
                }
            }
            else if (typingState
                        == OperationSetTypingNotifications.STATE_PAUSED)
            {
                stopTypingTimer();
            }
        }
    }

    /**
     * Stops the timer and sends a notification message.
     */
    public void stopTypingTimer()
    {
        typingState = OperationSetTypingNotifications.STATE_STOPPED;

        int result = chatPanel.getChatSession().getCurrentChatTransport()
            .sendTypingNotification(typingState);

        if (result == ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT)
            stoppedTypingTimer.stop();
    }

    /**
     * Opens the <tt>WritePanelRightButtonMenu</tt> when user clicks with the
     * right mouse button on the editor area.
     *
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    public void mouseClicked(MouseEvent e)
    {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());

            //SPELLCHECK
            ArrayList <JMenuItem> contributedMenuEntries
                = new ArrayList<JMenuItem>();

            for(ChatMenuListener listener : this.menuListeners)
            {
                contributedMenuEntries.addAll(
                    listener.getMenuElements(this.chatPanel, e));
            }

            for(JMenuItem item : contributedMenuEntries)
            {
                rightButtonMenu.add(item);
            }

            JPopupMenu rightMenu
                = rightButtonMenu.makeMenu(contributedMenuEntries);
            rightMenu.setInvoker(editorPane);
            rightMenu.setLocation(p.x, p.y);
            rightMenu.setVisible(true);
        }
    }

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    /**
     * Returns the <tt>WritePanelRightButtonMenu</tt> opened in this panel.
     * Used by the <tt>ChatWindow</tt>, when the ESC button is pressed, to
     * check if there is an open menu, which should be closed.
     *
     * @return the <tt>WritePanelRightButtonMenu</tt> opened in this panel
     */
    public WritePanelRightButtonMenu getRightButtonMenu()
    {
        return rightButtonMenu;
    }

    /**
     * Returns the write area text as an html text.
     *
     * @return the write area text as an html text.
     */
    public String getTextAsHtml()
    {
        String msgText = editorPane.getText();

        String formattedString = msgText.replaceAll(
            "<html>|<head>|<body>|</html>|</head>|</body>", "");

        formattedString = extractFormattedText(formattedString);
        // Returned string is formatted with newlines, etc. so we need to get
        // rid of them before checking for the ending <br/>.
        formattedString = formattedString.trim();

        if (formattedString.endsWith("<BR/>"))
            formattedString = formattedString
            .substring(0, formattedString.lastIndexOf("<BR/>"));

        return formattedString;
    }

    /**
     * Returns the write area text as a plain text without any formatting.
     *
     * @return the write area text as a plain text without any formatting.
     */
    public String getText()
    {
        try
        {
            Document doc = editorPane.getDocument();

            return doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            logger.error("Could not obtain write area text.", e);
        }

        return null;
    }


    /**
     * Clears write message area.
     */
    public void clearWriteArea()
    {
        try
        {
            this.editorPane.getDocument()
                .remove(0, editorPane.getDocument().getLength());

            if(smsMode)
            {
                // use this to reset sms counter
                setSmsLabelVisible(true);
                // set the reset values
                smsCharCountLabel.setText(String.valueOf(smsCharCount));
                smsNumberLabel.setText(String.valueOf(smsNumberCount));
            }
        }
        catch (BadLocationException e)
        {
            logger.error("Failed to obtain write panel document content.", e);
        }
    }

    /**
     * Appends the given text to the end of the contained HTML document. This
     * method is used to insert smileys when user selects a smiley from the
     * menu.
     *
     * @param text the text to append.
     */
    public void appendText(String text)
    {
        HTMLDocument doc = (HTMLDocument) editorPane.getDocument();

        Element currentElement
            = doc.getCharacterElement(editorPane.getCaretPosition());

        try
        {
            doc.insertAfterEnd(currentElement, text);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }

        this.editorPane.setCaretPosition(doc.getLength());
    }

    /**
     * Return all html paragraph content separated by <BR/> tags.
     *
     * @param msgText the html text.
     * @return the string containing only paragraph content.
     */
    private String extractFormattedText(String msgText)
    {
        String resultString = msgText.replaceAll("<p\\b[^>]*>", "");

        return resultString.replaceAll("<\\/p>", "<BR/>");
    }

    /**
     * Initializes the send via label and selector box.
     *
     * @return the chat transport selector box
     */
    private Component createChatTransportSelectorBox()
    {
        // Initialize the "send via" selector box and adds it to the send panel.
        if (transportSelectorBox == null)
        {
            transportSelectorBox = new ChatTransportSelectorBox(
                chatPanel,
                chatPanel.getChatSession(),
                chatPanel.getChatSession().getCurrentChatTransport());

            if((ConfigurationUtils.isHideAccountSelectionWhenPossibleEnabled()
                && transportSelectorBox.getMenu().getItemCount() <= 1)
                || !isIMAllowed())
                transportSelectorBox.setVisible(false);
        }

        return transportSelectorBox;
    }

    /**
     *
     * @param isVisible
     */
    public void setTransportSelectorBoxVisible(boolean isVisible)
    {
        if (isVisible)
        {
            if (transportSelectorBox == null)
            {
                createChatTransportSelectorBox();

                if (!transportSelectorBox.getMenu().isEnabled())
                {
                    // Show a message to the user that IM is not possible.
                    chatPanel.getChatConversationPanel().appendMessageToEnd(
                        "<h5>"
                            + StringEscapeUtils.escapeHtml4(GuiActivator
                                .getResources().getI18NString(
                                    "service.gui.MSG_NOT_POSSIBLE")) + "</h5>",
                        ChatHtmlUtils.HTML_CONTENT_TYPE);
                }
                else
                {
                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.anchor = GridBagConstraints.NORTHEAST;
                    constraints.fill = GridBagConstraints.NONE;
                    constraints.gridx = 0;
                    constraints.gridy = 0;
                    constraints.weightx = 0f;
                    constraints.weighty = 0f;
                    constraints.gridheight = 1;
                    constraints.gridwidth = 1;

                    centerPanel.add(transportSelectorBox, constraints, 0);
                }
            }
            else
            {
                if( ConfigurationUtils
                        .isHideAccountSelectionWhenPossibleEnabled()
                    && transportSelectorBox.getMenu().getItemCount() <= 1)
                {
                    transportSelectorBox.setVisible(false);
                }
                {
                    transportSelectorBox.setVisible(true);
                }
                centerPanel.repaint();
            }
        }
        else if (transportSelectorBox != null)
        {
            transportSelectorBox.setVisible(false);
            centerPanel.repaint();
        }
    }

    /**
     * Selects the given chat transport in the send via box.
     *
     * @param chatTransport The chat transport to be selected.
     * @param isMessageOrFileTransferReceived Boolean telling us if this change
     * of the chat transport correspond to an effective switch to this new
     * transform (a mesaage received from this transport, or a file transfer
     * request received, or if the resource timeouted), or just a status update
     * telling us a new chatTransport is now available (i.e. another device has
     * startup).
     */
    public void setSelectedChatTransport(
            final ChatTransport chatTransport,
            final boolean isMessageOrFileTransferReceived)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelectedChatTransport(
                        chatTransport,
                        isMessageOrFileTransferReceived);
                }
            });
            return;
        }

        // Check if this contact provider can manages several resources and thus
        // provides a resource timeout via the basic IM operation set.
        long timeout = -1;
        OperationSetBasicInstantMessaging opSetBasicIM
            = chatTransport.getProtocolProvider().getOperationSet(
                    OperationSetBasicInstantMessaging.class);
        if(opSetBasicIM != null)
        {
            timeout = opSetBasicIM.getInactivityTimeout();
        }

        if(isMessageOrFileTransferReceived)
        {
            isOutdatedResource = false;
        }

        // If this contact supports several resources, then schedule the timer:
        // - If the resource is outdated, then trigger the timer now (to try to
        // switch to the bare ID if now available).
        // - If the new reousrce transport is really effective (i.e. we have
        // received a message from this resource).
        if(timeout != -1
                && (isMessageOrFileTransferReceived || isOutdatedResource))
        {
            // If there was already a timeout, but the bare ID was not available
            // (i.e. a single resource present). Then call the timeout procedure
            // now in order to switch to the bare ID.
            if(isOutdatedResource)
            {
                timeout = 0;
            }
            // Cancels the preceding timer.
            if(outdatedResourceTimer != null)
            {
                outdatedResourceTimer.cancel();
                outdatedResourceTimer.purge();
            }
            // Schedules the timer.
            if(chatTransport.getResourceName() != null)
            {
                OutdatedResourceTimerTask task
                    = new OutdatedResourceTimerTask();
                outdatedResourceTimer = new java.util.Timer();
                outdatedResourceTimer.schedule(task, timeout);
            }
        }

        // Sets the new resource transport is really effective (i.e. we have
        // received a message from this resource).
        // if we do not have any selected resource, or the currently selected
        // if offline
        if(transportSelectorBox != null
            && (isMessageOrFileTransferReceived
                || (!transportSelectorBox.hasSelectedTransport()
                    || !chatPanel.getChatSession().getCurrentChatTransport()
                            .getStatus().isOnline())))
        {
            transportSelectorBox.setSelected(chatTransport);
        }
    }

    /**
     * Adds the given chatTransport to the given send via selector box.
     *
     * @param chatTransport the transport to add
     */
    public void addChatTransport(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addChatTransport(chatTransport);
                }
            });
            return;
        }

        if (transportSelectorBox != null)
        {
            transportSelectorBox.addChatTransport(chatTransport);

            // it was hidden cause we wanted to hide when there is only one
            // provider
            if(!transportSelectorBox.isVisible()
                && ConfigurationUtils
                           .isHideAccountSelectionWhenPossibleEnabled()
                && transportSelectorBox.getMenu().getItemCount() > 1)
            {
                transportSelectorBox.setVisible(true);
            }
        }
    }

    /**
     * Updates the status of the given chat transport in the send via selector
     * box and notifies the user for the status change.
     * @param chatTransport the <tt>chatTransport</tt> to update
     */
    public void updateChatTransportStatus(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateChatTransportStatus(chatTransport);
                }
            });
            return;
        }

        if (transportSelectorBox != null)
            transportSelectorBox.updateTransportStatus(chatTransport);
    }

    /**
     * Opens the selector box containing the protocol contact icons.
     * This is the menu, where user could select the protocol specific
     * contact to communicate through.
     */
    public void openChatTransportSelectorBox()
    {
        transportSelectorBox.getMenu().doClick();
    }

    /**
     * Removes the given chat status state from the send via selector box.
     *
     * @param chatTransport the transport to remove
     */
    public void removeChatTransport(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeChatTransport(chatTransport);
                }
            });
            return;
        }

        if (transportSelectorBox != null)
            transportSelectorBox.removeChatTransport(chatTransport);

        if(transportSelectorBox != null
            && transportSelectorBox.getMenu().getItemCount() == 1
            && ConfigurationUtils.isHideAccountSelectionWhenPossibleEnabled())
        {
            transportSelectorBox.setVisible(false);
        }
    }

    /**
     * Show the sms menu.
     * @param isVisible <tt>true</tt> to show the sms menu, <tt>false</tt> -
     * otherwise
     */
    public void setSmsLabelVisible(boolean isVisible)
    {
        if(disableMergedSmsMode && isIMAllowed())
            return;

        // Re-init sms count properties.
        smsCharCount = 160;
        smsNumberCount = 1;

        smsButton.setVisible(isVisible);

        centerPanel.repaint();
    }

    /**
     * Saves the given font configuration as default, thus making it the default
     * configuration for all chats.
     *
     * @param fontFamily the font family
     * @param fontSize the font size
     * @param isBold indicates if the font is bold
     * @param isItalic indicates if the font is italic
     * @param isUnderline indicates if the font is underline
     */
    public void saveDefaultFontConfiguration(   String fontFamily,
                                                int fontSize,
                                                boolean isBold,
                                                boolean isItalic,
                                                boolean isUnderline,
                                                Color color)
    {
        ConfigurationUtils.setChatDefaultFontFamily(fontFamily);
        ConfigurationUtils.setChatDefaultFontSize(fontSize);
        ConfigurationUtils.setChatFontIsBold(isBold);
        ConfigurationUtils.setChatFontIsItalic(isItalic);
        ConfigurationUtils.setChatFontIsUnderline(isUnderline);
        ConfigurationUtils.setChatDefaultFontColor(color);
    }

    /**
     * Sets the font family and size
     * @param family the family name
     * @param size the size
     */
    public void setFontFamilyAndSize(String family, int size)
    {
        // Family
        ActionEvent evt
            = new ActionEvent(  editorPane,
                                ActionEvent.ACTION_PERFORMED,
                                family);

        Action action = new StyledEditorKit.FontFamilyAction(family, family);
        action.actionPerformed(evt);

        // Size
        evt = new ActionEvent(editorPane,
            ActionEvent.ACTION_PERFORMED, Integer.toString(size));
        action = new StyledEditorKit.FontSizeAction(
                    Integer.toString(size), size);
        action.actionPerformed(evt);
    }

    /**
     * Enables the bold style
     * @param b TRUE enable - FALSE disable
     */
    public void setBoldStyleEnable(boolean b)
    {
        StyledEditorKit editorKit = (StyledEditorKit) editorPane.getEditorKit();
        MutableAttributeSet attr = editorKit.getInputAttributes();

        if (b && !StyleConstants.isBold(attr))
        {
            setStyleConstant(   new HTMLEditorKit.BoldAction(),
                                StyleConstants.Bold);
        }
    }

    /**
     * Enables the italic style
     * @param b TRUE enable - FALSE disable
     */
    public void setItalicStyleEnable(boolean b)
    {
        StyledEditorKit editorKit = (StyledEditorKit) editorPane.getEditorKit();
        MutableAttributeSet attr = editorKit.getInputAttributes();

        if (b && !StyleConstants.isItalic(attr))
        {
            setStyleConstant(   new HTMLEditorKit.ItalicAction(),
                                StyleConstants.Italic);
        }
    }

    /**
     * Enables the underline style
     * @param b TRUE enable - FALSE disable
     */
    public void setUnderlineStyleEnable(boolean b)
    {
        StyledEditorKit editorKit = (StyledEditorKit) editorPane.getEditorKit();
        MutableAttributeSet attr = editorKit.getInputAttributes();

        if (b && !StyleConstants.isUnderline(attr))
        {
            setStyleConstant(   new HTMLEditorKit.UnderlineAction(),
                                StyleConstants.Underline);
        }
    }

    /**
     * Sets the font color
     * @param color the color
     */
    public void setFontColor(Color color)
    {
        ActionEvent evt
            = new ActionEvent(editorPane, ActionEvent.ACTION_PERFORMED, "");

        Action action
            = new HTMLEditorKit.ForegroundAction(
                    Integer.toString(color.getRGB()),
                    color);

        action.actionPerformed(evt);
    }

    /**
     * Sets the given style constant.
     *
     * @param action the action
     * @param styleConstant the style constant
     */
    private void setStyleConstant(Action action, Object styleConstant)
    {
        ActionEvent event = new ActionEvent(editorPane,
                                            ActionEvent.ACTION_PERFORMED,
                                            styleConstant.toString());

        action.actionPerformed(event);
    }

    /**
     * Adds the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void addChatEditorMenuListener(ChatMenuListener l)
    {
        this.menuListeners.add(l);
    }

    /**
     * Removes the given {@link ChatMenuListener} to this <tt>Chat</tt>.
     * The <tt>ChatMenuListener</tt> is used to determine menu elements
     * that should be added on right clicks.
     *
     * @param l the <tt>ChatMenuListener</tt> to add
     */
    public void removeChatEditorMenuListener(ChatMenuListener l)
    {
        this.menuListeners.remove(l);
    }

    /**
     * Reloads menu.
     */
    public void loadSkin()
    {
        getRightButtonMenu().loadSkin();
    }

    public void changedUpdate(DocumentEvent documentevent) {}

    /**
     * Updates write panel size and adjusts sms properties if the sms menu
     * is visible.
     *
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent event)
    {
        // If we're in sms mode count the chars typed.
        if (smsButton.isVisible())
        {
            updateSmsCounters(event.getDocument().getLength());
        }
    }

    /**
     * Updates write panel size and adjusts sms properties if the sms menu
     * is visible.
     *
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent event)
    {
        // If we're in sms mode count the chars typed.
        if (smsButton.isVisible())
        {
            updateSmsCounters(event.getDocument().getLength());
        }
    }

    /**
     * Updates sms counters, 160 chars in one sms.
     * @param documentLength the current document length
     */
    private void updateSmsCounters(int documentLength)
    {
        smsCharCount = 160 - documentLength % 160;
        smsNumberCount = 1 + documentLength/160;

        smsCharCountLabel.setText(String.valueOf(smsCharCount));
        smsNumberLabel.setText(String.valueOf(smsNumberCount));
    }

    /**
     * Sets the background of the write area to the specified color.
     *
     * @param color The color to set the background to.
     */
    public void setEditorPaneBackground(Color color)
    {
        this.centerPanel.setBackground(color);
        this.editorPane.setBackground(color);
    }


    /**
     * The task called when the current transport resource timed-out (no
     * acitivty since a long time). Then this task resets the destination to the
     * bare id.
     */
    private class OutdatedResourceTimerTask
        extends TimerTask
    {
        /**
         * The action to be performed by this timer task.
         */
        public void run()
        {
            outdatedResourceTimer = null;

            if(chatPanel.getChatSession() != null)
            {
                Iterator<ChatTransport> transports
                    = chatPanel.getChatSession().getChatTransports();

                ChatTransport transport = null;
                while(transports.hasNext())
                {
                    transport = transports.next();

                    // We found the bare ID, then set it as the current resource
                    // transport.
                    // choose only online resources
                    if(transport.getResourceName() == null
                        && transport.getStatus().isOnline())
                    {
                        isOutdatedResource = false;
                        setSelectedChatTransport(transport, true);
                        return;
                    }
                }
            }

            // If there is no bare ID available, then set the current resource
            // transport as outdated.
            isOutdatedResource = true;
        }
    }

    /**
     * Initializes plug-in components for this container.
     */
    void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        Collection<ServiceReference<PluginComponentFactory>> serRefs;
        String osgiFilter
            = "(" + net.java.sip.communicator.service.gui.Container.CONTAINER_ID
                + "="
                + net.java.sip.communicator.service.gui.Container
                    .CONTAINER_CHAT_WRITE_PANEL.getID()
                + ")";

        try
        {
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        PluginComponentFactory.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            serRefs = null;
            logger.error("Could not obtain plugin reference.", ex);
        }
        if ((serRefs != null) && !serRefs.isEmpty())
        {
            for (ServiceReference<PluginComponentFactory> serRef : serRefs)
            {
                PluginComponentFactory factory
                    = GuiActivator.bundleContext.getService(serRef);
                PluginComponent component
                    = factory.getPluginComponentInstance(this);
                this.pluginComponents.add(component);

                ChatSession chatSession = chatPanel.getChatSession();

                if (chatSession != null)
                {
                    ChatTransport currentTransport
                        = chatSession.getCurrentChatTransport();
                    Object currentDescriptor = currentTransport.getDescriptor();

                    if (currentDescriptor instanceof Contact)
                    {
                        Contact contact = (Contact) currentDescriptor;

                        component.setCurrentContact(
                                contact,
                                currentTransport.getResourceName());
                    }
                }

                Object c = component.getComponent();

                if (c == null)
                    continue;

                GridBagConstraints constraints = new GridBagConstraints();

                constraints.anchor = GridBagConstraints.NORTHEAST;
                constraints.fill = GridBagConstraints.NONE;
                constraints.gridy = 0;
                constraints.gridheight = 1;
                constraints.weightx = 0f;
                constraints.weighty = 0f;
                constraints.insets = new Insets(0, 3, 0, 0);

                centerPanel.add((Component) c, constraints);
            }
        }
        GuiActivator.getUIService().addPluginComponentListener(this);
        this.centerPanel.repaint();
    }

    /**
     * Indicates that a new plugin component has been added. Adds it to this
     * container if it belongs to it.
     *
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();
        if (!factory.getContainer().equals(
                net.java.sip.communicator.service.
                    gui.Container.CONTAINER_CHAT_WRITE_PANEL))
            return;

        PluginComponent component = factory.getPluginComponentInstance(this);
        this.pluginComponents.add(component);

        ChatSession chatSession = chatPanel.getChatSession();
        if (chatSession != null)
        {
            ChatTransport currentTransport =
                chatSession.getCurrentChatTransport();
            Object currentDescriptor = currentTransport.getDescriptor();
            if (currentDescriptor instanceof Contact)
            {
                Contact contact = (Contact) currentDescriptor;

                component.setCurrentContact(
                    contact, currentTransport.getResourceName());
            }
        }

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 3, 0, 0);
        centerPanel.add((Component) component.getComponent(), constraints);

        this.centerPanel.repaint();
    }

    /**
     * Removes the according plug-in component from this container.
     * 
     * @param event the <tt>PluginComponentEvent</tt> that notified us
     */
    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponentFactory factory = event.getPluginComponentFactory();

        if (!factory.getContainer().equals(
            net.java.sip.communicator.service.
                gui.Container.CONTAINER_CHAT_WRITE_PANEL))
            return;

        Component c =
            (Component)factory.getPluginComponentInstance(this)
                .getComponent();
        this.pluginComponents.remove(c);

        this.centerPanel.remove(c);
        this.centerPanel.repaint();
    }

    /**
     * Event in case of chat transport changed, for example because a different
     * transport was selected.
     *
     * @param chatSession the chat session
     */
    @Override
    public void currentChatTransportChanged(ChatSession chatSession)
    {
        List<PluginComponent> components;
        synchronized (this.pluginComponents)
        {
            components = new ArrayList<PluginComponent>(this.pluginComponents);
        }
        // determine contact instance to use in event handling when calling
        // setCurrentContact
        final Contact contact;
        final Object descriptor = chatSession.getDescriptor();
        if (descriptor instanceof MetaContact)
        {
            contact = ((MetaContact) descriptor).getDefaultContact();
        }
        else if (descriptor instanceof Contact)
        {
            contact = (Contact) descriptor;
        }
        else if (descriptor == null)
        {
            // In case of null contact, just call setCurrentContact for
            // null Contact and get out. Nothing else to do here.
            for (PluginComponent c : components)
            {
                c.setCurrentContact((Contact) null);
            }
            return;
        }
        else
        {
            logger.warn(String.format("Unsupported descriptor type %s (%s),"
                + "this event will not be propagated.", descriptor, descriptor
                .getClass().getCanonicalName()));
            return;
        }
        // Call setCurrentContact on all registered pluginComponents such that
        // all get updated on the new state of the chat session
        final String resourceName =
            chatSession.getCurrentChatTransport().getResourceName();
        for (PluginComponent c : components)
        {
            try
            {
                c.setCurrentContact(contact, resourceName);
            }
            catch (RuntimeException e)
            {
                logger.error(
                    "BUG: setCurrentContact of PluginComponent instance: "
                        + c.getClass().getCanonicalName()
                        + " throws a RuntimeException.", e);
            }
        }
    }

    @Override
    public void currentChatTransportUpdated(int eventID)
    {
        // Nothing to do here, since we do not need to communicate update events
    }
}
