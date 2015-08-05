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

import com.sun.jna.*;

import net.java.sip.communicator.util.ServiceUtils;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import java.io.*;

/**
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class ConfigurationActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>ConfigurationActivator</tt> class
     * for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ConfigurationActivator.class);

    /**
     * The currently registered {@link ConfigurationService} instance.
     */
    private ConfigurationService cs;

    /**
     * Starts the configuration service
     *
     * @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        FileAccessService fas
            = ServiceUtils.getService(bundleContext, FileAccessService.class);

        if (fas != null)
        {
            File useDatabaseConfig;

            try
            {
                useDatabaseConfig
                    = fas.getPrivatePersistentFile(
                            ".usedatabaseconfig",
                            FileCategory.PROFILE);
            }
            catch (Exception ise)
            {

                // There is somewhat of a chicken-and-egg dependency between
                // FileConfigurationServiceImpl and ConfigurationServiceImpl:
                // FileConfigurationServiceImpl throws IllegalStateException if
                // certain System properties are not set,
                // ConfigurationServiceImpl will make sure that these properties
                //are set but it will do that later.
                // A SecurityException is thrown when the destination
                // is not writable or we do not have access to that folder
                useDatabaseConfig = null;
            }

            // BETA: if the marker file exists, use the database configuration
            if ((useDatabaseConfig != null) && useDatabaseConfig.exists())
            {
                logger.info("Using database configuration store.");
                this.cs = new JdbcConfigService(fas);
            }
        }

        if (this.cs == null)
            this.cs = LibJitsi.getConfigurationService();

        bundleContext.registerService(
                ConfigurationService.class.getName(),
                this.cs,
                null);

        fixPermissions(this.cs);
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
            CLibrary  libc = (CLibrary) Native.loadLibrary("c", CLibrary.class);

            libc.chmod(homeFolder.getAbsolutePath(), 0700);

            String fileName = cs.getConfigurationFilename();

            if(fileName != null)
            {
                File cf = new File(homeFolder, fileName);
                if(cf.exists())
                    libc.chmod(cf.getAbsolutePath(), 0600);
            }
        }
        catch(Throwable t)
        {
            logger.error(
                    "Error creating c lib instance for fixing file permissions",
                    t);

            if (t instanceof InterruptedException)
                Thread.currentThread().interrupt();
            else if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
    }

    /**
     * The JNA interface to the <tt>c</tt> library and the <tt>chmod</tt>
     * function we use to fix permissions of user files and folders.
     */
    public interface CLibrary
        extends Library
    {
        /**
         * Changes file permissions.
         *
         * @param path the path to the file or folder the permissions of which
         * are to be changed.
         * @param mode the mode operand
         * @return <tt>0</tt> upon successful completion; otherwise,
         * <tt>-1</tt>. If <tt>-1</tt> is returned, no change to the file mode
         * occurs.
         */
        public int chmod(String path, int mode);
    }
}
