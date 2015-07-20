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
package net.java.sip.communicator.impl.gui.main.chatroomslist.createforms;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the account,
 * for which the new chat room will be created.
 *
 * @author Yana Stamcheva
 */
public class SelectAccountPanel
    extends TransparentPanel
{
    /**
     *
     */
    private static final long serialVersionUID = 7709876019954774312L;

    private final JScrollPane tablePane = new JScrollPane(
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JTable accountsTable;

    private final DefaultTableModel tableModel = new DefaultTableModel();

    private final NewChatRoom newChatRoom;

    private final Iterator<ProtocolProviderService> protocolProvidersList;

    private final JPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1));

    private final JPanel rightPanel
            = new TransparentPanel(new BorderLayout(10, 10));

    private final SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_PROVIDERS_FOR_CHAT_ROOM"));

    private final JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources().getI18NString("service.gui.SELECT_ACCOUNT"),
        JLabel.CENTER);

    private final ButtonGroup radioButtonGroup = new ButtonGroup();

    /**
     * Creates and initializes the <tt>SelectAccountPanel</tt>.
     *
     * @param newChatRoom an object that collects all user choices through the
     * wizard
     * @param protocolProvidersList The list of available
     * <tt>ProtocolProviderServices</tt>, from which the user could select.
     */
    public SelectAccountPanel(NewChatRoom newChatRoom,
            Iterator<ProtocolProviderService> protocolProvidersList)
    {
        super(new BorderLayout());

        this.setBorder(BorderFactory
            .createEmptyBorder(10, 10, 10, 10));

        this.setPreferredSize(new Dimension(600, 400));

        this.newChatRoom = newChatRoom;

        this.protocolProvidersList = protocolProvidersList;

        this.infoLabel.setEditable(false);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(
            font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);

        this.rightPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);

        this.add(rightPanel, BorderLayout.CENTER);

        accountsTable = new JTable(tableModel)
        {
            @Override
            public void tableChanged(TableModelEvent e)
            {
              super.tableChanged(e);
              repaint();
            }
        };

        this.tableInit();
    }

    /**
     * Initializes the accounts table.
     */
    private void tableInit()
    {
        accountsTable.setPreferredScrollableViewportSize(new Dimension(500, 70));

        tableModel.addColumn("");
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT"));
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.PROTOCOL"));

        while(protocolProvidersList.hasNext())
        {
            ProtocolProviderService pps = protocolProvidersList.next();

            OperationSet opSet = pps.getOperationSet(
                OperationSetMultiUserChat.class);

            if(opSet == null)
                continue;

            String pName = pps.getProtocolDisplayName();

            byte[] protocolImage = pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16);

            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(new ImageIcon(protocolImage));

            JRadioButton radioButton = new JRadioButton();

            tableModel.addRow(new Object[]{radioButton,
                    pps, protocolLabel});

            radioButtonGroup.add(radioButton);
        }

        accountsTable.setRowHeight(22);

        accountsTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        accountsTable.getColumnModel().getColumn(0).setCellRenderer(
            new RadioButtonTableCellRenderer());

        accountsTable.getColumnModel().getColumn(0).setCellEditor(
            new RadioButtonCellEditor(new JCheckBox()));

        accountsTable.getColumnModel().getColumn(2)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel().getColumn(1)
            .setCellRenderer(new LabelTableCellRenderer());

        this.tablePane.getViewport().add(accountsTable);

        if (accountsTable.getModel().getRowCount() == 0)
        {
            JTextArea noAccountsTextArea = new JTextArea(
                GuiActivator.getResources().getI18NString(
                    "service.gui.NO_GROUP_CHAT_ACCOUNT_AVAILABLE"));

            noAccountsTextArea.setLineWrap(true);
            noAccountsTextArea.setPreferredSize(new Dimension(400, 200));
            noAccountsTextArea.setOpaque(false);

            this.rightPanel.add(noAccountsTextArea, BorderLayout.SOUTH);
        }
    }

    /**
     * Adds a <tt>CellEditorListener</tt> to the list of account, which will
     * listen for events triggered by user clicks on the check boxes in the
     * first column of the accounts table.
     *
     * @param l the <tt>CellEditorListener</tt> to add
     */
    public void addCheckBoxCellListener(CellEditorListener l)
    {
        if(accountsTable.getModel().getRowCount() != 0)
        {
            accountsTable.getCellEditor(0, 0).addCellEditorListener(l);
        }
    }

    /**
     * Checks whether there is a selected radio button in the table.
     * @return <code>true</code> if any of the check boxes is selected,
     * <code>false</code> otherwise.
     */
    public boolean isRadioSelected()
    {
        TableModel model = accountsTable.getModel();

        for (int i = 0; i < accountsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);

            if (value instanceof JRadioButton)
            {
                JRadioButton radioButton = (JRadioButton) value;

                if(radioButton.isSelected())
                    return true;
            }
        }

        return false;
    }

    /**
     * Set the selected account, which will be used in the rest of the wizard.
     */
    public void setSelectedAccount()
    {
        TableModel model = accountsTable.getModel();

        for (int i = 0; i < accountsTable.getRowCount(); i ++) {
            Object value = model.getValueAt(i, 0);

            if (value instanceof JRadioButton) {
                JRadioButton radioButton = (JRadioButton)value;
                if(radioButton.isSelected()){
                    newChatRoom.setProtocolProvider(
                        (ProtocolProviderService)model.getValueAt(i, 1));
                }
            }
        }
    }
}
