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

import javax.net.ssl.*;
import javax.swing.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
     * The service we use to interact with user for SSL certificate stuff.
     */
    private static CertificateVerificationService certVerification = null;

    /**
     * A reference to the NetworkAddressManagerService implementation instance
     * that is registered with the bundle context.
     */
    private static NetworkAddressManagerService netaddrService = null;

    /**
     * User credentials to access URL (protected by HTTP authentication and/or
     * by provisioning) if any.
     */
    private static UserCredentials userCredentials = null;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * HTTP method to request a page.
     */
    private String method = "POST";

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
System.out.println("PROVISIONING METHOD======" + provMethod);
        if (provMethod == null || provMethod.length() <= 0)
        {
            provMethod = getResourceService().getSettingsString(
                "plugin.provisioning.DEFAULT_PROVISIONING_METHOD");
System.out.println("PROVISIONING METHOD22222======" + provMethod);
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
     * Configure HTTP connection to provide HTTP authentication and SSL
     * truster.
     *
     * @param url provisioning URL
     * @param connection the <tt>URLConnection</tt>
     */
    private void configureHTTPConnection(URL url, HttpURLConnection connection)
    {
        try
        {
            connection.setRequestMethod(method);

            if(method.equals("POST"))
            {
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
            }

            if(connection instanceof HttpsURLConnection)
            {
                CertificateVerificationService vs =
                    getCertificateVerificationService();

                int port = url.getPort();

                /* if we do not specify port in the URL (http://domain.org:port)
                 * we have to set up the default port of HTTP (80) or
                 * HTTPS (443).
                 */
                if(port == -1)
                {
                    if(url.getProtocol().equals("http"))
                    {
                        port = 80;
                    }
                    else if(url.getProtocol().equals("https"))
                    {
                        port = 443;
                    }
                }

                ((HttpsURLConnection)connection).setSSLSocketFactory(
                        vs.getSSLContext(
                        url.getHost(), port).getSocketFactory());

                HostnameVerifier hv = new HostnameVerifier()
                {
                    public boolean verify(String urlHostName,
                            SSLSession session)
                    {
                        logger.warn("Warning: URL Host: " + urlHostName +
                                " vs. " + session.getPeerHost());
                        return true;
                    }
                };

                ((HttpsURLConnection)connection).setHostnameVerifier(hv);
            }
        }
        catch (Exception e)
        {
            logger.warn("Failed to initialize secure connection", e);
        }

        Authenticator.setDefault(new Authenticator()
        {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                // if there is something save return it
                ConfigurationService config = getConfigurationService();
                CredentialsStorageService credStorage =
                    getCredentialsStorageService();

                String uName
                    = (String) config.getProperty(
                            PROPERTY_PROVISIONING_USERNAME);

                if(uName != null)
                {
                    String pass = credStorage.loadPassword(
                            PROPERTY_PROVISIONING_PASSWORD);

                    if(pass != null)
                        return new PasswordAuthentication(uName,
                            pass.toCharArray());
                }

                if(userCredentials != null)
                {
                    return new PasswordAuthentication(
                        userCredentials.getUserName(),
                        userCredentials.getPassword());
                }
                else
                {
                    return null;
                }
            }
        });
    }

    /**
     * Handle authentication with the provisioning server.
     */
    private void handleProvisioningAuth()
    {
        ConfigurationService configService = getConfigurationService();
        CredentialsStorageService credService =
            getCredentialsStorageService();

        String username = configService.getString(
                PROPERTY_PROVISIONING_USERNAME);
        String password = credService.loadPassword(
                PROPERTY_PROVISIONING_PASSWORD);

        if(username != null && password != null)
        {
            /* we have already the credentials stored so return them */
            userCredentials = new UserCredentials();
            userCredentials.setUserName(username);
            userCredentials.setPassword(password.toCharArray());
            userCredentials.setPasswordPersistent(true);
            return;
        }

        AuthenticationWindow authWindow = new AuthenticationWindow(
                "provisioning", true, null);

        authWindow.setVisible(true);

        if(!authWindow.isCanceled())
        {
            userCredentials = new UserCredentials();
            userCredentials.setUserName(authWindow.getUserName());
            userCredentials.setPassword(authWindow.getPassword());
            userCredentials.setPasswordPersistent(
                authWindow.isRememberPassword());

            if(userCredentials.getUserName() == null)
            {
                userCredentials = null;
            }
        }
        else
        {
            userCredentials = null;
        }
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

            URL u = new URL(url);
            URLConnection uc = u.openConnection();
            OutputStreamWriter out = null;

            if(uc instanceof HttpURLConnection)
            {
                configureHTTPConnection(u, (HttpURLConnection)uc);
                ((HttpURLConnection)uc).setInstanceFollowRedirects(false);
                uc.setDoInput(true);
                uc.setDoOutput(true);
                out = new OutputStreamWriter(uc.getOutputStream());

                /* send out (via GET or POST) */
                StringBuffer content = new StringBuffer();
                InetAddress ipaddr = getNetworkAddressManagerService().
                        getLocalHost(InetAddress.getByName(u.getHost()));

                if(args != null && args.length > 0)
                {
                    for(String s : args)
                    {
                        if(s.equals("username=$username") ||
                                s.equals("username"))
                        {
                            if(userCredentials == null)
                            {
                                handleProvisioningAuth();
                            }

                            content.append("username=" +
                                    URLEncoder.encode(
                                            userCredentials.getUserName(),
                                            "UTF-8"));
                        }
                        else if(s.equals("password=$password") ||
                                s.equals("password"))
                        {
                            if(userCredentials == null)
                            {
                                handleProvisioningAuth();
                            }

                            content.append("password=" +
                                    URLEncoder.encode(
                                            userCredentials.
                                                    getPasswordAsString(),
                                                    "UTF-8"));
                        }
                        else if(s.equals("osname=$osname") ||
                                s.equals("osname"))
                        {
                            content.append("osname=" + URLEncoder.encode(
                                    System.getProperty("os.name"), "UTF-8"));
                        }
                        else if(s.equals("build=$build") ||
                                s.equals("build"))
                        {
                            content.append("build=" + URLEncoder.encode(
                                System.getProperty("sip-communicator.version"),
                                "UTF-8"));
                        }
                        else if(s.equals("ipaddr=$ipaddr") ||
                                s.equals("ipaddr"))
                        {
                            content.append("ipaddr=" + URLEncoder.encode(
                                    ipaddr.getHostAddress(), "UTF-8"));
                        }
                        else if(s.equals("hwaddr=$hwaddr") ||
                                s.equals("hwaddr"))
                        {
                            String hwaddr = null;

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

                                            hwaddr = buf.toString();
                                            content.append("hwaddr=" +
                                                    URLEncoder.encode(
                                                    hwaddr, "UTF-8"));
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        content.append("&");
                    }
                }

                out.write(content.toString());
                out.flush();

                int responseCode = ((HttpURLConnection)uc).getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_UNAUTHORIZED)
                {
                    /* remove stored username and password if authorization
                     * failed
                     */
                    getConfigurationService().removeProperty(
                            PROPERTY_PROVISIONING_USERNAME);
                    getCredentialsStorageService().removePassword(
                            PROPERTY_PROVISIONING_PASSWORD);
                    AuthenticationWindow authWindow = new AuthenticationWindow(
                            u.getHost(), true, null);

                    authWindow.setVisible(true);

                    userCredentials = new UserCredentials();
                    userCredentials.setUserName(authWindow.getUserName());
                    userCredentials.setPassword(authWindow.getPassword());
                    userCredentials.setPasswordPersistent(
                            authWindow.isRememberPassword());

                    if(userCredentials.getUserName() == null)
                    {
                        userCredentials = null;
                    }
                    else
                    {
                        tmpFile.delete();
                        return retrieveConfigurationFile(url);
                    }
                }
                else if(responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP)
                {
                    String loc =
                        ((HttpURLConnection)uc).getHeaderField("Location");

                    if(loc != null && (loc.startsWith("http://") ||
                            loc.startsWith("https://")))
                    {
                        tmpFile.delete();
                        /* TODO detect loops */
                        return retrieveConfigurationFile(loc);
                    }
                }
                else if(responseCode == HttpURLConnection.HTTP_OK)
                {
                    if(userCredentials != null &&
                            userCredentials.getUserName() != null &&
                            userCredentials.isPasswordPersistent())
                    {
                        // if save password is checked save the pass
                        getConfigurationService().setProperty(
                                PROPERTY_PROVISIONING_USERNAME,
                                userCredentials.getUserName());
                        getCredentialsStorageService().storePassword(
                            PROPERTY_PROVISIONING_PASSWORD,
                            userCredentials.getPasswordAsString());
                    }
                }
            }
            else
            {
                return null;
            }

            InputStream in = uc.getInputStream();

            // Chain a ProgressMonitorInputStream to the
            // URLConnection's InputStream
            final ProgressMonitorInputStream pin
                = new ProgressMonitorInputStream(null, u.toString(), in);

            // Set the maximum value of the ProgressMonitor
            ProgressMonitor pm = pin.getProgressMonitor();
            pm.setMaximum(uc.getContentLength());

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
                out.close();

                return temp;
            }
            catch (Exception e)
            {
                logger.error("Error saving", e);

                try
                {
                    pin.close();
                    bout.close();
                    out.close();
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
        String allowPrefix = getConfigurationService().getString(
                PROVISIONING_ALLOW_PREFIX_PROP);
        /* must escape the | character */
        String prefixes[] = (allowPrefix != null) ? allowPrefix.split("\\|") :
            null;

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

                if(prefixes != null)
                {
                    boolean isValid = false;

                    for(String s : prefixes)
                    {
                        if(key.startsWith(s))
                        {
                            isValid = true;
                            break;
                        }
                    }

                    /* current propertiy prefix is not allowed */
                    if(!isValid)
                    {
                        continue;
                    }
                }

                if(value instanceof String)
                {
                    if(((String)value).equals("${null}"))
                    {
                        getConfigurationService().removeProperty(key);
                        continue;
                    }
                }

                /* password => credentials storage service */
                if(key.endsWith(".PASSWORD"))
                {
                    getCredentialsStorageService().storePassword(
                            key.substring(0, key.lastIndexOf(".")),
                            (String)value);
                }
                else
                {
                    getConfigurationService().setProperty(key, value);
                }
            }

            /* save the "new" configuration */
            getConfigurationService().storeConfiguration();
            try
            {
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
     * Return the certificate verification service impl.
     * @return the CertificateVerification service.
     */
    private static CertificateVerificationService
        getCertificateVerificationService()
    {
        if(certVerification == null)
        {
            ServiceReference certVerifyReference
                = bundleContext.getServiceReference(
                    CertificateVerificationService.class.getName());
            if(certVerifyReference != null)
                certVerification
                = (CertificateVerificationService)bundleContext.getService(
                        certVerifyReference);
        }

        return certVerification;
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
