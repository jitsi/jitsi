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
package net.java.sip.communicator.plugin.generalconfig;

import java.io.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.win32.*;

import net.java.sip.communicator.util.*;

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
     * Process Status API. Used to obtain current running executable name.
     */
    public interface PSAPI extends StdCallLibrary
    {
        PSAPI INSTANCE = (PSAPI)Native.loadLibrary("psapi", PSAPI.class);
        int GetModuleFileNameExA (
            WinNT.HANDLE process,
            Pointer hModule,
            byte[] lpString,
            int nMaxCount);
    };

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
     * Returns the currently running process executable name with path.
     * @return the currently running process executable name with path.
     */
    public static String getModuleFilename()
    {
        byte[] exePathName = new byte[512];

        WinNT.HANDLE process = Kernel32.INSTANCE.GetCurrentProcess();

        int result = PSAPI.INSTANCE.GetModuleFileNameExA(
            process, new Pointer(0), exePathName, exePathName.length);
        return Native.toString(exePathName).substring(0, result);
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
              String executableFileName = null;
              String filePath = getModuleFilename();

              if(filePath != null && filePath.length() > 0)
              {
                  int ix = filePath.lastIndexOf(File.separatorChar);

                  if(ix > 0)
                      executableFileName = filePath.substring(ix + 1);
              }

              if(executableFileName == null)
              {
                  logger.warn("Missing information for process, " +
                      "shortcut will be created any way using defaults.");
                  // if missing we will use application name
                  // removing spaces,_,-
                  executableFileName = appName.replaceAll(" ", "")
                    .replaceAll("_", "").replaceAll("-","") + ".exe";
              }

              Advapi32Util.registrySetStringValue(
                      WinReg.HKEY_CURRENT_USER,
                      REGISTRY_STARTUP_KEY,
                      appName,
                      workingDirectory + File.separator + executableFileName);
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
