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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>FontChooserDialog</tt> is a dialog for font selection
 *
 * @author Damien Roth
 */
public class FontChooser
    extends JComponent
    implements ActionListener, ListSelectionListener
{
    private InputList fontFamilyPanel;

    private InputList fontSizePanel;

    private JCheckBox boldCheckBox;

    private JCheckBox italicCheckBox;

    private JCheckBox underlineCheckBox;

    private ColorLabel colorLabel;

    private JLabel previewLabel;

    private String[] fontFamilies;

    private final String[] fontSizes
        = { "8", "9", "10", "11", "12", "14", "16",
            "18", "20", "22", "24", "26", "28", "36", "48", "72" };

    private static final String previewText = "Preview Text";

    private static final String ACTCMD_CHOOSE_COLOR = "ACTCMD_CHOOSE_COLOR";

    public static final int OK_OPTION = 1;
    public static final int CANCEL_OPTION = 0;

    protected int option = CANCEL_OPTION;

    public FontChooser()
    {
        ResourceManagementService res = GuiActivator.getResources();

        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //-- Init InputList panels
        // Font family
        GraphicsEnvironment ge
            = GraphicsEnvironment.getLocalGraphicsEnvironment();
        this.fontFamilies = ge.getAvailableFontFamilyNames();
        this.fontFamilyPanel
            = new InputList(res.getI18NString("service.gui.FONT_FAMILY"),
                            this.fontFamilies);
        this.fontFamilyPanel.addListSelectionListener(this);

        // Font size
        this.fontSizePanel
            = new InputList(res.getI18NString("service.gui.FONT_SIZE"),
                            this.fontSizes);

        this.fontSizePanel.addListSelectionListener(this);

        JPanel listsPanels = new JPanel(new GridLayout(1, 2, 5, 5));
        listsPanels.add(this.fontFamilyPanel, BorderLayout.WEST);
        listsPanels.add(this.fontSizePanel, BorderLayout.EAST);

        //-- Style
        JLabel styleLabel
            = new JLabel(res.getI18NString("service.gui.FONT_STYLE"));
        styleLabel.setPreferredSize(new Dimension(100, 0));
        styleLabel.setFont(styleLabel.getFont().deriveFont(Font.BOLD));

        // Bold
        this.boldCheckBox
            = new SIPCommCheckBox(res.getI18NString("service.gui.FONT_BOLD"));
        this.boldCheckBox.addActionListener(this);
        this.boldCheckBox.setOpaque(false);
        // Italic
        this.italicCheckBox
            = new SIPCommCheckBox(res.getI18NString("service.gui.FONT_ITALIC"));
        this.italicCheckBox.addActionListener(this);
        // Underline
        this.underlineCheckBox
            = new SIPCommCheckBox(res.getI18NString("service.gui.FONT_UNDERLINE"));
        this.underlineCheckBox.addActionListener(this);

        // Panel
        JPanel styleGridPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        styleGridPanel.add(this.boldCheckBox);
        styleGridPanel.add(this.italicCheckBox);
        styleGridPanel.add(this.underlineCheckBox);

        JPanel stylePanel = new JPanel(new BorderLayout(10, 10));
        stylePanel.add(styleLabel, BorderLayout.WEST);
        stylePanel.add(styleGridPanel, BorderLayout.CENTER);

        //-- Color
        JLabel colorTextLabel
            = new JLabel(res.getI18NString("service.gui.FONT_COLOR"));
        colorTextLabel.setPreferredSize(new Dimension(100, 0));
        colorTextLabel.setFont(styleLabel.getFont().deriveFont(Font.BOLD));

        // Color label
        this.colorLabel = new ColorLabel();
        this.colorLabel.setOpaque(true);
        this.colorLabel.setBackground(Color.BLACK);
        this.colorLabel.setPreferredSize(new Dimension(18, 18));

        // Color button
        JButton colorButton
            = new JButton(res.getI18NString("service.gui.SELECT_COLOR"));
        colorButton.addActionListener(this);
        colorButton.setName(ACTCMD_CHOOSE_COLOR);

        // Panel
        JPanel colorPanelCenter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        colorPanelCenter.add(colorLabel);
        colorPanelCenter.add(colorButton);

        JPanel colorPanel = new JPanel(new BorderLayout());
        colorPanel.add(colorTextLabel, BorderLayout.WEST);
        colorPanel.add(colorPanelCenter, BorderLayout.CENTER);

        // Format Panel
        JPanel formatPanel = new JPanel();
        formatPanel.setLayout(new BoxLayout(formatPanel, BoxLayout.Y_AXIS));
        formatPanel.add(listsPanels);
        formatPanel.add(stylePanel);
        formatPanel.add(colorPanel);

        this.add(formatPanel, BorderLayout.NORTH);

        // Preview Label
        this.previewLabel = new JLabel(previewText, JLabel.CENTER);
        this.previewLabel.setOpaque(true);
        this.previewLabel.setBackground(Color.WHITE);
        this.previewLabel.setPreferredSize(new Dimension(0, 100));
        this.previewLabel.setBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1));

        this.add(this.previewLabel, BorderLayout.CENTER);

        initDefaults();
    }

    /**
     * Initializes previously saved default fonts.
     */
    private void initDefaults()
    {
        String defaultFontFamily
            = ConfigurationUtils.getChatDefaultFontFamily();

        int defaultFontSize
            = ConfigurationUtils.getChatDefaultFontSize();

        Color defaultFontColor
            = ConfigurationUtils.getChatDefaultFontColor();

        if (defaultFontFamily != null)
            setFontFamily(defaultFontFamily);

        if (defaultFontSize > 0)
            setFontSize(defaultFontSize);

        setBoldStyle(ConfigurationUtils.isChatFontBold());
        setItalicStyle(ConfigurationUtils.isChatFontItalic());
        setUnderlineStyle(ConfigurationUtils.isChatFontUnderline());

        if (defaultFontColor != null)
            setColor(defaultFontColor);
    }

    /**
     * Updates the font preview area.
     */
    private void updatePreview()
    {
        Font f = new Font(this.fontFamilyPanel.getSelected(),
                Font.PLAIN,
                this.fontSizePanel.getSelectedInt());

        String text = this.fontFamilyPanel.getSelected();

        if (this.boldCheckBox.isSelected())
            text = "<b>" + text + "</b>";
        if (this.italicCheckBox.isSelected())
            text = "<i>" + text + "</i>";
        if (this.underlineCheckBox.isSelected())
            text = "<u>" + text + "</u>";

        this.previewLabel.setFont(f);
        this.previewLabel.setForeground(this.colorLabel.getBackground());
        this.previewLabel.setText("<html>"+text+"</html>");
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() instanceof JButton)
        {
            JButton source = (JButton) e.getSource();
            String name = source.getName();
            if (name.equals(ACTCMD_CHOOSE_COLOR))
            {
                Color c = JColorChooser.showDialog(this,
                        "Color Chooser",
                        this.colorLabel.getBackground());
                this.colorLabel.setBackground(c);
            }
        }
        updatePreview();
    }

    public void valueChanged(ListSelectionEvent e)
    {
        updatePreview();
    }

    /**
     * Sets the family name of the font
     * @param family the family name
     */
    public void setFontFamily(String family)
    {
        if (family.endsWith(".plain"))
            family = family.replace(".plain", "");

        for (String f : fontFamilies)
        {
            String oldF = f;
            if (family.equals(f) || family.equals(f.replaceAll(" ", "")))
                fontFamilyPanel.setSelected(oldF);
        }

        updatePreview();
    }

    /**
     * Sets the size of the font
     * @param size
     */
    public void setFontSize(int size)
    {
        this.fontSizePanel.setSelectedInt(size);
        this.updatePreview();
    }

    /**
     * Enables the bold style
     * @param b TRUE enable - FALSE disable
     */
    public void setBoldStyle(boolean b)
    {
        this.boldCheckBox.setSelected(b);
        this.updatePreview();
    }

    /**
     * Enables the italic style
     * @param b TRUE enable - FALSE disable
     */
    public void setItalicStyle(boolean b)
    {
        this.italicCheckBox.setSelected(b);
        this.updatePreview();
    }

    /**
     * Enables the underline style
     * @param b TRUE enable - FALSE disable
     */
    public void setUnderlineStyle(boolean b)
    {
        this.underlineCheckBox.setSelected(b);
        this.updatePreview();
    }

    /**
     * Sets the font's color
     * @param c the color
     */
    public void setColor(Color c)
    {
        this.colorLabel.setBackground(c);
        this.updatePreview();
    }

    /**
     * Returns the family name of the selected font
     * @return the family name of the selected font
     *
     * @see #setFontFamily
     */
    public String getFontFamily()
    {
        return this.fontFamilyPanel.getSelected();
    }

    /**
     * Returns the size of the selected font
     * @return the size of the selected font
     *
     * @see #setFontSize
     */
    public int getFontSize()
    {
        return this.fontSizePanel.getSelectedInt();
    }

    /**
     * Checks if bold checkbox is selected
     * @return TRUE is the checkbox is selected - FALSE otherwise
     */
    public boolean isBoldStyleSelected()
    {
        return this.boldCheckBox.isSelected();
    }

    /**
     * Checks if italic checkbox is selected
     * @return TRUE is the checkbox is selected - FALSE otherwise
     */
    public boolean isItalicStyleSelected()
    {
        return this.italicCheckBox.isSelected();
    }

    /**
     * Checks if underline checkbox is selected
     * @return TRUE is the checkbox is selected - FALSE otherwise
     */
    public boolean isUnderlineStyleSelected()
    {
        return this.underlineCheckBox.isSelected();
    }

    /**
     * Returns the font color
     * @return the font color
     */
    public Color getFontColor()
    {
        return this.colorLabel.getBackground();
    }

    private static class ColorLabel extends JLabel
    {
        @Override
        public void paintComponent(Graphics g)
        {
            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.setColor(this.getBackground());

                g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    private class InputList
        extends JPanel
        implements KeyListener, ListSelectionListener
    {
        private JLabel label = new JLabel();

        private JTextField textField = new JTextField();

        private JList list;

        public InputList(String title, Object[] data)
        {
            this.setLayout(new BorderLayout());

            this.label.setText(title);
            this.label.setFont(this.label.getFont().deriveFont(Font.BOLD));
            this.add(this.label, BorderLayout.NORTH);

            JPanel middlePanel = new JPanel(new BorderLayout());

            this.textField.addKeyListener(this);
            middlePanel.add(this.textField, BorderLayout.NORTH);

            this.list = new JList(data);
            this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.list.setVisibleRowCount(5);
            this.list.setSelectedIndex(0);
            this.list.addListSelectionListener(this);
            this.setFocusable(false);
            middlePanel.add(new JScrollPane(this.list), BorderLayout.CENTER);

            this.add(middlePanel, BorderLayout.CENTER);
        }

        public void setSelected(String value)
        {
            ListModel l = this.list.getModel();
            boolean inList = false;

            for (int i=0; i<l.getSize() && !inList; i++)
                if (l.getElementAt(i).toString().equals(value))
                    inList = true;

            if (inList)
                this.list.setSelectedValue(value, true);
            else
                this.list.clearSelection();
            this.textField.setText(value);
        }

        public String getSelected()
        {
            return this.textField.getText();
        }

        public void setSelectedInt(int value)
        {
            this.setSelected(Integer.toString(value));
        }

        public int getSelectedInt()
        {
            try
            {
                return Integer.parseInt(this.getSelected());
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }

        public void addListSelectionListener(ListSelectionListener l)
        {
            this.list.addListSelectionListener(l);
        }

        public void valueChanged(ListSelectionEvent e)
        {
            String selectedValue = (String) this.list.getSelectedValue();
            String oldValue = this.textField.getText();

            this.textField.setText(selectedValue);
            if (!oldValue.equalsIgnoreCase(selectedValue))
            {
                this.textField.selectAll();
                this.textField.requestFocus();
            }
        }

        public void keyReleased(KeyEvent e)
        {
            if (e.getKeyCode() == KeyEvent.VK_LEFT
                || e.getKeyCode() == KeyEvent.VK_RIGHT)
                return;

            String elem, key = this.textField.getText().toLowerCase();
            ListModel model = this.list.getModel();

            for (int i=0; i<model.getSize(); i++)
            {
                elem = model.getElementAt(i).toString();
                if (elem.toLowerCase().startsWith(key))
                {
                    this.list.setSelectedValue(model.getElementAt(i), true);
                    break;
                }
            }
        }

        public void keyPressed(KeyEvent e)
        {
            int i = this.list.getSelectedIndex();
            if (e.getKeyCode() == KeyEvent.VK_UP && i > 0)
            {
                this.list.setSelectedIndex(i-1);
            }
            else if (e.getKeyCode() == KeyEvent.VK_DOWN &&
                    i < this.list.getModel().getSize()-1)
            {
                this.list.setSelectedIndex(i+1);
            }
        }

        // Not used
        public void keyTyped(KeyEvent e) {;}
    }

    // Dialog creation
    public int showDialog(Component parent)
    {
        JDialog dialog = createDialog(parent);
        dialog.setVisible(true);
        dialog.dispose();
        dialog = null;
        return this.option;
    }

    /**
     * Creates this dialog.
     *
     * @param parent the parent dialog
     * @return the created dialog
     */
    private SIPCommDialog createDialog(Component parent)
    {
        final SIPCommDialog d = new SIPCommDialog();

        ResourceManagementService res = GuiActivator.getResources();

        d.setTitle(res.getI18NString("service.gui.FONT"));
        d.setModal(true);

        // Ok and Cancel buttons
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.OK"));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                option = OK_OPTION;
                d.setVisible(false);
            }
        });
        buttonsPanel.add(okButton);

        JButton cancelButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.CANCEL"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                option = CANCEL_OPTION;
                d.setVisible(false);
            }
        });
        buttonsPanel.add(cancelButton);

        d.getContentPane().setLayout(new BorderLayout());
        d.getContentPane().add(this, BorderLayout.CENTER);
        d.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(parent);

        return d;
    }
}
