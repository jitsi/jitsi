/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message.history;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Calendar;
import java.util.Date;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.impl.gui.customcontrols.ExtListCellRenderer;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;

/**
 * The <tt>DatesPanel</tt> contains the list of history dates for a contact.
 * 
 * @author Yana Stamcheva
 */
public class DatesPanel extends JScrollPane
    implements ListSelectionListener {

    private  JList datesList = new JList();
    
    private DefaultListModel listModel = new DefaultListModel();
    
    private ExtListCellRenderer renderer = new ExtListCellRenderer();
    
    private Calendar calendar = Calendar.getInstance();
    
    private JPanel listPanel = new JPanel(new BorderLayout());
    
    private MessageHistoryService msgHistory = GuiActivator.getMsgHistoryService();
    
    private HistoryWindow historyWindow;
   
    /**
     * Creates an instance of <tt>DatesPanel</tt>.
     * 
     * @param historyWindow the parent <tt>HistoryWindow</tt>, where
     * this panel is contained.
     */
    public DatesPanel(HistoryWindow historyWindow) {
               
        this.historyWindow = historyWindow;
        
        this.datesList.setModel(listModel);
        
        this.datesList.setCellRenderer(renderer);
        
        this.datesList.setFont(Constants.FONT.deriveFont(Font.BOLD));
        
        this.datesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        this.listPanel.add(datesList, BorderLayout.NORTH);
        
        this.getViewport().add(listPanel);
        
        this.datesList.addListSelectionListener(this);
    }
    
    /**
     * Adds the given date to the list of dates.
     * @param date the date to add
     */
    public void addDate(Date date) {
        String dateString = this.dateToString(date);
        
        listModel.addElement(dateString);
    }
    
    /**
     * Removes the given date from the list of dates
     * @param date the date to remove
     */
    public void removeDate(Date date) {
        String dateString = this.dateToString(date);
        
        listModel.removeElement(dateString);
    }
    
    /**
     * Checks whether the given date is contained in the list
     * of history dates.
     * @param date the date to search for
     * @return TRUE if the given date is contained in the list
     * of history dates, FALSE otherwise
     */
    public boolean containsDate(Date date) {
        String dateString = this.dateToString(date);
        if(listModel.contains(dateString)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Transforms the given date into a string, which contains only
     * day, month and year.
     * @param date the date to transform into string
     * @return the string, obtained from the given date 
     */
    public String dateToString(Date date) {
        calendar.setTime(date);
        return this.processTime(calendar.get(Calendar.DAY_OF_MONTH)) + "/"
        + this.processTime(calendar.get(Calendar.MONTH) + 1) + "/"
        + this.processTime(calendar.get(Calendar.YEAR)); 
    }
    
    /**
     * Transforms the given string into date, which contains only
     * day, month and year.
     * @param dateString the string to transform into date
     * @return the <tt>Date</tt>, obtained from the given string
     */
    public Date stringToDate(String dateString) {
        
        int day = new Integer(dateString.substring(0, 2)).intValue();
        int month = new Integer(dateString.substring(3, 5)).intValue() - 1;
        int year = new Integer(dateString.substring(6)).intValue();
        
        calendar.set(year, month, day, 0, 0, 0);
        
        return calendar.getTime();
    }
    
    /**
     * Formats a time string.
     * 
     * @param time The time parameter could be hours, minutes or seconds.
     * @return The formatted minutes string.
     */
    private String processTime(int time) {

        String timeString = new Integer(time).toString();

        String resultString = "";
        if (timeString.length() < 2)
            resultString = resultString.concat("0").concat(timeString);
        else
            resultString = timeString;

        return resultString;
    }
    
    /**
     * Implements the <tt>ListSelectionListener.valueChanged</tt>.
     * Shows all history records for the selected date.
     */
    public void valueChanged(ListSelectionEvent e) {       
        int selectedIndex = this.datesList.getSelectedIndex();
        
        String dateString = (String)this.listModel.get(selectedIndex);
        
        Date nextDate;
        if(selectedIndex < listModel.getSize() - 1) {
            nextDate = stringToDate(
                    (String)this.listModel.get(selectedIndex + 1));
        }
        else {
            nextDate = new Date(System.currentTimeMillis()); 
        }
        
        this.historyWindow.showHistoryByPeriod(
                stringToDate(dateString),
                nextDate);
    } 
}
