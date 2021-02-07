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
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import lombok.extern.slf4j.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
@Slf4j
public class ConfigurationActivator
    extends DependentActivator
{
    /** Property name to force a properties file based configuration. */
    public static final String PNAME_USE_PROPFILE_CONFIG =
        "net.java.sip.communicator.impl.configuration.USE_PROPFILE_CONFIG";

    /**
     * The currently registered {@link ConfigurationService} instance.
     */
    private ConfigurationService cs;

    public ConfigurationActivator()
    {
        super(LibJitsi.class);
    }

    @Override
    public void startWithServices(BundleContext context) throws Exception
    {
        FileAccessService fas = LibJitsi.getFileAccessService();
        context.registerService(FileAccessService.class, fas, null);
        if (usePropFileConfigService(fas))
        {
            logger.info("Using properties file configuration store.");
            this.cs = LibJitsi.getConfigurationService();
        }
        else
        {
            this.cs = new JdbcConfigService(fas);
        }

        ConfigurationUtils.configService = this.cs;
        ConfigurationUtils.loadGuiConfigurations();
        context.registerService(ConfigurationService.class, this.cs, null);
        fixPermissions(this.cs);
    }

    private boolean usePropFileConfigService(FileAccessService fas)
    {
        if (Boolean.getBoolean(PNAME_USE_PROPFILE_CONFIG))
        {
            return true;
        }

        if (fas == null)
        {
            return true;
        }

        try
        {
            return fas.getPrivatePersistentFile(
                    ".usepropfileconfig",
                    FileCategory.PROFILE
                ).exists();
        }
        catch (Exception ise)
        {
            // There is somewhat of a chicken-and-egg dependency between
            // FileConfigurationServiceImpl and ConfigurationServiceImpl:
            // FileConfigurationServiceImpl throws IllegalStateException if
            // certain System properties are not set,
            // ConfigurationServiceImpl will make sure that these properties
            // are set but it will do that later.
            // A SecurityException is thrown when the destination
            // is not writable or we do not have access to that folder
            return true;
        }
    }

    /**
     * Causes the configuration service to store the properties object and
     * unregisters the configuration service.
     *
     * @param bundleContext <tt>BundleContext</tt>
     * @throws Exception if anything goes wrong while storing the properties
     * managed by the <tt>ConfigurationService</tt> implementation provided by
     * this bundle and while unregistering the service in question
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        this.cs.storeConfiguration();
        this.cs = null;
    }

    /**
     * Makes home folder and the configuration file readable and writable only
     * to the owner.
     *
     * @param cs the <tt>ConfigurationService</tt> instance to check for home
     * folder and configuration file.
     */
    private static void fixPermissions(ConfigurationService cs)
    {
        if(!OSUtils.IS_LINUX && !OSUtils.IS_MAC)
            return;

        try
        {
            // let's check config file and config folder
            File homeFolder
                = new File(cs.getScHomeDirLocation(), cs.getScHomeDirName());
            Set<PosixFilePermission> perms =
                new HashSet<PosixFilePermission>()
                {{
                    add(PosixFilePermission.OWNER_READ);
                    add(PosixFilePermission.OWNER_WRITE);
                    add(PosixFilePermission.OWNER_EXECUTE);
                }};
                Files.setPosixFilePermissions(
                    Paths.get(homeFolder.getAbsolutePath()), perms);

            String fileName = cs.getConfigurationFilename();
            if(fileName != null)
            {
                File cf = new File(homeFolder, fileName);
                if(cf.exists())
                {
                    perms = new HashSet<PosixFilePermission>()
                        {{
                            add(PosixFilePermission.OWNER_READ);
                            add(PosixFilePermission.OWNER_WRITE);
                        }};
                    Files.setPosixFilePermissions(
                        Paths.get(cf.getAbsolutePath()), perms);
                }
            }
        }
        catch(IOException t)
        {
            logger.error("Error while fixing config file permissions", t);
        }
    }
}
