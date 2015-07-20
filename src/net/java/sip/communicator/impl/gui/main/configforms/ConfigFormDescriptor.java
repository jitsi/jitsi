/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ConfigFormDescriptor</tt> saves information about the
 * <tt>ConfigurationForm</tt>. When a <tt>ConfigurationForm</tt> is added in the
 * <tt>ConfigurationWindow</tt> we create the corresponding descriptor and load
 * all the data we need in order to show this configuration form.
 *
 * @author Yana Stamcheva
 */
public class ConfigFormDescriptor
{
    private final Logger logger = Logger.getLogger(ConfigFormDescriptor.class);

    private final ConfigurationForm configForm;

    private ImageIcon configFormIcon;

    private Component configFormPanel;

    private String configFormTitle;

    /**
     * Loads the given <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to load
     */
    public ConfigFormDescriptor(ConfigurationForm configForm)
    {
        this.configForm = configForm;

        byte[] icon = null;

        try
        {
            icon = configForm.getIcon();

            configFormTitle = configForm.getTitle();
        }
        catch (Exception e)
        {
            logger.error("Could not load configuration form.", e);
        }

        if(icon != null)
            configFormIcon = new ImageIcon(icon);
    }

    /**
     * Returns the icon of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the icon of the corresponding <tt>ConfigurationForm</tt>
     */
    public ImageIcon getConfigFormIcon()
    {
        return configFormIcon;
    }

    /**
     * Returns the form of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the form of the corresponding <tt>ConfigurationForm</tt>
     */
    public Component getConfigFormPanel()
    {
        if (configFormPanel == null)
        {
            Object form = configForm.getForm();
            if ((form instanceof Component) == false)
            {
                throw new ClassCastException("ConfigurationFrame :"
                    + form.getClass()
                    + " is not a class supported by this ui implementation");
            }
            configFormPanel = (Component) form;
        }
        return configFormPanel;
    }

    /**
     * Returns the title of the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the title of the corresponding <tt>ConfigurationForm</tt>
     */
    public String getConfigFormTitle()
    {
        return configFormTitle;
    }

    /**
     * Returns the corresponding <tt>ConfigurationForm</tt>.
     *
     * @return the corresponding <tt>ConfigurationForm</tt>
     */
    public ConfigurationForm getConfigForm()
    {
        return configForm;
    }
}
