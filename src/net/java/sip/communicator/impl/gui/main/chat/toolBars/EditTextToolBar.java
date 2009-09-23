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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    extends TransparentPanel
{
    private Logger logger = Logger.getLogger(EditTextToolBar.class);

    private ChatWritePanel chatWritePanel;

    private JEditorPane chatEditorPane;

    private Action boldAction = new HTMLEditorKit.BoldAction();

    private Action italicAction = new HTMLEditorKit.ItalicAction();

    private Action underlineAction = new HTMLEditorKit.UnderlineAction();

    private SIPCommButton fontButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
        ImageLoader.getImage(ImageLoader.FONT_ICON));

    private SmileysSelectorBox smileysBox;

    private JToggleButton boldButton;

    private JToggleButton italicButton;

    private JToggleButton underlineButton;

    private ColorLabel colorLabel;

    /**
     * Creates an instance and constructs the <tt>EditTextToolBar</tt>.
     */
    public EditTextToolBar(ChatWritePanel writePanel)
    {
        this.chatWritePanel = writePanel;
        this.chatEditorPane = writePanel.getEditorPane();

        this.setOpaque(false);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.initStyleToolbarButtons();

        this.add(fontButton);

        this.initColorLabel();

        this.smileysBox = new SmileysSelectorBox(chatWritePanel);

        this.smileysBox.setName("smiley");
        this.smileysBox.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.INSERT_SMILEY") + " Ctrl-M");

        this.add(smileysBox);

        logger.trace("[GUI] Editor Pane font name: "
            + chatEditorPane.getFont().getName());

        fontButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                showFontChooserDialog();
            }
        });
    }

    /**
     * Initializes the label that changes font color.
     */
    private void initColorLabel()
    {
        this.colorLabel = new ColorLabel();
        colorLabel.setPreferredSize(new Dimension(18, 18));

        colorLabel.setOpaque(true);
        colorLabel.setBackground(Color.BLACK);

        this.add(colorLabel);

        colorLabel.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent event)
            {
                Color newColor =
                    JColorChooser.showDialog(new JColorChooser(),
                                            "Choose a colour",
                                            colorLabel.getBackground());

                if (newColor != null)
                    setFontColor(newColor);

                chatEditorPane.requestFocus();
            }
        });

        chatEditorPane.addCaretListener(new CaretListener()
        {
            public void caretUpdate(CaretEvent e)
            {
                selectColor(StyleConstants.Foreground, colorLabel);
            }
        });

        chatEditorPane.addKeyListener(new KeyAdapter()
        {
            public void keyTyped(KeyEvent e)
            {
                if (chatWritePanel.getText().length() > 0)
                {
                    Color currentColor
                        = (Color) ((HTMLEditorKit) chatEditorPane
                            .getEditorKit()).getInputAttributes().getAttribute(
                                StyleConstants.Foreground);

                    if (currentColor != null)
                        colorLabel.setBackground(currentColor);
                    else
                        colorLabel.setBackground(Color.BLACK);
                }
            }
        });

        chatEditorPane.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                selectColor(StyleConstants.Foreground, colorLabel);
            }
        });
    }

    /**
     * Initializes Bold, Italic and Underline toggle buttons.
     */
    private void initStyleToolbarButtons()
    {
        this.boldButton = initStyleToggleButton(
                ImageLoader.getImage(ImageLoader.TEXT_BOLD_BUTTON),
                boldAction,
                StyleConstants.Bold);

        this.add(boldButton);

        this.italicButton = initStyleToggleButton(
            ImageLoader.getImage(ImageLoader.TEXT_ITALIC_BUTTON),
            italicAction,
            StyleConstants.Italic);

        this.add(italicButton);

        this.underlineButton = initStyleToggleButton(
            ImageLoader.getImage(ImageLoader.TEXT_UNDERLINED_BUTTON),
            underlineAction,
            StyleConstants.Underline);

        this.add(underlineButton);

        this.addBindings();
    }

    /**
     * Initializes a toggle button.
     * 
     * @param action the action to associate with the button
     * @param styleConstant the style constant
     * @return the toggle button with the associated action and style constant
     */
    private JToggleButton initStyleToggleButton(Image buttonImage,
                                                final Action action,
                                                final Object styleConstant)
    {
        final SIPCommToggleButton button
            = new SIPCommToggleButton(
                ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
                ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
                buttonImage,
                ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON_PRESSED));

        button.setPreferredSize(new Dimension(18, 18));
        button.setName(styleConstant.toString());
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                ActionEvent event = new ActionEvent(chatEditorPane,
                                                    ActionEvent.ACTION_PERFORMED,
                                                    styleConstant.toString());

                action.actionPerformed(event);

                chatEditorPane.requestFocus();
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
                if (chatEditorPane.getText().length() > 0)
                {
                    button.setSelected(((HTMLEditorKit) chatEditorPane
                        .getEditorKit()).getInputAttributes().containsAttribute(
                            styleConstant, true));
                }
            }
        });

        chatEditorPane.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                selectButton(styleConstant, button);
            }
        });

        return button;
    }

    /**
     * Selects or deselects the given toggle button depending on the given
     * <tt>styleConstant</tt>.
     * 
     * @param styleConstant the style constant
     * @param button the button to select
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

        if (chatEditorPane.getText().length() > 0)
            button.setSelected(selected);
    }

    /**
     * Selects the color corresponding to the current style attribute.
     * 
     * @param styleConstant the style constant
     * @param colorLabel the color label to select the color from
     */
    private void selectColor(   final Object styleConstant,
                                final JLabel colorLabel)
    {
        Object selectedAttribute = null;

        if (chatEditorPane.getSelectedText() == null)
        {
            int index = chatEditorPane.getCaretPosition();
            selectedAttribute =
                ((HTMLDocument) chatEditorPane.getDocument())
                    .getCharacterElement(index - 1).getAttributes()
                    .getAttribute(styleConstant);
        }
        else
        {
            for (int index = chatEditorPane.getSelectionStart();
                index < chatEditorPane.getSelectionEnd(); index++)
            {
                AttributeSet attributes =
                    ((HTMLDocument) chatEditorPane.getDocument())
                        .getCharacterElement(index).getAttributes();

                if (attributes.getAttribute(styleConstant) != null)
                    selectedAttribute = attributes.getAttribute(styleConstant);
            }
        }

        if (selectedAttribute != null)
            colorLabel.setBackground((Color)selectedAttribute);
        else
            colorLabel.setBackground(Color.BLACK);
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

    private static class ColorLabel extends JLabel
    {
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
    
    /**
     * Shows the font chooser dialog
     */
    public void showFontChooserDialog()
    {
        FontChooser fontChooser = new FontChooser();
        
        fontChooser.setFontFamily(
            chatEditorPane.getFont().getFontName());
        fontChooser.setFontSize(
            chatEditorPane.getFont().getSize());

        fontChooser.setBoldStyle(boldButton.isSelected());
        fontChooser.setItalicStyle(italicButton.isSelected());
        fontChooser.setUnderlineStyle(underlineButton.isSelected());

        int result = fontChooser.showDialog(this.chatWritePanel);

        if (result == FontChooser.OK_OPTION)
        {
            // Font family and size
            setFontFamilyAndSize(fontChooser.getFontFamily(), fontChooser.getFontSize());

            // Font style
            setBoldStyleEnable(fontChooser.isBoldStyleSelected());
            setItalicStyleEnable(fontChooser.isItalicStyleSelected());
            setUnderlineStyleEnable(fontChooser.isUnderlineStyleSelected());

            // Font color
            setFontColor(fontChooser.getFontColor());
        }
        
        chatEditorPane.requestFocus();
    }
    
    /**
     * Sets the font family and size
     * @param family the family name
     * @param size the size
     */
    public void setFontFamilyAndSize(String family, int size)
    {
        // Family
        ActionEvent evt = new ActionEvent(chatEditorPane, ActionEvent.ACTION_PERFORMED, family);
        Action action = new StyledEditorKit.FontFamilyAction(family, family);
        action.actionPerformed(evt);
        
        // Size
        evt = new ActionEvent(chatEditorPane, ActionEvent.ACTION_PERFORMED, Integer.toString(size)); 
        action = new StyledEditorKit.FontSizeAction(Integer.toString(size), size);
        action.actionPerformed(evt);
    }
    
    /**
     * Enables the bold style
     * @param b TRUE enable - FALSE disable
     */
    public void setBoldStyleEnable(boolean b)
    {
        if ((b && !boldButton.isSelected()) || (!b && boldButton.isSelected()))
            boldButton.doClick();
    }
    
    /**
     * Enables the italic style
     * @param b TRUE enable - FALSE disable
     */
    public void setItalicStyleEnable(boolean b)
    {
        if ((b && !italicButton.isSelected()) || (!b && italicButton.isSelected()))
            italicButton.doClick();
    }
    
    /**
     * Enables the underline style
     * @param b TRUE enable - FALSE disable
     */
    public void setUnderlineStyleEnable(boolean b)
    {
        if ((b && !underlineButton.isSelected()) || (!b && underlineButton.isSelected()))
            underlineButton.doClick();
    }
    
    /**
     * Sets the font color
     * @param color the color
     */
    public void setFontColor(Color color)
    {
        colorLabel.setBackground(color);
        
        ActionEvent evt = new ActionEvent(chatEditorPane, ActionEvent.ACTION_PERFORMED, "");

        Action action =
            new HTMLEditorKit.ForegroundAction(Integer.toString(color.getRGB()), color);

        action.actionPerformed(evt);
    }

    /**
     * Returns the button used to show the list of smileys.
     * 
     * @return the button used to show the list of smileys.
     */
    public SmileysSelectorBox getSmileysSelectorBox()
    {
        return smileysBox;
    }

    /**
     * Returns TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE.
     * @return TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE
     */
    public boolean hasSelectedMenus()
    {
        return smileysBox.isMenuSelected();
    }
}
