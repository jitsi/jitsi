/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import java.util.*;

import net.java.sip.communicator.util.swing.*;

/**
 * Implements {@link TableModel} for encryption configuration (ZRTP, SDES and
 * MIKEY).
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public class EncryptionConfigurationTableModel
    extends MoveableTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private boolean[] selectionList;
    private String[] labelList;

    /**
     * Creates a new table model in order to manage the encryption protocols and
     * the corresponding priority.
     *
     * @param selectionList A list of boolean which is used to know of the
     * corresponding protocol (same index) from the labelList is enabled or
     * disabled.
     * @param labelList The list of encryption protocols in the priority
     * order.
     */
    public EncryptionConfigurationTableModel(
            boolean[] selectionList,
            String[] labelList)
    {
        this.init(selectionList, labelList);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return
            (columnIndex == 0)
                ? Boolean.class
                : super.getColumnClass(columnIndex);
    }

    public int getColumnCount()
    {
        return 2;
    }

    public int getRowCount()
    {
        //return getEncodings().length;
        return labelList.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch (columnIndex)
        {
            case 0:
                return selectionList[rowIndex];
            case 1:
                return labelList[rowIndex];
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if ((columnIndex == 0) && (value instanceof Boolean))
        {
            this.selectionList[rowIndex] = ((Boolean) value).booleanValue(); 

            // We fire the update event before setting the configuration
            // property in order to have more reactive user interface.
            fireTableCellUpdated(rowIndex, columnIndex);

            //encodingConfiguration.setPriorityConfig(encoding, priority);
        }
    }

    /**
     * Move the row.
     *
     * @param rowIndex index of the row
     * @param up true to move up, false to move down
     * @return the next row index
     */
    public int move(int rowIndex, boolean up)
    {
        int toRowIndex;
        if (up)
        {
            toRowIndex = rowIndex - 1;
            if (toRowIndex < 0)
                throw new IllegalArgumentException("rowIndex");
        }
        else
        {
            toRowIndex = rowIndex + 1;
            if (toRowIndex >= getRowCount())
                throw new IllegalArgumentException("rowIndex");
        }

        // Swaps the selection list.
        boolean tmpSelectionItem = this.selectionList[rowIndex];
        this.selectionList[rowIndex] = this.selectionList[toRowIndex];
        this.selectionList[toRowIndex] = tmpSelectionItem;

        // Swaps the label list.
        String tmpLabel = this.labelList[rowIndex];
        this.labelList[rowIndex] = this.labelList[toRowIndex];
        this.labelList[toRowIndex] = tmpLabel;

        fireTableRowsUpdated(rowIndex, toRowIndex);
        return toRowIndex;
    }

    /**
     * Returns the list of the enabled or disabled label in the priority order.
     *
     * @param enabledLabels If true this function will return the enabled label
     * list. Otherwise, it will return the disabled list.
     *
     * @return the list of the enabled or disabled label in the priority order.
     */
    public List<String> getLabels(boolean enabledLabels)
    {
        ArrayList<String> labels = new ArrayList<String>(this.labelList.length);
        for(int i = 0; i < this.labelList.length; ++i)
        {
            if(this.selectionList[i] == enabledLabels)
            {
                labels.add(this.labelList[i]);
            }
        }
        return labels;
    }

    /**
     * Returns if the label is enabled or disabled.
     *
     * @param label The label to be determined as enabled or disabled.
     *
     * @return True if the label given in parameter is enabled. False,
     * otherwise.
     */
    public boolean isEnabledLabel(String label)
    {
        for(int i = 0; i < this.labelList.length; ++i)
        {
            if(this.labelList[i].equals(label))
            {
                return this.selectionList[i];
            }
        }
        return false;
    }

    /**
     * Initiates this table model in order to manage the encryption protocols and
     * the corresponding priority.
     *
     * @param selectionList A list of boolean which is used to know of the
     * corresponding protocol (same index) from the labelList is enabled or
     * disabled.
     * @param labelList The list of encryption protocols in the priority
     * order.
     */
    public void init(boolean[] selectionList, String[] labelList)
    {
        this.selectionList = selectionList;
        this.labelList = labelList;
    }
}
