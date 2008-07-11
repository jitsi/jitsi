/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>EditTextToolBar</tt> is a <tt>JToolBar</tt> which contains
 * buttons for formatting a text, like make text in bold or italic, change the
 * font, etc. It contains only <tt>MsgToolbarButton</tt>s, which have a
 * specific background icon and rollover behaviour to differentiates them from
 * normal buttons.
 * 
 * @author Yana Stamcheva
 */
public class EditTextToolBar
    extends SIPCommToolBar
{
    private Logger logger = Logger.getLogger(EditTextToolBar.class);

    private ColorLabel colorLabel = new ColorLabel();

    private Integer[] fontSizeConstants =
        new Integer[]
        { 9, 10, 11, 12, 13, 14, 18, 24, 36, 48, 64,
            72, 96, 144, 288 };

    private JComboBox fontSizeCombo = new JComboBox(fontSizeConstants);

    private JComboBox fontNameCombo;

    private JEditorPane chatEditorPane;

    private Action boldAction = new HTMLEditorKit.BoldAction();

    private Action italicAction = new HTMLEditorKit.ItalicAction();

    private Action underlineAction = new HTMLEditorKit.UnderlineAction();

    /**
     * Creates an instance and constructs the <tt>EditTextToolBar</tt>.
     */
    public EditTextToolBar(JEditorPane panel)
    {
        this.chatEditorPane = panel;

        this.fontNameCombo = new JComboBox(getSystemFontFamilies());
        this.fontSizeCombo.setEditable(true);

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        this.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        this.fontSizeCombo.setPreferredSize(new Dimension(50, 21));

        colorLabel.setPreferredSize(new Dimension(21, 21));

        this.initToolbarButtons();

        this.addSeparator();

        this.add(fontNameCombo);
        this.add(fontSizeCombo);

        this.addSeparator();

        this.add(colorLabel);

        logger.trace("[GUI] Editor Pane font name: "
                + chatEditorPane.getFont().getName());

        fontNameCombo.setSelectedItem(chatEditorPane.getFont().getName());
        fontSizeCombo.setSelectedItem(chatEditorPane.getFont().getSize());

        fontNameCombo.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent evt)
            {
                String fontName = (String) evt.getItem();

                ActionEvent actionEvent =
                    new ActionEvent(chatEditorPane,
                        ActionEvent.ACTION_PERFORMED, "");

                Action action =
                    new HTMLEditorKit.FontFamilyAction(fontName, fontName);

                action.actionPerformed(actionEvent);

                chatEditorPane.requestFocus();
            }
        });

        fontSizeCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ActionEvent evt =
                    new ActionEvent(chatEditorPane,
                        ActionEvent.ACTION_PERFORMED, "");

                Integer fontSize =
                    (Integer) fontSizeCombo.getSelectedItem();

                Action action =
                    new HTMLEditorKit.FontSizeAction(   fontSize.toString(),
                                                        fontSize.intValue());

                action.actionPerformed(evt);

                chatEditorPane.requestFocus();
            }
        });

        colorLabel.setOpaque(true);
        colorLabel.setBackground(Color.BLACK);
        colorLabel.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent arg0)
            {
                Color newColor =
                    JColorChooser.showDialog(new JColorChooser(),
                        "Choose a colour", colorLabel.getBackground());

                colorLabel.setBackground(newColor);

                ActionEvent evt =
                    new ActionEvent(chatEditorPane,
                        ActionEvent.ACTION_PERFORMED, "");

                Action action =
                    new HTMLEditorKit.ForegroundAction(new Integer(newColor
                        .getRGB()).toString(), newColor);

                action.actionPerformed(evt);

                chatEditorPane.requestFocus();
            }
        });
    }

    private void initToolbarButtons()
    {
        JToggleButton boldButton =
            initStyleToggleButton(boldAction, StyleConstants.Bold);

        boldButton.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.TEXT_BOLD_BUTTON)));

        this.add(boldButton);

        JToggleButton italicButton =
            initStyleToggleButton(italicAction, StyleConstants.Italic);

        italicButton.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.TEXT_ITALIC_BUTTON)));

        this.add(italicButton);

        JToggleButton underlineButton =
            initStyleToggleButton(underlineAction, StyleConstants.Underline);

        underlineButton.setIcon(new ImageIcon(ImageLoader
            .getImage(ImageLoader.TEXT_UNDERLINED_BUTTON)));

        this.add(underlineButton);

        this.addBindings();
    }

    private JToggleButton initStyleToggleButton(final Action action,
        final Object styleConstant)
    {
        final JToggleButton button = new JToggleButton();
        button.setPreferredSize(new Dimension(25, 25));

        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                action.actionPerformed(e);
            }
        });

        chatEditorPane.addCaretListener(new CaretListener()
        {
            public void caretUpdate(CaretEvent e)
            {
                selectButton(styleConstant, button);
            }
        });

        chatEditorPane.addKeyListener(new KeyAdapter()
        {
            public void keyTyped(KeyEvent e)
            {
                button.setSelected(((HTMLEditorKit) chatEditorPane
                    .getEditorKit()).getInputAttributes().containsAttribute(
                    styleConstant, true));
            }
        });
        chatEditorPane.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                selectButton(styleConstant, button);
            }
        });
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                chatEditorPane.requestFocus();
            }
        });

        return button;
    }

    /**
     * Selects or deselects the given toggle button depending on the given
     * <tt>styleConstant</tt>.
     * 
     * @param styleConstant
     * @param button
     */
    private void selectButton(  final Object styleConstant,
                                final JToggleButton button)

    {
        boolean selected = false;

        if (chatEditorPane.getSelectedText() == null)
        {
            int index = chatEditorPane.getCaretPosition();
            selected =
                ((HTMLDocument) chatEditorPane.getDocument())
                    .getCharacterElement(index - 1).getAttributes()
                    .containsAttribute(styleConstant, true);
        }
        else
        {
            for (int index = chatEditorPane.getSelectionStart();
                index < chatEditorPane.getSelectionEnd(); index++)
            {
                AttributeSet attributes =
                    ((HTMLDocument) chatEditorPane.getDocument())
                        .getCharacterElement(index).getAttributes();

                selected =
                    selected
                        || attributes.containsAttribute(styleConstant, true);
            }
        }
        button.setSelected(selected);
    }

    /**
     * Adds key bindings for formatting actions.
     */
    private void addBindings()
    {
        InputMap inputMap = chatEditorPane.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,
            Event.SHIFT_MASK), DefaultEditorKit.deletePrevCharAction);

        // styles
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK),
            "font-bold"); //$NON-NLS-1$
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK),
            "font-bold"); //$NON-NLS-1$
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK),
            "font-italic"); //$NON-NLS-1$
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.CTRL_MASK),
            "font-italic"); //$NON-NLS-1$
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK),
            "font-underline"); //$NON-NLS-1$
    }

    /**
     * Returns all supported local system font names.
     * 
     * @return an array containing all supported local system font names.
     */
    private String[] getSystemFontFamilies()
    {
        // Get all font family names
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();

        return ge.getAvailableFontFamilyNames();
    }
    
    private class ColorLabel extends JLabel
    {
        public void paintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(this.getBackground());

            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
    }
}
