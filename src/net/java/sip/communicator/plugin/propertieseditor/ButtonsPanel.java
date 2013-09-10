/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The panel containing all buttons for the <tt>PropertiesEditorPanel</tt>.
 * 
 * @author Marin Dzhigarov
 */
public class ButtonsPanel 
    extends TransparentPanel 
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    private final ConfigurationService confService 
        = PropertiesEditorActivator.getConfigurationService();

    /**
     * The delete button.
     */
    private final JButton deleteButton;

    /**
     * The new button.
     */
    private final JButton newButton;

    /**
     * The props table.
     */
    private final JTable propsTable;

    /**
     * Creates an instance of <tt>ButtonsPanel</tt>.
     * @param propsTable the table containing all properties.
     * @param searchBox the search box panel containing the search box text 
     * field.
     */
    public ButtonsPanel(JTable propsTable, SearchField searchField)
    {
        this.propsTable = propsTable;

        ResourceManagementService r
            = PropertiesEditorActivator.getResourceManagementService();

        newButton = new JButton(r.getI18NString("service.gui.NEW"));
        deleteButton = new JButton(r.getI18NString("service.gui.DELETE"));
        newButton.setOpaque(false);
        deleteButton.setOpaque(false);

        JPanel buttonsPanel = new TransparentPanel(new GridLayout(0, 1, 8, 8));

        buttonsPanel.add(newButton);
        buttonsPanel.add(deleteButton);

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.NORTH);

        newButton.addActionListener(this);
        deleteButton.addActionListener(this);

        //default as nothing is selected
        defaultButtonState();
    }

    /**
     * Default state of buttons, as nothing is selected
     */
    public void defaultButtonState()
    {
        enableDeleteButton(false);
    }

    /**
     * Enable or disable the delete button.
     *
     * @param enable TRUE - to enable the delete button, FALSE - to disable it
     */
    public void enableDeleteButton(boolean enable)
    {
        this.deleteButton.setEnabled(enable);
    }

    /**
     * Performs corresponding actions, when a button is pressed.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();

        if (sourceButton.equals(newButton))
        {
            NewPropertyDialog dialog = new NewPropertyDialog();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            dialog.pack();
            dialog.setLocation(
                    (screenSize.width - dialog.getWidth()) / 2,
                    (screenSize.height - dialog.getHeight()) / 2);

            dialog.setVisible(true);
        }
        else if (sourceButton.equals(deleteButton))
        {
            int viewRow = propsTable.getSelectedRow();
            int modelRow =  propsTable.convertRowIndexToModel(viewRow);
            String selectedProperty
                = (String) propsTable.getModel().getValueAt(modelRow, 0);

            confService.removeProperty(selectedProperty);
            ((DefaultTableModel) propsTable.getModel()).removeRow(modelRow);
            propsTable.clearSelection();
            defaultButtonState();
        }
    }
}
