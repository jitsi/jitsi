/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The field used for searching in the properties table.
 * 
 * @author Marin Dzhigarov
 * 
 */
public class SearchField
    extends SIPCommTextField
    implements Skinnable
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID = SearchField.class.getName()
        + "FieldUI";

    /**
     * The object used for logging.
     */
    private Logger logger = Logger.getLogger(SearchField.class);

    TableRowSorter<TableModel> sorter;

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
 SearchFieldUI.class.getName());
    }

    /**
     * Creates an instance <tt>SearchField</tt>.
     * 
     * @param text the text we would like to enter by default
     * @param sorter the sorter which will be used for filtering.
     */
    public SearchField(String text, final TableRowSorter<TableModel> sorter)
    {
        super(text);

        if (getUI() instanceof SearchFieldUI)
        {
            ((SearchFieldUI) getUI()).setDeleteButtonEnabled(true);
            ((SearchFieldUI) getUI()).setCallButtonEnabled(false);
        }

        this.setBorder(null);
        this.setOpaque(false);
        this.setPreferredSize(new Dimension(100, 25));
        this.setDragEnabled(true);

        InputMap imap =
            getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        ActionMap amap = getActionMap();
        amap.put("escape", new AbstractAction()
        {
            /**
             * Serial Version UID.
             */
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e)
            {
                setText("");
            }
        });

        getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e)
            {
                RowFilter<TableModel, Object> rf = null;
                try
                {
                    rf = RowFilter.regexFilter(getText(), 0);
                }
                catch (java.util.regex.PatternSyntaxException exception)
                {
                    logger.info("Failed to compile regex.", exception);
                    return;
                }
                sorter.setRowFilter(rf);
            }

            public void removeUpdate(DocumentEvent e)
            {
                RowFilter<TableModel, Object> rf = null;
                try
                {
                    rf = RowFilter.regexFilter(getText(), 0);
                }
                catch (java.util.regex.PatternSyntaxException exception)
                {
                    logger.info("Failed to compile regex.", exception);
                    return;
                }
                sorter.setRowFilter(rf);
            }

            public void changedUpdate(DocumentEvent e)
            {
            }
        });

        loadSkin();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     * 
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }

    /**
     * Reloads text field UI defs.
     */
    public void loadSkin()
    {
        if (getUI() instanceof SearchFieldUI)
        {
            ((SearchFieldUI) getUI()).loadSkin();
        }
    }
}
