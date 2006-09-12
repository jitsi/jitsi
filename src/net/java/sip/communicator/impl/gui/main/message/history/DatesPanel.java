/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message.history;

import java.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.msghistory.*;
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
    
    private int lastSelectedIndex = -1;
    
    /**
     * Creates an instance of <tt>DatesPanel</tt>.
     *
     * @param historyWindow the parent <tt>HistoryWindow</tt>, where
     * this panel is contained.
     */
    public DatesPanel(HistoryWindow historyWindow) {

        this.historyWindow = historyWindow;

        this.setPreferredSize(new Dimension(100, 100));
        this.datesList.setModel(listModel);

        this.datesList.setCellRenderer(renderer);

        this.datesList.setFont(Constants.FONT.deriveFont(Font.BOLD));
        
        this.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 0),
                SIPCommBorders.getBoldRoundBorder()));

        this.datesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.listPanel.add(datesList, BorderLayout.NORTH);

        this.getViewport().add(listPanel);

        this.datesList.addListSelectionListener(this);
    }

    /**
     * Returns the number of dates contained in this dates panel.
     * @return the number of dates contained in this dates panel
     */
    public int getDatesNumber() {
        return listModel.size();
    }

    /**
     * Returns the date at the given index.
     * @param index the index of the date in the list model
     * @return the date at the given index
     */
    public Date getDate(int index) {
        return stringToDate((String)listModel.get(index));
    }

    /**
     * Returns the next date in the list.
     * @param date the date from which to start
     * @return the next date in the list
     */
    public Date getNextDate(Date date) {
        Date nextDate;
        int dateIndex = listModel.indexOf(dateToString(date));

        if(dateIndex < listModel.getSize() - 1) {
            nextDate = getDate(dateIndex + 1);
        }
        else {
            nextDate = new Date(System.currentTimeMillis());
        }
        return nextDate;
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
     * Removes all dates contained in this dates panel.
     */
    public void removeAllDates() {
        listModel.removeAllElements();
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
     * @return the <tt>Date</tt>, obtained from the given
        this.historyPanel.revalidate();
        this.historyPanel.repaint();
         string
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
        
        if(selectedIndex != -1 && lastSelectedIndex != selectedIndex) {
            this.setLastSelectedIndex(selectedIndex);
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

    /**
     * Selects the cell at the given index.
     * @param index the index of the cell to select
     */
    public void setSelected(int index) {
        this.datesList.setSelectedIndex(index);
    }

    public ListModel getModel() {
        return this.datesList.getModel();
    }

    public int getLastSelectedIndex()
    {
        return lastSelectedIndex;
    }

    public void setLastSelectedIndex(int lastSelectedIndex)
    {
        this.lastSelectedIndex = lastSelectedIndex;
    }
}
