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
package net.java.sip.communicator.plugin.msofficecomm;

import net.java.sip.communicator.util.Logger;

import com.sun.jna.platform.win32.*;

/**
 * Checks the registry keys used by 
 * Outlook.
 *
 * @author Hristo Terezov
 */
public class RegistryHandler
{
    /**
     * The logger.
     */
    private static Logger logger = Logger.getLogger(RegistryHandler.class);
    
    /**
     * The key under which the IM application is placed.
     */
    private static String REGISTRY_IM_APPLICATION_KEY 
        = "Software\\IM Providers";
    
    /**
     * The value under which the default IM application is placed.
     */
    private static String REGISTRY_DEFAULT_IM_APPLICATION_VALUE 
        = "DefaultIMApp";
    
    /**
     * The key for the outlook call integration.
     */
    private static String REGISTRY_CALL_INTEGRATION
        = "Software\\Microsoft\\Office\\Outlook\\Call Integration";
    
    /**
     * The value for the outlook call integration.
     */
    private static String REGISTRY_CALL_INTEGRATION_VALUE
        = "IMApplication";

    /**
     * The key for the outlook rtc application.
     */
    private static String REGISTRY_OFFICE11_RTC_APPLICATION
        = "Software\\Microsoft\\Office\\11.0\\Common\\PersonaMenu";
    
    /**
     * The key for the outlook rtc application.
     */
    private static String REGISTRY_OFFICE12_RTC_APPLICATION
    = "Software\\Microsoft\\Office\\12.0\\Common\\PersonaMenu";
    
    /**
     * The value for the rtc application.
     */
    private static String REGISTRY_RTC_APPLICATION_VALUE = "RTCApplication";
    
    /**
     * The key for Communicator IM App.
     */
    private static String REGISTRY_COMMUNICATOR_UP 
        = "Software\\IM Providers\\Communicator";
    
    /**
     * Up and running value.
     */
    private static String REGISTRY_UP_RUNNING_VALUE = "UpAndRunning";
    
    /**
     * Checks the existence of the registry keys for outlook call integration.
     */
    private static void checkCallIntegration()
    {
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, 
            REGISTRY_CALL_INTEGRATION)
            || !Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, 
            REGISTRY_CALL_INTEGRATION, 
            REGISTRY_CALL_INTEGRATION_VALUE))
        {
            logger.error(REGISTRY_CALL_INTEGRATION + 
                " doesn't exists in registry");
            return;
        }
        logger.info("Call integration: " +
            Advapi32Util.registryGetStringValue(
                WinReg.HKEY_LOCAL_MACHINE,
                REGISTRY_CALL_INTEGRATION,
                REGISTRY_CALL_INTEGRATION_VALUE));
    }
    
    /**
     * Checks the existence of the registry keys for outlook rtc application.
     */
    private static void checkRTCApplication()
    {
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_OFFICE11_RTC_APPLICATION)
            || !Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_OFFICE11_RTC_APPLICATION, 
            REGISTRY_RTC_APPLICATION_VALUE))
        {
            logger.error(REGISTRY_OFFICE11_RTC_APPLICATION + 
                " doesn't exists in registry");
        }
        else
        {
            logger.info("RTC application: " +
                Advapi32Util.registryGetIntValue(
                    WinReg.HKEY_CURRENT_USER,
                    REGISTRY_OFFICE11_RTC_APPLICATION,
                    REGISTRY_RTC_APPLICATION_VALUE));
        }
        
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_OFFICE12_RTC_APPLICATION)
            || !Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_OFFICE12_RTC_APPLICATION, 
            REGISTRY_RTC_APPLICATION_VALUE))
        {
            logger.error(REGISTRY_OFFICE12_RTC_APPLICATION + 
                " doesn't exists in registry");
        }
        else
        {
            logger.info("RTC application: " +
                Advapi32Util.registryGetIntValue(
                    WinReg.HKEY_CURRENT_USER,
                    REGISTRY_OFFICE12_RTC_APPLICATION,
                    REGISTRY_RTC_APPLICATION_VALUE));
        }
    }
    
    /**
     * Checks if the registry key for running Jitsi and Communicator.
     */
    private static void checkUpAndRunning()
    {
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_COMMUNICATOR_UP)
            || !Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_COMMUNICATOR_UP, 
            REGISTRY_UP_RUNNING_VALUE))
        {
            logger.error(REGISTRY_COMMUNICATOR_UP + 
                " doesn't exists in registry");
        }
        else
        {
            logger.info("Communicator up and running value: " + 
                Advapi32Util.registryGetIntValue(
                    WinReg.HKEY_CURRENT_USER,
                    REGISTRY_COMMUNICATOR_UP,
                    REGISTRY_UP_RUNNING_VALUE));
        }
        
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_IM_APPLICATION_KEY + "\\" + getApplicationName())
            || !Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_IM_APPLICATION_KEY + "\\" + getApplicationName(), 
            REGISTRY_UP_RUNNING_VALUE))
        {
            logger.error(REGISTRY_IM_APPLICATION_KEY + 
                " doesn't exists in registry");
        }
        else
        {
            logger.info("Up and running value: " + 
                Advapi32Util.registryGetIntValue(
                    WinReg.HKEY_CURRENT_USER,
                    REGISTRY_IM_APPLICATION_KEY + "\\" 
                        + getApplicationName(),
                    REGISTRY_UP_RUNNING_VALUE));
        }
    }
    
    /**
     * Logs registry information.
     */
    public static void checkRegistryKeys()
    {
        checkDefaultIMApp();
        checkRegisteredIMApp();
        checkCallIntegration();
        checkRTCApplication();
        checkUpAndRunning();
    }
    
    
    /**
     * Checks whether Jitsi is the default IM application.
     */
    private static void checkDefaultIMApp()
    {
        if(!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_IM_APPLICATION_KEY) || 
            !Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_IM_APPLICATION_KEY, 
            REGISTRY_DEFAULT_IM_APPLICATION_VALUE))
        {
            logger.error(REGISTRY_IM_APPLICATION_KEY 
                + " doesn't extsts");
            return;
        }

        logger.info("Default IM App: " + Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_IM_APPLICATION_KEY,
                REGISTRY_DEFAULT_IM_APPLICATION_VALUE));
    }

    /**
     * Checks whether Jitsi is registered as IM provider.
     */
    private static void checkRegisteredIMApp()
    {
        if(!Advapi32Util.registryKeyExists(
            WinReg.HKEY_LOCAL_MACHINE,
            REGISTRY_IM_APPLICATION_KEY + "\\" + getApplicationName()))
        {
            logger.error(REGISTRY_IM_APPLICATION_KEY  + "\\" + 
                getApplicationName() + " doesn;t exsts");
            return;
        }
        
        logger.info("Registered IM App friendly name: " 
            + Advapi32Util.registryGetStringValue(
                WinReg.HKEY_LOCAL_MACHINE,
                REGISTRY_IM_APPLICATION_KEY + "\\" + 
                getApplicationName(),
                "FriendlyName"));
    }
    
    /**
     * Returns the application name.
     * @return the application name
     */
    private static String getApplicationName()
    {
        return MsOfficeCommActivator.getResources().getSettingsString(
            "service.gui.APPLICATION_NAME");
    }
}
