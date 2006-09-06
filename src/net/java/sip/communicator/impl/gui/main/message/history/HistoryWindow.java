/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.history;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.msghistory.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>HistoryWindow</tt> is the window, where user could view or search
 * in the message history. The <tt>HistoryWindow</tt> could contain the history
 * for one or a group of <tt>MetaContact</tt>s.
 *
 * @author Yana Stamcheva
 */
public class HistoryWindow
    extends JFrame
    implements  ChatConversationContainer,
                ActionListener,
                MessageHistorySearchProgressListener {

    private static final Logger logger = Logger
        .getLogger(HistoryWindow.class.getName());

    private static final String HISTORY_WINDOW_WIDTH_PROPERTY
        = "net.java.sip.communicator.impl.ui.historyWindowWidth";

    private static final String HISTORY_WINDOW_HEIGHT_PROPERTY
        = "net.java.sip.communicator.impl.ui.historyWindowHeight";

    private static final String HISTORY_WINDOW_X_PROPERTY
        = "net.java.sip.communicator.impl.ui.historyWindowX";

    private static final String HISTORY_WINDOW_Y_PROPERTY
        = "net.java.sip.communicator.impl.ui.historyWindowY";

    private JPanel historyPanel = new JPanel(new BorderLayout());

    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

    private JProgressBar progressBar;

    private SearchPanel searchPanel;

    private JMenuBar historyMenuBar = new JMenuBar();

    private HistoryMenu historyMenu;

    private JPanel northPanel = new JPanel(new BorderLayout());

    private DatesPanel datesPanel;

    private MetaContact metaContact;

    private MessageHistoryService msgHistory;

    private MainFrame mainFrame;

    private static String KEYWORD_SEARCH = "KeywordSearch";

    private static String PERIOD_SEARCH = "PeriodSearch";

    private Hashtable dateHistoryTable = new Hashtable();

    private String lastExecutedSearch;

    private Date searchStartDate;

    private String searchKeyword;
    
    private Date currentStartDate;
    
    private Date currentEndDate;

    private String currentKeyword;
    
    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     * @param mainFrame the main application window
     * @param metaContact the <tt>MetaContact</tt> for which to display
     * a history
     */
    public HistoryWindow(MainFrame mainFrame, MetaContact metaContact) {

        this.progressBar = new JProgressBar(
            MessageHistorySearchProgressListener.PROGRESS_MINIMUM_VALUE,
            MessageHistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE);
                
        this.progressBar.setValue(0);
        this.progressBar.setStringPainted(true);
        
        this.msgHistory = GuiActivator.getMsgHistoryService();
        this.msgHistory.addSearchProgressListener(this);
        
        this.mainFrame = mainFrame;
        this.metaContact = metaContact;

        this.setTitle(Messages.getString(
                "historyContact", metaContact.getDisplayName()));

        this.datesPanel = new DatesPanel(this);
        this.historyMenu = new HistoryMenu(this);
        this.searchPanel = new SearchPanel(this);

        this.setSizeAndLocation();

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        this.initPanels();

        this.initData();

        this.addWindowListener(new HistoryWindowAdapter());
        
        ActionMap amap = this.getRootPane().getActionMap();
        
        amap.put("close", new CloseAction());
        
        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
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

        this.mainPanel.add(historyPanel, BorderLayout.CENTER);

        this.mainPanel.add(progressBar, BorderLayout.SOUTH);

        this.mainPanel.add(datesPanel, BorderLayout.WEST);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Initializes the history with a list of all dates, for which a history
     * with the given contact is availabled.
     */
    private void initData() {
        SwingUtilities.invokeLater(new Thread () {
           public void run() {
               Collection msgList = msgHistory.findByEndDate(
                   metaContact, new Date(System.currentTimeMillis()));
               
               Object[] msgArray = msgList.toArray();
               Date date = null;

               for (int i = 0; i < msgArray.length; i ++) {
                   Object o = msgArray[i];
          
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
               //Initializes the conversation panel with the data of the last
               //conversation.
               datesPanel.setSelected(datesPanel.getModel().getSize() - 1);
           } 
        });                  
    }

    /**
     * Shows a history for a given period.
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     */
    public void showHistoryByPeriod(Date startDate, Date endDate) {

        this.currentStartDate = startDate;
        this.currentEndDate = endDate;
        
        if(dateHistoryTable.containsKey(startDate)) {
            ChatConversationPanel convPanel
                = (ChatConversationPanel)dateHistoryTable.get(startDate);

            this.historyPanel.removeAll();
            
            this.historyPanel.add(convPanel);
            
            historyPanel.revalidate();
            historyPanel.repaint();
        }
        else {
            SwingUtilities.invokeLater(new Thread() {
               public void run() {
                   Collection msgList = msgHistory.findByPeriod(
                       metaContact, currentStartDate, currentEndDate);
            
                   historyPanel.removeAll();
                   
                   ChatConversationPanel convPanel
                       = new ChatConversationPanel(HistoryWindow.this);

                   historyPanel.add(convPanel);

                   createHistory(convPanel, msgList, null);
                   dateHistoryTable.put(currentStartDate, convPanel);
                   
                   historyPanel.revalidate();
                   historyPanel.repaint();
               } 
            });                         
        }
        this.lastExecutedSearch = PERIOD_SEARCH;
        this.searchStartDate = startDate;
    }

    /**
     * Shows a history for a given keyword.
     * @param keyword the keyword to search
     */
    public void showHistoryByKeyword(String keyword) {
        this.currentKeyword = keyword;
        
        SwingUtilities.invokeLater(new Thread () {
            public void run() {
                Collection msgList = msgHistory.findByKeyword(
                        metaContact, currentKeyword);
                
                Object[] msgArray = msgList.toArray();
                Date date = null;
                ChatConversationPanel convPanel = null;

                datesPanel.removeAllDates();
                for (int i = 0; i < msgArray.length; i ++) {
                    Object o = msgArray[i];

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
                
                datesPanel.revalidate();
                datesPanel.repaint();
                
                for(int i = 0; i < datesPanel.getDatesNumber(); i ++) {
                    Date initDate = datesPanel.getDate(i);

                    msgList = msgHistory.findByPeriod(
                            metaContact, initDate,
                            datesPanel.getNextDate(initDate));

                    if(dateHistoryTable.contains(initDate)) {
                        dateHistoryTable.remove(initDate);
                    }

                    convPanel = new ChatConversationPanel(HistoryWindow.this);

                    createHistory(convPanel, msgList, currentKeyword);
                    dateHistoryTable.put(initDate, convPanel);
                }
                datesPanel.setSelected(datesPanel.getDatesNumber() - 1);
                
                lastExecutedSearch = KEYWORD_SEARCH;
                searchKeyword = currentKeyword;
            }
        });
    }

    /**
     * Shows the history given by the collection into a ChatConversationPanel.
     * @param historyRecords a collection of history records
     */
    private void createHistory(ChatConversationPanel chatConvPanel,
            Collection historyRecords, String keyword) {

        if(historyRecords.size() > 0) {

            Iterator i = historyRecords.iterator();

            while (i.hasNext()) {

                Object o = i.next();

                if(o instanceof MessageDeliveredEvent) {

                    MessageDeliveredEvent evt = (MessageDeliveredEvent)o;

                    ProtocolProviderService protocolProvider = evt
                        .getDestinationContact().getProtocolProvider();

                    chatConvPanel.processMessage(
                            this.mainFrame.getAccount(protocolProvider),
                            evt.getTimestamp(), Constants.OUTGOING_MESSAGE,
                            evt.getSourceMessage().getContent(), keyword);
                }
                else if(o instanceof MessageReceivedEvent) {
                    MessageReceivedEvent evt = (MessageReceivedEvent)o;

                    chatConvPanel.processMessage(
                            evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(), Constants.INCOMING_MESSAGE,
                            evt.getSourceMessage().getContent(), keyword);
                }
            }
        }
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

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user clicks on the
     * refresh button. Executes ones more the last search.
     */
    public void actionPerformed(ActionEvent e) {

        if(lastExecutedSearch.equals(KEYWORD_SEARCH)) {            
            showHistoryByKeyword(searchKeyword);
        }
        else if(lastExecutedSearch.equals(PERIOD_SEARCH)) {
            showHistoryByPeriod(searchStartDate,
                    new Date(System.currentTimeMillis()));
        }
    }

    /**
     * Before closing the history window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class HistoryWindowAdapter extends WindowAdapter {

        public void windowClosing(WindowEvent e) {
            ConfigurationService configService
                = GuiActivator.getConfigurationService();

            try {
                configService.setProperty(
                    HISTORY_WINDOW_WIDTH_PROPERTY,
                    new Integer(getWidth()));

                configService.setProperty(
                    HISTORY_WINDOW_HEIGHT_PROPERTY,
                    new Integer(getHeight()));

                configService.setProperty(
                    HISTORY_WINDOW_X_PROPERTY,
                    new Integer(getX()));

                configService.setProperty(
                    HISTORY_WINDOW_Y_PROPERTY,
                    new Integer(getY()));
            }
            catch (PropertyVetoException e1) {
                logger.error("The proposed property change "
                        + "represents an unacceptable value");
            }
        }
    }

    /**
     * Sets the window size and position.
     */
    public void setSizeAndLocation() {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String width = configService.getString(HISTORY_WINDOW_WIDTH_PROPERTY);

        String height = configService.getString(HISTORY_WINDOW_HEIGHT_PROPERTY);

        String x = configService.getString(HISTORY_WINDOW_X_PROPERTY);

        String y = configService.getString(HISTORY_WINDOW_Y_PROPERTY);


        if(width != null && height != null) {
            this.setSize(new Integer(width).intValue(),
                    new Integer(height).intValue());
        }
        else {
            this.setSize(new Dimension(
                    Constants.HISTORY_WINDOW_WIDTH,
                    Constants.HISTORY_WINDOW_HEIGHT));
        }

        if(x != null && y != null) {
            this.setLocation(new Integer(x).intValue(),
                    new Integer(y).intValue());
        }
        else {
            this.setCenterLocation();
        }
    }

    /**
     * Positions this window in the center of the screen.
     */
    private void setCenterLocation(){
        this.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - this.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - this.getHeight()/2
                );
    }
    
    public void progressChanged(ProgressEvent evt) {
        this.progressBar.setValue(evt.getProgress());
    }
    
    /**
     * The <tt>CloseAction</tt> is an <tt>AbstractAction</tt> that closes the
     * current history window.
     */
    private class CloseAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
    
    /* Test the progress bar
    private class ProgressTimer extends Timer {

        public ProgressTimer() {
            //Set delay
            super(1000, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                progressBar.setValue(progressBar.getValue() + 20);
            }
        }
    }
    */
}
