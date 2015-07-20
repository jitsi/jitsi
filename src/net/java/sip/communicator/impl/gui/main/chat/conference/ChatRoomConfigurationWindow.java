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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The configuration window for a chat room.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Damian Minkov
 */
@SuppressWarnings("serial")
public class ChatRoomConfigurationWindow
    extends SIPCommFrame
    implements ActionListener
{
    /**
     * The configuration form contained in this window.
     */
    protected ChatRoomConfigurationForm configForm;

    /**
     * The scroll pane contained in the "General" tab.
     */
    protected JScrollPane generalScrollPane = new JScrollPane();

    /**
     * The main panel.
     */
    protected JPanel mainPanel = new TransparentPanel();

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
     * The panel containing all buttons.
     */
    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.CENTER));

    /**
     * The panel containing the title.
     */
    private TitlePanel titlePanel = new TitlePanel();

    /**
     * A map all configuration components.
     */
    protected Hashtable<String, JComponent> uiFieldsTable
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
            "service.gui.CHAT_ROOM_OPTIONS"));

        this.generalScrollPane.setPreferredSize(new Dimension(820, 520));
        this.generalScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.generalScrollPane.setOpaque(false);
        this.generalScrollPane.getViewport().setOpaque(false);

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

        this.mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.saveButton.addActionListener(this);
        this.cancelButton.addActionListener(this);

        this.buttonsPanel.add(saveButton);
        this.buttonsPanel.add(cancelButton);

        this.generalScrollPane.getViewport().add(mainPanel);

        this.getContentPane().add(titlePanel, BorderLayout.NORTH);
        this.getContentPane().add(generalScrollPane, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        titlePanel.setOpaque(false);
        buttonsPanel.setOpaque(false);
        mainPanel.setOpaque(false);
        generalScrollPane.setOpaque(false);

        this.loadConfigurationForm();
    }

    /**
     * Loads the configuration form obtained from the chat room.
     */
    protected void loadConfigurationForm()
    {
        Iterator<ChatRoomConfigurationFormField> configurationSet
            = configForm.getConfigurationSet();

        while(configurationSet.hasNext())
        {
            ChatRoomConfigurationFormField formField
                = configurationSet.next();

            Iterator<?> values = formField.getValues();
            Iterator<String> options = formField.getOptions();

            JComponent field;
            JLabel label = new JLabel("", JLabel.RIGHT);

            if(formField.getLabel() != null)
                label.setText(formField.getLabel() + ": ");

            String fieldType = formField.getType();

            if(fieldType.equals(ChatRoomConfigurationFormField.TYPE_BOOLEAN))
            {
                // Create a check box when the field is of type boolean.
                field = new SIPCommCheckBox(formField.getLabel());
                label.setText("");

                if(values.hasNext())
                {
                    ((JCheckBox)field)
                        .setSelected((Boolean)values.next());
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
            {
                field = new JLabel();

                if(values.hasNext())
                {
                    String value = values.next().toString();

                    ((JLabel) field).setText(value);
                    field.setFont(new Font(null, Font.ITALIC, 10));
                    field.setForeground(Color.GRAY);
                }
            }
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_LIST_MULTI))
            {
                field = new TransparentPanel(new GridLayout(0, 1));

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
                ChatRoomConfigurationFormField.TYPE_TEXT_SINGLE)
                    || fieldType.equals(
                            ChatRoomConfigurationFormField.TYPE_ID_SINGLE))
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
            else if(fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_ID_MULTI))
            {
                StringBuffer buff = new StringBuffer();

                while(values.hasNext())
                {
                    String value = values.next().toString();
                    buff.append(value);

                    if(values.hasNext())
                        buff.append(System.getProperty("line.separator"));
                }
                field = new JTextArea(buff.toString());
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
            if(!fieldType.equals(
                ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
            {
                uiFieldsTable.put(formField.getName(), field);
            }

            JPanel fieldPanel = new TransparentPanel(new GridLayout(1,2));
            fieldPanel.setOpaque(false);

            if(!(field instanceof JLabel))
                fieldPanel.setBorder(
                    BorderFactory.createEmptyBorder(0, 0, 8, 0));
            else
                fieldPanel.setBorder(
                    BorderFactory.createEmptyBorder(0, 0, 1, 0));

            fieldPanel.add(label);
            fieldPanel.add(field);

            this.mainPanel.add(fieldPanel);
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

                    if(formField.getType().equals(
                        ChatRoomConfigurationFormField.TYPE_ID_MULTI))
                    {
                        // extract values
                        StringTokenizer idTokens = new StringTokenizer(
                            newValue, System.getProperty("line.separator"));
                        while(idTokens.hasMoreTokens())
                        {
                            formField.addValue(idTokens.nextToken());
                        }
                    }
                    else
                        formField.addValue(newValue);
                }
                else if (c instanceof AbstractButton)
                {
                    boolean isSelected = ((AbstractButton)c).isSelected();

                    formField.addValue(isSelected);
                }
                else if (c instanceof JComboBox)
                {
                    Object selectedObject = ((JComboBox)c).getSelectedItem();

                    formField.addValue(selectedObject);
                }
                else if (c instanceof JPanel)
                {
                    Component[] components = c.getComponents();

                    for(Component comp : components)
                    {
                        if(!(comp instanceof JCheckBox))
                            continue;

                        JCheckBox checkBox = (JCheckBox) comp;

                        formField.addValue(checkBox.getText());
                    }
                }
            }

            new Thread()
            {
                @Override
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
                            "service.gui.CHAT_ROOM_CONFIGURATION_SUBMIT_FAILED")
                            , e).showDialog();
                    }
                }
            }.start();
        }

        this.dispose();
    }

    @Override
    protected void close(boolean isEscaped)
    {
        this.dispose();
    }

    /**
     * Overwrites the setVisible method in order to set the
     * position of this window before showing it.
     * @param isVisible indicates if this frame should be visible
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        if(isVisible)
            setLocationRelativeTo(null);

        super.setVisible(isVisible);
    }
}
