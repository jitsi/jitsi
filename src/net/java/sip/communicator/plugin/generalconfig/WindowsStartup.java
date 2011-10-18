/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.generalconfig;

import com.sun.jna.platform.win32.*;
import net.java.sip.communicator.util.*;

import java.io.*;

/**
 * Take care of application auto startup. Reading and writing the registry.
 *
 * @author Damian Minkov
 */
public class WindowsStartup
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(WindowsStartup.class);

    /**
     * The key under which startup keys are placed.
     */
    private static String REGISTRY_STARTUP_KEY =
            "Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    /**
     * Checks whether startup is enabled.
     * @param appName the application name.
     * @return is startup enabled.
     */
    public static boolean isStartupEnabled(String appName)
    {
        return Advapi32Util.registryValueExists(
                WinReg.HKEY_CURRENT_USER,
                REGISTRY_STARTUP_KEY,
                appName);
    }

    /**
     * Creates or deletes registry key for application autostart.
     *
     * @param appName the application name
     * @param workingDirectory the current working directory, normally the
     *  place where the application executable is placed.
     * @param isAutoStart <tt>true</tt> to create registry key, <tt>false</tt>
     *  to delete it.
     */
    public static void setAutostart(String appName,
                                    String workingDirectory,
                                    boolean isAutoStart)
    {
          if(isAutoStart)
          {
              Advapi32Util.registrySetStringValue(
                      WinReg.HKEY_CURRENT_USER,
                      REGISTRY_STARTUP_KEY,
                      appName,
                      workingDirectory + File.separator + "run.exe");
          }
          else
          {
              try
              {
                  Advapi32Util.registryDeleteValue(
                      WinReg.HKEY_CURRENT_USER,
                      REGISTRY_STARTUP_KEY,
                      appName);
              }
              catch(Throwable t)
              {
                  logger.warn("Cannot remove startup key or don't exist", t);
              }
          }
    }
}
