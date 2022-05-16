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
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.notification.event.*;

/**
 * @author Alexandre Maillard
 * @author Yana Stamcheva
 */
public class NotificationsTable
    extends JScrollPane
    implements NotificationChangeListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The enabled state of the notification action.
     */
    public static final String ENABLED = "Enabled";

    /**
     * The disabled state of the notification action.
     */
    public static final String DISABLED = "Disabled";

    /**
     * The notifications table.
     */
    private final NotificationTable notifTable;

    /**
     * The notifications table model.
     */
    private final NotificationsTableModel model;

    /**
     * The configuration panel.
     */
    private final NotificationConfigurationPanel configPanel;

    /**
     * The notification service.
     */
    private NotificationService notificationService = null;

    /**
     * Creates an instance of the <tt>NotificationsTable</tt>.
     * @param columns an array containing all columns
     * @param columnToolTips an array containing all column tooltips
     * @param panel the parent configuration panel
     */
    NotificationsTable( Object columns[],
                        String columnToolTips[],
                        NotificationConfigurationPanel panel)
    {
        configPanel = panel;

        notificationService
                = NotificationConfigurationActivator.getNotificationService();
        notificationService.addNotificationChangeListener(this);

        String strTmp = new String();

        model = new NotificationsTableModel(columns, 0);

        notifTable = new NotificationTable(model, columnToolTips);

        notifTable.setRowSelectionAllowed(true);
        notifTable.getTableHeader().setReorderingAllowed(false);
        notifTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notifTable.addMouseListener(new NotificationsCellListener());
        notifTable.setDefaultRenderer(  String.class,
                                        new StringTableRenderer());
        notifTable.setDefaultRenderer(  NotificationEntry.class,
                                        new StringTableRenderer());
        notifTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    if (!e.getValueIsAdjusting())
                    {
                        int row = notifTable.getSelectedRow();

                        if (row > -1)
                        {
                            configPanel.setNotificationEntry(
                                    getNotificationEntry(row));
                        }
                    }
                }
            });

        initTableData();

        // Sort the table by description; otherwise, it looks chaotic.
        if (columns.length != 0)
        {
            TableRowSorter<NotificationsTableModel> sorter
                = new TableRowSorter<NotificationsTableModel>(model);
            int column = columns.length - 1;
            java.util.List<RowSorter.SortKey> sortKeys
                = Arrays.asList(
                        new RowSorter.SortKey(
                                column,
                                SortOrder.ASCENDING));

            sorter.setComparator(
                    column,
                    new Comparator<NotificationEntry>()
                    {
                        public int compare(
                                NotificationEntry ne1,
                                NotificationEntry ne2)
                        {
                            String s1
                                = Resources.getString(
                                        "plugin.notificationconfig.event."
                                            + ne1.getEvent());
                            String s2
                                = Resources.getString(
                                        "plugin.notificationconfig.event."
                                            + ne2.getEvent());

                            if (s1 == null)
                                return (s2 == null) ? 0 : -1;
                            else
                                return (s2 == null) ? 1 : s1.compareTo(s2);
                        }
                    });
            sorter.setSortKeys(sortKeys);
            notifTable.setRowSorter(sorter);
        }

        for(int i = 0; i < columns.length; i++)
        {
            TableColumn tmp = notifTable.getColumnModel().getColumn(i);
            if(columns[i].getClass() != strTmp.getClass())
            {
                tmp.setHeaderRenderer(
                    new TableHeaderRenderer((JLabel)columns[i]));
                tmp.setHeaderValue("");
            }
            else
            {
                tmp.setHeaderValue(columns[i]);
            }

            if(i < columns.length - 1)
            {
                tmp.setMaxWidth(25);
                tmp.setMinWidth(25);
                tmp.setPreferredWidth(25);
            }
        }
        this.getViewport().add(notifTable);
    }

    /**
     * Initializes table's data.
     */
    private void initTableData()
    {
        for(String eventType : notificationService.getRegisteredEvents())
        {
            PopupMessageNotificationAction popupHandler
                = (PopupMessageNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_POPUP_MESSAGE);

            CommandNotificationAction programHandler
                = (CommandNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_COMMAND);

            SoundNotificationAction soundHandler
                = (SoundNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventType, NotificationAction.ACTION_SOUND);

            NotificationEntry entry = new NotificationEntry(
                notificationService.isActive(eventType),
                programHandler != null && programHandler.isEnabled(),
                (programHandler != null)
                    ? programHandler.getDescriptor()
                    : null,
                popupHandler != null
                    && popupHandler.isEnabled(),
                soundHandler != null
                    && soundHandler.isEnabled()
                    && soundHandler.isSoundNotificationEnabled(),
                soundHandler != null
                    && soundHandler.isEnabled()
                    && soundHandler.isSoundPlaybackEnabled(),
                soundHandler != null
                    && soundHandler.isEnabled()
                    && soundHandler.isSoundPCSpeakerEnabled(),
                (soundHandler != null)
                    ? soundHandler.getDescriptor()
                    : null,
                eventType);

            addEntry(entry);
        }
    }

    /**
     * Adds a line to the notifications table.
     * @param entry the <tt>NotificationsTableEntry</tt> to add
     */
    private void addEntry(NotificationEntry entry)
    {
        Object row[] = new Object[7];

        row[0] = Boolean.valueOf(entry.getEnabled());
        row[1] = (entry.getProgram()) ? ENABLED : DISABLED;
        row[2] = entry.getPopup() ? ENABLED : DISABLED;
        row[3] = (entry.getSoundNotification()) ? ENABLED : DISABLED;
        row[4] = (entry.getSoundPlayback()) ? ENABLED : DISABLED;
        row[5] = (entry.getSoundPCSpeaker()) ? ENABLED : DISABLED;
        row[6] = entry;

        this.addLine(row);
    }

    /**
     * Adds a line to the notifications table.
     * @param entry the <tt>NotificationsTableEntry</tt> to add
     */
    private void setEntry(NotificationEntry entry)
    {
        int row = indexOfEntry(entry);

        notifTable.setValueAt(new Boolean(entry.getEnabled()), row, 0);
        notifTable.setValueAt((entry.getProgram()) ? ENABLED : DISABLED, row, 1);
        notifTable.setValueAt(entry.getPopup() ? ENABLED : DISABLED, row, 2);
        notifTable.setValueAt((entry.getSoundNotification())
            ? ENABLED : DISABLED, row, 3);
        notifTable.setValueAt((entry.getSoundPlayback())
            ? ENABLED : DISABLED, row, 4);
        notifTable.setValueAt((entry.getSoundPCSpeaker())
            ? ENABLED : DISABLED, row, 5);
        notifTable.setValueAt(entry, row, 6);
    }

    /**
     * Removes the row corresponding to the given <tt>entry</tt> from the table.
     * @param entry the <tt>NotificationEntry</tt> to remove
     */
    private void removeEntry(NotificationEntry entry)
    {
        int row = indexOfEntry(entry);
        notifTable.removeRowSelectionInterval(row, row);
    }

    /**
     * Adds a line to the notifications table.
     * @param data the array of data to add
     */
    private void addLine(Object data[])
    {
        if(data.length == model.getColumnCount())
            model.addRow(data);
    }

    /**
     * Returns the currently selected row.
     * @return the currently selected row
     */
    public int getSelectedRow()
    {
        return notifTable.getSelectedRow() ;
    }

    /**
     * Returns the number of lines of the table.
     * @return the number of lines of the table
     */
    public int getRowCount()
    {
        return notifTable.getRowCount();
    }

    /**
     * Selects the given row.
     * @param row the row to select
     */
    public void setSelectedRow(int row)
    {
        notifTable.setRowSelectionInterval(row, row);
    }

    /**
     * Returns the notification entry at the given <tt>row</tt>.
     * @param row the row number, which notification entry we're looking for
     * @return the notification entry at the given <tt>row</tt>
     */
    public NotificationEntry getNotificationEntry(int row)
    {
        return (NotificationEntry) notifTable
            .getValueAt(row, notifTable.getColumnCount() - 1);
    }

    /**
     * Returns the notification entry at the given <tt>row</tt>.
     * @param eventName the name of the event, which corresponding
     * <tt>NotificationEntry</tt> we're looking for
     * @return the notification entry at the given <tt>row</tt>
     */
    private NotificationEntry getNotificationEntry(String eventName)
    {
        for (int row = 0; row < notifTable.getRowCount(); row++)
        {
            NotificationEntry entry = getNotificationEntry(row);

            if (entry.getEvent() == eventName)
                return entry;
        }
        return null;
    }

    /**
     * Adding a mouse listener on the table.
     * @param l the <tt>MouseListener</tt> to add
     */
    @Override
    public void addMouseListener(MouseListener l)
    {
       notifTable.addMouseListener(l);
    }

    /**
     * Returns the row number corresponding to the given <tt>point</tt>.
     * @param point the point under which is the row we're looking for
     * @return the row number corresponding to the given <tt>point</tt>
     */
    public int rowAtPoint(Point point)
    {
        return notifTable.rowAtPoint(point);
    }

    /**
     * Returns the row index of the given <tt>entry</tt>.
     * @param entry the entry, which row we're looking for
     * @return the row index of the given <tt>entry</tt>
     */
    private int indexOfEntry(NotificationEntry entry)
    {
        for (int row = 0; row < notifTable.getRowCount(); row++)
        {
            NotificationEntry e = getNotificationEntry(row);

            if (e.equals(entry))
                return row;
        }
        return -1;
    }

    /**
     * Returns the icon value of the given column if the the column supports an
     * icon value, otherwise returns null.
     * @param column the number of the column
     * @return the icon value of the given column if the the column supports an
     * icon value, otherwise returns null
     */
    public static Icon getColumnIconValue(int column)
    {
        if(column == 1)
        {
            return new ImageIcon(Resources.getImageInBytes(
                "plugin.notificationconfig.PROG_ICON"));
        }
        else if(column == 2)
        {
            return new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.POPUP_ICON"));
        }
        else if(column == 3)
        {
            return new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON_NOTIFY"));
        }
        else if(column == 4)
        {
            return new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON_PLAYBACK"));
        }
        else if(column == 5)
        {
            return new ImageIcon(Resources.getImageInBytes(
                            "plugin.notificationconfig.SOUND_ICON"));
        }
        return null;
    }

    /**
     * Extends the JTable to make easier to use with this plug-in.
     */
    static class NotificationTable extends JTable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private String[] columnToolTips;

        /**
         * Creates an instance of MyJTable.
         * @param model the model of the table
         */
        NotificationTable(TableModel model, String[] toolTips)
        {
            super(model);
            this.columnToolTips = toolTips;
        }

        /**
         * Creates the default table header.
         * @return the table header
         */
        @Override
        protected JTableHeader createDefaultTableHeader()
        {
            return new JTableHeader(columnModel)
            {
                /**
                 * Serial version UID.
                 */
                private static final long serialVersionUID = 0L;

                @Override
                public String getToolTipText(MouseEvent e)
                {
                    java.awt.Point p = e.getPoint();
                    int index = columnModel.getColumnIndexAtX(p.x);
                    int realIndex =
                            columnModel.getColumn(index).getModelIndex();
                    return Resources.getString(columnToolTips[realIndex]);
                }
            };
        }
    }

    /**
     * The data model used in the notifications table.
     */
    private class NotificationsTableModel
        extends DefaultTableModel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Creates an instance of <tt>NotificationsTableModel</tt>.
         * @param columns the array of column names
         * @param rowCount the number of rows
         */
        NotificationsTableModel(Object[] columns, int rowCount)
        {
            super(columns, rowCount);
        }

        /**
         * Returns the class of the given column.
         * @param c the column number
         * @return  the class of the given column
         */
        @Override
        public Class<?> getColumnClass(int c)
        {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col)
        {
            if (col == 0)
                return true;
            return false;
        }
    }

    /**
     * Mouse listener that listens for clicks on image columns.
     */
    private class NotificationsCellListener
        extends MouseAdapter
    {
        /**
         * Invoked when the mouse button has been clicked (pressed
         * and released) on a component.
         * @param e the <tt>MouseEvent</tt> that notified us
         */
        @Override
        public void mouseClicked(MouseEvent e)
        {
            int row = notifTable.rowAtPoint(e.getPoint());
            int col = notifTable.columnAtPoint(e.getPoint());

            Object o = notifTable.getValueAt(row, col);

            if (col > 0 && col < 6)
                if (o.equals(ENABLED))
                    notifTable.setValueAt(DISABLED, row, col);
                else
                    notifTable.setValueAt(ENABLED, row, col);

            NotificationEntry entry = getNotificationEntry(row);

            switch(col)
            {
            case 0:
                boolean isActive
                    = notifTable.getValueAt(row, 0).equals(Boolean.TRUE);
                entry.setEnabled(isActive);

                notificationService.setActive(entry.getEvent(), isActive);

                notifTable.repaint();
                break;
            case 1:
                boolean isProgram
                    = notifTable.getValueAt(row, 1).equals(ENABLED);

                entry.setProgram(isProgram);
                if(isProgram)
                {
                    notificationService.registerNotificationForEvent(
                            entry.getEvent(),
                            NotificationAction.ACTION_COMMAND,
                            entry.getProgramFile(),
                            "");
                }
                else
                {
                    notificationService.removeEventNotificationAction(
                            entry.getEvent(),
                            NotificationAction.ACTION_COMMAND);
                }
                break;
            case 2:
                boolean isPopup = notifTable.getValueAt(row, 2).equals(ENABLED);
                entry.setPopup(isPopup);

                if(isPopup)
                {
                    notificationService.registerNotificationForEvent(
                            entry.getEvent(),
                            NotificationAction.ACTION_POPUP_MESSAGE,
                            "",
                            "");
                }
                else
                {
                    notificationService.removeEventNotificationAction(
                            entry.getEvent(),
                            NotificationAction.ACTION_POPUP_MESSAGE);
                }
                break;
            case 3:
                boolean isSoundNotification
                    = notifTable.getValueAt(row, 3).equals(ENABLED);
                entry.setSoundNotification(isSoundNotification);

                SoundNotificationAction soundNotificationAction
                    = (SoundNotificationAction)
                    notificationService.getEventNotificationAction(
                            entry.getEvent(), NotificationAction.ACTION_SOUND);
                soundNotificationAction
                    .setSoundNotificationEnabled(isSoundNotification);
                notificationService.registerNotificationForEvent(
                        entry.getEvent(),
                        soundNotificationAction);
                break;
            case 4:
                boolean isSoundPlayback
                    = notifTable.getValueAt(row, 4).equals(ENABLED);
                entry.setSoundPlayback(isSoundPlayback);

                SoundNotificationAction soundPlaybackAction
                    = (SoundNotificationAction)
                    notificationService.getEventNotificationAction(
                            entry.getEvent(), NotificationAction.ACTION_SOUND);

                soundPlaybackAction.setSoundPlaybackEnabled(isSoundPlayback);
                notificationService.registerNotificationForEvent(
                        entry.getEvent(),
                        soundPlaybackAction);
                break;
            case 5:
                boolean isPCSpeakerSound
                    = notifTable.getValueAt(row, 5).equals(ENABLED);
                entry.setSoundPCSpeaker(isPCSpeakerSound);

                SoundNotificationAction soundPCSpeakerAction
                    = (SoundNotificationAction)
                    notificationService.getEventNotificationAction(
                            entry.getEvent(), NotificationAction.ACTION_SOUND);
                soundPCSpeakerAction.setSoundPCSpeakerEnabled(isPCSpeakerSound);
                notificationService.registerNotificationForEvent(
                        entry.getEvent(),
                        soundPCSpeakerAction);
                break;

            };

            configPanel.setNotificationEntry(entry);
        }
    }

    /**
     * Action Listener Service Notifications
     * @param event the <tt>NotificationActionTypeEvent</tt> that notified us
     */
    public void actionAdded(NotificationActionTypeEvent event)
    {
        String eventName = event.getSourceEventType();
        NotificationEntry entry = getNotificationEntry(eventName);

        NotificationAction handler = event.getActionHandler();
        boolean isActionEnabled = (handler != null && handler.isEnabled());

        if(entry == null)
        {
            entry = new NotificationEntry();
            addEntry(entry);
        }

        entry.setEvent(eventName);

        if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_POPUP_MESSAGE))
        {
            entry.setPopup(isActionEnabled);
        }
        else if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_COMMAND))
        {
            entry.setProgram(isActionEnabled);

            entry.setProgramFile(((CommandNotificationAction)event
                    .getActionHandler()).getDescriptor());
        }
        else if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_SOUND))
        {
            entry.setSoundNotification(isActionEnabled);
            entry.setSoundPlayback(isActionEnabled);

            entry.setSoundFile(((SoundNotificationAction)event
                    .getActionHandler()).getDescriptor());
        }
        entry.setEnabled(notificationService.isActive(eventName));

        setEntry(entry);
        notifTable.repaint();
    }

    /**
     * Action Listener Service Notifications
     * @param event the <tt>NotificationActionTypeEvent</tt> that notified us
     */
    public void actionRemoved(NotificationActionTypeEvent event)
    {
        String eventName = event.getSourceEventType();
        NotificationEntry entry = getNotificationEntry(eventName);

        if(entry == null)
            return;

        if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_POPUP_MESSAGE))
        {
            entry.setPopup(false);
        }
        else if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_COMMAND))
        {
            entry.setProgram(false);
            entry.setProgramFile("");
        }
        else if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_SOUND))
        {
            entry.setSoundNotification(false);
            entry.setSoundPlayback(false);
            entry.setSoundPCSpeaker(false);
            entry.setSoundFile("");
        }

        setEntry(entry);
        notifTable.repaint();
    }

    /**
     * Action Listener Service Notifications
     * @param event the <tt>NotificationActionTypeEvent</tt> that notified us
     */
    public void actionChanged(NotificationActionTypeEvent event)
    {
        String eventName = event.getSourceEventType();
        NotificationEntry entry = getNotificationEntry(eventName);

        if(entry == null)
            return;

        if(event.getActionHandler().getActionType()
                .equals(NotificationAction.ACTION_COMMAND))
        {
            entry.setProgramFile(((CommandNotificationAction)event
                    .getActionHandler()).getDescriptor());
        }
        else if(event.getActionHandler().getActionType()
                        .equals(NotificationAction.ACTION_SOUND))
        {
            entry.setSoundFile(((SoundNotificationAction)event
                    .getActionHandler()).getDescriptor());
        }

        entry.setEnabled(notificationService.isActive(eventName));

        setEntry(entry);
        notifTable.repaint();
    }

    /**
     * Adds the event to the notifications table.
     * @param event the event to add
     */
    public void eventTypeAdded(NotificationEventTypeEvent event)
    {
        String eventName = event.getSourceEventType();
        NotificationEntry entry = getNotificationEntry(eventName);

        if(entry == null)
        {
            PopupMessageNotificationAction popupHandler
                = (PopupMessageNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventName, NotificationAction.ACTION_POPUP_MESSAGE);

            CommandNotificationAction programHandler
                = (CommandNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventName, NotificationAction.ACTION_COMMAND);

            SoundNotificationAction soundHandler
                = (SoundNotificationAction) notificationService
                    .getEventNotificationAction(
                        eventName, NotificationAction.ACTION_SOUND);

            entry = new NotificationEntry(
                notificationService.isActive(event.getSourceEventType()),
                programHandler != null && programHandler.isEnabled(),
                (programHandler != null) ? programHandler.getDescriptor() : null,
                popupHandler != null && popupHandler.isEnabled(),
                soundHandler != null && soundHandler.isSoundNotificationEnabled(),
                soundHandler != null && soundHandler.isSoundPlaybackEnabled(),
                soundHandler != null && soundHandler.isSoundPCSpeakerEnabled(),
                (soundHandler != null) ? soundHandler.getDescriptor() : null,
                eventName);

            addEntry(entry);

            notifTable.repaint();
        }
    }

    /**
     * Removes the event from the notifications table.
     * @param event the event to remove
     */
    public void eventTypeRemoved(NotificationEventTypeEvent event)
    {
        String eventName = event.getSourceEventType();
        NotificationEntry entry = getNotificationEntry(eventName);

        if(entry != null)
            removeEntry(entry);

        notifTable.repaint();
    }

    /**
     * Clears the content of the notifications table.
     */
    public void clear()
    {
        for(int numrows = model.getRowCount(), i = numrows - 1; i >=0; i--)
            model.removeRow(i);
    }
}
