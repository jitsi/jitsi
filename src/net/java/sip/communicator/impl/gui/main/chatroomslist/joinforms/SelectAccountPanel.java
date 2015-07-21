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
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the account,
 * for which the new chat room will be created.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SelectAccountPanel
    extends TransparentPanel
    implements Skinnable
{
    /**
     * An Eclipse generated serial version UID.
     */
    private static final long serialVersionUID = 4717173525426074284L;

    /**
     * The index of the column that contains protocol names and icons.
     */
    private static final int PROTOCOL_COLUMN_INDEX = 0;

    /**
     * The index of the column that contains account display names
     */
    private static final int ACCOUNT_COLUMN_INDEX = 1;

    private final JScrollPane tablePane = new JScrollPane(
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JTable accountsTable;

    private final AccountsTableModel tableModel = new AccountsTableModel();

    private final NewChatRoom joinChatRoom;

    private final Iterator<ChatRoomProviderWrapper> chatRoomProvidersList;

    private final JPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private final JPanel rightPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    private final JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_CHAT_ICON)));

    private final SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_PROVIDERS_FOR_CHAT_ROOM"));

    private final JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_ACCOUNT"),
        JLabel.CENTER);

    /**
     * Creates and initializes the <tt>SelectAccountPanel</tt>.
     *
     * @param joinChatRoom an object that collects all user choices through the
     * wizard
     * @param chatRoomProviders The list of available
     * <tt>ChatRoomProviderWrapper</tt>s, from which the user could select.
     */
    public SelectAccountPanel(
        NewChatRoom joinChatRoom,
        Iterator<ChatRoomProviderWrapper> chatRoomProviders)
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(600, 400));
        this.joinChatRoom = joinChatRoom;

        this.chatRoomProvidersList = chatRoomProviders;

        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));

        this.infoLabel.setEditable(false);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(
            font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);

        this.add(iconLabel, BorderLayout.WEST);

        this.add(rightPanel, BorderLayout.CENTER);

        accountsTable = new JTable(tableModel)
        {
            /**
             * An eclipse generated serial version uid.
             */
            private static final long serialVersionUID = 6321836989166142791L;

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

        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.PROTOCOL"));
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT"));

        while(chatRoomProvidersList.hasNext())
        {
            ChatRoomProviderWrapper provider = chatRoomProvidersList.next();

            String pName = provider.getName();

            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);

            byte[] providerImage = provider.getProtocolProvider()
                .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if (providerImage != null)
                protocolLabel.setIcon(new ImageIcon(providerImage));

            tableModel.addRow(new Object[]{protocolLabel, provider});

        }

        accountsTable.setRowHeight(22);

        accountsTable.getColumnModel().getColumn(ACCOUNT_COLUMN_INDEX)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel()
            .getColumn(PROTOCOL_COLUMN_INDEX).sizeWidthToFit();
        accountsTable.getColumnModel().getColumn(PROTOCOL_COLUMN_INDEX)
            .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel()
            .getColumn(ACCOUNT_COLUMN_INDEX).setPreferredWidth(300);

        this.tablePane.getViewport().add(accountsTable);

        if (accountsTable.getModel().getRowCount() == 0)
        {
            JTextArea noAccountsTextArea = new JTextArea(
                GuiActivator.getResources()
                .getI18NString("service.gui.NO_GROUP_CHAT_ACCOUNT_AVAILABLE"));

            noAccountsTextArea.setLineWrap(true);
            noAccountsTextArea.setPreferredSize(new Dimension(400, 200));
            noAccountsTextArea.setOpaque(false);

            this.rightPanel.add(noAccountsTextArea, BorderLayout.SOUTH);
        }
    }

    /**
     * Adds a <tt>ListSelectionListener</tt> to the list of accounts, which will
     * listen for events triggered by user clicks on the rows in the table.
     *
     * @param listener the <tt>ListSelectionListener</tt> to add
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        accountsTable.getSelectionModel().addListSelectionListener(listener);
    }

    /**
     * Checks whether there is a selected row in the table.
     *
     * @return <tt>true</tt> if a row is selected and <tt>false</tt> otherwise.
     */
    public boolean isRowSelected()
    {
        return accountsTable.getSelectedRow() != -1;
    }

    /**
     * Determine the selected account, which will be used in the rest of the
     * wizard.
     */
    public void initSelectedAccount()
    {
        TableModel model = accountsTable.getModel();

        int selectedRow = accountsTable.getSelectedRow();
        if(selectedRow != -1)
        {
            joinChatRoom.setChatRoomProvider((ChatRoomProviderWrapper)
                        model.getValueAt(selectedRow, ACCOUNT_COLUMN_INDEX));
        }
    }

    /**
     * The table model that we use for the accounts table. The only reason we
     * need a model is to make sure our table is not editable.
     */
    private static class AccountsTableModel
        extends DefaultTableModel
    {
        /**
         * An eclipse generated serial version uid.
         */
        private static final long serialVersionUID = 4259505654018472178L;

        /**
         * Returns fale regardless of parameter values.
         *
         * @param   row             the row whose value is to be queried
         * @param   column          the column whose value is to be queried
         * @return                  false
         */
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    /**
     *
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_CHAT_ICON)));
    }
}
