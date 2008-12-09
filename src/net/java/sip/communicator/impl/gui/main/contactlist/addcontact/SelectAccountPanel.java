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
import net.java.sip.communicator.impl.gui.utils.Constants;
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
    private Logger logger = Logger.getLogger(SelectAccountPanel.class);

    private JScrollPane tablePane = new JScrollPane();

    private JTable accountsTable = new JTable();

    private BooleanToCheckTableModel tableModel =
        new BooleanToCheckTableModel();

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

    private JButton addAccountButton =
        new JButton(
            GuiActivator.getResources().getI18NString("service.gui.ADD_ACCOUNT"));

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

        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);
        this.rightPanel.add(tablePane, BorderLayout.CENTER);
        this.rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.buttonPanel.add(addAccountButton);

        this.addAccountButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                NewAccountDialog newAccountDialog = new NewAccountDialog();

                newAccountDialog.pack();
                newAccountDialog.setVisible(true);
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

        tableModel.addColumn("");
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.ACCOUNT"));
        tableModel.addColumn(
            GuiActivator.getResources().getI18NString("service.gui.PROTOCOL"));

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
                protocolImage =
                    ImageIO.read(new ByteArrayInputStream(pps.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            }
            catch (IOException e)
            {
                logger.error("Could not read image.", e);
            }

            JLabel protocolLabel = new JLabel();
            protocolLabel.setText(pName);
            protocolLabel.setIcon(new ImageIcon(protocolImage));

            tableModel.addRow(new Object[]
            { new Boolean(false), pps, protocolLabel });
        }

        accountsTable.setRowHeight(22);
        accountsTable.setModel(tableModel);

        accountsTable.getColumnModel().getColumn(0).sizeWidthToFit();
        accountsTable.getColumnModel().getColumn(2).setCellRenderer(
            new LabelTableCellRenderer());
        accountsTable.getColumnModel().getColumn(1).setCellRenderer(
            new LabelTableCellRenderer());

        this.tablePane.getViewport().add(accountsTable);
    }

    public void addCheckBoxCellListener(CellEditorListener l)
    {
        if (accountsTable.getModel().getRowCount() != 0)
        {
            accountsTable.getCellEditor(0, 0).addCellEditorListener(l);
        }
    }

    /**
     * Checks whether there is a selected check box in the table.
     * 
     * @return <code>true</code> if any of the check boxes is selected,
     *         <code>false</code> otherwise.
     */
    public boolean isCheckBoxSelected()
    {
        boolean isSelected = false;
        TableModel model = accountsTable.getModel();

        for (int i = 0; i < accountsTable.getRowCount(); i++)
        {
            Object value = model.getValueAt(i, 0);

            if (value instanceof Boolean)
            {
                Boolean check = (Boolean) value;
                if (check.booleanValue())
                {
                    isSelected = check.booleanValue();
                }
            }
        }
        return isSelected;
    }

    public void setSelectedAccounts()
    {
        TableModel model = accountsTable.getModel();

        for (int i = 0; i < accountsTable.getRowCount(); i++)
        {
            Object value = model.getValueAt(i, 0);

            if (value instanceof Boolean)
            {
                Boolean check = (Boolean) value;
                if (check.booleanValue())
                {
                    newContact
                        .addProtocolProvider((ProtocolProviderService) model
                            .getValueAt(i, 1));
                }
            }
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

            tableModel.addRow(new Object[]
            { new Boolean(false), sourcePProvider, protocolLabel });
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            for (int i = 0; i < tableModel.getRowCount(); i++)
            {
                ProtocolProviderService protocolProvider =
                    (ProtocolProviderService) tableModel.getValueAt(i, 1);

                if (protocolProvider.equals(sourcePProvider))
                {
                    tableModel.removeRow(i);
                    break;
                }
            }
        }
    }
}
