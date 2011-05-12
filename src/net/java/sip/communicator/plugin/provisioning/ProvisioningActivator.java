/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.provisioning;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator the provisioning system. It will gather provisioning URL depending
 * on the configuration (DHCP, manual, ...), retrieve configuration file and
 * push properties to the <tt>ConfigurationService</tt>.
 */
public class ProvisioningActivator
    implements BundleActivator
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProvisioningActivator.class);

    /**
     * The current BundleContext.
     */
    private static BundleContext bundleContext = null;

    /**
     * Name of the provisioning URL in the configuration service.
     */
    private static final String PROPERTY_PROVISIONING_URL
        = "net.java.sip.communicator.plugin.provisioning.URL";

    /**
     * Name of the provisioning username in the configuration service
     * authentication).
     */
    private static final String PROPERTY_PROVISIONING_USERNAME
        = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Name of the provisioning password in the configuration service (HTTP
     * authentication).
     */
    private static final String PROPERTY_PROVISIONING_PASSWORD
        = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the property that contains the provisioning method (i.e. DHCP,
     * DNS, manual, ...).
     */
    private static final String PROVISIONING_METHOD_PROP
        = "net.java.sip.communicator.plugin.provisioning.METHOD";

    /**
     * Name of the property that contains enforce prefix list (separated by
     * pipe) for the provisioning. The retrieved configuration properties will
     * be checked against these prefixes to avoid having incorrect content in
     * the configuration file (such as HTML content resulting of HTTP error).
     */
    private static final String PROVISIONING_ALLOW_PREFIX_PROP
        = "provisioning.ALLOW_PREFIX";

    /**
     * Name of the enforce prefix property.
     */
    private static final String PROVISIONING_ENFORCE_PREFIX_PROP
        = "provisioning.ENFORCE_PREFIX";

    /**
     * Name of the UUID property.
     */
    public static final String PROVISIONING_UUID_PROP
        = "net.java.sip.communicator.UUID";

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * A reference to the CredentialsStorageService implementation instance
     * that is registered with the bundle context.
     */
    private static CredentialsStorageService credentialsService = null;

    /**
     * A reference to the NetworkAddressManagerService implementation instance
     * that is registered with the bundle context.
     */
    private static NetworkAddressManagerService netaddrService = null;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * List of allowed configuration prefixes.
     */
    private List<String> allowedPrefixes = new ArrayList<String>();

    /**
     * Starts this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong during the start of the bundle
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        String url = null;

        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [STARTED]");

        ProvisioningActivator.bundleContext = bundleContext;

        String uuid = (String)getConfigurationService().getProperty(
                PROVISIONING_UUID_PROP);

        if(uuid == null || uuid.equals(""))
        {
            uuid = UUID.randomUUID().toString();
            getConfigurationService().setProperty(PROVISIONING_UUID_PROP, uuid);
        }

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.ADVANCED_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.provisioning.ProvisioningForm",
                getClass().getClassLoader(),
                "plugin.provisioning.PLUGIN_ICON",
                "plugin.provisioning.PROVISIONING",
                2000, true),
            properties);

        String method = getConfigurationService().getString(
                PROVISIONING_METHOD_PROP);

        if(method == null || method.equals("NONE"))
        {
            return;
        }

        ServiceReference serviceReferences[] = bundleContext.
            getServiceReferences(ProvisioningDiscoveryService.class.getName(),
                    null);

        /* search the provisioning discovery implementation that correspond to
         * the method name
         */
        if(serviceReferences != null)
        {
            for(ServiceReference ref : serviceReferences)
            {
                ProvisioningDiscoveryService provdisc =
                    (ProvisioningDiscoveryService)bundleContext.getService(ref);

                if(provdisc.getMethodName().equals(method))
                {
                    /* may block for sometime depending on the method used */
                    url = provdisc.discoverURL();
                    break;
                }
            }
        }

        if(url == null)
        {
            /* try to see if provisioning URL is stored in properties */
            url = getConfigurationService().getString(
                    PROPERTY_PROVISIONING_URL);
        }

        if(url != null)
        {
            File file = retrieveConfigurationFile(url);

            if(file != null)
            {
                updateConfiguration(file);

                /* store the provisioning URL in local configuration in case
                 * the provisioning discovery failed (DHCP/DNS unavailable, ...)
                 */
                getConfigurationService().setProperty(
                        PROPERTY_PROVISIONING_URL, url);
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [REGISTERED]");
    }

    /**
     * Stops this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong during the stop of the bundle
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        ProvisioningActivator.bundleContext = null;

        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [STOPPED]");
    }

    /**
     * Indicates if the provisioning has been enabled.
     *
     * @return <tt>true</tt> if the provisioning is enabled, <tt>false</tt> -
     * otherwise
     */
    public static String getProvisioningMethod()
    {
        String provMethod
            = getConfigurationService().getString(PROVISIONING_METHOD_PROP);

        if (provMethod == null || provMethod.length() <= 0)
        {
            provMethod = getResourceService().getSettingsString(
                "plugin.provisioning.DEFAULT_PROVISIONING_METHOD");

            if (provMethod != null && provMethod.length() > 0)
                setProvisioningMethod(provMethod);
        }

        return provMethod;
    }

    /**
     * Enables the provisioning with the given method. If the provisioningMethod
     * is null disables the provisioning.
     *
     * @param provisioningMethod the provisioning method
     */
    public static void setProvisioningMethod(String provisioningMethod)
    {
        getConfigurationService().setProperty(
            PROVISIONING_METHOD_PROP, provisioningMethod);
    }

    /**
     * Returns the provisioning URI.
     *
     * @return the provisioning URI
     */
    public static String getProvisioningUri()
    {
        return getConfigurationService().getString(PROPERTY_PROVISIONING_URL);
    }

    /**
     * Sets the provisioning URI.
     *
     * @param uri the provisioning URI to set
     */
    public static void setProvisioningUri(String uri)
    {
        getConfigurationService().setProperty(
            PROPERTY_PROVISIONING_URL, uri);
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference resourceReference
                = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

            resourceService =
                (ResourceManagementService) bundleContext
                    .getService(resourceReference);
        }

        return resourceService;
    }

    /**
     * Retrieve configuration file from provisioning URL.
     * This method is blocking until configuration file is retrieved from the
     * network or if an exception happen
     *
     * @param url provisioning URL
     * @return provisioning file downloaded
     */
    private File retrieveConfigurationFile(String url)
    {
        File tmpFile = null;

        try
        {
            String arg = null;
            String args[] = null;
            final File temp = File.createTempFile("provisioning",
                    ".properties");

            tmpFile = temp;

            URL u = new URL(url);
            InetAddress ipaddr = getNetworkAddressManagerService().
                getLocalHost(InetAddress.getByName(u.getHost()));

            if(url.indexOf("${uuid}") != -1)
            {
                url = url.replace("${uuid}", (String)getConfigurationService()
                        .getProperty(PROVISIONING_UUID_PROP));
            }

            if(url.indexOf("${osname}") != -1)
            {
                url = url.replace("${osname}", System.getProperty("os.name"));
            }

            if(url.indexOf("${build}") != -1)
            {
                url = url.replace("${build}",
                        System.getProperty("sip-communicator.version"));
            }

            if(url.indexOf("${ipaddr}") != -1)
            {
                url = url.replace("${ipaddr}", ipaddr.getHostAddress());
            }

            if(url.indexOf("${hwaddr}") != -1)
            {
                if(ipaddr != null)
                {
                    /* find the hardware address of the interface
                     * that has this IP address
                     */
                    Enumeration<NetworkInterface> en =
                        NetworkInterface.getNetworkInterfaces();

                    while(en.hasMoreElements())
                    {
                        NetworkInterface iface = en.nextElement();

                        Enumeration<InetAddress> enInet =
                            iface.getInetAddresses();

                        while(enInet.hasMoreElements())
                        {
                            InetAddress inet = enInet.nextElement();

                            if(inet.equals(ipaddr))
                            {
                                byte hw[] =
                                getNetworkAddressManagerService().
                                    getHardwareAddress(iface);
                                StringBuffer buf =
                                    new StringBuffer();

                                for(byte h : hw)
                                {
                                    int hi = h >= 0 ? h : h + 256;
                                    String t = new String(
                                            (hi <= 0xf) ? "0" : "");
                                    t += Integer.toHexString(hi);
                                    buf.append(t);
                                    buf.append(":");
                                }

                                buf.deleteCharAt(buf.length() - 1);

                                url = url.replace("${hwaddr}",
                                        buf.toString());

                                break;
                            }
                        }
                    }
                }
            }

            if(url.contains("?"))
            {
                /* do not handle URL of type http://domain/index.php? (no
                 * parameters)
                 */
                if((url.indexOf('?') + 1) != url.length())
                {
                    arg = url.substring(url.indexOf('?') + 1);
                    args = arg.split("&");
                }
                url = url.substring(0, url.indexOf('?'));
            }

            String[] paramNames = null;
            String[] paramValues = null;
            int usernameIx = -1;
            int passwordIx = -1;

            if(args != null && args.length > 0)
            {
                paramNames = new String[args.length];
                paramValues = new String[args.length];

                for(int i = 0; i < args.length; i++)
                {
                    String s = args[i];

                    if(s.startsWith("username="))
                    {
                        paramNames[i] = "username";
                        paramValues[i] = "";
                        usernameIx = i;
                    }
                    else if(s.startsWith("password="))
                    {
                        paramNames[i] = "password";
                        paramValues[i] = "";
                        passwordIx = i;
                    }
                    else if(s.startsWith("uuid="))
                    {
                        paramNames[i] = "uuid";
                        paramValues[i] = s.substring(s.indexOf("=") + 1);
                    }
                    else if(s.startsWith("osname="))
                    {
                        paramNames[i] = "osname";
                        paramValues[i] = s.substring(s.indexOf("=") + 1);
                    }
                    else if(s.startsWith("build="))
                    {
                        paramNames[i] = "build";
                        paramValues[i] = s.substring(s.indexOf("=") + 1);
                    }
                    else if(s.startsWith("ipaddr="))
                    {
                        paramNames[i] = "ipaddr";
                        paramValues[i] = s.substring(s.indexOf("=") + 1);
                    }
                    else if(s.startsWith("hwaddr="))
                    {
                        paramNames[i] = "hwaddr";
                        paramValues[i] = s.substring(s.indexOf("=") + 1);
                    }
                    else
                    {
                        paramNames[i] = "";
                        paramValues[i] = "";
                    }
                }
            }

            HttpUtils.HTTPResponseResult res =
                HttpUtils.postForm(
                    url,
                    PROPERTY_PROVISIONING_USERNAME,
                    PROPERTY_PROVISIONING_PASSWORD,
                    paramNames,
                    paramValues,
                    usernameIx,
                    passwordIx);

            // if there was an error in retrieving stop
            if(res == null)
                return null;

            InputStream in = res.getContent();

            // Chain a ProgressMonitorInputStream to the
            // URLConnection's InputStream
            final ProgressMonitorInputStream pin
                = new ProgressMonitorInputStream(null, u.toString(), in);

            // Set the maximum value of the ProgressMonitor
            ProgressMonitor pm = pin.getProgressMonitor();
            pm.setMaximum((int)res.getContentLength());

            final BufferedOutputStream bout
                = new BufferedOutputStream(new FileOutputStream(temp));

            try
            {
                int read = -1;
                byte[] buff = new byte[1024];

                while((read = pin.read(buff)) != -1)
                {
                    bout.write(buff, 0, read);
                }

                pin.close();
                bout.flush();
                bout.close();

                return temp;
            }
            catch (Exception e)
            {
                logger.error("Error saving", e);

                try
                {
                    pin.close();
                    bout.close();
                }
                catch (Exception e1)
                {
                }

                return null;
            }
        }
        catch (Exception e)
        {
            if (logger.isInfoEnabled())
                logger.info("Error retrieving provisioning file!", e);
            tmpFile.delete();
            return null;
        }
    }

    /**
     * Update configuration with properties retrieved from provisioning URL.
     *
     * @param file provisioning file
     */
    private void updateConfiguration(final File file)
    {
        Properties fileProps = new OrderedProperties();
        InputStream in = null;

        try
        {
            in = new BufferedInputStream(new FileInputStream(file));
            fileProps.load(in);

            Iterator<Map.Entry<Object, Object> > it
                = fileProps.entrySet().iterator();

            while(it.hasNext())
            {
                Map.Entry<Object, Object> entry = it.next();
                String key = (String)entry.getKey();
                Object value = entry.getValue();

                if(key.equals(PROVISIONING_ALLOW_PREFIX_PROP))
                {
                    String prefixes[] = ((String)value).split("\\|");

                    /* updates allowed prefixes list */
                    for(String s : prefixes)
                    {
                        allowedPrefixes.add(s);
                    }
                    continue;
                }
                else if(key.equals(PROVISIONING_ENFORCE_PREFIX_PROP))
                {
                    checkEnforcePrefix((String)value);
                    continue;
                }

                /* check that properties is allowed */
                if(!isPrefixAllowed(key))
                {
                    continue;
                }

                processProperty(key, value);
            }

            try
            {
                /* save and reload the "new" configuration */
                getConfigurationService().storeConfiguration();
                getConfigurationService().reloadConfiguration();
            }
            catch(Exception e)
            {
                logger.error("Cannot reload configuration");
            }
        }
        catch(IOException e)
        {
            logger.warn("Error during load of provisioning file");
        }
        finally
        {
            try
            {
                in.close();
                file.delete();
            }
            catch(IOException e)
            {
            }
        }
    }

    /**
     * Check if a property name belongs to the allowed prefixes.
     *
     * @param key property key name
     * @return true if key is allowed, false otherwise
     */
    private boolean isPrefixAllowed(String key)
    {
        if(allowedPrefixes.size() > 0)
        {
            for(String s : allowedPrefixes)
            {
                if(key.startsWith(s))
                {
                    return true;
                }
            }

            /* current property prefix is not allowed */
            return false;
        }
        else
        {
            /* no allowed prefixes configured so key is valid by default */
            return true;
        }
    }

    /**
     * Process a new property. If value equals "${null}", it means to remove the
     * property in the configuration service. If the key name end with
     * "PASSWORD", its value is encrypted through credentials storage service,
     * otherwise the property is added/updated in the configuration service.
     *
     * @param key property key name
     * @param value property value
     */
    private void processProperty(String key, Object value)
    {
        if((value instanceof String) && ((String)value).equals("${null}"))
        {
            getConfigurationService().removeProperty(key);
        }
        else if(key.endsWith(".PASSWORD"))
        {
            /* password => credentials storage service */
            getCredentialsStorageService().storePassword(
                    key.substring(0, key.lastIndexOf(".")),
                    (String)value);
        }
        else
        {
            getConfigurationService().setProperty(key, value);
        }
    }

    /**
     * Walk through all properties and make sure all properties keys match
     * a specific set of prefixes defined in configuration.
     *
     * @param enforcePrefix list of enforce prefix.
     */
    private void checkEnforcePrefix(String enforcePrefix)
    {
        ConfigurationService config = getConfigurationService();
        String prefixes[] = null;

        if(enforcePrefix == null)
        {
            return;
        }

        /* must escape the | character */
        prefixes = enforcePrefix.split("\\|");

        /* get all properties */
        for (String key : config.getAllPropertyNames())
        {
            boolean isValid = false;

            for(String k : prefixes)
            {
                if(key.startsWith(k))
                {
                    isValid = true;
                    break;
                }
            }

            /* property name does is not in the enforce prefix list
             * so remove it
             */
            if(!isValid)
            {
                config.removeProperty(key);
            }
        }
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService)bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        if (credentialsService == null)
        {
            ServiceReference credentialsReference
                = bundleContext.getServiceReference(
                    CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext
                                        .getService(credentialsReference);
        }
        return credentialsService;
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService.
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if (netaddrService == null)
        {
            ServiceReference netaddrReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            netaddrService
                = (NetworkAddressManagerService) bundleContext
                                        .getService(netaddrReference);
        }
        return netaddrService;
    }
}
