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
package net.java.sip.communicator.plugin.thunderbird;

import static net.java.sip.communicator.plugin.thunderbird
    .ThunderbirdContactSourceService.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * This ConfigurationForm shows the list of Thunderbird address books
 * and allow users to manage them.
 *
 * @author Ingo Bauersachs
 */
public class ThunderbirdConfigForm
    extends TransparentPanel
    implements ConfigurationForm,
               ActionListener,
               DocumentListener
{
    /** Serial version UID. */
    private static final long serialVersionUID = 0L;

    /** Resource service */
    private ResourceManagementService R = ThunderbirdActivator.getResources();

    private JTextField txtFilename;
    private JTextField txtPrefix;
    private JButton cmdBrowse;
    private JCheckBox chkEnabled;

    /**
     * Creates a new instance of this class.
     */
    public ThunderbirdConfigForm()
    {
        super(new BorderLayout());
        this.initComponents();
    }

    /**
     * Inits the swing components
     */
    private void initComponents()
    {
        JPanel pnl = new TransparentPanel();
        pnl.setLayout(new GridLayout(0, 2));
        add(pnl, BorderLayout.NORTH);

        chkEnabled = new SIPCommCheckBox(
            R.getI18NString("plugin.thunderbird.ENABLED"));
        pnl.add(chkEnabled);
        pnl.add(new JLabel("")); //empty to wrap the grid to the next line

        txtFilename = new JTextField();
        txtFilename.setEditable(false);
        pnl.add(txtFilename);

        cmdBrowse = new JButton(R.getI18NString("service.gui.BROWSE") + "...");
        pnl.add(cmdBrowse);

        JLabel lblPrefix = new JLabel(
            R.getI18NString("plugin.thunderbird.PHONE_PREFIX"));
        pnl.add(lblPrefix);

        txtPrefix = new JTextField();
        txtPrefix.getDocument().addDocumentListener(this);
        pnl.add(txtPrefix);

        List<ThunderbirdContactSourceService> activeServices
            = ThunderbirdActivator.getActiveServices();
        if (activeServices.size() > 0)
        {
            chkEnabled.setSelected(true);
            ThunderbirdContactSourceService service = activeServices.get(0);
            txtFilename.setText(service.getFilename());
            txtPrefix.setText(service.getPhoneNumberPrefix());
        }
        else
        {
            chkEnabled.setSelected(false);
        }

        updateStates();
        chkEnabled.addActionListener(this);
        txtFilename.getDocument().addDocumentListener(this);
        cmdBrowse.addActionListener(this);
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getTitle
     */
    public String getTitle()
    {
        return R.getI18NString("plugin.thunderbird.CONFIG_FORM_TITLE");
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getIcon
     */
    public byte[] getIcon()
    {
        return null;
    }

    /**
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getForm
     */
    public Object getForm()
    {
        return this;
    }

    /**
     * Required by ConfirgurationForm interface
     *
     * Returns the index of this configuration form in the configuration window.
     * This index is used to put configuration forms in the desired order.
     * <p>
     * 0 is the first position
     * -1 means that the form will be put at the end
     * </p>
     * @return the index of this configuration form in the configuration window.
     *
     * @see net.java.sip.communicator.service.gui.ConfigurationForm#getIndex
     */
    public int getIndex()
    {
        return 3;
    }

    /**
     * Processes buttons events (new, modify, remove)
     *
     * @see java.awt.event.ActionListener#actionPerformed
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cmdBrowse)
        {
            browseForMab();
            ThunderbirdActivator.getActiveServices().get(0)
                .setFilename(txtFilename.getText());
        }
        else if (e.getSource() == chkEnabled)
        {
            if (chkEnabled.isSelected())
            {
                browseForMab();
                if (txtFilename.getText() != null)
                {
                    String bprop = PNAME_BASE_THUNDERBIRD_CONFIG + ".1";

                    ConfigurationService config
                        = ThunderbirdActivator.getConfigService();
                    config.setProperty(bprop, "1");
                    config.setProperty(bprop + "." + PNAME_INDEX, 1);
                    config.setProperty(bprop + "." + PNAME_FILENAME,
                        txtFilename.getText());
                    config.setProperty(bprop + "." + PNAME_DISPLAYNAME,
                        "Thunderbird");
                    config.setProperty(bprop + "." + PNAME_PREFIX,
                        txtPrefix.getText());
                    ThunderbirdActivator.add(bprop);
                }
            }
            else
            {
                for (ThunderbirdContactSourceService svc
                    : ThunderbirdActivator.getActiveServices())
                {
                    ThunderbirdActivator.remove(svc);
                }

                txtFilename.setText(null);
                txtPrefix.setText(null);
            }

            updateStates();
        }
    }

    /**
     * Opens a file browser dialog to select a Thunderbird .mab file. If the
     * user has chosen an existing file, the name is set to the filename
     * textbox.
     */
    private void browseForMab()
    {
        FilenameFilter ff = new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                String extension = "";
                int i = name.lastIndexOf('.');
                if (i > 0)
                {
                    extension = name.substring(i + 1);
                }

                return "mab".equals(extension);
            }
        };

        FileDialog fd = new FileDialog((Frame)null);
        fd.setFilenameFilter(ff);

        if (OSUtils.IS_WINDOWS)
        {
            File f = new File(
                new File(
                    System.getenv("APPDATA"), "Thunderbird"),
                    "Profiles");
            if (f.exists())
            {
                fd.setDirectory(f.getAbsolutePath());
            }
        }
        else if (OSUtils.IS_LINUX)
        {
            File f = new File(
                System.getProperty("user.home"),
                ".thunderbird");

            if (!f.exists())
            {
                f = new File(
                    System.getProperty("user.home"),
                    ".mozilla-thunderbird");
            }

            if (f.exists())
            {
                fd.setDirectory(f.getAbsolutePath());
            }
        }
        else if (OSUtils.IS_MAC)
        {
            File f = new File(
                System.getProperty("user.home"),
                "/Library/Profiles");

            if (!f.exists())
            {
                f = new File(
                    System.getProperty("user.home"),
                    "Application Support/Thunderbird/Profiles");
            }

            if (f.exists())
            {
                fd.setDirectory(f.getAbsolutePath());
            }
        }

        fd.setVisible(true);
        if (fd.getFile() != null)
        {
            File f = new File(fd.getDirectory(), fd.getFile());
            if (f.exists())
            {
                txtFilename.setText(f.getAbsolutePath());
            }
        }
    }

    /**
     * Enables or disables the controls enabled state based on the enabled
     * checkbox.
     */
    private void updateStates()
    {
        txtFilename.setEnabled(chkEnabled.isSelected());
        txtPrefix.setEnabled(chkEnabled.isSelected());
        cmdBrowse.setEnabled(chkEnabled.isSelected());
    }

    /**
     * Indicates if this is an advanced configuration form.
     * @return <tt>true</tt> if this is an advanced configuration form,
     * otherwise it returns <tt>false</tt>
     */
    public boolean isAdvanced()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.
     * DocumentEvent)
     */
    public void insertUpdate(DocumentEvent e)
    {
        changedUpdate(e);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.
     * DocumentEvent)
     */
    public void removeUpdate(DocumentEvent e)
    {
        changedUpdate(e);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.
     * DocumentEvent)
     */
    public void changedUpdate(DocumentEvent e)
    {
        if (e.getDocument() == txtPrefix.getDocument())
        {
            ThunderbirdActivator.getActiveServices().get(0)
                .setPhoneNumberPrefix(txtPrefix.getText());
        }
    }
}
