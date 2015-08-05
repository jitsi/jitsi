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
package net.java.sip.communicator.plugin.chatconfig.replacement;

import java.util.*;

import javax.swing.table.*;

import net.java.sip.communicator.plugin.chatconfig.*;
import net.java.sip.communicator.service.replacement.*;

import org.jitsi.service.configuration.*;

/**
 * Table model for the table in <tt>ReplacementConfigPanel</tt> listing all
 * available replacement sources
 *
 * @author Purvesh Sahoo
 */
public class ReplacementConfigurationTableModel
    extends AbstractTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The source list of all the available replacement sources
     */
    private ArrayList<String> sourceList;

    /**
     * The configuration service
     */
    private static ConfigurationService configService =
        ChatConfigActivator.getConfigurationService();

    /**
     * Creates an instance of <tt>ReplacementConfigurationTableModel</tt> by
     * specifying the source list.
     *
     * @param source the source list to initialize the table model with
     */
    public ReplacementConfigurationTableModel(ArrayList<String> source)
    {
        this.sourceList = source;
    }

    /**
     * @param columnIndex
     * @return the Class of the column. <tt>Boolean</tt> for the first column.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return (columnIndex == 0) ? Boolean.class : super
            .getColumnClass(columnIndex);
    }

    /**
     * {@inheritDoc }
     */
    public int getColumnCount()
    {
        return 2;
    }

    /**
     * {@inheritDoc }
     */
    public int getRowCount()
    {
        return sourceList.size();
    }

    /**
     * @param rowIndex the row index.
     * @param columnIndex the column index
     *
     * @return the value specified rowIndex and columnIndex. boolean in case of
     *         the first column, String replacement source label in case of the
     *         second column; null otherwise
     */
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        String sourceName = sourceList.get(rowIndex);
        ReplacementService source =
            ChatConfigActivator.getReplacementSources().get(sourceName);

        switch (columnIndex)
        {
        case 0:
            boolean e =
                configService.getBoolean(ReplacementProperty
                    .getPropertyName(source.getSourceName()), true);
            return e;
        case 1:
            return sourceName;
        default:
            return null;
        }

    }

    /**
     * @param rowIndex the row index
     * @param columnIndex the column index
     *
     * @return boolean; true for first column false otherwise
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    /**
     * Set the value at rowIndex and columnIndex. Sets the replacement source
     * property enabled/disabled based on whether the first column is true or
     * false.
     *
     * @param value The object to set at rowIndex and columnIndex
     * @param rowIndex
     * @param columnIndex
     */
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if ((columnIndex == 0) && (value instanceof Boolean))
        {
            String sourceName = sourceList.get(rowIndex);
            ReplacementService source =
                ChatConfigActivator.getReplacementSources().get(sourceName);

            boolean e = (Boolean) value;
            configService.setProperty(ReplacementProperty
                .getPropertyName(source.getSourceName()), e);

            fireTableCellUpdated(rowIndex, columnIndex);

        }
    }

}
