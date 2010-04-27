/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.Container;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.addgroup.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>AddContactDialog</tt> is the dialog containing the form for adding
 * a contact.
 *
 * @author Yana Stamcheva
 */
public class AddContactDialog
    extends SIPCommDialog
    implements  ExportedWindow,
                ActionListener,
                WindowFocusListener
{
    private final Logger logger
        = Logger.getLogger(AddContactDialog.class.getName());

    private final  JLabel accountLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.SELECT_ACCOUNT") + ": ");

    private final JComboBox accountCombo = new JComboBox();

    private final JLabel groupLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.SELECT_GROUP") + ": ");

    private final JComboBox groupCombo = new JComboBox();

    private final JLabel contactAddressLabel = new JLabel(
        GuiActivator.getResources().getI18NString(
            "service.gui.CONTACT_NAME") + ": ");

    private final JTextField contactAddressField = new JTextField();

    private final JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD"));

    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    private final MainFrame mainFrame;

    private MetaContact metaContact;

    /**
     * Creates an instance of <tt>AddContactDialog</tt> that represents a dialog
     * that adds a new contact to an already existing meta contact.
     * 
     * @param mainFrame The <tt>MainFrame</tt> parent window.
     */
    public AddContactDialog(MainFrame mainFrame)
    {
        super(mainFrame);

        this.mainFrame = mainFrame;

        this.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.ADD_CONTACT"));

        this.init();
    }

    /**
     * Creates an <tt>AddContactDialog</tt> by specifying the parent window and
     * a meta contact, to which to add the new contact.
     * @param parentWindow the parent window
     * @param metaContact the meta contact, to which to add the new contact
     */
    public AddContactDialog(MainFrame parentWindow, MetaContact metaContact)
    {
        this(parentWindow);

        this.metaContact = metaContact;

        this.setSelectedGroup(metaContact.getParentMetaContactGroup());
        this.groupCombo.setEnabled(false);

        this.setTitle(GuiActivator.getResources()
                        .getI18NString("service.gui.ADD_CONTACT_TO")
                         + " " + metaContact.getDisplayName());

    }

    /**
     * Selects the given protocol provider in the account combo box.
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to select
     */
    public void setSelectedAccount(ProtocolProviderService protocolProvider)
    {
        accountCombo.setSelectedItem(protocolProvider);
    }

    /**
     * Selects the given <tt>group</tt> in the group combo box.
     * @param group the <tt>MetaContactGroup</tt> to select
     */
    public void setSelectedGroup(MetaContactGroup group)
    {
        groupCombo.setSelectedItem(group);
    }

    /**
     * Sets the address of the contact to add.
     * @param contactAddress the address of the contact to add
     */
    public void setContactAddress(String contactAddress)
    {
        contactAddressField.setText(contactAddress);
    }

    /**
     * Initializes the dialog.
     */
    private void init()
    {
        JLabel imageLabel = new JLabel(
            GuiActivator.getResources().getImage(
                "service.gui.icons.ADD_CONTACT_DIALOG_ICON"));

        imageLabel.setVerticalAlignment(JLabel.TOP);

        TransparentPanel labelsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        TransparentPanel fieldsPanel
            = new TransparentPanel(new GridLayout(0, 1, 5, 5));

        labelsPanel.add(accountLabel);
        fieldsPanel.add(accountCombo);
        initAccountCombo();
        accountCombo.setRenderer(new AccountComboRenderer());

        labelsPanel.add(groupLabel);
        fieldsPanel.add(groupCombo);
        initGroupCombo();
        groupCombo.setRenderer(new GroupComboRenderer());

        labelsPanel.add(contactAddressLabel);
        fieldsPanel.add(contactAddressField);

        TransparentPanel dataPanel = new TransparentPanel(new BorderLayout());

        dataPanel.add(labelsPanel, BorderLayout.WEST);
        dataPanel.add(fieldsPanel);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(20, 20));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(imageLabel, BorderLayout.WEST);
        mainPanel.add(dataPanel, BorderLayout.CENTER);

        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(createButtonsPanel(), BorderLayout.SOUTH);

        this.setPreferredSize(new Dimension(450, 200));
        this.addWindowFocusListener(this);
    }

    /**
     * Creates the buttons panel.
     * @return the created buttons panel
     */
    private Container createButtonsPanel()
    {
        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.ADD"));
        this.cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        buttonsPanel.add(addButton);
        buttonsPanel.add(cancelButton);

        return buttonsPanel;
    }

    /**
     * Initializes account combo box.
     */
    private void initAccountCombo()
    {
        Iterator<ProtocolProviderService> providers
            = mainFrame.getProtocolProviders();

        while (providers.hasNext())
        {
            ProtocolProviderService provider = providers.next();

            boolean isHidden = provider.getAccountID().getAccountProperty(
                    ProtocolProviderFactory.IS_PROTOCOL_HIDDEN) != null;

            if(isHidden)
                continue;

            OperationSet opSet
                = provider.getOperationSet(OperationSetPresence.class);

            if (opSet == null)
                continue;

            accountCombo.addItem(provider);
        }
    }

    /**
     * Initializes groups combo box.
     */
    private void initGroupCombo()
    {
        groupCombo.addItem(GuiActivator.getContactListService().getRoot());

        Iterator<MetaContactGroup> groupList
            = GuiActivator.getContactListService().getRoot().getSubgroups();

        while(groupList.hasNext())
        {
            groupCombo.addItem(groupList.next());
        }

        final String newGroupString = GuiActivator.getResources()
            .getI18NString("service.gui.CREATE_GROUP");

        groupCombo.addItem(newGroupString);

        groupCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (groupCombo.getSelectedItem().equals(newGroupString))
                {
                    CreateGroupDialog dialog
                        = new CreateGroupDialog(AddContactDialog.this, false);
                    dialog.setModal(true);
                    dialog.setVisible(true);

                    MetaContactGroup newGroup = dialog.getNewMetaGroup();

                    if (newGroup != null)
                    {
                        groupCombo.insertItemAt(newGroup,
                                groupCombo.getItemCount() - 2);
                        groupCombo.setSelectedItem(newGroup);
                    }
                    else
                        groupCombo.setSelectedIndex(0);
                }
            }
        });
    }

    /**
     * Indicates that the "Add" buttons has been pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if (button.equals(addButton))
        {
            if (metaContact != null)
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        GuiActivator.getContactListService()
                            .addNewContactToMetaContact(
                                (ProtocolProviderService) accountCombo
                                    .getSelectedItem(),
                                metaContact,
                                contactAddressField.getText());
                    }
                }.start();
            }
            else
            {
                final String contactName = contactAddressField.getText();

                new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            GuiActivator.getContactListService()
                                .createMetaContact(
                                    (ProtocolProviderService) accountCombo
                                        .getSelectedItem(),
                                    (MetaContactGroup) groupCombo
                                        .getSelectedItem(),
                                    contactName);
                        }
                        catch (MetaContactListException ex)
                        {
                            logger.error(ex);
                            ex.printStackTrace();
                            int errorCode = ex.getErrorCode();

                            if (errorCode
                                    == MetaContactListException
                                        .CODE_CONTACT_ALREADY_EXISTS_ERROR)
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                                    GuiActivator.getResources().getI18NString(
                                            "service.gui.ADD_CONTACT_EXIST_ERROR",
                                            new String[]{contactName}),
                                    ex)
                                .showDialog();
                            }
                            else if (errorCode
                                    == MetaContactListException
                                        .CODE_NETWORK_ERROR)
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                                    GuiActivator.getResources().getI18NString(
                                        "service.gui.ADD_CONTACT_NETWORK_ERROR",
                                        new String[]{contactName}),
                                    ex)
                                .showDialog();
                            }
                            else
                            {
                                new ErrorDialog(mainFrame,
                                    GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_ERROR_TITLE"),
                                    GuiActivator.getResources().getI18NString(
                                            "service.gui.ADD_CONTACT_ERROR",
                                            new String[]{contactName}),
                                    ex)
                                .showDialog();
                            }
                        }
                    }
                }.start();
            }
        }
        dispose();
    }

    /**
     * Indicates that this dialog is about to be closed.
     * @param isEscaped indicates if the dialog is closed by pressing the
     * Esc key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Indicates that the window has gained the focus. Requests the focus in
     * the text field.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    public void windowGainedFocus(WindowEvent e)
    {
        this.contactAddressField.requestFocus();
    }

    public void windowLostFocus(WindowEvent e)
    {
    }

    /**
     * A custom renderer displaying accounts in a combo box.
     */
    private static class AccountComboRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            ProtocolProviderService provider = (ProtocolProviderService) value;

            if (provider != null)
            {
                Image protocolImg
                    = ImageLoader.getBytesInImage(provider.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16));

                if (protocolImg != null)
                    this.setIcon(new ImageIcon(protocolImg));

                this.setText(provider.getAccountID().getDisplayName());
            }

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

            return this;
        }
    }

    /**
     * A custom renderer displaying groups in a combo box.
     */
    private static class GroupComboRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            if (value instanceof String)
            {
                this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 0, 0, 0)));
                this.setText((String) value);
            }
            else
            {
                this.setBorder(null);
                MetaContactGroup group = (MetaContactGroup) value;

                if (group.equals(GuiActivator
                    .getContactListService().getRoot()))
                    this.setText(GuiActivator.getResources()
                        .getI18NString("service.gui.SELECT_NO_GROUP"));
                else
                    this.setText(group.getGroupName());
            }

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
            return this;
        }
    }

    /**
     * Brings this window to front.
     */
    public void bringToFront()
    {
        this.bringToFront();
    }

    /**
     * Returns this exported window identifier.
     * @return the identifier of this window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ADD_CONTACT_WINDOW;
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Maximizes the window.
     */
    public void maximize()
    {
        this.maximize();
    }

    /**
     * Minimizes the window.
     */
    public void minimize()
    {
        this.minimize();
    }

    /**
     * This method can be called to pass any params to the exported window. This
     * method will be automatically called by
     * {@link UIService#getExportedWindow(WindowID, Object[])} in order to set
     * the parameters passed.
     *
     * @param windowParams the parameters to pass.
     */
    public void setParams(Object[] windowParams) {}
}
