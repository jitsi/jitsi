/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The configuration window for a chat room.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class ChatRoomConfigurationWindow
    extends SIPCommFrame
    implements ActionListener
{
    /**
     * The configuration form contained in this window.
     */
    private ChatRoomConfigurationForm configForm;

    /**
     * The scroll pane contained in the "General" tab.
     */
    private JScrollPane generalScrollPane = new JScrollPane();

    /**
     * The scroll pane contained in the "Options" tab.
     */
    private JScrollPane optionsScrollPane = new JScrollPane();

    /**
     * The main panel.
     */
    private JPanel mainPanel = new JPanel();

    /**
     * The button that stores the data.
     */
    private JButton saveButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.APPLY"));

    /**
     * The cancel button.
     */
    private JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The panel contained in the "Options" tab.
     */
    private JPanel roomOptionsPanel = new JPanel(new GridLayout(0, 1));

    /**
     * The panel containing all buttons.
     */
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The tabbed pane containing the "General" and "Options" tabs.
     */
    private JTabbedPane tabbedPane = new SIPCommTabbedPane(false, false);

    /**
     * The panel containing the title.
     */
    private TitlePanel titlePanel = new TitlePanel();

    /**
     * A map all configuration components.
     */
    private Hashtable<String, JComponent> uiFieldsTable
        = new Hashtable<String, JComponent>();

    /**
     * Creates an instance of <tt>ChatRoomConfigurationWindow</tt> and
     * initializes the configuration form.
     *
     * @param chatRoomName the name of the room
     * @param configForm the configuration form to load in this configuration
     * window
     */
    public ChatRoomConfigurationWindow(String chatRoomName,
        ChatRoomConfigurationForm configForm)
    {
        super(false);

        this.configForm = configForm;

        this.setTitle(GuiActivator.getResources().getI18NString(
            "service.gui.CHAT_ROOM_CONFIGURATION",
            new String[]{chatRoomName}));

        titlePanel.setTitleText(GuiActivator.getResources().getI18NString(
            "service.gui.SETTINGS"));

        this.generalScrollPane.setPreferredSize(new Dimension(550, 500));
        this.generalScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.optionsScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));
        this.tabbedPane.setBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        this.saveButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(saveButton);
        this.buttonsPanel.add(cancelButton);

        this.generalScrollPane.getViewport().add(mainPanel);
        this.optionsScrollPane.getViewport().add(roomOptionsPanel,
            BorderLayout.NORTH);

        this.tabbedPane.add(
            GuiActivator.getResources().getI18NString("service.gui.GENERAL"),
            generalScrollPane);

        this.tabbedPane.add(
            GuiActivator.getResources().getI18NString("service.gui.OPTIONS"),
            optionsScrollPane);

        this.getContentPane().add(titlePanel, BorderLayout.NORTH);
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        titlePanel.setOpaque(false);
        tabbedPane.setOpaque(false);
        buttonsPanel.setOpaque(false);
        roomOptionsPanel.setOpaque(false);
        optionsScrollPane.setOpaque(false);
        mainPanel.setOpaque(false);
        generalScrollPane.setOpaque(false);

        this.roomOptionsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15),
            BorderFactory.createTitledBorder(
                GuiActivator.getResources()
                    .getI18NString("service.gui.CHAT_ROOM_OPTIONS"))));

        this.loadConfigurationForm();
    }

    /**
     * Loads the configuration form obtained from the chat room.
     */
    private void loadConfigurationForm()
    {
        Iterator<ChatRoomConfigurationFormField> configurationSet
            = configForm.getConfigurationSet();

        int labelWidth = computeLabelWidth(configForm);

        while(configurationSet.hasNext())
        {
            ChatRoomConfigurationFormField formField
                = configurationSet.next();

            Iterator<?> values = formField.getValues();
            Iterator<String> options = formField.getOptions();

            JComponent field = null;
            JLabel label = new JLabel();

            if(formField.getLabel() != null)
            {
                label.setText(formField.getLabel() + ": ");
            }

            String fieldType = formField.getType();

            if(fieldType.equals(ChatRoomConfigurationFormField.TYPE_BOOLEAN))
            {
                // Create a check box when the field is of type boolean.
                field = new SIPCommCheckBox(formField.getLabel());

                if(values.hasNext())
                {
                    ((JCheckBox)field)
                        .setSelected(((Boolean)values.next()).booleanValue());
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
            {
                // Create a not editable text area when we have a fixed text.

                field = new JTextArea();

                ((JTextArea)field).setEditable(false);

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JTextArea) field).setText(value);
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_LIST_MULTI))
            {
                field = new JPanel(new GridLayout(0, 1));

                field.setBorder(BorderFactory.createLineBorder(Color.GRAY));

                Hashtable<Object, JCheckBox> optionCheckBoxes
                    = new Hashtable<Object, JCheckBox>();

                while(options.hasNext())
                {
                    Object option = options.next();
                    JCheckBox checkBox = new SIPCommCheckBox(option.toString());

                    field.add(checkBox);
                    optionCheckBoxes.put(option, checkBox);
                }

                while(values.hasNext())
                {
                    Object value = values.next();

                    (optionCheckBoxes.get(value)).setSelected(true);
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_LIST_SINGLE))
            {
                field = new JComboBox();

                while(options.hasNext())
                {
                    ((JComboBox) field).addItem(options.next());
                }

                if(values.hasNext())
                {
                    ((JComboBox)field).setSelectedItem(values.next());
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_MULTI))
            {
                field = new JEditorPane();

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JEditorPane) field).setText(value);
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_SINGLE))
            {
                field = new JTextField();

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JTextField) field).setText(value);
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_PRIVATE))
            {
                field = new JPasswordField();

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JPasswordField) field).setText(value);
                }
            }
            else
            {
                if(label.getText() == null)
                    continue;

                field = new JTextField();

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JTextField) field).setText(value);
                }
            }

            // If the field is not fixed (i.e. could be changed) we would like
            // to save it in a list in order to use it later when user saves
            // the configuration data.            
            if(!fieldType.equals(ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
            {
                uiFieldsTable.put(formField.getName(), field);
            }

            // If the field is of type boolean we would like to separate it in
            // the options panel.
            if(fieldType.equals(ChatRoomConfigurationFormField.TYPE_BOOLEAN))
            {
                roomOptionsPanel.add(field);
            }
            else
            {
                JPanel fieldPanel = new JPanel(new BorderLayout());
                fieldPanel.setOpaque(false);
                fieldPanel.setBorder(
                    BorderFactory.createEmptyBorder(0, 0, 10, 0));

                label.setPreferredSize(new Dimension(labelWidth, 30));
                label.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
                
                fieldPanel.add(label, BorderLayout.WEST);
                fieldPanel.add(field, BorderLayout.CENTER);

                this.mainPanel.add(fieldPanel);
            }
        }
    }

    /**
     * Computes the maximum width of a label in the configuration form.
     * 
     * @param configForm the configuration form containing all labels.
     * @return the maximum width of a label in the configuration form
     */
    private int computeLabelWidth(ChatRoomConfigurationForm configForm)
    {
        int labelWidth = 0;

        Iterator<ChatRoomConfigurationFormField> configurationSet
            = configForm.getConfigurationSet();

        while(configurationSet.hasNext())
        {
            ChatRoomConfigurationFormField formField
                = configurationSet.next();

            if(formField.getLabel() == null)
                continue;

            JLabel label = new JLabel(formField.getLabel());

            int newLabelWidth = SwingUtilities.computeStringWidth(
                label.getFontMetrics(label.getFont()), formField.getLabel());

            if(newLabelWidth > labelWidth)
                labelWidth = newLabelWidth;
        }

        // We add 10 pixels to be sure that even after adding the ':' char
        // the label will rest visible.
        return labelWidth + 10;
    }

    /**
     * Saves all configuration settings when the "Save" button is pressed.
     * @param e the <tt>ActionEvent</tt> that notified us of the button action
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();

        if(button.equals(saveButton))
        {
            Iterator<ChatRoomConfigurationFormField> configurationSet
                = configForm.getConfigurationSet();

            while(configurationSet.hasNext())
            {
                ChatRoomConfigurationFormField formField
                    = configurationSet.next();

                // If the field is of type fixed the user could not change it,
                // so we skip it.
                if(formField.getType().equals(
                    ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
                    continue;

                JComponent c
                    = uiFieldsTable.get(formField.getName());

                if (c instanceof JTextComponent)
                {
                    String newValue = ((JTextComponent)c).getText();

                    formField.addValue(newValue);
                }
                else if (c instanceof AbstractButton)
                {
                    boolean isSelected = ((AbstractButton)c).isSelected();

                    formField.addValue(new Boolean(isSelected));
                }
                else if (c instanceof JComboBox)
                {
                    Object selectedObject = ((JComboBox)c).getSelectedItem();

                    formField.addValue(selectedObject);
                }
                else if (c instanceof JPanel)
                {
                    Component[] components = c.getComponents();

                    for(int i = 0; i < components.length; i++)
                    {
                        if(!(components[i] instanceof JCheckBox))
                            continue;

                        JCheckBox checkBox = (JCheckBox) components[i];

                        formField.addValue(checkBox.getText());
                    }
                }
            }

            new Thread()
            {
                public void run()
                {
                    try
                    {
                        configForm.submit();
                    }
                    catch (Exception e)
                    {
                        new ErrorDialog(
                            ChatRoomConfigurationWindow.this,
                            GuiActivator.getResources().getI18NString(
                            "service.gui.ERROR"),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.CHAT_ROOM_CONFIGURATION_SUBMIT_FAILED"),
                            e).showDialog();
                    }
                }
            }.start();
        }

        this.dispose();
    }

    protected void close(boolean isEscaped) {}
}
