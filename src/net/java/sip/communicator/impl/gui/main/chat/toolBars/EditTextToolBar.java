/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.toolBars;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>EditTextToolBar</tt> is a <tt>JToolBar</tt> which contains buttons
 * for formatting a text, like make text in bold or italic, change the font,
 * etc. It contains only <tt>MsgToolbarButton</tt>s, which have a specific
 * background icon and rollover behaviour to differentiates them from normal
 * buttons.
 *  
 * @author Yana Stamcheva
 */
public class EditTextToolBar extends SIPCommToolBar {

    private SIPCommButton textBoldButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.TEXT_BOLD_BUTTON), ImageLoader
            .getImage(ImageLoader.TEXT_BOLD_ROLLOVER_BUTTON));

    private SIPCommButton textItalicButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.TEXT_ITALIC_BUTTON), ImageLoader
            .getImage(ImageLoader.TEXT_ITALIC_ROLLOVER_BUTTON));

    private SIPCommButton textUnderlinedButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.TEXT_UNDERLINED_BUTTON), ImageLoader
            .getImage(ImageLoader.TEXT_UNDERLINED_ROLLOVER_BUTTON));

    private SIPCommButton alignLeftButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.ALIGN_LEFT_BUTTON), ImageLoader
            .getImage(ImageLoader.ALIGN_LEFT_ROLLOVER_BUTTON));

    private SIPCommButton alignRightButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.ALIGN_RIGHT_BUTTON), ImageLoader
            .getImage(ImageLoader.ALIGN_RIGHT_ROLLOVER_BUTTON));

    private SIPCommButton alignCenterButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.ALIGN_CENTER_BUTTON), ImageLoader
            .getImage(ImageLoader.ALIGN_CENTER_ROLLOVER_BUTTON));

    private JComboBox fontSizeCombo = new JComboBox();

    private JComboBox fontNameCombo = new JComboBox();

    /**
     * Creates an instance and constructs the <tt>EditTextToolBar</tt>.
     */
    public EditTextToolBar() {

        this.setRollover(true);
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        this.fontSizeCombo.setPreferredSize(new Dimension(55, 21));

        this.add(textBoldButton);
        this.add(textItalicButton);
        this.add(textUnderlinedButton);

        this.addSeparator();

        this.add(fontNameCombo);
        this.add(fontSizeCombo);

        this.addSeparator();

        this.add(alignLeftButton);
        this.add(alignCenterButton);
        this.add(alignRightButton);
    }
}
