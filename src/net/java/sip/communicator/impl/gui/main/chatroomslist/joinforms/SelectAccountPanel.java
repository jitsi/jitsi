/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.util.*;

import java.awt.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the account,
 * for which the new chat room will be created.
 * 
 * @author Yana Stamcheva
 */
public class SelectAccountPanel extends JPanel
{
    private Logger logger = Logger.getLogger(SelectAccountPanel.class);
    
    private JScrollPane tablePane = new JScrollPane(
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    private JTable accountsTable;
    
    private DefaultTableModel tableModel = new DefaultTableModel();
    
    private NewChatRoom joinChatRoom;
    
    private Iterator protocolProvidersList;
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
            Messages.getI18NString("selectProvidersForChatRoom").getText());
    
    private JLabel infoTitleLabel = new JLabel(
            Messages.getI18NString("selectAccount").getText(), 
                JLabel.CENTER);
    
    private ButtonGroup radioButtonGroup = new ButtonGroup();
    
    /**
     * Creates and initializes the <tt>SelectAccountPanel</tt>.
     * 
     * @param joinChatRoom an object that collects all user choices through the
     * wizard
     * @param protocolProvidersList The list of available 
     * <tt>ProtocolProviderServices</tt>, from which the user could select.
     */
    public SelectAccountPanel(NewChatRoom joinChatRoom, 
            Iterator protocolProvidersList)
    {
        super(new BorderLayout());

        this.setPreferredSize(new Dimension(600, 400));
        this.joinChatRoom = joinChatRoom;

        this.protocolProvidersList = protocolProvidersList;

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
        tableModel.addColumn(Messages.getI18NString("account").getText());
        tableModel.addColumn(Messages.getI18NString("protocol").getText());

        while(protocolProvidersList.hasNext())
        {
            ProtocolProviderService pps 
                = (ProtocolProviderService)protocolProvidersList.next();

            OperationSet opSet = pps.getOperationSet(
                OperationSetMultiUserChat.class);

            if(opSet == null)
                continue;

            String pName = pps.getProtocolDisplayName();

            Image protocolImage = null;
            try
            {
                protocolImage = ImageIO.read(
                    new ByteArrayInputStream(pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
            catch (IOException e)
            {
                logger.error("Could not read image.", e);
            }

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
                Messages.getI18NString("noMultiChatAccountAvailable").getText());

            noAccountsTextArea.setLineWrap(true);
            noAccountsTextArea.setPreferredSize(new Dimension(400, 200));

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
                    joinChatRoom.setProtocolProvider(
                        (ProtocolProviderService)model.getValueAt(i, 1));
                }
            }
        }
    }
}
