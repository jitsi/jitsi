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
public class GeneralConfigurationForm extends JPanel implements
        ConfigurationForm {

    private JCheckBox launchOnStartUpCheck = new JCheckBox(Messages
            .getI18NString("launchOnStartUp").getText());

    private JCheckBox alwaysOnTopCheck = new JCheckBox(Messages
            .getI18NString("alwaysOnTop").getText());

    private JCheckBox updateAutomaticallyCheck = new JCheckBox(Messages
            .getI18NString("updateAutomatically").getText());

    private JCheckBox enableNotificationsCheck = new JCheckBox(Messages
            .getI18NString("enableNotifications").getText());

    private JCheckBox activateWhenMinimizedCheck = new JCheckBox(Messages
            .getI18NString("activateOnlyWhenMinimized").getText());

    private JPanel appliBehaviourPanel = new JPanel(new GridLayout(0, 1));

    private JPanel updatesPanel = new JPanel(new GridLayout(0, 1));

    private JPanel notificationsPanel = new JPanel(new GridLayout(0, 1));

    public GeneralConfigurationForm() {

        super(new GridLayout(4, 1));

        this.setPreferredSize(new Dimension(400, 300));

        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.appliBehaviourPanel.setBorder(BorderFactory.createTitledBorder(
            Messages.getI18NString("application").getText()));

        this.updatesPanel.setBorder(BorderFactory.createTitledBorder(
            Messages.getI18NString("updates").getText()));

        this.notificationsPanel.setBorder(BorderFactory.createTitledBorder(
            Messages.getI18NString("notifications").getText()));

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
        return Messages.getI18NString("general").getText();
    }

    public byte[] getIcon() {
        return ImageLoader.getImageInBytes(
                ImageLoader.QUICK_MENU_CONFIGURE_ICON);
    }

    public Object getForm() {
        return this;
    }
}
