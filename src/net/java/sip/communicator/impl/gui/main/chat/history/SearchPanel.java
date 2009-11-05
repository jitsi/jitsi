/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>SearchPanel</tt> is the panel, where user could make a search in
 * the message history. The search could be made by specifying a date
 * or an hour, or searching by a keyword.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SearchPanel
    extends TransparentPanel
    implements  ActionListener,
                DocumentListener
{
    private JTextField searchTextField = new JTextField();

    /*
    private JRadioButton todayMessagesRadio = new JRadioButton(Messages
            .getString("service.gui.TODAY"));

    private JRadioButton yesterdayMessagesRadio = new JRadioButton(Messages
            .getString("service.gui.YESTERDAY")); //$NON-NLS-1$

    private JRadioButton allMessagesRadio = new JRadioButton(Messages
            .getString("service.gui.ALL")); //$NON-NLS-1$

    private ButtonGroup radiosGroup = new ButtonGroup();

    private JLabel dateLabel
        = new JLabel(Messages.getString("service.gui.DATE") + ": "); //$NON-NLS-1$

    private JTextField dateTextField = new JTextField(10);

    private JLabel hourLabel
        = new JLabel(Messages.getString("service.gui.HOUR") + ": "); //$NON-NLS-1$

    private JTextField hourTextField = new JTextField(10);

    private JLabel lastNMessagesLabel = new JLabel(
            Messages.getString("service.gui.LAST") + ": "); //$NON-NLS-1$

    private JTextField lastNMessagesTextField = new JTextField(10);
    
    private SIPCommButton extendedSearchButton = new SIPCommButton
        (Messages.getString("service.gui.EXTENDED_CRITERIA"), //$NON-NLS-1$
        Constants.RIGHT_ARROW_ICON, Constants.RIGHT_ARROW_ROLLOVER_ICON);
    
    private SIPCommButton extendedSearchOpenedButton = new SIPCommButton
        (Messages.getString("service.gui.EXTENDED_CRITERIA"), //$NON-NLS-1$
        Constants.BOTTOM_ARROW_ICON, Constants.BOTTOM_ARROW_ROLLOVER_ICON);

    private JPanel datePanel = new JPanel(new GridLayout(0, 2, 10, 0));

    private JPanel dateCenteredPanel = new JPanel(new FlowLayout(
            FlowLayout.CENTER));

    private JPanel detailsPanel = new JPanel(new BorderLayout());

    private JPanel detailsLabelsPanel = new JPanel(new GridLayout(0, 1, 5, 5));

    private JPanel detailsFieldsPanel = new JPanel(new GridLayout(0, 1, 5, 5));

    private JPanel checksPanel = new JPanel(new GridLayout(0, 1));
    */
    
    private final HistoryWindow historyWindow;

    // private JPanel extendedSearchPanel = new JPanel(new BorderLayout());

    /**
     * Creates an instance of the <tt>SearchPanel</tt>.
     */
    public SearchPanel(HistoryWindow historyWindow)
    {
        super(new BorderLayout(5, 5));

        this.historyWindow = historyWindow;

        this.init();
    }

    /**
     * Constructs the <tt>SearchPanel</tt>.
     */
    private void init()
    {
        String searchString
            = GuiActivator.getResources().getI18NString("service.gui.SEARCH");
        JLabel searchLabel = new JLabel(searchString + ": ");
        JButton searchButton
            = new JButton(
                    searchString,
                    new ImageIcon(
                            ImageLoader.getImage(ImageLoader.SEARCH_ICON)));

        this.searchTextField.getDocument().addDocumentListener(this);

        this.add(searchLabel, BorderLayout.WEST);
        this.add(searchTextField, BorderLayout.CENTER);

        /*
        this.detailsLabelsPanel.add(dateLabel);
        this.detailsLabelsPanel.add(hourLabel);
        this.detailsLabelsPanel.add(lastNMessagesLabel);

        this.detailsFieldsPanel.add(dateTextField);
        this.detailsFieldsPanel.add(hourTextField);
        this.detailsFieldsPanel.add(lastNMessagesTextField);

        this.detailsPanel.add(detailsLabelsPanel, BorderLayout.WEST);
        this.detailsPanel.add(detailsFieldsPanel, BorderLayout.CENTER);

        this.radiosGroup.add(allMessagesRadio);
        this.radiosGroup.add(todayMessagesRadio);
        this.radiosGroup.add(yesterdayMessagesRadio);

        this.checksPanel.add(allMessagesRadio);
        this.checksPanel.add(todayMessagesRadio);
        this.checksPanel.add(yesterdayMessagesRadio);

        this.datePanel.add(checksPanel);
        this.datePanel.add(detailsPanel);

        this.dateCenteredPanel.add(datePanel);
        */
        
        searchButton.setName("search");
        searchButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.SEARCH"));
        
        // this.extendedSearchButton.setName("extendedSearch");
        // this.extendedSearchOpenedButton.setName("extendedSearchOpened");

        searchButton.addActionListener(this);

        this.historyWindow.getRootPane().setDefaultButton(searchButton);
        // this.extendedSearchPanel.add(extendedSearchButton,
        // BorderLayout.CENTER);
        this.add(searchButton, BorderLayout.EAST);

        // this.add(extendedSearchPanel, BorderLayout.SOUTH);

        // this.extendedSearchButton.addActionListener(this);
        // this.extendedSearchOpenedButton.addActionListener(this);

        //this.enableDefaultSearchSettings();
    }

    /**
     * Handles the <tt>ActionEvent</tt> which occured when user clicks
     * the Search button.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equalsIgnoreCase("search")) {
            
            historyWindow.showHistoryByKeyword(searchTextField.getText());
        }
        /*
         * else if(buttonName.equalsIgnoreCase("extendedSearch")){
         *
         * this.extendedSearchPanel.removeAll();
         * this.extendedSearchPanel.add(extendedSearchOpenedButton,
         * BorderLayout.NORTH); this.extendedSearchPanel.add(dateCenteredPanel,
         * BorderLayout.CENTER);
         *
         * this.getParent().validate(); } else
         * if(buttonName.equalsIgnoreCase("extendedSearchOpened")){
         *
         * this.extendedSearchPanel.removeAll();
         * this.extendedSearchPanel.add(extendedSearchButton,
         * BorderLayout.CENTER);
         *
         * this.getParent().validate(); }
         */
    }

    public void insertUpdate(DocumentEvent e)
    {}

    public void removeUpdate(DocumentEvent e)
    {
        if (searchTextField.getText() == null
                || searchTextField.getText().equals(""))
        {
            historyWindow.showHistoryByKeyword("");
        }
    }

    public void changedUpdate(DocumentEvent e)
    {}

    /**
     * Enables the settings for a default search.
     */
    /*
    private void enableDefaultSearchSettings() {
        this.allMessagesRadio.setSelected(true);
    }
    */
}
