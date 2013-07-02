/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import com.sun.jna.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import java.io.*;

/**
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
     * The <tt>BundleContext</tt> in which the configuration bundle has been
     * started and has not been stopped yet.
     */
    private static BundleContext bundleContext;

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
        ConfigurationActivator.bundleContext = bundleContext;

        ConfigurationService configurationService
            = LibJitsi.getConfigurationService();

        if (configurationService != null)
        {
            bundleContext.registerService(
                    ConfigurationService.class.getName(),
                    configurationService,
                    null);

            fixPermissions(configurationService);
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
    }

    /**
     * Gets the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet.
     *
     * @return the <tt>BundleContext</tt> in which the configuration bundle has
     * been started and has not been stopped yet
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Makes home folder and the configuration file readable and writable
     * only to the owner.
     * @param configurationService the config service instance to check
     *                             for home folder and name.
     */
    private static void fixPermissions(
        ConfigurationService configurationService)
    {
        if(!OSUtils.IS_LINUX && !OSUtils.IS_MAC)
            return;

        try
        {
            // let's check config file and config folder
            File homeFolder = new File(
                configurationService.getScHomeDirLocation(),
                configurationService.getScHomeDirName());

            CLibrary  libc = (CLibrary) Native.loadLibrary("c", CLibrary.class);
            libc.chmod(homeFolder.getAbsolutePath(), 0700);

            String fileName = configurationService.getConfigurationFilename();

            if(fileName != null)
            {
                File cf = new File(homeFolder, fileName);
                if(cf.exists())
                {
                    libc.chmod(cf.getAbsolutePath(), 0600);
                }
            }
        }
        catch(Throwable t)
        {
            logger.error(
                "Error creating c lib instance for fixing file permissions", t);
        }
    }

    /**
     * The jna interface to the c library and the chmod we use
     * to fix permissions of user files.
     */
    public interface CLibrary
        extends Library
    {
        /**
         * Changes file permissions.
         * @param path the path to file or folder which permissions we will
         *             change.
         * @param mode the mode operand
         * @return 0 shall be returned upon successful completion;
         *         otherwise, -1 shall be returned. If -1 is returned,
         *         no change to the file mode occurs.
         */
        public int chmod(String path, int mode);
    }
}
