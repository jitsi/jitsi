/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.util.swing.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class IceConfigPanel
    extends TransparentPanel
{
    /**
     * The check box allowing the user to choose to use ICE.
     */
    private final JCheckBox iceBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.USE_ICE"));

    /**
     * The check box allowing the user to choose to automatically discover
     * STUN servers.
     */
    private final JCheckBox autoDiscoverBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.AUTO_DISCOVER_STUN"));

    /**
     * The table model for our additional stun servers table.
     */
    private final DefaultTableModel tableModel = new StunServerTableModel();

    /**
     * The stun server table.
     */
    private final JTable table = new JTable(tableModel);

    /**
     * Creates an instance of <tt>IceConfigPanel</tt>.
     */
    public IceConfigPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        iceBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoDiscoverBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel checkBoxPanel = new TransparentPanel(new GridLayout(0, 1));
        checkBoxPanel.add(iceBox);
        checkBoxPanel.add(autoDiscoverBox);

        add(checkBoxPanel);
        add(Box.createVerticalStrut(10));
        add(createAdditionalServersComponent());
    }

    /**
     * Creates the list of additional STUN/TURN servers that are added by the
     * user.
     * @return the created component
     */
    private Component createAdditionalServersComponent()
    {
        table.setPreferredScrollableViewportSize(new Dimension(450, 60));

        tableModel.addColumn(
            Resources.getString("plugin.jabberaccregwizz.IP_ADDRESS")
            + "/" + Resources.getString("plugin.jabberaccregwizz.PORT"));
        tableModel.addColumn(
            Resources.getString("plugin.jabberaccregwizz.SUPPORT_TURN"));

        table.setDefaultRenderer(   StunServer.class,
                                    new StunServerCellRenderer());

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        JButton addButton
            = new JButton(Resources.getString("service.gui.ADD"));
        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                StunConfigDialog stunDialog = new StunConfigDialog();

                stunDialog.setVisible(true);
            }
        });

        JButton editButton
            = new JButton(Resources.getString("service.gui.EDIT"));
        editButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                StunServer stunServer
                    = (StunServer) tableModel.getValueAt(
                        table.getSelectedRow(), 0);

                if (stunServer != null)
                {
                    StunConfigDialog dialog
                        = new StunConfigDialog( stunServer.getIpAddress(),
                                                stunServer.getPort(),
                                                stunServer.isSupportTurn(),
                                                stunServer.getUsername(),
                                                stunServer.getPassword());

                    dialog.setVisible(true);
                }
            }
        });

        JButton deleteButton
            = new JButton(Resources.getString("service.gui.DELETE"));
        deleteButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                tableModel.removeRow(table.getSelectedRow());
            }
        });

        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);

        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            Resources.getString(
                "plugin.jabberaccregwizz.ADDITIONAL_STUN_SERVERS")));
        mainPanel.add(scrollPane);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    /**
     * The STUN configuration window.
     */
    private class StunConfigDialog extends SIPCommDialog
    {
        private final JPanel mainPanel = new TransparentPanel(new BorderLayout());
        private final JTextField addressField = new JTextField();
        private final JTextField portField = new JTextField();
        private final JCheckBox supportTurnCheckBox = new JCheckBox(
            Resources.getString("plugin.jabberaccregwizz.SUPPORT_TURN"));
        private final JTextField usernameField = new JTextField();
        private final JPasswordField passwordField = new JPasswordField();
        private JEditorPane errorMessagePane;

        public StunConfigDialog(String address,
                                String port,
                                boolean isSupportTurn,
                                String username,
                                char[] password)
        {
            this();

            addressField.setText(address);
            portField.setText(port);
            supportTurnCheckBox.setSelected(isSupportTurn);
            usernameField.setText(username);
            passwordField.setText(password.toString());
        }

        public StunConfigDialog()
        {
            super(false);

            setTitle(Resources.getString(
                "plugin.jabberaccregwizz.ADD_STUN_SERVER"));

            JLabel addressLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.IP_ADDRESS"));
            JLabel portLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.PORT"));
            JLabel usernameLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.USERNAME"));
            JLabel passwordLabel = new JLabel(
                Resources.getString("service.gui.PASSWORD"));

            TransparentPanel labelsPanel
                = new TransparentPanel(new GridLayout(0, 1));

            labelsPanel.add(new JLabel());
            labelsPanel.add(addressLabel);
            labelsPanel.add(portLabel);
            labelsPanel.add(usernameLabel);
            labelsPanel.add(passwordLabel);

            TransparentPanel valuesPanel
                = new TransparentPanel(new GridLayout(0, 1));

            valuesPanel.add(supportTurnCheckBox);
            valuesPanel.add(addressField);
            valuesPanel.add(portField);
            valuesPanel.add(usernameField);
            valuesPanel.add(passwordField);

            JButton addButton
                = new JButton(Resources.getString("service.gui.ADD"));
            JButton cancelButton
                = new JButton(Resources.getString("service.gui.CANCEL"));

            addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    String address = addressField.getText();
                    String port = portField.getText();
                    String username = usernameField.getText();
                    char[] password = passwordField.getPassword();

                    String errorMessage = null;
                    if (address == null || address.length() <= 0)
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.NO_STUN_ADDRESS");

                    if (username == null || username.length() <= 0)
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.NO_STUN_USERNAME");

                    if (port == null || port.length() <= 0)
                        port = JabberAccountRegistration.DEFAULT_STUN_PORT;

                    if (containsStunServer(address, port))
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.STUN_ALREADY_EXIST");

                    if (errorMessage != null)
                    {
                        loadErrorMessage(errorMessage);
                        return;
                    }

                    StunServer stunServer = new StunServer(
                        address,
                        port,
                        supportTurnCheckBox.isSelected(),
                        username,
                        password,
                        table.getRowCount());

                    addStunServer(stunServer);

                    dispose();
                }
            });

            cancelButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    dispose();
                }
            });

            TransparentPanel buttonsPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonsPanel.add(addButton);
            buttonsPanel.add(cancelButton);

            mainPanel.add(labelsPanel, BorderLayout.WEST);
            mainPanel.add(valuesPanel, BorderLayout.CENTER);
            mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            getContentPane().add(mainPanel, BorderLayout.NORTH);
            pack();
        }

        /**
         * Loads the given error message in the current dialog, by re-validating
         * the content.
         *
         * @param errorMessage The error message to load.
         */
        private void loadErrorMessage(String errorMessage)
        {
            if (errorMessagePane == null)
            {
                errorMessagePane = new JEditorPane();

                errorMessagePane.setOpaque(false);
                errorMessagePane.setForeground(Color.RED);

                mainPanel.add(errorMessagePane, BorderLayout.NORTH);
            }

            errorMessagePane.setText(errorMessage);
            mainPanel.revalidate();
            mainPanel.repaint();

            this.pack();

            //WORKAROUND: there's something wrong happening in this pack and
            //components get cluttered, partially hiding the password text field.
            //I am under the impression that this has something to do with the
            //message pane preferred size being ignored (or being 0) which is why
            //I am adding it's height to the dialog. It's quite ugly so please fix
            //if you have something better in mind.
            this.setSize(getWidth(), getHeight()+errorMessagePane.getHeight());
        }
        @Override
        protected void close(boolean escaped) {}
    }

    /**
     * A custom cell renderer used in the cell containing the
     * <tt>StunServer</tt> instance.
     */
    private static class StunServerCellRenderer
        extends DefaultTableCellRenderer
    {
        // We need a place to store the color the JLabel should be returned 
        // to after its foreground and background colors have been set 
        // to the selection background color. 
        // These ivars will be made protected when their names are finalized. 
        private Color unselectedForeground; 
        private Color unselectedBackground; 

        /**
         * Overrides <code>JComponent.setForeground</code> to assign
         * the unselected-foreground color to the specified color.
         * 
         * @param c set the foreground color to this value
         */
        public void setForeground(Color c)
        {
            super.setForeground(c); 
            unselectedForeground = c; 
        }

        /**
         * Overrides <code>JComponent.setBackground</code> to assign
         * the unselected-background color to the specified color.
         *
         * @param c set the background color to this value
         */
        public void setBackground(Color c)
        {
            super.setBackground(c); 
            unselectedBackground = c; 
        }

        public Component getTableCellRendererComponent( JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column)
        {

            if (value instanceof StunServer)
            {
                StunServer stunServer = (StunServer) value;

                this.setText(   stunServer.getIpAddress()
                                + "/" + stunServer.getPort());

                if (isSelected)
                {
                    super.setForeground(table.getSelectionForeground());
                    super.setBackground(table.getSelectionBackground());
                }
                else
                {
                     super.setForeground((unselectedForeground != null)
                         ? unselectedForeground 
                         : table.getForeground());
                     super.setBackground((unselectedBackground != null)
                         ? unselectedBackground 
                         : table.getBackground());
                }
            }
            else
                return super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            return this;
        }
    }

    /**
     * A custom table model, with a non editable cells and a custom class column
     * objects.
     *
     */
    private class StunServerTableModel
        extends DefaultTableModel
    {
        /**
         * Returns the class of the objects contained in the column given by
         * the index. The class is used to distinguish which renderer should be
         * used.
         *
         * @param columnIndex  the column being queried
         * @return the class of objects contained in the column
         */
        public Class getColumnClass(int columnIndex)
        {
            return getValueAt(0, columnIndex).getClass();
        }

        /**
         * Returns <tt>false</tt> to indicate that none of the columns is
         * editable.
         *
         * @param row the row whose value is to be queried
         * @param column the column whose value is to be queried
         * @return false
         */
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }

    /**
     * Indicates if ice should be used for this account.
     * @return <tt>true</tt> if ICE should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    boolean isUseIce()
    {
        return iceBox.isSelected();
    }

    /**
     * Sets the <tt>useIce</tt> property.
     * @param isUseIce <tt>true</tt> to indicate that ICE should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    void setUseIce(boolean isUseIce)
    {
        iceBox.setSelected(isUseIce);
    }

    /**
     * Indicates if the stun server should be automatically discovered.
     * @return <tt>true</tt> if the stun server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    boolean isAutoDiscoverStun()
    {
        return autoDiscoverBox.isSelected();
    }

    /**
     * Sets the <tt>autoDiscoverStun</tt> property.
     * @param isAutoDiscover <tt>true</tt> to indicate that stun server should
     * be auto-discovered, <tt>false</tt> - otherwise.
     */
    void setAutoDiscoverStun(boolean isAutoDiscover)
    {
        autoDiscoverBox.setSelected(isAutoDiscover);
    }

    /**
     * Returns the list of additional stun servers entered by the user.
     * @return the list of additional stun servers entered by the user
     */
    @SuppressWarnings("unchecked")
    List<StunServer> getAdditionalStunServers()
    {
        LinkedList<StunServer> serversList = new LinkedList<StunServer>();
        Iterator i = tableModel.getDataVector().iterator();

        while(i.hasNext())
        {
            Vector row = (Vector) i.next();

            serversList.add((StunServer) row.elementAt(0));
        }

        return serversList;
    }

    /**
     * Adds the given <tt>stunServer</tt> to the list of additional stun
     * servers.
     * @param stunServer the stun server to add
     */
    void addStunServer(StunServer stunServer)
    {
        tableModel.addRow(new Object[]{stunServer,
            stunServer.isSupportTurn()});
    }

    /**
     * Indicates if a stun server with the given <tt>address</tt> and
     * <tt>port</tt> already exists in the additional stun servers table.
     * @param address the STUN server address to check
     * @param port the STUN server port to check
     * @return <tt>true</tt> if a STUN server with the given <tt>address</tt>
     * and <tt>port</tt> already exists in the table, otherwise returns
     * <tt>false</tt>
     */
    boolean containsStunServer(String address, String port)
    {
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            StunServer stunServer = (StunServer) tableModel.getValueAt(i, 0);

            if (stunServer.getIpAddress().equalsIgnoreCase(address)
                && stunServer.getPort().equalsIgnoreCase(port))
                return true;
        }
        return false;
    }
}
