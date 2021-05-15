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
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import net.java.sip.communicator.impl.neomedia.codec.video.h264.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.service.neomedia.codec.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.jitsi.utils.*;

public class EncodingConfigurationTab
    extends TransparentPanel
{
    private final JTable table;

    private final EncodingConfigurationTableModel tableModel;

    private final JButton upButton;

    private final JButton downButton;

    public EncodingConfigurationTab(MediaType type,
        EncodingConfiguration encodingConfiguration)
    {
        setLayout(new BorderLayout());

        // encodingConfiguration is null when it is loaded
        // from the general config
        boolean isEncodingConfigurationNull = false;
        if (encodingConfiguration == null)
        {
            isEncodingConfigurationNull = true;
            encodingConfiguration
                = NeomediaActivator.getMediaServiceImpl()
                .getCurrentEncodingConfiguration();
        }

        ResourceManagementService resources = NeomediaActivator.getResources();

        table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
        {
            @Override
            public Component getTableCellRendererComponent(JTable rtable,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column)
            {
                Component component = super.getTableCellRendererComponent(
                    rtable, value, isSelected, hasFocus, row, column);
                component.setEnabled(rtable != null && rtable.isEnabled());
                return component;
            }
        });

        String upKey = "impl.media.configform.UP";
        upButton = new JButton(resources.getI18NString(upKey));
        upButton.setMnemonic(resources.getI18nMnemonic(upKey));
        upButton.setOpaque(false);
        upButton.addActionListener(e -> move(true));

        String downKey = "impl.media.configform.DOWN";
        downButton = new JButton(resources.getI18NString(downKey));
        downButton.setMnemonic(resources.getI18nMnemonic(downKey));
        downButton.setOpaque(false);
        downButton.addActionListener(e -> move(false));

        Container buttonBar = new TransparentPanel(new GridLayout(0, 1));
        buttonBar.add(upButton);
        buttonBar.add(downButton);

        Container parentButtonBar = new TransparentPanel(new BorderLayout());
        parentButtonBar.add(buttonBar, BorderLayout.NORTH);

        tableModel = new EncodingConfigurationTableModel(
            type,
            encodingConfiguration);
        table.setModel(tableModel);

        // The first column contains the check boxes which enable/disable their
        // associated encodings and it doesn't make sense to make it wider than
        // the check boxes.
        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());

        table.getSelectionModel()
            .addListSelectionListener(this::tableSelectionListener);
        tableSelectionListener(null);

        setPreferredSize(new Dimension(350, 100));
        setMaximumSize(new Dimension(350, 100));

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(parentButtonBar, BorderLayout.EAST);

        // show openh264 panel on mac & windows, only for video and only in
        // general video encodings
        if (type == MediaType.VIDEO
            && isEncodingConfigurationNull
            && (OSUtils.IS_MAC || OSUtils.IS_WINDOWS))
        {
            add(OpenH264Retriever.getConfigPanel(), BorderLayout.SOUTH);
        }
    }

    private void move(boolean up)
    {
        int index = tableModel.move(table.getSelectedRow(), up);
        table.getSelectionModel().setSelectionInterval(index, index);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        table.setEnabled(enabled);
        if (enabled)
        {
            tableSelectionListener(null);
        }
        else
        {
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        }
    }

    private void tableSelectionListener(ListSelectionEvent event)
    {
        if (table.getSelectedRowCount() == 1)
        {
            int selectedRow = table.getSelectedRow();
            if (selectedRow > -1)
            {
                upButton.setEnabled(selectedRow > 0);
                downButton.setEnabled(selectedRow < table.getRowCount() - 1);
                return;
            }
        }

        upButton.setEnabled(false);
        downButton.setEnabled(false);
    }
}
