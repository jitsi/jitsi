/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * The <tt>SelectAccountPanel</tt> is where the user should select the
 * account, where the new contact will be created.
 * 
 * @author Yana Stamcheva
 */
public class SelectAccountPanel
    extends TransparentPanel
    implements ServiceListener
{
    /**
     * An Eclipse generated serial version UID
     */
    private static final long serialVersionUID = 8635141487622436216L;
    
    /**
     * The index of the column that contains protocol names and icons.
     */
    private static final int PROTOCOL_COLUMN_INDEX = 0;
    
    /**
     * The index of the column that contains account display names
     */
    private static final int ACCOUNT_COLUMN_INDEX = 1;

    private Logger logger = Logger.getLogger(SelectAccountPanel.class);

    private JScrollPane tablePane = new JScrollPane();

    private JTable accountsTable = new JTable();
    
    private AccountsTableModel tableModel = new AccountsTableModel();

    private NewContact newContact;

    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout(10, 10));

    private JLabel iconLabel =
        new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));

    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_PROVIDERS_WIZARD_MSG"));

    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_PROVIDERS_WIZARD"),
        JLabel.CENTER);

    private TransparentPanel buttonPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JButton addAccountButton
        = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.ADD_ACCOUNT"));

    /**
     * Creates and initializes the <tt>SelectAccountPanel</tt>.
     * 
     * @param newContact An object that collects all user choices through the
     *            wizard.
     * @param protocolProvidersList The list of available
     *            <tt>ProtocolProviderServices</tt>, from which the user
     *            could select.
     */
    public SelectAccountPanel(NewContact newContact,
        Iterator<ProtocolProviderService> protocolProvidersList)
    {
        super(new BorderLayout());

        this.newContact = newContact;

        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.setPreferredSize(new Dimension(500, 200));

        this.iconLabel
            .setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.infoLabel.setEditable(false);

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        
        accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        this.rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.buttonPanel.add(addAccountButton);

        this.addAccountButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                NewAccountDialog.showNewAccountDialog();
            }
        });

        this.add(iconLabel, BorderLayout.WEST);

        this.rightPanel.setBorder(BorderFactory
            .createEmptyBorder(0, 10, 10, 10));

        this.add(rightPanel, BorderLayout.CENTER);

        this.tableInit(protocolProvidersList);

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Initializes the accounts table.
     */
    private void tableInit(Iterator<ProtocolProviderService> protocolProvidersList)
    {
        accountsTable
            .setPreferredScrollableViewportSize(new Dimension(500, 70));

        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.PROTOCOL"));
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT"));

        while (protocolProvidersList.hasNext())
        {
            ProtocolProviderService pps = protocolProvidersList.next();

            OperationSet opSet =
                pps.getOperationSet(OperationSetPresence.class);

            if (opSet == null)
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

            tableModel.addRow(new Object[] { protocolLabel, pps });
        }

        accountsTable.setRowHeight(22);
        accountsTable.setModel(tableModel);

        accountsTable.getColumnModel().getColumn(PROTOCOL_COLUMN_INDEX)
                        .setCellRenderer(new LabelTableCellRenderer());
        accountsTable.getColumnModel()
                        .getColumn(PROTOCOL_COLUMN_INDEX).sizeWidthToFit();
        
        accountsTable.getColumnModel().getColumn(ACCOUNT_COLUMN_INDEX)
                        .setCellRenderer( new LabelTableCellRenderer());
        
        accountsTable.getColumnModel()
                        .getColumn(ACCOUNT_COLUMN_INDEX).setPreferredWidth(300);
        
        this.tablePane.getViewport().add(accountsTable);
    }

    /**
     * Determines if an account has been selected in the accounts table and 
     * sets it in <tt>newContact.</tt>
     */
    public void initSelectedAccount()
    {
        TableModel model = accountsTable.getModel();

        int selectedRow = accountsTable.getSelectedRow();
        if(selectedRow != -1)
        {
            newContact.addProtocolProvider((ProtocolProviderService) model
                            .getValueAt(selectedRow, ACCOUNT_COLUMN_INDEX));
        }
    }

    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object sourceService =
            GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService sourcePProvider =
            (ProtocolProviderService) sourceService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            String pName = sourcePProvider.getProtocolDisplayName();

            Image protocolImage = null;
            try
            {
                protocolImage =
                    ImageIO.read(new ByteArrayInputStream(sourcePProvider
                        .getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
            catch (IOException e)
            {
                logger.error("Could not read image.", e);
            }

            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(new ImageIcon(protocolImage));

            tableModel.addRow(new Object[]{ protocolLabel, sourcePProvider });
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            for (int i = 0; i < tableModel.getRowCount(); i++)
            {
                ProtocolProviderService protocolProvider =
                    (ProtocolProviderService) tableModel
                        .getValueAt(i,ACCOUNT_COLUMN_INDEX);

                if (protocolProvider.equals(sourcePProvider))
                {
                    tableModel.removeRow(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Registers listener as a PropertyChangeListener on the table in this 
     * 
     * @param listener the listener that we'd like to register with the 
     * accounts table
     */
    public void addListSelectionListener(ListSelectionListener listener)
    {
        accountsTable.getSelectionModel().addListSelectionListener(listener);
    }
    
    /**
     * Determines whether or not there's a currently selected account in the 
     * accounts table.
     * 
     * @return true if an account has been selected in the accounts table and
     * false otherwise.
     */
    public boolean isAccountSelected()
    {
        return accountsTable.getSelectedRow() != -1;
    }
    
    /**
     * The table model that we use for the accounts table. The only reason we
     * need a model is to make sure our table is not editable.
     */
    private static class AccountsTableModel extends DefaultTableModel
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
    };
}
