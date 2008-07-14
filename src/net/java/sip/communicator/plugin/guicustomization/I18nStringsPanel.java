package net.java.sip.communicator.plugin.guicustomization;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.resources.*;

public class I18nStringsPanel
    extends JPanel
{
    private JScrollPane tablePane = new JScrollPane();

    private JScrollPane languageListPane = new JScrollPane();

    private JTable stringsTable = new JTable();

    private JList languageList = new JList();

    private DefaultListModel languageListModel = new DefaultListModel();

    private JButton addLanguageButton = new JButton("Add new language");

    private JPanel leftPanel = new JPanel(new BorderLayout());

    private Hashtable languagesTable = new Hashtable();

    public I18nStringsPanel()
    {
        super(new BorderLayout());

        this.add(tablePane, BorderLayout.CENTER);
        this.add(leftPanel, BorderLayout.WEST);

        leftPanel.add(languageListPane, BorderLayout.CENTER);
        leftPanel.add(addLanguageButton, BorderLayout.NORTH);

        languageListPane.getViewport().add(languageList);
        languageList.setModel(languageListModel);
        languageList
            .addListSelectionListener(new LanguageListSelectionListener());

        addLanguageButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                NewLanguageDialog dialog = new NewLanguageDialog();

                dialog.pack();
                dialog.setLocation(
                    (Toolkit.getDefaultToolkit().getScreenSize().width
                        - dialog.getWidth()) / 2,
                    (Toolkit.getDefaultToolkit().getScreenSize().height
                        - dialog.getHeight()) / 2);
                dialog.setVisible(true);
            }
        });

        tablePane.getViewport().add(stringsTable);

        stringsTable.setShowGrid(true);
        stringsTable.setGridColor(Color.GRAY);

        this.initLocalesList();
        this.languageList.setSelectedIndex(0);
    }

    private void initLocalesList()
    {
        ResourceManagementService resourceService
            = GuiCustomizationActivator.getResources();

        Iterator locales = resourceService.getAvailableLocales();

        Locale locale;
        CustomTableModel stringsTableModel;

        while(locales.hasNext())
        {
            locale = (Locale) locales.next();

            languageListModel.addElement(locale.getLanguage());

            stringsTableModel = new CustomTableModel();

            stringsTableModel.addColumn("Key");
            stringsTableModel.addColumn("Text");

            this.initStringsTable(stringsTableModel, locale);

            languagesTable.put( locale.getLanguage(),
                                stringsTableModel);
        }
    }

    private void initStringsTable(CustomTableModel tableModel, Locale l)
    {
        Iterator stringKeys = GuiCustomizationActivator
            .getResources().getI18nStringsByLocale(l);

        while (stringKeys.hasNext())
        {
            String key = (String) stringKeys.next();
            String value
                = GuiCustomizationActivator.getResources()
                    .getI18NString(key, l);

            tableModel.addRow(new Object[]{  key, value});
        }
    }

    private class LanguageListSelectionListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting())
            {
                CustomTableModel newModel
                    = (CustomTableModel) languagesTable
                        .get(languageList.getSelectedValue());

                stringsTable.setModel(newModel);
                stringsTable.getColumnModel().getColumn(1)
                    .setCellRenderer(new TextAreaCellRenderer());
                stringsTable.repaint();
            }
        }
    }
    
    private class NewLanguageDialog
        extends JDialog
        implements ActionListener
    {
        private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        private JLabel enterLocaleLabel = new JLabel("Enter new locale: ");

        private JComboBox localeBox
            = new JComboBox(Locale.getAvailableLocales());

        private JButton okButton = new JButton("Ok");

        private JButton cancelButton = new JButton("Cancel");

        private JPanel buttonPanel
            = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        public NewLanguageDialog()
        {
            this.getContentPane().add(mainPanel);

            this.mainPanel.add(enterLocaleLabel, BorderLayout.WEST);
            this.mainPanel.add(localeBox, BorderLayout.CENTER);
            this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            this.buttonPanel.add(okButton);
            this.buttonPanel.add(cancelButton);

            this.okButton.addActionListener(this);
            this.cancelButton.addActionListener(this);

            this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));
            this.mainPanel.setPreferredSize(new Dimension(210, 120));
            
            this.getRootPane().setDefaultButton(okButton);
        }

        public void actionPerformed(ActionEvent evt)
        {
            JButton button = (JButton) evt.getSource();

            if (button.equals(okButton))
            {
                Locale locale = (Locale)localeBox.getSelectedItem();

                languageListModel.addElement(locale);

                CustomTableModel stringsTableModel = new CustomTableModel();

                stringsTableModel.addColumn("Key");
                stringsTableModel.addColumn("Text");

                initStringsTable(stringsTableModel, locale);

                languagesTable.put( locale,
                                    stringsTableModel);

                this.dispose();
            }
            else if (button.equals(cancelButton))
            {
                this.dispose();
            }
        }
    }
    
    Hashtable<String, Hashtable<String, String>> getLanguages()
    {
        Hashtable res = new Hashtable();
        Enumeration e = languageListModel.elements();
        while (e.hasMoreElements())
        {
            String locale = (String)e.nextElement();
            
            CustomTableModel model = 
                (CustomTableModel)languagesTable.get(locale);
            
            Hashtable strings = new Hashtable();
            
            int rows = model.getRowCount();
            for (int i = 0; i < rows; i++)
            {
                String key = (String)model.getValueAt(i, 0);
                String val = (String)model.getValueAt(i, 1);
                
                strings.put(key, val);
            }
            
            res.put(locale, strings);
        }
        
        return res;
    }
}
