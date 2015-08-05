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
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.dict4j.*;

/**
 * Class managing the list of strategies
 *
 * @author ROTH Damien
 */
public class StrategiesList
    extends JList
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ListModel model;
    private CellRenderer renderer;

    /**
     * Create an instance of the <tt>StrategiesList</tt>
     */
    public StrategiesList()
    {
        super();

        this.model = new ListModel();
        this.renderer = new CellRenderer();

        this.setCellRenderer(this.renderer);
        this.setModel(model);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setVisibleRowCount(6);
    }

    /**
     * Stores a new set of strategies
     * @param strategies List of strategies
     */
    public void setStrategies(List<Strategy> strategies)
    {
        this.model.setStrategies(strategies);
    }

    /**
     * Remove all the strategies of the list
     */
    public void clear()
    {
        this.model.clear();
    }

    /**
     * Automatic selection of strategies
     * @param initStrategy
     */
    public void autoSelectStrategy(String initStrategy)
    {
        int index = -1;

        if (initStrategy.length() > 0)
        {   // saved strategy
            index = this.model.indexOf(initStrategy);
        }
        if (index < 0)
        {
            // First case : levenstein distance
            index = this.model.indexOf("lev");
        }
        if (index < 0)
        {
            // Second case : soundex
            index = this.model.indexOf("soundex");
        }
        if (index < 0)
        {
            // Last case : prefix
            index = this.model.indexOf("prefix");
        }

        // If the index is still < 0, we select the first index
        if (index < 0)
        {
            index = 0;
        }
        if (index < this.getVisibleRowCount())
        {
            // If the index is visible row, we don't need to scroll
            this.setSelectedIndex(index);
        }
        else
        {
            // Otherwise, we scroll to the selected value
            this.setSelectedValue(this.model.getElementAt(index), true);
        }
    }

    /**
     * Class managing the list datas
     *
     * @author ROTH Damien
     */
    static class ListModel
        extends AbstractListModel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private List<Strategy> data;

        /**
         * Create an instance of <tt>ListModel</tt>
         */
        public ListModel()
        {
            data = new ArrayList<Strategy>();
        }

        /**
         * Stores the strategies into this model
         * @param strategies the strategies list
         */
        public void setStrategies(List<Strategy> strategies)
        {
            data = strategies;
            fireContentsChanged(this, 0, data.size());
        }

        /**
         * Remove all the strategies of the list
         */
        public void clear()
        {
            data.clear();
        }

        /**
         * Implements <tt>ListModel.getElementAt</tt>
         */
        public Strategy getElementAt(int row)
        {
            return data.get(row);
        }

        /**
         * Implements <tt>ListModel.getSize</tt>
         */
        public int getSize()
        {
            return data.size();
        }

        /**
         * Find the index of a strategy.
         *
         * @param strategyCode the code of the strategy
         * @return the index of the strategy
         */
        public int indexOf(String strategyCode)
        {
            for (int i = 0, size = data.size(); i < size; i++)
            {
                if (data.get(i).getCode().equals(strategyCode))
                    return i;
            }
            return -1;
        }
    }

    /**
     * Class managing the cell rendering
     *
     * @author ROTH Damien
     */
    static class CellRenderer
        extends JLabel
        implements ListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * implements <tt>ListCellRenderer.getListCellRendererComponent</tt>
         */
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
        {
            setText(((Strategy) value).getName());

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);

            return this;
        }
    }
}

