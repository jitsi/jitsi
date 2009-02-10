/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the account,
 * for which the new chat room will be created.
 * 
 * @author Yana Stamcheva
 */
public class SelectAccountPanel extends TransparentPanel
{
    private JScrollPane tablePane = new JScrollPane(
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    private JTable accountsTable;
    
    private DefaultTableModel tableModel = new DefaultTableModel();
    
    private NewChatRoom joinChatRoom;
    
    private Iterator chatRoomProvidersList;
    
    private JPanel labelsPanel = new TransparentPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new TransparentPanel(new BorderLayout(5, 5));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_PROVIDERS_FOR_CHAT_ROOM"));
    
    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_ACCOUNT"), 
        JLabel.CENTER);
    
    private ButtonGroup radioButtonGroup = new ButtonGroup();
    
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

        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);

        this.add(iconLabel, BorderLayout.WEST);

        this.add(rightPanel, BorderLayout.CENTER);

        this.tableInit();
    }
    
    /**
     * Initializes the accounts table.
     */
    private void tableInit()
    {
        accountsTable = new JTable(tableModel)
        {
            public void tableChanged(TableModelEvent e)
            {
              super.tableChanged(e);
              repaint();
            }
        };

        accountsTable.setPreferredScrollableViewportSize(new Dimension(500, 70));

        tableModel.addColumn("");
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT"));
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.PROTOCOL"));

        while(chatRoomProvidersList.hasNext())
        {
            ChatRoomProviderWrapper provider 
                = (ChatRoomProviderWrapper) chatRoomProvidersList.next();

            String pName = provider.getName();

            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);

            byte[] providerImage = provider.getImage();

            if (providerImage != null)
                protocolLabel.setIcon(new ImageIcon(providerImage));

            JRadioButton radioButton = new JRadioButton();

            tableModel.addRow(new Object[]{radioButton,
                    provider, protocolLabel});

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
                GuiActivator.getResources()
                .getI18NString("service.gui.NO_GROUP_CHAT_ACCOUNT_AVAILABLE"));

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

        for (int i = 0; i < accountsTable.getRowCount(); i ++)
        {
            Object value = model.getValueAt(i, 0);

            if (value instanceof JRadioButton)
            {
                JRadioButton radioButton = (JRadioButton) value;

                if(radioButton.isSelected())
                {
                    joinChatRoom.setChatRoomProvider(
                        (ChatRoomProviderWrapper) model.getValueAt(i, 1));
                }
            }
        }
    }
}
