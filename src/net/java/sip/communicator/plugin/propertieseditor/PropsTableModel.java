/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import java.beans.*;

import javax.swing.event.*;
import javax.swing.table.*;
import org.jitsi.service.configuration.*;

/**
 * @author Marin Dzhigarov
 * @author Pawel Domas 
 *
 */
public class PropsTableModel 
    implements TableModel, 
    PropertyChangeListener 
{

    /**
     * The configuration service.
     */
    private ConfigurationService confService = PropertiesEditorActivator.getConfigurationService();

    /**
     * Creates an instance of <tt>PropsTableModel</tt>.
     */
    PropsTableModel() {}

    public void Register()
    {
        confService.addPropertyChangeListener(this);
    }
    public void Unregister()
    {
        confService.removePropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        for (TableModelListener l : listeners.getListeners(TableModelListener.class))
        {
            l.tableChanged(new TableModelEvent(this));
        }
    }

    public int getRowCount()
    {
        return confService.getAllPropertyNames().size();
    }

    /**
     * Returns the count of table columns.
     * @return int the count of table columns
     */
    public int getColumnCount()
    {
        return 2;
    }

    /**
     * Returns the column name given column index.
     * @param columnIndex The column index.
     * @return The column name.
     */
    public String getColumnName(int columnIndex)
    {
        if(columnIndex == 0)
        {
            return "Property";
        } else 
        {
            return "Value";
        }
    }

    /**
     * Returns the class of the column given a column index
     */
    public Class<?> getColumnClass(int columnIndex) 
    {
        return String.class;
    }

    /**
     * Returns true if the given cell is edittable.
     * Edittable cells in this model are cells with column index 1.
     * @param rowIndex The row index.
     * @param columnIndex The column index.
     * @return True if the column index is 1
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) 
    {
        return columnIndex == 1;
    }

    /**
     * Returns the value in the cell given by row and column.
     */
    public Object getValueAt(int rowIndex, int columnIndex) 
    {
        if (columnIndex == 0) 
        {
            return confService.getAllPropertyNames().get(rowIndex);
        } else 
        {
            return confService.getProperty(confService.getAllPropertyNames().get(rowIndex));
        }
    }

    /**
     * Sets aValue at a cell with coordinates [rowIndex, columnIndex]
     * @param aValue The given value.
     * @param rowIndex The row index that the value will be set to.
     * @param columnIndex The column index that the value will be set to.
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) 
    {
        String property = confService.getAllPropertyNames().get(rowIndex);
        confService.setProperty(property, aValue);
    }

    private EventListenerList listeners = new EventListenerList();

    public void addTableModelListener(TableModelListener l) 
    {
        listeners.add(TableModelListener.class, l);
    }

    public void removeTableModelListener(TableModelListener l) 
    {
        listeners.remove(TableModelListener.class, l);
    }
}