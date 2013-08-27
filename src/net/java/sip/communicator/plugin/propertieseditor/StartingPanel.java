/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;

/**
 * The <tt>StartingPanel</tt> wraps the <tt>WarningPanel</tt> and
 * <tt>PropertiesEditorPanel</tt>. Its main purpose is to choose which one of
 * the panels to set visible.
 * 
 * @author Marin Dzhigarov
 */
public class StartingPanel
    extends TransparentPanel
{
    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    private ConfigurationService confService = PropertiesEditorActivator
        .getConfigurationService();

    /*
     * The WarningPanel
     */
    WarningPanel warningPanel = new WarningPanel(this);

    /**
     * The PropertiesEditorPanel
     */
    PropertiesEditorPanel propertiesEditorPanel = new PropertiesEditorPanel();

    /**
     * Creates an instance <tt>StartingPanel</tt>
     */
    public StartingPanel()
    {
        super(new BorderLayout());
        boolean showWarning =
            confService.getBoolean(Constants.SHOW_WARNING_MSG_PROP, true);

        if (showWarning)
        {
            add(warningPanel, BorderLayout.CENTER);
        }
        else
        {
            add(propertiesEditorPanel, BorderLayout.CENTER);
        }
    }
}
