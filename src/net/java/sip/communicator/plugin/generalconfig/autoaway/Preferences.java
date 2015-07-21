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
package net.java.sip.communicator.plugin.generalconfig.autoaway;

import net.java.sip.communicator.plugin.generalconfig.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import java.beans.*;

/**
 * Preferences for the Status Update
 *
 * @author Thomas Hofer
 *
 */
public final class Preferences
{
    /**
     * Property indicating whether status change on away is enabled.
     */
    private static final String ENABLE
        = "net.java.sip.communicator.plugin.statusupdate.enable";

    /**
     * Property indicating the time in minutes to consider a pc in idle state.
     */
    private static final String TIMER
        = "net.java.sip.communicator.plugin.statusupdate.timer";

    /**
     * The default value to be displayed and to be considered
     * for {@link Preferences#TIMER}.
     */
    public static final int DEFAULT_TIMER = 15;

    /**
     * Whether change status on away is enabled.
     * @return whether change status on away is enabled.
     */
    static boolean isEnabled()
    {
        // if enabled start
        String enabledDefault
            = GeneralConfigPluginActivator.getResources().getSettingsString(
                    Preferences.ENABLE);

        return
            GeneralConfigPluginActivator
                .getConfigurationService()
                    .getBoolean(
                            Preferences.ENABLE,
                            Boolean.parseBoolean(enabledDefault));
    }

    /**
     * Returns the time in minutes to consider a pc in idle state.
     * @return  the time in minutes to consider a pc in idle state.
     */
    static int getTimer()
    {
        ConfigurationService cfg
            = GeneralConfigPluginActivator.getConfigurationService();
        ResourceManagementService resources
            = GeneralConfigPluginActivator.getResources();

        String enabledDefault = resources.getSettingsString(Preferences.ENABLE);

        String timerDefaultStr = resources.getSettingsString(Preferences.TIMER);
        int timerDefault = DEFAULT_TIMER;

        if (timerDefaultStr != null)
        {
            try
            {
                timerDefault = Integer.parseInt(timerDefaultStr);
            }
            catch (NumberFormatException nfe)
            {
            }
        }

        return
            cfg.getBoolean(
                    Preferences.ENABLE,
                    Boolean.parseBoolean(enabledDefault))
                ? cfg.getInt(Preferences.TIMER, timerDefault)
                : 0;
    }

    /**
     * Save data in the configuration file
     * @param enabled is enabled
     * @param timer the time value to save
     */
    static void saveData(boolean enabled, String timer)
    {
        ConfigurationService cfg
            = GeneralConfigPluginActivator.getConfigurationService();

        cfg.setProperty(Preferences.ENABLE, Boolean.toString(enabled));
        cfg.setProperty(Preferences.TIMER, timer);
    }

    /**
     * Adds listener to detect property changes.
     * @param listener the listener to notify.
     */
    static void addEnableChangeListener(PropertyChangeListener listener)
    {
        // listens for changes in configuration enable/disable
        GeneralConfigPluginActivator
            .getConfigurationService()
                .addPropertyChangeListener(
                        ENABLE,
                        listener);
    }

    /**
     * Adds listener to detect timer property changes.
     * @param listener the listener to notify.
     */
    static void addTimerChangeListener(PropertyChangeListener listener)
    {
        // listens for changes in configuration enable/disable
        GeneralConfigPluginActivator
            .getConfigurationService()
                .addPropertyChangeListener(
                        TIMER,
                        listener);
    }
}
