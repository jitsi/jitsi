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
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import org.jitsi.service.resources.*;

/**
 * Creates a component containing a table and two buttons for the encodings of
 * type(AUDIO or VIDEO) or to sort the priority of the encryption protocols.
 *
 * @author Vincent Lucas
 */
public class PriorityTable
    extends TransparentPanel
{
    /**
     * The table containing the different elements to sort by priority.
     */
    private JTable table;

    /**
     * The button to increase the priority of one item by moving it up in the
     * table.
     */
    private JButton upButton;

    /**
     * The button to decrease the priority of one item by moving it down in the
     * table.
     */
    private JButton downButton;

    /**
     * The preferred width of all panels.
     */
    private final static int WIDTH = 350;

    /**
     * Creates a component for the encodings of type(AUDIO or VIDEO) or to sort
     * the priority of the encryption protocols.
     * @param tableModel The table model to display encodings (AUDIO or VIDEO),
     * or to sort the priority of the encryption protocols.
     * @param height The height (preferred and maximum height) of the component.
     * @return the component.
     */
    public PriorityTable(
            MoveableTableModel tableModel,
            int height)
    {
        super(new BorderLayout());

        ResourceManagementService resources = DesktopUtilActivator.getResources();
        String key;

        table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);

        key = "impl.media.configform.UP";
        upButton = new JButton(resources.getI18NString(key));
        upButton.setMnemonic(resources.getI18nMnemonic(key));
        upButton.setOpaque(false);

        key = "impl.media.configform.DOWN";
        downButton = new JButton(resources.getI18NString(key));
        downButton.setMnemonic(resources.getI18nMnemonic(key));
        downButton.setOpaque(false);

        Container buttonBar = new TransparentPanel(new GridLayout(0, 1));
        buttonBar.add(upButton);
        buttonBar.add(downButton);

        Container parentButtonBar = new TransparentPanel(new BorderLayout());
        parentButtonBar.add(buttonBar, BorderLayout.NORTH);

        //Container container = new TransparentPanel(new BorderLayout());
        this.setPreferredSize(new Dimension(WIDTH, height));
        this.setMaximumSize(new Dimension(WIDTH, height));

        this.add(new JScrollPane(table), BorderLayout.CENTER);
        this.add(parentButtonBar, BorderLayout.EAST);

        table.setModel(tableModel);

        /*
         * The first column contains the check boxes which enable/disable their
         * associated encodings and it doesn't make sense to make it wider than
         * the check boxes.
         */
        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());

        ListSelectionListener tableSelectionListener =
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent event)
                {
                    if (table.getSelectedRowCount() == 1)
                    {
                        int selectedRow = table.getSelectedRow();
                        if (selectedRow > -1)
                        {
                            upButton.setEnabled(selectedRow > 0);
                            downButton.setEnabled(selectedRow < (table
                                .getRowCount() - 1));
                            return;
                        }
                    }
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                }
            };
        table.getSelectionModel().addListSelectionListener(
            tableSelectionListener);
        tableSelectionListener.valueChanged(null);

        ActionListener buttonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                Object source = event.getSource();
                boolean up;
                if (source == upButton)
                    up = true;
                else if (source == downButton)
                    up = false;
                else
                    return;

                move(up);
            }
        };
        upButton.addActionListener(buttonListener);
        downButton.addActionListener(buttonListener);
    }

    /**
     * Used to move encoding options.
     * @param table the table with encodings
     * @param up move direction.
     */
    private void move(boolean up)
    {
        int index =
            ((MoveableTableModel) table.getModel()).move(table
                .getSelectedRow(), up);
        table.getSelectionModel().setSelectionInterval(index, index);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.table.setEnabled(enabled);
        this.upButton.setEnabled(enabled);
        this.downButton.setEnabled(enabled);
    }
}
