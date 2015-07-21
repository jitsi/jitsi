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
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.filetransfer.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.history.event.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>HistoryWindow</tt> is the window, where user could view or search
 * in the message history. The <tt>HistoryWindow</tt> could contain the history
 * for one or a group of <tt>MetaContact</tt>s.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class HistoryWindow
    extends SIPCommFrame
    implements  ChatConversationContainer,
                HistorySearchProgressListener,
                MessageListener,
                ChatRoomMessageListener,
                Skinnable
{
    private static final Logger logger = Logger.getLogger(HistoryWindow.class);

    private static final String[] HISTORY_FILTER
        = new String[]
                {
                    MessageHistoryService.class.getName(),
                    FileHistoryService.class.getName()
                };

    /**
     * The horizontal and vertical spacing between the UI components of this
     * instance defined in one place for the purposes of consistency. Hopefully,
     * one day it should be defined at a global application level to achieve
     * consistency with the UI elsewhere.
     */
    private static final int SPACING = 5;

    private ChatConversationPanel chatConvPanel;

    private final JPanel mainPanel
        = new TransparentPanel(new BorderLayout(SPACING, SPACING));

    private JProgressBar progressBar;

    private HistoryMenu historyMenu;

    private DatesPanel datesPanel;

    private Object historyContact;

    private MetaHistoryService history;

    private Hashtable<Date, HTMLDocument> dateHistoryTable
        = new Hashtable<Date, HTMLDocument>();

    private final JLabel readyLabel
        = new JLabel(
                GuiActivator.getResources().getI18NString("service.gui.READY"));

    private String searchKeyword;

    private Set<Date> datesDisplayed = new LinkedHashSet<Date>();

    private Date ignoreProgressDate;

    private int lastProgress = 0;

    /**
     * If the <code>historyContact</code> is a <code>MetaContact</code>,
     * contains the <code>OperationSetBasicInstantMessaging</code> instances to
     * which this <code>HistoryWindow</code> has added itself as a
     * <code>MessageListener</code>.
     */
    private java.util.List<OperationSetBasicInstantMessaging>
        basicInstantMessagings;

    /**
     * If the <code>historyContact</code> is a <code>ChatRoomWrapper</code>,
     * specifies the <code>ChatRoom</code> to which this
     * <code>HistoryWindow</code> has added itself as a
     * <code>ChatRoomMessageListener</code>.
     */
    private ChatRoom chatRoom;

    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     * @param historyContact the <tt>MetaContact</tt> or the <tt>ChatRoom</tt>
     */
    public HistoryWindow(Object historyContact)
    {
        this.historyContact = historyContact;

        chatConvPanel = new ChatConversationPanel(this);
        chatConvPanel.getChatTextPane()
            .setTransferHandler(new ExtendedTransferHandler());
        this.progressBar = new JProgressBar(
            HistorySearchProgressListener.PROGRESS_MINIMUM_VALUE,
            HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE);

        this.progressBar.setValue(0);
        this.progressBar.setStringPainted(true);

        this.history = GuiActivator.getMetaHistoryService();
        this.history.addSearchProgressListener(this);

        this.datesPanel = new DatesPanel(this);
        this.historyMenu = new HistoryMenu(this);

        this.initPanels();

        this.initDates();

        this.pack();

        if (historyContact instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) historyContact;

            this.setTitle(GuiActivator.getResources().getI18NString(
                    "service.gui.HISTORY_CONTACT",
                    new String[]{metaContact.getDisplayName()}));

            Iterator<Contact> protoContacts = metaContact.getContacts();

            basicInstantMessagings
                = new ArrayList<OperationSetBasicInstantMessaging>();
            while(protoContacts.hasNext())
            {
                Contact protoContact = protoContacts.next();

                OperationSetBasicInstantMessaging basicInstantMessaging
                    = protoContact
                        .getProtocolProvider()
                            .getOperationSet(
                                OperationSetBasicInstantMessaging.class);

                if (basicInstantMessaging != null)
                {
                    basicInstantMessaging.addMessageListener(this);
                    basicInstantMessagings.add(basicInstantMessaging);
                }
            }
        }
        else if (historyContact instanceof ChatRoomWrapper)
        {
            chatRoom = ((ChatRoomWrapper) historyContact).getChatRoom();
            chatRoom.addMessageListener(this);
        }
    }

    /**
     * Constructs the window, by adding all components and panels.
     */
    private void initPanels()
    {
        this.mainPanel
                .setBorder(
                    BorderFactory
                        .createEmptyBorder(SPACING, SPACING, SPACING, SPACING));
        this.mainPanel.setPreferredSize(new Dimension(500, 400));

        this.mainPanel.add(new SearchPanel(this), BorderLayout.NORTH);
        this.mainPanel.add(chatConvPanel, BorderLayout.CENTER);
        this.mainPanel.add(datesPanel, BorderLayout.WEST);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Initializes the history with a list of all dates, for which a history
     * with the given contact is available.
     */
    private void initDates()
    {
        this.initProgressBar(null);
        new DatesLoader().start();
    }

    /**
     * Shows a history for a given period.
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     */
    public void showHistoryByPeriod(Date startDate, Date endDate)
    {
        if((searchKeyword == null || searchKeyword.length() == 0)
                && dateHistoryTable.containsKey(startDate))
        {
            HTMLDocument document = dateHistoryTable.get(startDate);

            this.chatConvPanel.setContent(document);
        }
        else
        {
            this.chatConvPanel.clear();
            //init progress bar by precising the date that will be loaded.
            this.initProgressBar(startDate);

            new MessagesLoader(startDate, endDate).start();
        }
    }

    /**
     * Shows a history for a given keyword.
     * @param keyword the keyword to search
     */
    public void showHistoryByKeyword(String keyword)
    {
        this.initProgressBar(null);

        chatConvPanel.clear();
        datesPanel.setLastSelectedIndex(-1);

        new KeywordDatesLoader(keyword).start();

        searchKeyword = keyword;
    }

    /**
     * Shows the history given by the collection into a ChatConversationPanel.
     * @param historyRecords a collection of history records
     * @return an <tt>HTMLDocument</tt> containing the history given by
     * <tt>historyRecords</tt>
     */
    private HTMLDocument createHistory(Collection<Object> historyRecords)
    {
        if((historyRecords != null) && (historyRecords.size() > 0))
        {
            Iterator<Object> i = historyRecords.iterator();
            String processedMessage = "";
            while (i.hasNext())
            {
                Object o = i.next();

                ChatMessage chatMessage = null;
                ProtocolProviderService protocolProvider = null;

                if(o instanceof MessageDeliveredEvent)
                {
                    MessageDeliveredEvent evt = (MessageDeliveredEvent) o;

                    protocolProvider
                        = evt.getDestinationContact().getProtocolProvider();

                    chatMessage = new ChatMessage(
                            GuiActivator.getUIService().getMainFrame()
                                .getAccountAddress(protocolProvider),
                            GuiActivator.getUIService().getMainFrame()
                                .getAccountDisplayName(protocolProvider),
                            evt.getTimestamp(),
                            Chat.OUTGOING_MESSAGE,
                            null,
                            evt.getSourceMessage().getContent(),
                            evt.getSourceMessage().getContentType(),
                            evt.getSourceMessage().getMessageUID(),
                            null);
                }
                else if(o instanceof MessageReceivedEvent)
                {
                    MessageReceivedEvent evt = (MessageReceivedEvent) o;

                    protocolProvider
                        = evt.getSourceContact().getProtocolProvider();

                    chatMessage = new ChatMessage(
                        evt.getSourceContact().getAddress(),
                        evt.getSourceContact().getDisplayName(),
                        evt.getTimestamp(),
                        Chat.INCOMING_MESSAGE,
                        null,
                        evt.getSourceMessage().getContent(),
                        evt.getSourceMessage().getContentType(),
                        evt.getSourceMessage().getMessageUID(),
                        null);
                }
                else if(o instanceof ChatRoomMessageReceivedEvent)
                {
                    ChatRoomMessageReceivedEvent evt
                        = (ChatRoomMessageReceivedEvent) o;

                    protocolProvider
                        = evt.getSourceChatRoom().getParentProvider();

                    chatMessage = new ChatMessage(
                        evt.getSourceChatRoomMember().getName(),
                        evt.getTimestamp(), Chat.INCOMING_MESSAGE,
                        evt.getMessage().getContent(),
                        evt.getMessage().getContentType());
                }
                else if(o instanceof ChatRoomMessageDeliveredEvent)
                {
                    ChatRoomMessageDeliveredEvent evt
                        = (ChatRoomMessageDeliveredEvent) o;

                    protocolProvider
                        = evt.getSourceChatRoom().getParentProvider();

                    chatMessage = new ChatMessage(
                        evt.getSourceChatRoom().getParentProvider()
                        .getAccountID().getUserID(),
                        evt.getTimestamp(), Chat.INCOMING_MESSAGE,
                        evt.getMessage().getContent(),
                        evt.getMessage().getContentType());
                }
                else if (o instanceof FileRecord)
                {
                    FileRecord fileRecord = (FileRecord) o;

                    protocolProvider
                        = fileRecord.getContact().getProtocolProvider();

                    FileHistoryConversationComponent component
                        = new FileHistoryConversationComponent(fileRecord);

                    chatConvPanel.addComponent(component);
                }

                if (chatMessage != null)
                {
                    processedMessage = chatConvPanel.processMessage(
                            chatMessage,
                            searchKeyword,
                            protocolProvider,
                            chatMessage.getContactName());

                    chatConvPanel.appendMessageToEnd(processedMessage,
                        ChatHtmlUtils.HTML_CONTENT_TYPE);
                }
            }
        }

        this.chatConvPanel.setDefaultContent();

        return this.chatConvPanel.getContent();
    }

    /**
     * Implements <tt>ChatConversationContainer.setStatusMessage</tt> method.
     */
    public void addTypingNotification(String message) {}

    /**
     * Implements <tt>ChatConversationContainer.getWindow</tt> method.
     * @return this window
     */
    public Window getConversationContainerWindow()
    {
        return this;
    }

    /**
     * Returns the next date from the history.
     * When <tt>date</tt> is the last one, we return the current date,
     * means we are loading today messages (till now).
     *
     * @param date The date which indicates where to start.
     * @return the date after the given date
     */
    public Date getNextDateFromHistory(Date date)
    {
        Iterator<Date> iterator = datesDisplayed.iterator();
        while(iterator.hasNext())
        {
            Date curr = iterator.next();
            if(curr.equals(date))
            {
                if(iterator.hasNext())
                    return iterator.next();
                else
                    break;
            }
        }

        return new Date(System.currentTimeMillis());
    }

    /**
     * Handles the ProgressEvent triggered from the history when processing
     * a query.
     * @param evt the <tt>ProgressEvent</tt> that notified us
     */
    public void progressChanged(ProgressEvent evt)
    {
        int progress = evt.getProgress();

        if((lastProgress != progress)
                && evt.getStartDate() == null
                || evt.getStartDate() != ignoreProgressDate)
        {
            this.progressBar.setValue(progress);

            if(progressBar.getPercentComplete() == 1.0)
            {
                // Wait 1 sec and remove the progress bar from the main panel.
                Timer progressBarTimer = new Timer(1 * 1000, null);

                progressBarTimer.setRepeats(false);
                progressBarTimer.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        mainPanel.remove(progressBar);
                        mainPanel.add(readyLabel, BorderLayout.SOUTH);
                        mainPanel.revalidate();
                        mainPanel.repaint();
                        progressBar.setValue(0);
                    }
                });
                progressBarTimer.start();
            }

            lastProgress = progress;
        }
    }

    /**
     * Loads history dates.
     */
    private class DatesLoader extends Thread
    {
        @Override
        public void run()
        {
            Collection<Object> msgList = null;

            if (historyContact instanceof MetaContact)
            {
                msgList = history.findByEndDate(
                    HISTORY_FILTER,
                    historyContact,
                    new Date(System.currentTimeMillis()));
            }
            else if(historyContact instanceof ChatRoomWrapper)
            {
                ChatRoomWrapper chatRoomWrapper
                    = (ChatRoomWrapper) historyContact;

                if(chatRoomWrapper.getChatRoom() == null)
                    return;

                msgList = history.findByEndDate(
                    HISTORY_FILTER,
                    chatRoomWrapper.getChatRoom(),
                    new Date(System.currentTimeMillis()));
            }

            if (msgList != null)
            for (Object o : msgList)
            {
                Date date = new Date(0);

                if (o instanceof MessageDeliveredEvent)
                {
                    MessageDeliveredEvent evt = (MessageDeliveredEvent)o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof MessageReceivedEvent)
                {
                    MessageReceivedEvent evt = (MessageReceivedEvent)o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof ChatRoomMessageReceivedEvent)
                {
                    ChatRoomMessageReceivedEvent
                        evt = (ChatRoomMessageReceivedEvent) o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof ChatRoomMessageDeliveredEvent)
                {
                    ChatRoomMessageDeliveredEvent
                        evt = (ChatRoomMessageDeliveredEvent) o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof FileRecord)
                {
                    FileRecord fileRecord = (FileRecord) o;
                    date = fileRecord.getDate();
                }

                boolean containsDate = false;
                Iterator<Date> iterator = datesDisplayed.iterator();
                while(iterator.hasNext())
                {
                    Date currDate = iterator.next();
                    containsDate
                        = (GuiUtils.compareDatesOnly(date, currDate) == 0);

                    if(containsDate)
                        break;
                }

                if(!containsDate)
                {
                    datesDisplayed.add(date);
                }
            }

            if((msgList != null) && (msgList.size() > 0))
            {
                Runnable updateDatesPanel = new Runnable() {
                    public void run() {
                        Date date = null;
                        for(Date curr : datesDisplayed)
                        {
                            date = curr;
                            if(!datesPanel.containsDate(date))
                                datesPanel.addDate(date);
                        }
                        if(date != null) {
                            ignoreProgressDate = date;
                        }
                        //Initializes the conversation panel with the data of
                        //the last conversation.
                        int lastDateIndex = datesPanel.getDatesNumber() - 1;
                        datesPanel.setSelected(lastDateIndex);
                    }
                };
                SwingUtilities.invokeLater(updateDatesPanel);
            }
        }
     }

    /**
     * Loads history messages in the right panel.
     */
    private class MessagesLoader extends Thread
    {
        private final Date startDate;
        private final Date endDate;

        /**
         * Creates a MessageLoader thread charged to load history messages in
         * the right panel.
         *
         * @param startDate the start date of the history to load
         * @param endDate the end date of the history to load
         */
        public MessagesLoader (Date startDate, Date endDate)
        {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public void run()
        {
            final Collection<Object> msgList;

            if(historyContact instanceof MetaContact)
            {
                msgList = history.findByPeriod(
                    HISTORY_FILTER,
                    historyContact,
                    startDate, endDate);
            }
            else if (historyContact instanceof ChatRoomWrapper)
            {
                ChatRoomWrapper chatRoomWrapper
                    = (ChatRoomWrapper) historyContact;

                if(chatRoomWrapper.getChatRoom() == null)
                    return;

                msgList = history.findByPeriod(
                    HISTORY_FILTER,
                    chatRoomWrapper.getChatRoom(),
                    startDate, endDate);
            }
            else
                msgList = null;

            Runnable updateMessagesPanel = new Runnable()
            {
                public void run()
                {
                    HTMLDocument doc = createHistory(msgList);

                    if(searchKeyword == null || searchKeyword.length() == 0)
                    {
                        dateHistoryTable.put(startDate, doc);
                    }
                }
            };
            SwingUtilities.invokeLater(updateMessagesPanel);
        }
    }

    /**
     * Loads dates found for keyword.
     */
    private class KeywordDatesLoader extends Thread
    {
        private Vector<Date> keywordDatesVector = new Vector<Date>();
        private final String keyword;

        /**
         * Creates a KeywordDatesLoader thread charged to load a list of dates
         * of messages found by the given keyword.
         *
         * @param keyword the keyword to search for
         */
        public KeywordDatesLoader(String keyword)
        {
            this.keyword = keyword;
        }

        @Override
        public void run()
        {
            Collection<Object> msgList = null;

            if (historyContact instanceof MetaContact)
            {
                msgList = history.findByKeyword(
                    HISTORY_FILTER,
                    historyContact, keyword);
            }
            else if (historyContact instanceof ChatRoomWrapper)
            {
                ChatRoomWrapper chatRoomWrapper
                    = (ChatRoomWrapper) historyContact;

                if (chatRoomWrapper.getChatRoom() == null)
                    return;

                msgList = history.findByKeyword(
                    HISTORY_FILTER,
                    chatRoomWrapper.getChatRoom(), keyword);
            }

            if (msgList != null)
            for (Object o : msgList)
            {
                Date date = new Date(0);

                if (o instanceof MessageDeliveredEvent)
                {
                    MessageDeliveredEvent evt = (MessageDeliveredEvent)o;
                    date = evt.getTimestamp();
                }
                else if (o instanceof MessageReceivedEvent)
                {
                    MessageReceivedEvent evt = (MessageReceivedEvent)o;
                    date = evt.getTimestamp();
                }

                long milisecondsPerDay = 24*60*60*1000;
                for(Date date1 : datesDisplayed)
                {
                    if(Math.floor(date1.getTime()/milisecondsPerDay)
                        == Math.floor(date.getTime()/milisecondsPerDay)
                        && !keywordDatesVector.contains(date1))
                    {
                        keywordDatesVector.add(date1);
                    }
                }
            }

            Runnable updateDatesPanel = new Runnable()
            {
                public void run()
                {
                    datesPanel.removeAllDates();
                    if(keywordDatesVector.size() > 0)
                    {
                        Date date = null;
                        for(int i = 0; i < keywordDatesVector.size(); i++)
                        {
                            date = keywordDatesVector.get(i);

                            /* I have tried to remove and add dates in the
                             * datesList. A lot of problems occured because
                             * it seems that the list generates selection events
                             * when removing elements. This was solved but after
                             * that a problem occured when one and the same
                             * selection was done twice.
                             *
                             * if(!keywordDatesVector.contains(date)) {
                             *    datesPanel.removeDate(date);
                             * }
                             * else {
                             *    if(!datesPanel.containsDate(date)) {
                             *        datesPanel.addDate(date);
                             *    }
                            }*/
                            if(!datesPanel.containsDate(date))
                                datesPanel.addDate(date);
                        }
                        if(date != null)
                        {
                            ignoreProgressDate = date;
                        }
                        datesPanel.setSelected(datesPanel.getDatesNumber() - 1);
                    }
                    else
                    {
                        chatConvPanel.setDefaultContent();
                    }
                }
            };
            SwingUtilities.invokeLater(updateDatesPanel);
        }
    }

    /**
     * Implements the <tt>SIPCommFrame</tt> close method, which is invoked when
     * user presses the Esc key. Checks if the popup menu is visible and if
     * this is the case hides it, otherwise saves the current history window
     * size and location and disposes the window.
     * @param isEscaped indicates if the window has been closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        /*
         * Remove all listeners in order to have this instance ready for garbage
         * collection.
         */
        history.removeSearchProgressListener(this);

        if (basicInstantMessagings != null)
        {
            for (OperationSetBasicInstantMessaging basicInstantMessaging
                : basicInstantMessagings)
                basicInstantMessaging.removeMessageListener(this);
            basicInstantMessagings = null;
        }

        if (chatRoom != null)
        {
            chatRoom.removeMessageListener(this);
            chatRoom = null;
        }

        if(chatConvPanel != null
            && chatConvPanel.getRightButtonMenu() != null
            && chatConvPanel.getRightButtonMenu().isVisible())
        {
            chatConvPanel.getRightButtonMenu().setVisible(false);
        }
        else if(historyMenu != null && historyMenu.isPopupMenuVisible())
        {
            MenuSelectionManager.defaultManager().clearSelectedPath();
        }
        else
        {
            GuiActivator.getUIService().getHistoryWindowManager()
                .removeHistoryWindowForContact(historyContact);

            if(datesPanel != null)
                datesPanel.dispose();

            if(chatConvPanel != null)
                chatConvPanel.dispose();

            this.dispose();
        }
    }

    /**
     * Removes the "Ready" label and adds the progress bar in the bottom of the
     * history window.
     */
    private void initProgressBar(Date date)
    {
        if(date == null || date != ignoreProgressDate)
        {
            this.mainPanel.remove(readyLabel);
            this.mainPanel.add(progressBar, BorderLayout.SOUTH);
            this.mainPanel.revalidate();
            this.mainPanel.repaint();
        }
    }

    /**
     * Implements MessageListener.messageReceived method in order to refresh the
     * history when new message is received.
     */
    public void messageReceived(MessageReceivedEvent evt)
    {
        Contact sourceContact = evt.getSourceContact();
        Message sourceMessage = evt.getSourceMessage();

        this.processMessage(
                sourceContact,
                evt.getTimestamp(),
                Chat.INCOMING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());
    }

    /**
     * Implements MessageListener.messageDelivered method in order to refresh the
     * history when new message is sent.
     */
    public void messageDelivered(MessageDeliveredEvent evt)
    {
        Contact destContact = evt.getDestinationContact();
        Message sourceMessage = evt.getSourceMessage();

        this.processMessage(
                destContact,
                evt.getTimestamp(),
                Chat.OUTGOING_MESSAGE,
                sourceMessage.getContent(),
                sourceMessage.getContentType());
    }

    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {}

    /**
     * Processes the message given by the parameters.
     *
     * @param contact the message source or destination contact
     * @param timestamp the timestamp of the message
     * @param messageType INCOMING or OUTGOING
     * @param messageContent the content text of the message
     * @param messageContentType the content type of the message
     */
    private void processMessage(Contact contact,
                                Date timestamp,
                                String messageType,
                                String messageContent,
                                String messageContentType)
    {
        if (!(historyContact instanceof MetaContact))
            return;

        Contact containedContact = ((MetaContact) historyContact)
            .getContact(contact.getAddress(),
                        contact.getProtocolProvider());

        if(containedContact != null)
        {
            int lastDateIndex = datesPanel.getDatesNumber() - 1;

            // If dates aren't yet loaded we don't process the message.
            if(lastDateIndex < 0)
                return;

            Date lastDate = datesPanel.getDate(lastDateIndex);

            if(lastDate != null
                && GuiUtils.compareDatesOnly(lastDate, timestamp) == 0)
            {
                HTMLDocument document = dateHistoryTable.get(lastDate);

                if(document != null)
                {
                    ChatMessage chatMessage = new ChatMessage(
                        contact.getDisplayName(),
                        timestamp,
                        messageType,
                        messageContent,
                        messageContentType);

                    String processedMessage = chatConvPanel.processMessage(
                        chatMessage, searchKeyword,
                        contact.getProtocolProvider(),
                        contact.getAddress());

                    if (processedMessage != null)
                    {
                        // ChatConversationPanel#processMessage may return null
                        // if the message turns out to be a consecutive message.
                        this.appendMessageToDocument(
                            document, processedMessage);
                    }
                }
            }
            else if (lastDate == null
                || GuiUtils.compareDatesOnly(lastDate, timestamp) < 0)
            {
                long milisecondsPerDay = 24*60*60*1000;

                Date date = new Date(timestamp.getTime()
                    - timestamp.getTime() % milisecondsPerDay);

                datesDisplayed.add(date);
                if(!datesPanel.containsDate(date))
                    datesPanel.addDate(date);
            }
        }
    }

    /**
     * Appends the given string at the end of the given html document.
     *
     * @param doc the document to append to
     * @param chatString the string to append
     */
    private void appendMessageToDocument(HTMLDocument doc, String chatString)
    {
        Element root = doc.getDefaultRootElement();

        try
        {
            doc.insertAfterEnd(root
                    .getElement(root.getElementCount() - 1), chatString);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }

    public void messageDelivered(ChatRoomMessageDeliveredEvent evt) {}

    public void messageDeliveryFailed(ChatRoomMessageDeliveryFailedEvent evt) {}

    public void messageReceived(ChatRoomMessageReceivedEvent evt) {}

    /**
     * Re-process history.
     */
    public void loadSkin()
    {
        dateHistoryTable.clear();

        Date startDate = datesPanel.getDate(datesPanel.getLastSelectedIndex());
        this.chatConvPanel.clear();

        //init progress bar by precising the date that will be loaded.
        this.initProgressBar(startDate);

        new MessagesLoader(startDate, getNextDateFromHistory(startDate)).start();
    }
}
