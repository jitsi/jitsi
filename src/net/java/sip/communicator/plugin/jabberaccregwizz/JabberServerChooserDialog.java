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
import java.io.*;
import java.net.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.xml.parsers.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;
import org.jitsi.util.xml.*;
import org.osgi.framework.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * A dialog that shows the list of available Jabber servers.
 *
 * @author Nicolas Grandclaude
 */
public class JabberServerChooserDialog
    extends SIPCommDialog
    implements ListSelectionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger
        .getLogger(JabberServerChooserDialog.class);

    // Servers Table
    private JTable serversTable;

    private JTextArea chooseArea = new JTextArea(Resources
        .getString("plugin.jabberaccregwizz.CHOOSE_SERVER_TEXT"));

    // Panel
    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    private JPanel buttonPanel = new TransparentPanel(new FlowLayout(
            FlowLayout.RIGHT));

    private Box buttonBox = new Box(BoxLayout.X_AXIS);

    private JPanel chooseAreaPanel = new TransparentPanel(new BorderLayout());

    private JPanel westPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel eastPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JLabel westIconLabel = new JLabel();

    private JButton okButton
        = new JButton(Resources.getString("service.gui.OK"));

    private JButton cancelButton = new JButton(Resources
        .getString("service.gui.CANCEL"));

    private Vector<String> servers = new Vector<String>();

    private FileAccessService faService = null;

    private String[] columnNames =
    {   Resources.getString("plugin.jabberaccregwizz.SERVER_COLUMN"),
        Resources.getString("plugin.jabberaccregwizz.COMMENT_COLUMN")};

    /**
     * If the OK button is pressed.
     */
    public boolean isOK = false;

    /**
     * The selected server.
     */
    public String serverSelected;

    /**
     * Creates an instance of <tt>JabberServerChooserDialog</tt>.
     */
    public JabberServerChooserDialog()
    {
        this.setSize(new Dimension(550, 450));
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle(Resources.getString(
            "plugin.jabberaccregwizz.CHOOSE_SERVER_TITLE"));
        this.setModal(true);

        // Place the window in the center
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(screenSize.width / 2 - this.getWidth() / 2,
            screenSize.height / 2 - this.getHeight() / 2);

        this.init();
    }
    /**
     * Initializes all panels, buttons, etc.
     */
    private void init()
    {
        chooseArea.setEditable(false);
        chooseArea.setOpaque(false);
        chooseArea.setLineWrap(true);
        chooseArea.setWrapStyleWord(true);

        chooseAreaPanel
            .setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 10));

        chooseAreaPanel.add(chooseArea, BorderLayout.NORTH);

        eastPanel.add(chooseAreaPanel, BorderLayout.NORTH);

        // West Jabber icon
        westIconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20), BorderFactory
                .createTitledBorder("")));
        try
        {
            westIconLabel.setIcon(new ImageIcon(ImageIO
                .read(new ByteArrayInputStream(Resources
                    .getImage(Resources.PAGE_IMAGE)))));
        }
        catch (IOException e)
        {
            logger.error("Could not read image.", e);
        }

        this.westPanel.add(westIconLabel, BorderLayout.NORTH);
        this.mainPanel.add(westPanel, BorderLayout.WEST);

        // Table with servers and comments
        serversTable = new JTable(new ServerChooserTableModel());
        serversTable.setRowHeight(22);
        serversTable.getSelectionModel().addListSelectionListener(this);
        serversTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serversTable.setPreferredScrollableViewportSize(new Dimension(500, 70));

        // Fill the servers array with servers from servers.xml
        fillTable();

        JScrollPane scrollPane = new JScrollPane(serversTable);
        eastPanel.add(scrollPane, BorderLayout.CENTER);

        // Ok button
        okButton.setMnemonic(Resources.getMnemonic("service.gui.OK"));
        okButton.setEnabled(false);

        // Cancel button
        cancelButton.setMnemonic(Resources.getMnemonic("service.gui.CANCEL"));

        // Box with Ok and Cancel
        buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(cancelButton);
        buttonPanel.add(buttonBox);

        okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                isOK = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent evt)
            {
                dispose();
            }
        });

        this.mainPanel.add(eastPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);

        this.setVisible(true);
    }


    /**
     * Fill the servers array variable with data from the remote servers.xml
     */
    public void fillTable()
    {
        BundleContext bc = JabberAccRegWizzActivator.bundleContext;

        ServiceReference faServiceReference = bc
            .getServiceReference(FileAccessService.class.getName());

        faService = (FileAccessService) bc.getService(faServiceReference);

        File localServersListFile = null;
        try
        {
            localServersListFile = faService.getTemporaryFile();
            URL file = new URL("http://xmpp.net/services.xml");
            InputStream stream = file.openStream();

            try
            {
                // Copy the remote file to the disk
                byte[] buf = new byte[2048];
                int len;
                if (stream.available() > 0)
                {
                    FileOutputStream fos
                        = new FileOutputStream(localServersListFile);

                    while ((len = stream.read(buf)) > 0)
                    {
                        fos.write(buf, 0, len);
                    }
                    fos.close();
                }
            }
            finally
            {
                stream.close();
            }

            FileInputStream fis = new FileInputStream(localServersListFile);
            DocumentBuilder constructor
                = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            Document document = constructor.parse(fis);
            Element root = document.getDocumentElement();

            NodeList list = root.getElementsByTagName("item");

            // Read the xml and fill servers variable for the JTable
            for (int i = 0; i < list.getLength(); i++)
            {
                Element e = (Element) list.item(i);
                servers.add(new String(e.getAttribute("jid")));
            }
            fis.close();

        }
        catch (Exception e)
        {
            logger.error(
                "Failed to get a reference to the Jabber servers list file.", e);
        }
        finally
        {
            if (localServersListFile != null)
            {
                localServersListFile.delete();
            }
        }
    }

    /**
     * When a table row is selected enable the "Ok" button, otherwise disable it.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        int row = serversTable.getSelectedRow();
        if (row != -1)
        {
            okButton.setEnabled(true);
            serverSelected = (String) serversTable.getValueAt(row, 0);
        }
        else
        {
            okButton.setEnabled(false);
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }

    /**
     * The table model used for the table containing all servers.
     */
    private class ServerChooserTableModel extends AbstractTableModel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private Document serverComments;

        private NodeList commentsList;

        public ServerChooserTableModel()
        {
            try
            {
                // Create the builder and parse the file
                serverComments
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder()
                            .parse(Resources.getPropertyInputStream(
                                    "plugin.jabberaccregwizz.SERVER_COMMENTS"));
            }
            catch (SAXException e)
            {
                logger.error("Failed to parse server comments.", e);
            }
            catch (ParserConfigurationException e)
            {
                logger.error("Failed to parse server comments.", e);
            }
            catch (IOException e)
            {
                logger.error("Failed to parse server comments.", e);
            }

            Element root = serverComments.getDocumentElement();
            commentsList = root.getElementsByTagName("item");
        }

        public int getColumnCount()
        {
            return 2;
        }

        public int getRowCount()
        {
            return servers.size();
        }

        @Override
        public String getColumnName(int col)
        {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col)
        {
            String commentString = new String("");
            if (col == 0) // Column 1 (Server name)
            {
                return servers.get(row);
            }
            else
            { // Column 2 (Comment)

                int i = 0;
                Element e = (Element) commentsList.item(i);

                while ((i < commentsList.getLength())
                    && (e.getAttribute("jid").equals(servers.get(row)) == false))
                {
                    e = (Element) commentsList.item(i);
                    i++;
                }

                if (e.getAttribute("jid").equals(servers.get(row)))
                {
                    commentString = e.getAttribute("comment");
                }

                return commentString;
            }
        }
    }
}
