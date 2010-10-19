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
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>EditTextToolBar</tt> is a <tt>JToolBar</tt> which contains
 * buttons for formatting a text, like make text in bold or italic, change the
 * font, etc. It contains only <tt>MsgToolbarButton</tt>s, which have a
 * specific background icon and rollover behaviour to differentiates them from
 * normal buttons.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @author Adam Netocny
 */
public class EditTextToolBar
    extends TransparentPanel
    implements Skinnable
{
    private final ChatWritePanel chatWritePanel;

    private final JEditorPane chatEditorPane;

    private final SmileysSelectorBox smileysBox;

    private JToggleButton boldButton;

    private JToggleButton italicButton;

    private JToggleButton underlineButton;

    private ColorLabel colorLabel;

    private SIPCommButton fontButton;

    /**
     * Creates an instance and constructs the <tt>EditTextToolBar</tt>.
     * @param writePanel the panel containing the chat write area
     */
    public EditTextToolBar(ChatWritePanel writePanel)
    {
        this.chatWritePanel = writePanel;
        this.chatEditorPane = writePanel.getEditorPane();

        this.setOpaque(false);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.initStyleToolbarButtons();

        fontButton
            = new SIPCommButton(
                ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
                ImageLoader.getImage(ImageLoader.FONT_ICON));
        fontButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                showFontChooserDialog();
            }
        });
        this.add(fontButton);

        this.initColorLabel();

        this.smileysBox = new SmileysSelectorBox(chatWritePanel);

        this.smileysBox.setName("smiley");
        this.smileysBox.setToolTipText(GuiActivator.getResources()
            .getI18NString("service.gui.INSERT_SMILEY") + " Ctrl-M");

        SIPCommMenuBar smileyMenuBar = new SIPCommMenuBar();
        smileyMenuBar.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        smileyMenuBar.add(smileysBox);

        this.add(smileyMenuBar);
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
            @Override
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
            @Override
            public void keyTyped(KeyEvent e)
            {
                String chatWritePanelText = chatWritePanel.getText();
                if (chatWritePanelText != null &&
                        chatWritePanelText.length() > 0)
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
            @Override
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
        this.boldButton
                = initStyleToggleButton(
                    ImageLoader.getImage(ImageLoader.TEXT_BOLD_BUTTON),
                    new HTMLEditorKit.BoldAction(),
                    StyleConstants.Bold);
        this.italicButton
                = initStyleToggleButton(
                    ImageLoader.getImage(ImageLoader.TEXT_ITALIC_BUTTON),
                    new HTMLEditorKit.ItalicAction(),
                    StyleConstants.Italic);
        this.underlineButton
                = initStyleToggleButton(
                    ImageLoader.getImage(ImageLoader.TEXT_UNDERLINED_BUTTON),
                    new HTMLEditorKit.UnderlineAction(),
                    StyleConstants.Underline);

        /*
         * The update of the style toggle buttons used to be very slow because
         * each of it had to retrieve the same data on its own (most notably,
         * JEditorPane#getText()) and make almost the same checks. That is why
         * the update logic is performed for all of them, not individually i.e.
         * the data is retrieved once, the checks are made and all style toggle
         * buttons are updated.
         */
        final Object[] styleConstants
                = new Object[]
                        {
                            StyleConstants.Bold,
                            StyleConstants.Italic,
                            StyleConstants.Underline
                        };
        final JToggleButton[] buttons
                = new JToggleButton[]
                        {
                            boldButton,
                            italicButton,
                            underlineButton
                        };

        chatEditorPane.addCaretListener(new CaretListener()
        {
            public void caretUpdate(CaretEvent e)
            {
                selectStyleToggleButtons(styleConstants, buttons);
            }
        });
        chatEditorPane.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                if (chatEditorPane.getText().length() > 0)
                {
                    AttributeSet attributes
                        = ((HTMLEditorKit) chatEditorPane.getEditorKit())
                            .getInputAttributes();

                    for (int i = 0; i < buttons.length; i++)
                        buttons[i]
                            .setSelected(
                                attributes
                                    .containsAttribute(
                                        styleConstants[i],
                                        true));
                }
            }
        });
        chatEditorPane.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                selectStyleToggleButtons(styleConstants, buttons);
            }
        });

        this.addBindings();
    }

    /**
     * Initializes a toggle button.
     *
     * @param buttonImage the <tt>Image</tt> to be used as the icon of the new
     * button
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

        add(button);
        return button;
    }

    /**
     * Selects or deselects the given toggle buttons depending on the given
     * <tt>styleConstants</tt>.
     * 
     * @param styleConstants the style constants
     * @param buttons the buttons to select
     */
    private void selectStyleToggleButtons(
            Object[] styleConstants,
            JToggleButton[] buttons)
    {
        if (chatEditorPane.getText().length() < 1)
            return;

        if (chatEditorPane.getSelectedText() == null)
        {
            AttributeSet attributes
                = ((HTMLDocument) chatEditorPane.getDocument())
                    .getCharacterElement(chatEditorPane.getCaretPosition() - 1)
                        .getAttributes();

            for (int i = 0; i < buttons.length; i++)
                buttons[i]
                    .setSelected(
                        attributes.containsAttribute(styleConstants[i], true));
        }
        else
        {
            int selectionStart = chatEditorPane.getSelectionStart();
            int selectionEnd = chatEditorPane.getSelectionEnd();
            HTMLDocument htmlDocument
                = (HTMLDocument) chatEditorPane.getDocument();

            for (int buttonIndex = 0;
                    buttonIndex < buttons.length;
                    buttonIndex++)
            {
                boolean selected = false;
                Object styleConstant = styleConstants[buttonIndex];

                for (int selectionIndex = selectionStart;
                        selectionIndex < selectionEnd;
                        selectionIndex++)
                {
                    AttributeSet attributes
                        = htmlDocument
                            .getCharacterElement(selectionIndex)
                                .getAttributes();

                    selected
                        = attributes.containsAttribute(styleConstant, true);
                    if (selected)
                        break;
                }

                buttons[buttonIndex].setSelected(selected);
            }
        }
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
            setFontFamilyAndSize(   fontChooser.getFontFamily(),
                                    fontChooser.getFontSize());

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
        ActionEvent evt
            = new ActionEvent(  chatEditorPane,
                                ActionEvent.ACTION_PERFORMED,
                                family);
        Action action = new StyledEditorKit.FontFamilyAction(family, family);
        action.actionPerformed(evt);

        // Size
        evt = new ActionEvent(chatEditorPane,
            ActionEvent.ACTION_PERFORMED, Integer.toString(size));
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
        if ((b && !italicButton.isSelected())
                || (!b && italicButton.isSelected()))
            italicButton.doClick();
    }

    /**
     * Enables the underline style
     * @param b TRUE enable - FALSE disable
     */
    public void setUnderlineStyleEnable(boolean b)
    {
        if ((b && !underlineButton.isSelected())
                || (!b && underlineButton.isSelected()))
            underlineButton.doClick();
    }

    /**
     * Sets the font color
     * @param color the color
     */
    public void setFontColor(Color color)
    {
        colorLabel.setBackground(color);

        ActionEvent evt
            = new ActionEvent(chatEditorPane, ActionEvent.ACTION_PERFORMED, "");
        Action action
            = new HTMLEditorKit.ForegroundAction(
                    Integer.toString(color.getRGB()),
                    color);

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

    /**
     * Loads the skin.
     */
    public void loadSkin()
    {
        fontButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON));
        fontButton.setIconImage(
                ImageLoader.getImage(ImageLoader.FONT_ICON));

        ((SIPCommToggleButton)this.boldButton).setBgImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.boldButton).setBgRolloverImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.boldButton).setIconImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.TEXT_BOLD_BUTTON)).getImage());
        ((SIPCommToggleButton)this.boldButton).setPressedImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON_PRESSED)).getImage());

        ((SIPCommToggleButton)this.italicButton).setBgImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.italicButton).setBgRolloverImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.italicButton).setIconImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.TEXT_ITALIC_BUTTON)).getImage());
        ((SIPCommToggleButton)this.italicButton).setPressedImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON_PRESSED)).getImage());

        ((SIPCommToggleButton)this.underlineButton).setBgImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.underlineButton).setBgRolloverImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON)).getImage());
        ((SIPCommToggleButton)this.underlineButton).setIconImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.TEXT_UNDERLINED_BUTTON)).getImage());
        ((SIPCommToggleButton)this.underlineButton).setPressedImage(
                new ImageIcon(ImageLoader.getImage(
                ImageLoader.EDIT_TOOLBAR_BUTTON_PRESSED)).getImage());
    }
}
