/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

/**
 * @author Yana Stamcheva
 */
public class AppearanceConfigurationForm extends JPanel
    implements ConfigurationForm {

    private JCheckBox launchOnStartUpCheck 
        = new JCheckBox(Messages.getI18NString("launchOnStartUp").getText());

    private JPanel appliBehaviourPanel = new JPanel(new GridLayout(0, 1));

    public AppearanceConfigurationForm() {
        super(new BorderLayout());

        this.appliBehaviourPanel.setBorder(BorderFactory
                .createTitledBorder(
                    Messages.getI18NString("application").getText()));

        this.appliBehaviourPanel.add(launchOnStartUpCheck);

        this.add(appliBehaviourPanel, BorderLayout.NORTH);
    }

    public String getTitle() {
        return Messages.getI18NString("appearance").getText();
    }

    public byte[] getIcon() {
        return ImageLoader.getImageInBytes(ImageLoader.SEARCH_ICON);
    }

    public Object getForm() {
        return this;
    }

    public int getIndex()
    {
        return 2;
    }
}
