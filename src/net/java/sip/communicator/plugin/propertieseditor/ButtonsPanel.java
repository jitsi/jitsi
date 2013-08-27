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

    private ConfigurationService confService 
        = PropertiesEditorActivator.getConfigurationService();
    
    private ResourceManagementService resourceManagementService 
        = PropertiesEditorActivator.getResourceManagementService();

    /**
     * The delete button.
     */
    private JButton deleteButton = new JButton(
        resourceManagementService.getI18NString("service.gui.DELETE"));

    /**
     * The new button.
     */
    private JButton newButton = new JButton(
        resourceManagementService.getI18NString("service.gui.NEW"));

    /**
     * The panel, containing all buttons.
     */
    private JPanel buttonsPanel
        = new TransparentPanel(new GridLayout(0, 1, 8, 8));

    /**
     * The props table.
     */
    private JTable propsTable;

    /**
     * Instance of the search box panel.
     */
    private SearchField searchField;

    /**
     * Creates an instance of <tt>ButtonsPanel</tt>.
     * @param propsTable the table containing all properties.
     * @param searchBox the search box panel containing the search box text 
     * field.
     */
    public ButtonsPanel(JTable propsTable, SearchField searchField)
    {
        this.propsTable = propsTable;

        this.searchField = searchField;

        this.setLayout(new BorderLayout());

        this.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        this.newButton.setOpaque(false);
        this.deleteButton.setOpaque(false);

        this.buttonsPanel.add(newButton);
        this.buttonsPanel.add(deleteButton);

        this.add(buttonsPanel, BorderLayout.NORTH);

        this.newButton.addActionListener(this);
        this.deleteButton.addActionListener(this);

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

            dialog.pack();
            dialog.setLocation(
                    Toolkit.getDefaultToolkit().getScreenSize().width/2
                        - dialog.getWidth()/2,
                    Toolkit.getDefaultToolkit().getScreenSize().height/2
                        - dialog.getHeight()/2
                    );

            dialog.setVisible(true);

        } else if (sourceButton.equals(deleteButton))
        {
            int viewRow = propsTable.getSelectedRow();
            int modelRow = 
                propsTable.convertRowIndexToModel(viewRow);
            String selectedProperty
                = (String)propsTable.getModel().getValueAt(modelRow, 0);
            confService.removeProperty(selectedProperty);
            propsTable.clearSelection();
            defaultButtonState();
            /**
             * Resets the text in the search box text field in order to fire 
             * a new value change event for the FilterRow to update the view.
             */
            String text = searchField.getText();
            searchField.setText(text);
        }
    }
}
