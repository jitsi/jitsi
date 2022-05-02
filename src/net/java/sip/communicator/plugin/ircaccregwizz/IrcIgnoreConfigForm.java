package net.java.sip.communicator.plugin.ircaccregwizz;

import net.java.sip.communicator.impl.protocol.irc.properties.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import org.jitsi.service.resources.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

/**
 * Implements the Irc ignore contact messages configuration panel.
 *
 * @author Duncan Robertson
 */
public class IrcIgnoreConfigForm
    extends TransparentPanel
    implements ConfigurationForm, ListSelectionListener, ActionListener
{
    /**
     * The "ignore" field
     */
    private final SIPCommTextField ignoreField = new SIPCommTextField("");

    /**
     * The <tt>ConfigurationService</tt> to be used to access configuration
     */
    private final org.jitsi.service.configuration.ConfigurationService configurationService
        = IrcAccRegWizzActivator.getConfigurationService();

    /**
     * Resource management service instance.
     */
    private static ResourceManagementService Resources
        = IrcAccRegWizzActivator.getResources();

    /**
     * Opens the new directory registration wizard
     */
    private SIPCommTextButton newButton = new SIPCommTextButton("+");

    /**
     * Pops a directory deletion confirmation dialog
     */
    private SIPCommTextButton removeButton = new SIPCommTextButton("-");

    /**
     * Displays the ignored irc contacts account.
     */
    private JList<String> ignoreList = new JList<>();

    /**
     * List model for ignore list.
     */
    private DefaultListModel<String> ignoreListModel = new DefaultListModel<>();

    /**
     * Contains the new/modify/remove buttons
     */
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));

    /**
     * Contains the ignore list
     */
    private SIPCommScrollPane scrollPane = new SIPCommScrollPane();

    /**
     * Initialize a new <tt>IrcIgnoreConfigForm</tt> instance.
     */
    public IrcIgnoreConfigForm()
    {
        super(new BorderLayout());
        Box box = Box.createVerticalBox();
        add(box, BorderLayout.NORTH);

        TransparentPanel contentPanel = new TransparentPanel();
        contentPanel.setLayout(new BorderLayout(10, 10));

        box.add(contentPanel);

        TransparentPanel inputPanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        TransparentPanel mainPanel
            = new TransparentPanel(new GridLayout(0, 1, 2, 2));
        mainPanel.setLayout(new BorderLayout(10, 10));

        inputPanel.add(new JLabel(Resources.getI18NString(
            "plugin.irc.IRC_IGNORE_DESCRIPTION")), BorderLayout.NORTH);
        inputPanel.add(ignoreField, BorderLayout.SOUTH);
        contentPanel.add(mainPanel, BorderLayout.CENTER);

        removeButton.setEnabled(false);

        newButton.setSize(newButton.getMinimumSize());
        removeButton.setSize(removeButton.getMinimumSize());

        ignoreList.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION);
        ignoreList.setModel(ignoreListModel);
        ignoreList.getSelectionModel().addListSelectionListener(this);

        /* consistency with the google accounts config form */

        scrollPane.getViewport().add(ignoreList);
        mainPanel.add(inputPanel,  BorderLayout.NORTH);
        mainPanel.add(scrollPane,  BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        mainPanel.setPreferredSize(new Dimension(500, 400));

        buttonsPanel.add(newButton);
        buttonsPanel.add(removeButton);

        newButton.setActionCommand("new");
        newButton.addActionListener(this);
        removeButton.addActionListener(this);
        removeButton.setActionCommand("remove");

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        String[] currentIgnores = configurationService.getString(
            IrcProperties.PROP_IRC_IGNORE,"")
            .split(",");
        for (String value :
            currentIgnores) {
            ignoreListModel.addElement(value);
        }
    }

    /**
     * Processes buttons events (new, remove)
     *
     * @see ActionListener#actionPerformed
     */
    public void actionPerformed(ActionEvent e)
    {
        int row = ignoreList.getSelectedIndex();

        if (e.getActionCommand().equals("new") && ignoreField.getText() != null && !ignoreField.getText().isEmpty())
        {
            ignoreListModel.addElement(ignoreField.getText().trim().toLowerCase());
            ignoreField.setText("");
        }

        if (e.getActionCommand().equals("remove") && row != -1)
        {
            ignoreListModel.removeElementAt(row);
        }

        Enumeration<String> values = ignoreListModel.elements();
        String propertyValue = "";
        if(values.hasMoreElements())
        {
            values.nextElement();
        }
        while (values.hasMoreElements())
        {
            propertyValue = propertyValue + "," + values.nextElement();
        }
        configurationService.setProperty(
            IrcProperties.PROP_IRC_IGNORE, propertyValue);
    }

    /**
     * Required by ListSelectionListener. Enables the "modify"
     * button when a server is selected in the table
     *
     * @param e event triggered
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if(ignoreList.getSelectedIndex() == -1)
        {
            removeButton.setEnabled(false);
        }
        else
        {
            removeButton.setEnabled(true);
        }
    }

    @Override
    public String getTitle()
    {
        return Resources.getI18NString("plugin.irc.IRC_IGNORE_CONFIG");
    }

    @Override
    public byte[] getIcon()
    {
        return new byte[0];
    }

    @Override
    public Object getForm()
    {
        return this;
    }

    @Override
    public int getIndex()
    {
        return -1;
    }

    @Override
    public boolean isAdvanced()
    {
        return true;
    }
}
