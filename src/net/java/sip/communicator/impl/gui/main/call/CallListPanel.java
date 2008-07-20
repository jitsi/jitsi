/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.customcontrols.SIPCommSmartComboBox.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CallListPanel</tt> is the panel that contains the call list.
 * 
 * @author Yana Stamcheva
 */
public class CallListPanel
    extends JPanel
    implements  ActionListener,
                PluginComponentListener
{
    private Logger logger = Logger.getLogger(CallListPanel.class);

    private JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
    
    private JLabel searchLabel = new JLabel(
        Messages.getI18NString("search").getText() + ": ");
    
    private SIPCommSmartComboBox searchComboBox = new SIPCommSmartComboBox();
    
    private CallList callList = new CallList();
    
    private JScrollPane scrollPane = new JScrollPane();
    
    private JPanel pluginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private CallHistoryService callHistory;
    
    private Date lastDateFromHistory;
    
    public static final int NUMBER_OF_CALLS = 30;
    
    private MainFrame mainFrame;
    
    private boolean filteredSearch = false;
    
    /**
     * Creates an instance of <tt>CallListPanel</tt>.
     */
    public CallListPanel(MainFrame mainFrame)
    {
        super(new BorderLayout());

        this.mainFrame = mainFrame;

        this.callHistory = GuiActivator.getCallHistoryService();

        this.searchComboBox.addActionListener(this);

        new LoadLastCallsFromHistory().start();

        this.initPanels();

        this.initPluginComponents();

        this.setPreferredSize(new Dimension(200, 450));
        this.setMinimumSize(new Dimension(80, 200));
    }
    
    /**
     * Initiates components contained in this panel.
     */
    private void initPanels()
    {
        this.scrollPane.setViewport(new ImageBackgroundViewport());
        this.scrollPane.getViewport().setView(callList);

        this.searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.searchPanel.add(searchLabel, BorderLayout.WEST);
        this.searchPanel.add(searchComboBox, BorderLayout.CENTER);
        this.searchPanel.add(pluginPanel, BorderLayout.NORTH);

        this.add(searchPanel, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Initiates plugin components.
     */
    private void initPluginComponents()
    {
        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_CALL_HISTORY.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            logger.error("Could not obtain plugin reference.", exc);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i ++)
            {
                PluginComponent component = (PluginComponent) GuiActivator
                    .bundleContext.getService(serRefs[i]);;

                this.pluginPanel.add((Component)component.getComponent());
            }
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }
    
    /**
     * Loads last n (NUMBER_OF_CALLS) number of calls from history and initiates
     * the call list and the combo box.    
     */
    private void loadHistoryCalls(Collection historyCalls)
    {        
        Iterator lastCalls = historyCalls.iterator();
        
        while(lastCalls.hasNext()) {
            CallRecord callRecord = (CallRecord)lastCalls.next();
            
            Date callStartDate = callRecord.getStartTime();
            
            if(lastDateFromHistory == null) {
                callList.addItem(processDate(callStartDate));
                lastDateFromHistory = callStartDate;
            }
            else {
                int compareResult = GuiUtils.compareDates(
                        callStartDate, lastDateFromHistory);
                
                if(compareResult != 0) {                    
                    callList.addItem(processDate(callStartDate));
                    lastDateFromHistory = callStartDate;
                }
            }
            
            Iterator participants
                = callRecord.getParticipantRecords().iterator();
            
            while(participants.hasNext()) {
                CallParticipantRecord participantRecord
                    = (CallParticipantRecord) participants.next();
             
                String participantName
                    = participantRecord.getParticipantAddress();
                
                addToCallComboBox(participantName);
                addToSearchComboBox(participantName);
                
                String direction;
                if(callRecord.getDirection().equals(CallRecord.IN))
                    direction = GuiCallParticipantRecord.INCOMING_CALL;
                else
                    direction = GuiCallParticipantRecord.OUTGOING_CALL;
            
                callList.addItem(
                        new GuiCallParticipantRecord(participantRecord,
                        direction));
            }            
        }
        if(callList.getModel().getSize() > 0)
            this.callList.addItem(
                Messages.getI18NString("olderCalls").getText() + "...");
    }
    
    /**
     * Adds the given object to the search combo box.
     * 
     * @param obj the object to add to the search combo box
     */
    private void addToSearchComboBox(Object obj)
    {
        FilterableComboBoxModel comboModel =
            (FilterableComboBoxModel)searchComboBox.getModel();
        
        String allItem = Messages.getI18NString("all").getText();

        if(!comboModel.contains(allItem)) {
            searchComboBox.addItem(allItem);
        }
        
        if(!comboModel.contains(obj))
            comboModel.addElement(obj);
        
    }
    
    /**
     * Adds the given object to the call combo box.
     * 
     * @param obj the object to add to the call combo box
     */
    private void addToCallComboBox(Object obj)
    {
        FilterableComboBoxModel callComboModel =
            (FilterableComboBoxModel)mainFrame.getCallManager()
                .getCallComboBox().getModel();
        
        if(!callComboModel.contains(obj)
                && callComboModel.getSize() <= CallComboBox.MAX_COMBO_SIZE)
            callComboModel.addElement(obj);
    }
    
    /**
     * Process the given date. If the date is the current date returns "Today"
     * string, otherwise returns the formatted date. 
     * @param date the date to process
     * @return a String representing the given date. If the date is the current
     * date returns "Today" string, otherwise returns a formatted string
     * representing the date
     */
    private String processDate(Date date)
    {
        String resultString;
        long currentDate = System.currentTimeMillis();
        
        if(GuiUtils.compareDates(date, new Date(currentDate)) == 0) {
            
            resultString = Messages.getI18NString("today").getText();
        }
        else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            
            resultString = GuiUtils.formatDate(date);
        }
        
        return resultString;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user selects an item
     * in the search combo box.
     */
    public void actionPerformed(ActionEvent e)
    {        
        String item = (String) searchComboBox.getEditor().getItem();
        
        if(item.length() > 0) {
            
            this.lastDateFromHistory = null;
            
            this.callList.removeAll();
            
            if(item.equals(Messages.getI18NString("all").getText())) {
                this.lastDateFromHistory = null;
                
                this.callList.removeAll();
                
                new LoadLastCallsFromHistory().start();
                
                filteredSearch = false;
            }
            else
                new LoadParticipantCallsFromHistory(item).start();
            
            filteredSearch = true;
        }
        else if (filteredSearch){
            
            this.lastDateFromHistory = null;
            
            this.callList.removeAll();
            
            new LoadLastCallsFromHistory().start();
            
            filteredSearch = false;
        }
    }
    
    /**
     * Makes a query to the call history in order to load in the call list last
     * N call records. 
     */
    private class LoadLastCallsFromHistory extends Thread
    {
        private Collection historyCalls;
        
        public void run()
        {
            historyCalls
                = callHistory.findLast(NUMBER_OF_CALLS);
        
            SwingUtilities.invokeLater(new Runnable() {

                public void run()
                {
                    loadHistoryCalls(historyCalls);
                }
            });
        }
    }
    
    /**
     * Makes a query to the call history in order to load in the call list all
     * call records for a given participant id. 
     */
    private class LoadParticipantCallsFromHistory extends Thread
    {   
        private LinkedList historyCallsCopy; 
        private String participantID;
        
        public LoadParticipantCallsFromHistory(String participantID)
        {
            this.participantID = participantID;
        }
        
        public void run()
        {
            Collection historyCalls
                = callHistory.findByParticipant(participantID);
            
            historyCallsCopy = new LinkedList(historyCalls);
            
            while(historyCallsCopy.size() > NUMBER_OF_CALLS) {
                historyCallsCopy.remove(historyCallsCopy.size() - 1);
            }
            
            SwingUtilities.invokeLater(new Runnable() {

                public void run()
                {
                    loadHistoryCalls(historyCallsCopy);
                }
            });
        }
    }

    /**
     * Returns the call list contained in this panel.
     * @return the call list contained in this panel
     */
    public CallList getCallList()
    {
        return callList;
    }
    
    public void addCallRecord(int index, GuiCallParticipantRecord callRecord)
    {
        if(callList.getModel().getSize() == 0) {
            callList.addItem(processDate(callRecord.getStartTime()), index);            
        }
        index ++;
        
        this.callList.addItem(callRecord, index);
        
        String participantName = callRecord.getParticipantName();
        
        this.addToCallComboBox(participantName);
        this.addToSearchComboBox(participantName);
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!c.getContainer()
                .equals(Container.CONTAINER_CALL_HISTORY))
            return;

        this.pluginPanel.add((Component) c.getComponent());
 
        this.revalidate();
        this.repaint();
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        // If the container id doesn't correspond to the id of the plugin
        // container we're not interested.
        if(!c.getContainer()
                .equals(Container.CONTAINER_CALL_HISTORY))
            return;
        
        this.pluginPanel.remove((Component) c.getComponent());
    }
}
