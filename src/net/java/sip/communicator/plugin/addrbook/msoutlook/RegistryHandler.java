/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

import com.sun.jna.platform.win32.*;

import net.java.sip.communicator.plugin.addrbook.*;

/**
 * Reading and writing the registry keys used by 
 * Outlook 2010 and higher.
 *
 * @author Hristo Terezov
 */
public class RegistryHandler
{
    /**
     * The key under which the default IM application is placed.
     */
    private static String REGISTRY_DEFAULT_IM_APPLICATION_KEY 
        = "Software\\IM Providers";

    /**
     * The value under which the default IM application is placed.
     */
    private static String REGISTRY_DEFAULT_IM_APPLICATION_VALUE 
        = "DefaultIMApp";

    /**
     * Default IM application for communicator. This value is used to unset 
     * Jitsi as default application.
     */
    private static String REGISTRY_DEFAULT_IM_APPLICATION_COMMUNICATOR 
        = "Communicator";
    
    /**
     * The key under which the default IM application is placed.
     */
    private static String REGISTRY_DEFAULT_MAIL_CLIENT_KEY 
        = "Software\\Clients\\Mail";

    /**
     * Checks whether given application is the default IM application or not.
     * @param appName the application name.
     * @return is the default IM application or not.
     */
    public static boolean isDefaultIMApp(String appName)
    {
        return Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_DEFAULT_IM_APPLICATION_KEY,
                REGISTRY_DEFAULT_IM_APPLICATION_VALUE).equals(appName);
    }

    /**
     * Checks whether Jitsi is the default IM application.
     * @return is Jitsi the default IM application or not.
     */
    public static boolean isJitsiDefaultIMApp()
    {
        if(!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, 
            REGISTRY_DEFAULT_IM_APPLICATION_KEY, 
            REGISTRY_DEFAULT_IM_APPLICATION_VALUE))
            return false;

        return Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_DEFAULT_IM_APPLICATION_KEY,
                REGISTRY_DEFAULT_IM_APPLICATION_VALUE).equals(
                    getApplicationName());
    }

    /**
     * Sets given application as default IM application
     *
     * @param appName the application name
     */
    public static void setDefaultIMApp(String appName)
    {

              Advapi32Util.registrySetStringValue(
                      WinReg.HKEY_CURRENT_USER,
                      REGISTRY_DEFAULT_IM_APPLICATION_KEY,
                      REGISTRY_DEFAULT_IM_APPLICATION_VALUE,
                      appName);
    }
    
    /**
     * Sets Outlook as default mail client.
     */
    public static void setOutlookAsDefaultMailClient()
    {

              Advapi32Util.registrySetStringValue(
                      WinReg.HKEY_CURRENT_USER,
                      REGISTRY_DEFAULT_MAIL_CLIENT_KEY,
                      null,
                      "Microsoft Outlook");
    }

    /**
     * Sets Jitsi as default IM application.
     */
    public static void setJitsiAsDefaultApp()
    {
        String appName = getApplicationName();
        if(!isDefaultIMApp(appName))
            setDefaultIMApp(appName);
    }

    /**
     * Unsets Jitsi as default IM application. Overrides the registry value
     * with setting communicator as default IM application.
     */
    public static void unsetDefaultApp()
    {
        if(isDefaultIMApp(getApplicationName()))
            setDefaultIMApp(REGISTRY_DEFAULT_IM_APPLICATION_COMMUNICATOR);
    }

    /**
     * Returns the application name.
     * @return the application name
     */
    private static String getApplicationName()
    {
        return AddrBookActivator.getResources().getSettingsString(
            "service.gui.APPLICATION_NAME");
    }
}
