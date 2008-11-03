/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import say.swing.*;

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

    private ChatWritePanel chatWritePanel;

    private JEditorPane chatEditorPane;

    private Action boldAction = new HTMLEditorKit.BoldAction();

    private Action italicAction = new HTMLEditorKit.ItalicAction();

    private Action underlineAction = new HTMLEditorKit.UnderlineAction();

    private SIPCommButton fontButton = new SIPCommButton(
        ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
        ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON),
        ImageLoader.getImage(ImageLoader.FONT_ICON),
        ImageLoader.getImage(ImageLoader.EDIT_TOOLBAR_BUTTON));

    private SmiliesSelectorBox smiliesBox;

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

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));

        this.initStyleToolbarButtons();

        this.addSeparator();

        this.add(fontButton);
        this.addSeparator();

        this.initColorLabel();

        this.addSeparator();

        this.smiliesBox = new SmiliesSelectorBox(
            ImageLoader.getDefaultSmiliesPack(), chatWritePanel);

        this.smiliesBox.setName("smiley");
        this.smiliesBox.setToolTipText(
            Messages.getI18NString("insertSmiley").getText() + " Ctrl-M");

        this.add(smiliesBox);

        logger.trace("[GUI] Editor Pane font name: "
            + chatEditorPane.getFont().getName());

        fontButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                JFontChooser fontChooser = new JFontChooser();
                fontChooser.setSelectedFontFamily(
                    chatEditorPane.getFont().getFontName());
                fontChooser.setSelectedFontSize(
                    chatEditorPane.getFont().getSize());

                if (boldButton.isSelected())
                    fontChooser.setSelectedFontStyle(Font.BOLD);
                else if (italicButton.isSelected())
                    fontChooser.setSelectedFontStyle(Font.ITALIC);

                int result = fontChooser.showDialog(chatWritePanel);

                chatEditorPane.requestFocus();

                if (result == JFontChooser.OK_OPTION)
                {
                    String fontName = fontChooser.getSelectedFontFamily();
                    int fontSize = fontChooser.getSelectedFontSize();
                    int fontStyle = fontChooser.getSelectedFontStyle();

                    ActionEvent fontNameActionEvent
                        = new ActionEvent(  chatEditorPane,
                                            ActionEvent.ACTION_PERFORMED,
                                            fontName);

                    Action action = new StyledEditorKit.FontFamilyAction(
                                fontName,
                                fontName);

                    action.actionPerformed(fontNameActionEvent);

                    ActionEvent fontSizeActionEvent
                        = new ActionEvent(  chatEditorPane,
                                            ActionEvent.ACTION_PERFORMED,
                                            new Integer(fontSize).toString());

                    action
                        = new StyledEditorKit.FontSizeAction(
                            new Integer(fontSize).toString(),
                            fontSize);

                    action.actionPerformed(fontSizeActionEvent);

                    if (fontStyle == Font.BOLD)
                    {
                        if (!boldButton.isSelected())
                            boldButton.doClick();

                        if (italicButton.isSelected())
                            italicButton.doClick();
                    }
                    else if (fontStyle == Font.ITALIC)
                    {
                        if (!italicButton.isSelected())
                            italicButton.doClick();

                        if (boldButton.isSelected())
                            boldButton.doClick();
                    }
                    else if (fontStyle == (Font.BOLD + Font.ITALIC))
                    {
                        if (!boldButton.isSelected())
                            boldButton.doClick();

                        if (!italicButton.isSelected())
                            italicButton.doClick();
                    }
                    else
                    {
                        if (boldButton.isSelected())
                            boldButton.doClick();

                        if (italicButton.isSelected())
                            italicButton.doClick();
                    }
                }
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

                if (newColor != null) {
                    colorLabel.setBackground(newColor);

                    ActionEvent evt =
                        new ActionEvent(chatEditorPane,
                            ActionEvent.ACTION_PERFORMED, "");

                    Action action =
                        new HTMLEditorKit.ForegroundAction(new Integer(newColor
                            .getRGB()).toString(), newColor);

                    action.actionPerformed(evt);
                }

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

        return ge.getAvailableFontFamilyNames(Locale.getDefault());
    }

    /**
     * 
     */
    private class ColorLabel extends JLabel
    {
        public void paintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(this.getBackground());

            g.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
    }

    /**
     * Returns the button used to show the list of smilies.
     * 
     * @return the button used to show the list of smilies.
     */
    public SmiliesSelectorBox getSmiliesSelectorBox()
    {
        return smiliesBox;
    }

    /**
     * Returns TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE.
     * @return TRUE if there are selected menus in this toolbar, otherwise
     * returns FALSE
     */
    public boolean hasSelectedMenus()
    {
        if(smiliesBox.isMenuSelected())
            return true;

        return false;
    }
}
