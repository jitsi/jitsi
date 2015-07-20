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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.*;

/**
 * ICE configuration panel.
 *
 * @author Yana Stamcheva
 */
public class IceConfigPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
     * The check box allowing the user to choose to use the default
     * SIP Communicator STUN server.
     */
    private final JCheckBox defaultStunBox = new SIPCommCheckBox(
        Resources.getResources().getI18NString(
                "plugin.jabberaccregwizz.USE_DEFAULT_STUN_SERVER",
                new String[]{Resources.getResources().getSettingsString(
                        "service.gui.APPLICATION_NAME")}));

    /**
     * The table model for our additional stun servers table.
     */
    private final ServerTableModel tableModel = new ServerTableModel();

    /**
     * The stun server table.
     */
    private final JTable table = new JTable(tableModel);

    /**
     * The check box allowing the user to choose to use JingleNodes.
     */
    private final JCheckBox jnBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.USE_JINGLE_NODES"));

    /**
     * The check box allowing the user to choose to automatically discover
     * JingleNodes relays.
     */
    private final JCheckBox jnAutoDiscoverBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.AUTO_DISCOVER_JN"));

    /**
     * The check box allowing the user to choose to use JingleNodes.
     */
    private final JCheckBox upnpBox = new SIPCommCheckBox(
        Resources.getString("plugin.jabberaccregwizz.USE_UPNP"));

    /**
     * The table model for our additional stun servers table.
     */
    private final ServerTableModel jnTableModel =
        new ServerTableModel();

    /**
     * The JingleNodes server table.
     */
    private final JTable jnTable = new JTable(jnTableModel);

    /**
     * Creates an instance of <tt>IceConfigPanel</tt>.
     */
    public IceConfigPanel()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        iceBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoDiscoverBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        defaultStunBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        /* ICE and STUN/TURN discovery are enabled by default for a new account,
         * these properties will be overridden for existing account when
         * they get loaded
         */
        iceBox.setSelected(true);
        autoDiscoverBox.setSelected(true);
        defaultStunBox.setSelected(true);


        jnBox.setSelected(true);
        jnAutoDiscoverBox.setSelected(true);

        upnpBox.setSelected(true);

        JPanel checkBoxPanel = new TransparentPanel(new GridLayout(0, 1));
        checkBoxPanel.add(iceBox);
        checkBoxPanel.add(upnpBox);
        checkBoxPanel.add(autoDiscoverBox);
        checkBoxPanel.add(defaultStunBox);

        add(checkBoxPanel);
        add(Box.createVerticalStrut(10));
        add(createAdditionalServersComponent());

        checkBoxPanel = new TransparentPanel(new GridLayout(0, 1));
        checkBoxPanel.add(jnBox);
        checkBoxPanel.add(jnAutoDiscoverBox);

        add(checkBoxPanel);
        add(Box.createVerticalStrut(10));
        add(createAdditionalJingleNodesComponent());
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
            + "/" + Resources.getString("service.gui.PORT"));
        tableModel.addColumn(
            Resources.getString("plugin.jabberaccregwizz.SUPPORT_TURN"));

        table.setDefaultRenderer(   StunServerDescriptor.class,
                                    new ServerCellRenderer());

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        final JButton addButton
            = new JButton(Resources.getString("service.gui.ADD"));
        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                StunConfigDialog stunDialog = new StunConfigDialog(false);
                stunDialog.setModal(true);
                stunDialog.setVisible(true);
            }
        });

        final JButton editButton
            = new JButton(Resources.getString("service.gui.EDIT"));
        editButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(table.getSelectedRow() < 0)
                    return;

                StunServerDescriptor stunServer
                    = (StunServerDescriptor) tableModel.getValueAt(
                        table.getSelectedRow(), 0);

                if (stunServer != null)
                {
                    StunConfigDialog dialog = new StunConfigDialog(
                                    stunServer.getAddress(),
                                    stunServer.getPort(),
                                    stunServer.isTurnSupported(),
                                    StringUtils.getUTF8String(
                                                    stunServer.getUsername()),
                                    StringUtils.getUTF8String(
                                                    stunServer.getPassword()));
                    dialog.setModal(true);
                    dialog.setVisible(true);
                }
            }
        });

        final JButton deleteButton
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

        table.addMouseListener(new MouseAdapter()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getComponent().isEnabled() &&
                        evt.getButton() == MouseEvent.BUTTON1 &&
                        evt.getClickCount() == 2)
                {
                    editButton.doClick();
                }
            }
        });

        return mainPanel;
    }

    /**
     * The STUN configuration window.
     */
    private class StunConfigDialog extends SIPCommDialog
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The main panel
         */
        private final JPanel mainPanel
                                = new TransparentPanel(new BorderLayout());

        /**
         * The address of the stun server.
         */
        private final JTextField addressField = new JTextField();

        /**
         * The port number field
         */
        private final JTextField portField = new JTextField();

        /**
         * The input verifier that we will be using to validate port numbers.
         */
        private final PortVerifier portVerifier = new PortVerifier();

        /**
         * The check box where user would indicate whether a STUN server is also
         * a TURN server.
         */
        private final JCheckBox supportTurnCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.jabberaccregwizz.SUPPORT_TURN"));

        /**
         * The user name field
         */
        private final JTextField usernameField = new JTextField();

        /**
         * The password field.
         */
        private final JPasswordField passwordField = new JPasswordField();

        /**
         * The pane where we show errors.
         */
        private JEditorPane errorMessagePane;

        /**
         * If the dialog is open via "edit" button.
         */
        private final boolean isEditMode;

        /**
         * Default STUN/TURN port.
         */
        private static final String DEFAULT_STUN_PORT = "3478";

        /**
         * Previous server name (in case of edit).
         */
        private String previousServer = null;

        /**
         * Previous port number (in case of edit).
         */
        private int previousPort = 0;

        /**
         * Creates a new StunConfigDialog with filled in values.
         *
         * @param address the IP or FQDN of the server
         * @param port the port number
         * @param isSupportTurn a <tt>boolean</tt> indicating whether the server
         * is also a TURN relay
         * @param username the username we should use with this server
         * @param password the password we should use with this server
         */
        public StunConfigDialog(String  address,
                                int     port,
                                boolean isSupportTurn,
                                String  username,
                                String  password)
        {
            this(true);

            addressField.setText(address);
            portField.setText(Integer.toString( port ));
            supportTurnCheckBox.setSelected(isSupportTurn);
            usernameField.setText(username);
            passwordField.setText(password);

            previousServer = address;
            previousPort = port;

            if(isSupportTurn)
            {
                usernameField.setEnabled(true);
                passwordField.setEnabled(true);
            }
        }

        /**
         * Creates an empty dialog.
         *
         * @param editMode true if the dialog is in "edit" state, false means
         * "add" state
         */
        public StunConfigDialog(boolean editMode)
        {
            super(false);

            this.isEditMode = editMode;

            setTitle(Resources.getString(!editMode ?
                "plugin.jabberaccregwizz.ADD_STUN_SERVER" :
                    "plugin.jabberaccregwizz.EDIT_STUN_SERVER"));

            JLabel addressLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.IP_ADDRESS"));
            JLabel portLabel = new JLabel(
                Resources.getString("service.gui.PORT"));
            JLabel usernameLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.TURN_USERNAME"));
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

            usernameField.setEnabled(false);
            passwordField.setEnabled(false);

            portField.setText(DEFAULT_STUN_PORT);
            valuesPanel.add(supportTurnCheckBox);
            valuesPanel.add(addressField);
            valuesPanel.add(portField);
            valuesPanel.add(usernameField);
            valuesPanel.add(passwordField);

            //register an input verifier so that we would only accept valid
            //port numbers.
            portField.setInputVerifier(portVerifier);

            JButton addButton
                = new JButton(Resources.getString("service.gui.OK"));
            JButton cancelButton
                = new JButton(Resources.getString("service.gui.CANCEL"));

            addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    String address = addressField.getText();
                    String portStr = portField.getText();
                    StunServerDescriptor stunServer = null;

                    //Don't proceed with an invalid port field
                    if(!portVerifier.verify(portField))
                    {
                        loadErrorMessage(Resources.getSettingsString(
                                "plugin.jabberaccregwizz.PORT_FIELD_INVALID"));
                        return;
                    }
                    int port = -1;
                    if(portStr != null && portStr.trim().length() > 0)
                        port = Integer.parseInt( portField.getText() );

                    String username = usernameField.getText();
                    char[] password = passwordField.getPassword();

                    String errorMessage = null;
                    if (address == null || address.length() <= 0)
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.NO_STUN_ADDRESS");

                    if ((username == null || username.length() <= 0) &&
                            supportTurnCheckBox.isSelected())
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.NO_STUN_USERNAME");

                    if(isEditMode)
                    {
                        // if user edit address or port, we have to find the row
                        // with the previous value
                        stunServer = getStunServer(previousServer,
                                previousPort);
                    }
                    else
                    {
                        stunServer = getStunServer(address, port);
                    }

                    if(stunServer != null && !isEditMode)
                    {
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.STUN_ALREADY_EXIST");
                    }

                    if (errorMessage != null)
                    {
                        loadErrorMessage(errorMessage);
                        return;
                    }

                    if(!isEditMode)
                    {
                        stunServer = new StunServerDescriptor(
                                address, port, supportTurnCheckBox.isSelected(),
                                username, new String( password ));

                        addStunServer(stunServer);
                    }
                    else
                    {
                        /* edit an existing STUN/TURN server */
                        stunServer.setAddress(address);
                        stunServer.setPort(port);
                        stunServer.setTurnSupported(
                                supportTurnCheckBox.isSelected());
                        stunServer.setUsername(username);
                        stunServer.setPassword(new String(password));

                        modifyStunServer(stunServer);
                    }
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

            supportTurnCheckBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent evt)
                {
                    if(evt.getStateChange() == ItemEvent.SELECTED)
                    {
                        /* show TURN user/password textfield */
                        usernameField.setEnabled(true);
                        passwordField.setEnabled(true);
                    }
                    else
                    {
                        /* hide TURN user/password textfield */
                        usernameField.setEnabled(false);
                        passwordField.setEnabled(false);
                    }
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
            //message pane preferred size being ignored (or being 0) which is
            //why I am adding it's height to the dialog. It's quite ugly so
            //please fix if you have something better in mind.
            this.setSize(getWidth(), getHeight() +
                    errorMessagePane.getHeight());
        }

        /**
         * Dummy implementation that we are not using.
         *
         * @param escaped unused
         */
        @Override
        protected void close(boolean escaped) {}
    }

    /**
     * A custom cell renderer used in the cell containing the
     * <tt>StunServer</tt> instance.
     */
    private static class ServerCellRenderer
        extends DefaultTableCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        // We need a place to store the color the JLabel should be returned
        // to after its foreground and background colors have been set
        // to the selection background color.
        // These vars will be made protected when their names are finalized.
        /** the fore ground color to use when not selected */
        private Color unselectedForeground;

        /** the fore ground color to use when selected */
        private Color unselectedBackground;

        /**
         * Overrides <code>JComponent.setForeground</code> to assign
         * the unselected-foreground color to the specified color.
         *
         * @param c set the foreground color to this value
         */
        @Override
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
        @Override
        public void setBackground(Color c)
        {
            super.setBackground(c);
            unselectedBackground = c;
        }

        /**
         * Returns a cell renderer for the specified cell.
         *
         * @param table the {@link JTable} that the cell belongs to.
         * @param value the cell value
         * @param isSelected indicates whether the cell is selected
         * @param hasFocus indicates whether the cell is currently on focus.
         * @param row the row index
         * @param column the column index
         *
         * @return the cell renderer
         */
        @Override
        public Component getTableCellRendererComponent( JTable table,
                                                        Object value,
                                                        boolean isSelected,
                                                        boolean hasFocus,
                                                        int row,
                                                        int column)
        {

            if (value instanceof StunServerDescriptor)
            {
                StunServerDescriptor stunServer = (StunServerDescriptor) value;

                this.setText(   stunServer.getAddress()
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
            else if(value instanceof JingleNodeDescriptor)
            {
                JingleNodeDescriptor jn = (JingleNodeDescriptor) value;

                this.setText(jn.getJID());

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
     */
    private class ServerTableModel
        extends DefaultTableModel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Returns the class of the objects contained in the column given by
         * the index. The class is used to distinguish which renderer should be
         * used.
         *
         * @param columnIndex  the column being queried
         * @return the class of objects contained in the column
         */
        @Override
        public Class<?> getColumnClass(int columnIndex)
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
        @Override
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
    protected boolean isUseIce()
    {
        return iceBox.isSelected();
    }

    /**
     * Sets the <tt>useIce</tt> property.
     * @param isUseIce <tt>true</tt> to indicate that ICE should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    protected void setUseIce(boolean isUseIce)
    {
        iceBox.setSelected(isUseIce);
    }

    /**
     * Indicates if the stun server should be automatically discovered.
     * @return <tt>true</tt> if the stun server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    protected boolean isAutoDiscoverStun()
    {
        return autoDiscoverBox.isSelected();
    }

    /**
     * Sets the <tt>autoDiscoverStun</tt> property.
     * @param isAutoDiscover <tt>true</tt> to indicate that stun server should
     * be auto-discovered, <tt>false</tt> - otherwise.
     */
    protected void setAutoDiscoverStun(boolean isAutoDiscover)
    {
        autoDiscoverBox.setSelected(isAutoDiscover);
    }

    /**
     * Indicates if the default stun server should be used
     * @return <tt>true</tt> if the default stun server should be used,
     * otherwise returns <tt>false</tt>.
     */
    protected boolean isUseDefaultStunServer()
    {
        return defaultStunBox.isSelected();
    }

    /**
     * Sets the <tt>defaultStun</tt> property.
     * @param isDefaultStun <tt>true</tt> to indicate that the default stun
     * server should be used, <tt>false</tt> otherwise.
     */
    protected void setUseDefaultStunServer(boolean isDefaultStun)
    {
        defaultStunBox.setSelected(isDefaultStun);
    }

    /**
     * Returns the list of additional stun servers entered by the user.
     *
     * @return the list of additional stun servers entered by the user
     */
    @SuppressWarnings("unchecked")//getDataVector() is simply not parameterized
    protected List<StunServerDescriptor> getAdditionalStunServers()
    {
        LinkedList<StunServerDescriptor> serversList
                                    = new LinkedList<StunServerDescriptor>();

        Vector<Vector<StunServerDescriptor>> serverRows
                                    = tableModel.getDataVector();

        for(Vector<StunServerDescriptor> row : serverRows)
            serversList.add(row.elementAt(0));

        return serversList;
    }

    /**
     * Adds the given <tt>stunServer</tt> to the list of additional stun
     * servers.
     * @param stunServer the stun server to add
     */
    protected void addStunServer(StunServerDescriptor stunServer)
    {
        tableModel.addRow(new Object[]{stunServer,
            stunServer.isTurnSupported()});
    }

    /**
     * Remove all <tt>stunServer</tt>s to the list of additional stun
     * servers.
     */
    protected void removeAllStunServer()
    {
        int i = tableModel.getRowCount();
        while(i != 0)
        {
            tableModel.removeRow(0);
            i--;
        }
    }

    /**
     * Modify the given <tt>stunServer</tt> from the list of stun servers.
     *
     * @param stunServer the stun server to modify
     */
    protected void modifyStunServer(StunServerDescriptor stunServer)
    {
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            StunServerDescriptor server
                = (StunServerDescriptor) tableModel.getValueAt(i, 0);

            if(stunServer == server)
            {
                tableModel.setValueAt(stunServer, i, 0);
                tableModel.setValueAt(stunServer.isTurnSupported(), i, 1);
                return;
            }
        }
    }

    /**
     * Indicates if a stun server with the given <tt>address</tt> and
     * <tt>port</tt> already exists in the additional stun servers table.
     *
     * @param address the STUN server address to check
     * @param port the STUN server port to check
     *
     * @return <tt>StunServerDescriptor</tt> if a STUN server with the given
     * <tt>address</tt> and <tt>port</tt> already exists in the table, otherwise
     *  returns <tt>null</tt>
     */
    protected StunServerDescriptor getStunServer(String address, int port)
    {
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            StunServerDescriptor stunServer
                = (StunServerDescriptor) tableModel.getValueAt(i, 0);

            if (stunServer.getAddress().equalsIgnoreCase(address)
                && stunServer.getPort() == port)
                return stunServer;
        }
        return null;
    }

    /**
     * The input verifier that we use to verify port numbers.
     */
    private static class PortVerifier extends InputVerifier
    {
        /**
         * Checks whether the JComponent's input is a valid port number. This
         * has no side effects. It returns a boolean indicating
         * the status of the argument's input.
         *
         * @param input the JComponent to verify
         * @return <tt>true</tt> when valid, <tt>false</tt> when invalid or when
         * <tt>input</tt> is not a {@link JTextField} instance.
         */
        @Override
        public boolean verify(JComponent input)
        {
            if ( !( input instanceof JTextField ) )
                return false;

            JTextField portField = (JTextField)input;

            String portStr = portField.getText();

            int port = -1;

            //we accept empty strings as that would mean the default port.
            if( portStr== null || portStr.trim().length() == 0)
                return true;

            try
            {
                 port = Integer.parseInt(portStr);
            }
            catch(Throwable t)
            {
                //something's wrong and whatever it is - we don't care we
                //simply return false.
                return false;
            }

            return NetworkUtils.isValidPortNumber(port);
        }
    }

    /**
     * Creates the list of additional JingleNodes that are added by the user.
     *
     * @return the created component
     */
    private Component createAdditionalJingleNodesComponent()
    {
        jnTable.setPreferredScrollableViewportSize(new Dimension(450, 60));

        jnTableModel.addColumn(
            Resources.getString("plugin.jabberaccregwizz.JID_ADDRESS"));
        jnTableModel.addColumn(
            Resources.getString("plugin.jabberaccregwizz.RELAY_SUPPORT"));

        jnTable.setDefaultRenderer(JingleNodeDescriptor.class,
                                    new ServerCellRenderer());

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(jnTable);

        final JButton addButton
            = new JButton(Resources.getString("service.gui.ADD"));
        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JNConfigDialog jnDialog = new JNConfigDialog(false);
                jnDialog.setModal(true);
                jnDialog.setVisible(true);
            }
        });

        final JButton editButton
            = new JButton(Resources.getString("service.gui.EDIT"));
        editButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(jnTable.getSelectedRow() < 0)
                    return;

                JingleNodeDescriptor jn
                    = (JingleNodeDescriptor) jnTableModel.getValueAt(
                        jnTable.getSelectedRow(), 0);

                if (jn != null)
                {
                    JNConfigDialog dialog = new JNConfigDialog(
                                    jn.getJID(), jn.isRelaySupported());
                    dialog.setModal(true);
                    dialog.setVisible(true);
                }
            }
        });

        final JButton deleteButton
            = new JButton(Resources.getString("service.gui.DELETE"));
        deleteButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                jnTableModel.removeRow(jnTable.getSelectedRow());
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
                "plugin.jabberaccregwizz.ADDITIONAL_JINGLE_NODES")));
        mainPanel.add(scrollPane);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        jnTable.addMouseListener(new MouseAdapter()
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public void mouseClicked(MouseEvent evt)
            {
                if (evt.getComponent().isEnabled() &&
                        evt.getButton() == MouseEvent.BUTTON1 &&
                        evt.getClickCount() == 2)
                {
                    editButton.doClick();
                }
            }
        });
        return mainPanel;
    }

    /**
     * The JingleNodes configuration window.
     */
    private class JNConfigDialog extends SIPCommDialog
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * The main panel
         */
        private final JPanel mainPanel
                                = new TransparentPanel(new BorderLayout());

        /**
         * The address of the stun server.
         */
        private final JTextField addressField = new JTextField();

        /**
         * The check box where user would indicate whether a STUN server is also
         * a TURN server.
         */
        private final JCheckBox supportRelayCheckBox = new SIPCommCheckBox(
            Resources.getString("plugin.jabberaccregwizz.RELAY_SUPPORT"));

        /**
         * The pane where we show errors.
         */
        private JEditorPane errorMessagePane;

        /**
         * If the dialog is open via "edit" button.
         */
        private final boolean isEditMode;

        /**
         * Previous JID (in case of edit).
         */
        private String previousJID = null;

        /**
         * Creates a new JNConfigDialog with filled in values.
         *
         * @param address the IP or FQDN of the server
         * @param isRelaySupport a <tt>boolean</tt> indicating whether the node
         * supports relay
         */
        public JNConfigDialog(String  address, boolean isRelaySupport)
        {
            this(true);

            previousJID = address;
            addressField.setText(address);
            supportRelayCheckBox.setSelected(isRelaySupport);
        }

        /**
         * Creates an empty dialog.
         *
         * @param editMode true if the dialog is in "edit" state, false means
         * "add" state
         */
        public JNConfigDialog(boolean editMode)
        {
            super(false);

            this.isEditMode = editMode;

            setTitle(Resources.getString(!editMode ?
                "plugin.jabberaccregwizz.ADD_JINGLE_NODE" :
                    "plugin.jabberaccregwizz.EDIT_JINGLE_NODE"));

            JLabel addressLabel = new JLabel(
                Resources.getString("plugin.jabberaccregwizz.JID_ADDRESS"));

            TransparentPanel labelsPanel
                = new TransparentPanel(new GridLayout(0, 1));

            labelsPanel.add(addressLabel);
            labelsPanel.add(new JLabel());

            TransparentPanel valuesPanel
                = new TransparentPanel(new GridLayout(0, 1));

            valuesPanel.add(addressField);
            valuesPanel.add(supportRelayCheckBox);

            JButton addButton
                = new JButton(Resources.getString("service.gui.OK"));
            JButton cancelButton
                = new JButton(Resources.getString("service.gui.CANCEL"));

            addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    String address = addressField.getText();
                    JingleNodeDescriptor jnServer = null;

                    String errorMessage = null;
                    if (address == null || address.length() <= 0)
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.NO_STUN_ADDRESS");

                    if(isEditMode)
                    {
                        jnServer = getJingleNodes(previousJID);
                    }
                    else
                    {
                        jnServer = getJingleNodes(address);
                    }

                    if(jnServer != null && !isEditMode)
                    {
                        errorMessage = Resources.getString(
                            "plugin.jabberaccregwizz.STUN_ALREADY_EXIST");
                    }

                    if (errorMessage != null)
                    {
                        loadErrorMessage(errorMessage);
                        return;
                    }

                    if(!isEditMode)
                    {
                        jnServer = new JingleNodeDescriptor(
                                address, supportRelayCheckBox.isSelected());

                        addJingleNodes(jnServer);
                    }
                    else
                    {
                        /* edit an existing Jingle Node */
                        jnServer.setAddress(address);
                        jnServer.setRelay(supportRelayCheckBox.isSelected());
                        modifyJingleNodes(jnServer);
                    }
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

            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20,
                    20));
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
            //message pane preferred size being ignored (or being 0) which is
            //why I am adding it's height to the dialog. It's quite ugly so
            //please fix if you have something better in mind.
            this.setSize(getWidth(), getHeight() +
                    errorMessagePane.getHeight());
        }

        /**
         * Dummy implementation that we are not using.
         *
         * @param escaped unused
         */
        @Override
        protected void close(boolean escaped) {}
    }

    /**
     * Indicates if Jingle Nodes should be used for this account.
     *
     * @return <tt>true</tt> if Jingle Nodes should be used for this account,
     * otherwise returns <tt>false</tt>
     */
    protected boolean isUseJingleNodes()
    {
        return jnBox.isSelected();
    }

    /**
     * Sets the <tt>useJingleNodes</tt> property.
     *
     * @param isUseJN <tt>true</tt> to indicate that Jingle Nodes should be
     * used for this account, <tt>false</tt> - otherwise.
     */
    protected void setUseJingleNodes(boolean isUseJN)
    {
        jnBox.setSelected(isUseJN);
    }

    /**
     * Indicates if the Jingle Nodes relays should be automatically discovered.
     *
     * @return <tt>true</tt> if the Jingle Nodes relays should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    protected boolean isAutoDiscoverJingleNodes()
    {
        return jnAutoDiscoverBox.isSelected();
    }

    /**
     * Sets the <tt>autoDiscoverJingleNodes</tt> property.
     *
     * @param isAutoDiscover <tt>true</tt> to indicate that Jingle Nodes relays
     * should be auto-discovered, <tt>false</tt> - otherwise.
     */
    protected void setAutoDiscoverJingleNodes(boolean isAutoDiscover)
    {
        jnAutoDiscoverBox.setSelected(isAutoDiscover);
    }

    /**
     * Returns the list of additional Jingle Nodes entered by the user.
     *
     * @return the list of additional Jingle Nodes entered by the user
     */
    @SuppressWarnings("unchecked")//getDataVector() is simply not parameterized
    protected List<JingleNodeDescriptor> getAdditionalJingleNodes()
    {
        LinkedList<JingleNodeDescriptor> serversList
                                    = new LinkedList<JingleNodeDescriptor>();

        Vector<Vector<JingleNodeDescriptor>> serverRows
                                    = jnTableModel.getDataVector();

        for(Vector<JingleNodeDescriptor> row : serverRows)
            serversList.add(row.elementAt(0));

        return serversList;
    }

    /**
     * Indicates if a JingleNodes with the given <tt>address</tt> already exists
     * in the additional stun servers table.
     *
     * @param address the JingleNodes address to check
     *
     * @return <tt>JingleNodesDescriptor</tt> if a Jingle Node with the given
     * <tt>address</tt> already exists in the table, otherwise returns
     * <tt>null</tt>
     */
    protected JingleNodeDescriptor getJingleNodes(String address)
    {
        for (int i = 0; i < jnTableModel.getRowCount(); i++)
        {
            JingleNodeDescriptor jn
                = (JingleNodeDescriptor) jnTableModel.getValueAt(i, 0);

            if (jn.getJID().equalsIgnoreCase(address))
                return jn;
        }
        return null;
    }

    /**
     * Adds the given <tt>jingleNode</tt> to the list of additional JingleNodes
     *
     * @param jingleNode the Jingle Node server to add
     */
    protected void addJingleNodes(JingleNodeDescriptor jingleNode)
    {
        jnTableModel.addRow(new Object[]{jingleNode,
            jingleNode.isRelaySupported()});
    }

    /**
     * Remove all <tt>jingleNode</tt>s to the list of additional Jingle Nodes.
     */
    protected void removeAllJingleNodes()
    {
        int i = jnTableModel.getRowCount();
        while(i != 0)
        {
            jnTableModel.removeRow(0);
            i--;
        }
    }

    /**
     * Modify the given <tt>jingleNode</tt> from the list of Jingle Nodes.
     *
     * @param jingleNode the Jingle Node to modify
     */
    protected void modifyJingleNodes(JingleNodeDescriptor jingleNode)
    {
        for (int i = 0; i < jnTableModel.getRowCount(); i++)
        {
            JingleNodeDescriptor node
                = (JingleNodeDescriptor) jnTableModel.getValueAt(i, 0);

            if(jingleNode == node)
            {
                jnTableModel.setValueAt(jingleNode, i, 0);
                jnTableModel.setValueAt(jingleNode.isRelaySupported(), i, 1);
                return;
            }
        }
    }

    /**
     * Indicates if UPnP should be used for this account.
     * @return <tt>true</tt> if UPnP should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    protected boolean isUseUPNP()
    {
        return upnpBox.isSelected();
    }

    /**
     * Sets the <tt>useUPNP</tt> property.
     * @param isUseUPNP <tt>true</tt> to indicate that UPNP should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    protected void setUseUPNP(boolean isUseUPNP)
    {
        upnpBox.setSelected(isUseUPNP);
    }
}
