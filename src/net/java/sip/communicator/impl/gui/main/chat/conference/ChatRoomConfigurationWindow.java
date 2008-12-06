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

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.swing.*;

/**
 * The configuration window for the chat room.
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomConfigurationWindow
    extends SIPCommFrame
    implements ActionListener
{   
    private ChatRoomConfigurationForm configForm;
    
    private JScrollPane generalScrollPane = new JScrollPane();
    private JScrollPane optionsScrollPane = new JScrollPane();
    
    private JPanel mainPanel = new JPanel();
    
    private JButton saveButton = new JButton(
        Messages.getI18NString("apply").getText());
    
    private JButton cancelButton = new JButton(
        Messages.getI18NString("cancel").getText());
    
    private JPanel roomOptionsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private JTabbedPane tabbedPane = new SIPCommTabbedPane(false, false);
    
    private TitlePanel titlePanel = new TitlePanel();
    
    private Hashtable uiFieldsTable = new Hashtable();
    
    /**
     * Creates an instance of <tt>ChatRoomConfigurationWindow</tt> and
     * initializes the configuration form.
     * 
     * @param configForm the configuration form to load in this configuration
     * window
     */
    public ChatRoomConfigurationWindow(String chatRoomName,
        ChatRoomConfigurationForm configForm)
    {
        this.configForm = configForm;
        
        this.setTitle(Messages.getI18NString(
            "chatRoomConfiguration", new String[]{chatRoomName}).getText());
        
        titlePanel.setTitleText(Messages.getI18NString(
            "configuration").getText());
        
        this.generalScrollPane.setPreferredSize(new Dimension(550, 500));
        this.generalScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
            Messages.getI18NString("general").getText(),
            generalScrollPane);
        
        this.tabbedPane.add(
            Messages.getI18NString("options").getText(),
            optionsScrollPane);
        
        this.getContentPane().add(titlePanel, BorderLayout.NORTH);
        this.getContentPane().add(tabbedPane, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        
        this.roomOptionsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15),
            BorderFactory.createTitledBorder(
                Messages.getI18NString("chatRoomOptions").getText())));
        
        this.loadConfigurationForm();
    }
    
    /**
     * Loads the configuration form obtained from the chat room.
     */
    private void loadConfigurationForm()
    {
        Iterator configurationSet = configForm.getConfigurationSet();
        
        int labelWidth = computeLabelWidth(configForm);
        
        while(configurationSet.hasNext())
        {
            ChatRoomConfigurationFormField formField
                = (ChatRoomConfigurationFormField) configurationSet.next();
            
            Iterator values = formField.getValues();
            Iterator options = formField.getOptions();
            
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
                
                Hashtable optionCheckBoxes = new Hashtable();
                
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
                    
                    ((JCheckBox)optionCheckBoxes.get(value)).setSelected(true);
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
                fieldPanel.setBorder(
                    BorderFactory.createEmptyBorder(0, 0, 10, 0));
                
                label.setPreferredSize(new Dimension(labelWidth, 30));
                
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
        
        Iterator configurationSet = configForm.getConfigurationSet();
        
        while(configurationSet.hasNext())
        {
            ChatRoomConfigurationFormField formField
                = (ChatRoomConfigurationFormField) configurationSet.next();
         
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
     * 
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        
        if(button.equals(saveButton))
        {
            Iterator configurationSet = configForm.getConfigurationSet();
            
            while(configurationSet.hasNext())
            {
                ChatRoomConfigurationFormField formField
                    = (ChatRoomConfigurationFormField) configurationSet.next();
             
                // If the field is of type fixed the user could not change it,
                // so we skip it.
                if(formField.getType().equals(
                    ChatRoomConfigurationFormField.TYPE_TEXT_FIXED))
                    continue;

                JComponent c
                    = (JComponent) uiFieldsTable.get(formField.getName());

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
                            Messages.getI18NString("error").getText(),
                            Messages.getI18NString(
                                "chatRoomConfigFormSubmitFailed").getText(),
                            e).showDialog();
                    }
                }
            }.start();
        }
        
        this.dispose();
    }
    
    protected void close(boolean isEscaped)
    {
    }
}
