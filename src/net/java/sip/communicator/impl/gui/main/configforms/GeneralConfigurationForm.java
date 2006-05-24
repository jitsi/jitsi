/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * @author Yana Stamcheva
 */
public class GeneralConfigurationForm extends JPanel implements
        ConfigurationForm {

    private JCheckBox launchOnStartUpCheck = new JCheckBox(Messages
            .getString("launchOnStartUp"));

    private JCheckBox alwaysOnTopCheck = new JCheckBox(Messages
            .getString("alwaysOnTop"));

    private JCheckBox updateAutomaticallyCheck = new JCheckBox(Messages
            .getString("updateAutomatically"));

    private JCheckBox enableNotificationsCheck = new JCheckBox(Messages
            .getString("enableNotifications"));

    private JCheckBox activateWhenMinimizedCheck = new JCheckBox(Messages
            .getString("activateOnlyWhenMinimized"));

    private JPanel appliBehaviourPanel = new JPanel(new GridLayout(0, 1));

    private JPanel updatesPanel = new JPanel(new GridLayout(0, 1));

    private JPanel notificationsPanel = new JPanel(new GridLayout(0, 1));

    public GeneralConfigurationForm() {

        super(new GridLayout(4, 1));

        this.setPreferredSize(new Dimension(400, 300));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.appliBehaviourPanel.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("application")));

        this.updatesPanel.setBorder(BorderFactory.createTitledBorder(Messages
                .getString("updates")));

        this.notificationsPanel.setBorder(BorderFactory
                .createTitledBorder(Messages.getString("notifications")));

        this.appliBehaviourPanel.add(launchOnStartUpCheck);
        this.appliBehaviourPanel.add(alwaysOnTopCheck);

        this.add(appliBehaviourPanel);

        this.updatesPanel.add(updateAutomaticallyCheck);

        this.add(updatesPanel);

        this.notificationsPanel.add(enableNotificationsCheck);
        this.notificationsPanel.add(activateWhenMinimizedCheck);

        this.add(notificationsPanel);
    }

    public String getTitle() {
        return Messages.getString("general");
    }

    public Icon getIcon() {
        return new ImageIcon(ImageLoader
                .getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON));
    }

    public Component getForm() {
        return this;
    }
}
