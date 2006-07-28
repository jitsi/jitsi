/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.gui.ConfigurationForm;

/**
 * @author Yana Stamcheva
 */
public class AppearanceConfigurationForm extends JPanel
    implements ConfigurationForm {

    private JCheckBox launchOnStartUpCheck 
        = new JCheckBox(Messages.getString("launchOnStartUp"));

    private JPanel appliBehaviourPanel = new JPanel(new GridLayout(0, 1));

    public AppearanceConfigurationForm() {
        super(new BorderLayout());

        this.appliBehaviourPanel.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("application")));

        this.appliBehaviourPanel.add(launchOnStartUpCheck);

        this.add(appliBehaviourPanel, BorderLayout.NORTH);
    }

    public String getTitle() {
        return Messages.getString("appearance");
    }

    public byte[] getIcon() {
        return ImageLoader.getImageInBytes(ImageLoader.QUICK_MENU_SEARCH_ICON);
    }

    public Object getForm() {
        return this;
    }
}
