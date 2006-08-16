/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.history;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.msghistory.*;
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
public class HistoryWindow extends JFrame
    implements ChatConversationContainer, ActionListener {

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

    private JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JButton refreshButton = new JButton(Messages.getString("refresh"));

    private SearchPanel searchPanel;

    private JMenuBar historyMenuBar = new JMenuBar();

    private HistoryMenu historyMenu;

    private JPanel northPanel = new JPanel(new BorderLayout());

    private DatesPanel datesPanel;

    private MetaContact metaContact;

    private MessageHistoryService msgHistory = GuiActivator.getMsgHistoryService();

    private MainFrame mainFrame;

    private static String KEYWORD_SEARCH = "KeywordSearch";

    private static String PERIOD_SEARCH = "PeriodSearch";

    private Hashtable dateHistoryTable = new Hashtable();

    private String lastExecutedSearch;

    private Date searchStartDate;

    private String searchKeyword;

    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     * @param mainFrame the main application window
     * @param metaContact the <tt>MetaContact</tt> for which to display
     * a history
     */
    public HistoryWindow(MainFrame mainFrame, MetaContact metaContact) {

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
    }

    /**
     * Constructs the window, by adding all components and panels.
     */
    private void initPanels() {

        this.refreshButton.addActionListener(this);

        this.refreshPanel.add(refreshButton);

        this.historyMenuBar.add(historyMenu);

        this.northPanel.add(historyMenuBar, BorderLayout.NORTH);

        this.northPanel.add(searchPanel, BorderLayout.CENTER);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.mainPanel.add(northPanel, BorderLayout.NORTH);

        this.mainPanel.add(historyPanel, BorderLayout.CENTER);

        this.mainPanel.add(refreshPanel, BorderLayout.SOUTH);

        this.mainPanel.add(datesPanel, BorderLayout.WEST);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Initializes the history with a list of all dates, for which a history
     * with the given contact is availabled.
     */
    private void initData() {
        Collection c = this.msgHistory.findByEndDate(
                this.metaContact, new Date(System.currentTimeMillis()));

        Object[] msgArray = c.toArray();
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
        this.datesPanel.setSelected(datesPanel.getModel().getSize() - 1);
    }

    /**
     * Shows a history for a given period.
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     */
    public void showHistoryByPeriod(Date startDate, Date endDate) {

        ChatConversationPanel convPanel = null;

        this.historyPanel.removeAll();

        if(dateHistoryTable.containsKey(startDate)) {
            convPanel = (ChatConversationPanel)dateHistoryTable.get(startDate);

            this.historyPanel.add(convPanel);
        }
        else {
            Collection msgList = this.msgHistory.findByPeriod(
                    this.metaContact, startDate, endDate);

            convPanel = new ChatConversationPanel(this);

            this.historyPanel.add(convPanel);

            this.createHistory(convPanel, msgList, null);
            this.dateHistoryTable.put(startDate, convPanel);
        }
        this.historyPanel.revalidate();
        this.historyPanel.repaint();

        this.lastExecutedSearch = PERIOD_SEARCH;

        this.searchStartDate = startDate;
    }

    /**
     * Shows a history for a given keyword.
     * @param keyword the keyword to search
     */
    public void showHistoryByKeyword(String keyword) {

        Collection msgList = this.msgHistory.findByKeyword(
                this.metaContact, keyword);

        Object[] msgArray = msgList.toArray();
        Date date = null;
        ChatConversationPanel convPanel = null;

        this.datesPanel.removeAllDates();
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

        for(int i = 0; i < datesPanel.getDatesNumber(); i ++) {
            Date initDate = datesPanel.getDate(i);

            msgList = this.msgHistory.findByPeriod(
                    this.metaContact, initDate,
                    datesPanel.getNextDate(initDate));

            if(dateHistoryTable.contains(initDate)) {
                dateHistoryTable.remove(initDate);
            }

            convPanel = new ChatConversationPanel(this);

            this.createHistory(convPanel, msgList, keyword);
            this.dateHistoryTable.put(initDate, convPanel);
        }

        this.datesPanel.setSelected(datesPanel.getDatesNumber() - 1);

        this.lastExecutedSearch = KEYWORD_SEARCH;
        this.searchKeyword = keyword;
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
}
