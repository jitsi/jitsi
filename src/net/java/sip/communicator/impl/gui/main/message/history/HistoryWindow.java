/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.history;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.message.ChatConversationContainer;
import net.java.sip.communicator.impl.gui.main.message.ChatConversationPanel;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.history.QueryResultSet;
import net.java.sip.communicator.service.history.records.HistoryRecord;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;

/**
 * The <tt>HistoryWindow</tt> is the window, where user could view or search
 * in the message history. The <tt>HistoryWindow</tt> could contain the history
 * for one or a group of <tt>MetaContact</tt>s.
 * 
 * @author Yana Stamcheva
 */
public class HistoryWindow extends JFrame
    implements ChatConversationContainer {

    private JPanel historyPane = new JPanel(new BorderLayout());

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private NavigationPanel navigationPanel = new NavigationPanel();

    private SearchPanel searchPanel;

    private JMenuBar historyMenuBar = new JMenuBar();

    private HistoryMenu historyMenu;

    private JPanel northPanel = new JPanel(new BorderLayout());
    
    private DatesPanel datesPanel;

    private Vector contacts;

    private MetaContact metaContact;

    private String title = Messages.getString("history") + " - ";

    private MessageHistoryService msgHistory = GuiActivator.getMsgHistoryService();
    
    private MainFrame mainFrame;
    
    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     * @param mainFrame the main application window
     * @param metaContact the <tt>MetaContact</tt> for which to display
     * a history
     */
    public HistoryWindow(MainFrame mainFrame, MetaContact metaContact) {
        
        this.mainFrame = mainFrame;
        this.metaContact = metaContact;
        
        this.setTitle(metaContact.getDisplayName());
        
        this.datesPanel = new DatesPanel(this);
        this.historyMenu = new HistoryMenu(this);
        this.searchPanel = new SearchPanel(this);
        
        this.setSize(Constants.HISTORY_WINDOW_WIDTH,
                Constants.HISTORY_WINDOW_HEIGHT);

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        this.initData();
        
        this.initPanels();
    }

    /**
     * Initializes the history with a list of all dates, for which a history
     * with the given contact is availabled.
     */
    private void initData() {
        Collection msgList = this.msgHistory.findByEndDate(
                this.metaContact, new Date(System.currentTimeMillis()));
        
        Iterator i = msgList.iterator();
                
        Date date = null;
        while (i.hasNext()) {
            Object o = i.next();
            
            if (o instanceof MessageDeliveredEvent) {
                MessageDeliveredEvent evt = (MessageDeliveredEvent)o; 
         
                date = evt.getTimestamp();
            }
            else if (o instanceof MessageReceivedEvent) {
                MessageReceivedEvent evt = (MessageReceivedEvent)o;
                date = evt.getTimestamp();
            }
                           
            if(!datesPanel.containsDate(date)) {
                datesPanel.addDate(date);
            }
        }
    }
    
    /**
     * Constructs the window, by adding all components and panels.
     */
    private void initPanels() {
        
        this.historyMenuBar.add(historyMenu);

        this.northPanel.add(historyMenuBar, BorderLayout.NORTH);

        this.northPanel.add(searchPanel, BorderLayout.CENTER);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        this.mainPanel.add(northPanel, BorderLayout.NORTH);

        this.mainPanel.add(historyPane, BorderLayout.CENTER);

        //this.mainPanel.add(navigationPanel, BorderLayout.SOUTH);
        
        this.mainPanel.add(datesPanel, BorderLayout.WEST);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows a history for a given period.
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     */
    public void showHistoryByPeriod(Date startDate, Date endDate) {
        
        Collection msgList = this.msgHistory.findByPeriod(
                this.metaContact, startDate, endDate);
        
        showHistory(msgList);
    }
    
    /**
     * Shows a history for a given keyword.
     * @param keyword the keyword to search
     */
    public void showHistoryByKeyword(String keyword) {
        
        Collection msgList = this.msgHistory.findByKeyword(
                this.metaContact, keyword);
        
        showHistory(msgList);
    }
    
    /**
     * Shows the history given by the collection into a ChatConversationPanel.
     * @param historyRecords a collection of history records
     */
    private void showHistory(Collection historyRecords) {
        this.historyPane.removeAll();
        
        if(historyRecords.size() > 0) {
            
            Iterator i = historyRecords.iterator();
            
            ChatConversationPanel chatConvPanel
                = new ChatConversationPanel(this);
            
            while (i.hasNext()) {
                
                Object o = i.next();
                
                if(o instanceof MessageDeliveredEvent) {
                    
                    MessageDeliveredEvent evt = (MessageDeliveredEvent)o;
                    
                    ProtocolProviderService protocolProvider = evt
                        .getDestinationContact().getProtocolProvider();
                    
                    chatConvPanel.processMessage(
                            this.mainFrame.getAccount(protocolProvider),
                            evt.getTimestamp(), Constants.OUTGOING_MESSAGE,
                            evt.getSourceMessage().getContent());
                }
                else if(o instanceof MessageReceivedEvent) {
                    MessageReceivedEvent evt = (MessageReceivedEvent)o;
                                    
                    chatConvPanel.processMessage(
                            evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(), Constants.INCOMING_MESSAGE,
                            evt.getSourceMessage().getContent());
                }
            }            
            this.historyPane.add(chatConvPanel, BorderLayout.CENTER);
        }
        this.historyPane.revalidate();
    }

    /**
     * Implements <tt>ChatConversationContainer.setStatusMessage</tt> method.
     */
    public void setStatusMessage(String message) {
        //TODO : setStatusMessage(String message)
    }
    
    /**
     * Implements <tt>ChatConversationContainer.getWindow</tt> method.
     */
    public Window getWindow() {
        return this;
    }
}
