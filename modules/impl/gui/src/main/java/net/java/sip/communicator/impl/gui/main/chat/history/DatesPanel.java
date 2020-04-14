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
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * The <tt>DatesPanel</tt> contains the list of history dates for a contact.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class DatesPanel
    extends SIPCommScrollPane
    implements ListSelectionListener
{
    private final JList datesList = new JList();

    /**
     * The <tt>ListModel</tt> of {@link #datesList} explicitly stored in order
     * to have it as a <tt>DefaultListModel</tt> instance.
     */
    private final DefaultListModel listModel = new DefaultListModel();

    private final HistoryWindow historyWindow;

    private int lastSelectedIndex = -1;

    /**
     * Creates an instance of <tt>DatesPanel</tt>.
     *
     * @param historyWindow the parent <tt>HistoryWindow</tt>, where
     * this panel is contained.
     */
    public DatesPanel(HistoryWindow historyWindow)
    {
        this.historyWindow = historyWindow;

        this.setPreferredSize(new Dimension(100, 100));
        this.setBorder(SIPCommBorders.getBoldRoundBorder());
        this.setOpaque(false);

        this.datesList.setModel(listModel);
        this.datesList.setCellRenderer(new DatesListRenderer());
        this.datesList.setFont(datesList.getFont().deriveFont(Font.BOLD));
        this.datesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.datesList.addListSelectionListener(this);

        JPanel listPanel = new TransparentPanel(new BorderLayout());
        listPanel.add(datesList, BorderLayout.NORTH);

        this.setViewportView(listPanel);
        this.getVerticalScrollBar().setUnitIncrement(30);
    }

    /**
     * Returns the number of dates contained in this dates panel.
     * @return the number of dates contained in this dates panel
     */
    public int getDatesNumber()
    {
        synchronized (listModel)
        {
            return listModel.size();
        }
    }

    /**
     * Returns the date at the given index.
     * @param index the index of the date in the list model
     * @return the date at the given index
     */
    public Date getDate(int index)
    {
        synchronized (listModel)
        {
            return (Date)listModel.get(index);
        }
    }

    /**
     * Returns the next date in the list.
     * @param date the date from which to start
     * @return the next date in the list
     */
    public Date getNextDate(Date date)
    {
        synchronized (listModel)
        {
            Date nextDate;
            int dateIndex = listModel.indexOf(date);

            if(dateIndex < listModel.getSize() - 1) {
                nextDate = getDate(dateIndex + 1);
            }
            else {
                nextDate = new Date(System.currentTimeMillis());
            }
            return nextDate;
        }
    }

    /**
     * Adds the given date to the list of dates.
     * @param date the date to add
     */
    public void addDate(Date date)
    {
        synchronized (listModel)
        {
            int listSize = listModel.size();
            boolean dateAdded = false;
            if(listSize > 0) {
                for(int i = 0; i < listSize; i ++) {
                    Date dateFromList = (Date)listModel.get(i);
                    if(dateFromList.after(date)) {
                        listModel.add(i, date);
                        dateAdded = true;
                        break;
                    }
                }
                if(!dateAdded) {
                    listModel.addElement(date);
                }
            }
            else {
                listModel.addElement(date);
            }
        }
    }

    /**
     * Removes all dates contained in this list.
     */
    public void removeAllDates()
    {
        synchronized (listModel)
        {
            listModel.removeAllElements();
        }
    }

    /**
     * Checks whether the given date is contained in the list
     * of history dates.
     * @param date the date to search for
     * @return TRUE if the given date is contained in the list
     * of history dates, FALSE otherwise
     */
    public boolean containsDate(Date date)
    {
        synchronized (listModel)
        {
            return listModel.contains(date);
        }
    }

    /**
     * Implements the <tt>ListSelectionListener.valueChanged</tt>.
     * Shows all history records for the selected date.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        synchronized (listModel)
        {
            int selectedIndex = this.datesList.getSelectedIndex();

            if(selectedIndex != -1 && lastSelectedIndex != selectedIndex)
            {
                this.setLastSelectedIndex(selectedIndex);
                Date date = (Date)this.listModel.get(selectedIndex);

                this.historyWindow.showHistoryByPeriod(
                        date,
                        historyWindow.getNextDateFromHistory(date));
            }
        }
    }

    /**
     * Selects the cell at the given index.
     * @param index the index of the cell to select
     */
    public void setSelected(int index)
    {
        this.datesList.setSelectedIndex(index);
    }

    /**
     * Returns the model of the contained list.
     * @return the model of the contained list
     */
    public ListModel getModel()
    {
        return listModel;
    }

    /**
     * Returns the index that was last selected.
     * @return the index that was last selected
     */
    public int getLastSelectedIndex()
    {
        return lastSelectedIndex;
    }

    /**
     * Sets the last selected index.
     * @param lastSelectedIndex the last selected index
     */
    public void setLastSelectedIndex(int lastSelectedIndex)
    {
        this.lastSelectedIndex = lastSelectedIndex;
    }
}
