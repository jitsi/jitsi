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
package net.java.sip.communicator.plugin.provisioning;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;

import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.provisioning.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.apache.http.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;
// disambiguation

/**
 * Provisioning service.
 *
 * @author Sebastien Vincent
 */
public class ProvisioningServiceImpl
    implements ProvisioningService
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProvisioningServiceImpl.class);

    /**
     * Name of the UUID property.
     */
    public static final String PROVISIONING_UUID_PROP
        = "net.java.sip.communicator.UUID";

    /**
     * Name of the provisioning URL in the configuration service.
     */
    private static final String PROPERTY_PROVISIONING_URL
        = "net.java.sip.communicator.plugin.provisioning.URL";

    /**
     * Name of the provisioning username in the configuration service
     * authentication).
     */
    static final String PROPERTY_PROVISIONING_USERNAME
        = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";

    /**
     * Name of the provisioning password in the configuration service (HTTP
     * authentication).
     */
    static final String PROPERTY_PROVISIONING_PASSWORD
        = "net.java.sip.communicator.plugin.provisioning.auth";

    /**
     * Name of the property that contains the provisioning method (i.e. DHCP,
     * DNS, manual, ...).
     */
    private static final String PROVISIONING_METHOD_PROP
        = "net.java.sip.communicator.plugin.provisioning.METHOD";

    /**
     * Name of the property, whether provisioning is mandatory.
     */
    private static final String PROPERTY_PROVISIONING_MANDATORY
        = "net.java.sip.communicator.plugin.provisioning.MANDATORY";

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
     * List of allowed configuration prefixes.
     */
    private List<String> allowedPrefixes = new ArrayList<String>();

    /**
     * Authentication username.
     */
    private static String provUsername = null;

    /**
     * Authentication password.
     */
    private static String provPassword = null;

    /**
     * Prefix that can be used to indicate a property that will be
     * set as a system property.
     */
    private static final String SYSTEM_PROP_PREFIX = "${system}.";

     /**
      * Constructor.
      */
     public ProvisioningServiceImpl()
     {
         // check if UUID is already configured
         String uuid = (String)ProvisioningActivator.getConfigurationService().
             getProperty(PROVISIONING_UUID_PROP);

         if(uuid == null || uuid.equals(""))
         {
             uuid = UUID.randomUUID().toString();
             ProvisioningActivator.getConfigurationService().setProperty(
                 PROVISIONING_UUID_PROP, uuid);
         }

     }

     /**
      * Starts provisioning.
      *
      * @param url provisioning URL
      */
     void start(String url)
     {
         if(url == null)
         {
             /* try to see if provisioning URL is stored in properties */
             url = getProvisioningUri();
         }

         if(!StringUtils.isNullOrEmpty(url))
         {
             InputStream data = retrieveConfigurationFile(url);

             if(data != null)
             {
                 /* store the provisioning URL in local configuration in case
                  * the provisioning discovery failed (DHCP/DNS unavailable, ...)
                  */
                 ProvisioningActivator.getConfigurationService().setProperty(
                         PROPERTY_PROVISIONING_URL, url);

                 updateConfiguration(data);
             }
         }
     }

     /**
      * Indicates if the provisioning has been enabled.
      *
      * @return <tt>true</tt> if the provisioning is enabled, <tt>false</tt> -
      * otherwise
      */
     public String getProvisioningMethod()
     {
         String provMethod
             = ProvisioningActivator.getConfigurationService().getString(
                 PROVISIONING_METHOD_PROP);

         if (provMethod == null || provMethod.length() <= 0)
         {
             provMethod = ProvisioningActivator.getResourceService().
                 getSettingsString(
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
     public void setProvisioningMethod(String provisioningMethod)
     {
         ProvisioningActivator.getConfigurationService().setProperty(
             PROVISIONING_METHOD_PROP, provisioningMethod);
     }

     /**
      * Returns the provisioning URI.
      *
      * @return the provisioning URI
      */
     public String getProvisioningUri()
     {
         String provUri
             = ProvisioningActivator.getConfigurationService().getString(
                 PROPERTY_PROVISIONING_URL);

         if (provUri == null || provUri.length() <= 0)
         {
             provUri = ProvisioningActivator.getResourceService().
                 getSettingsString(
                     "plugin.provisioning.DEFAULT_PROVISIONING_URI");

             if (provUri != null && provUri.length() > 0)
                 setProvisioningUri(provUri);
         }
         return provUri;
     }

     /**
      * Sets the provisioning URI.
      *
      * @param uri the provisioning URI to set
      */
     public void setProvisioningUri(String uri)
     {
         ProvisioningActivator.getConfigurationService().setProperty(
             PROPERTY_PROVISIONING_URL, uri);
     }

    /**
     * Returns provisioning username.
     *
     * @return provisioning username
     */
    public String getProvisioningUsername()
    {
        return provUsername;
    }

    /**
     * Returns provisioning password.
     *
     * @return provisioning password
     */
    public String getProvisioningPassword()
    {
        return provPassword;
    }

    /**
     * Retrieve configuration file from provisioning URL.
     * This method is blocking until configuration file is retrieved from the
     * network or if an exception happen
     *
     * @param url provisioning URL
     * @return Stream of provisioning data
     */
    private InputStream retrieveConfigurationFile(String url)
    {
        return retrieveConfigurationFile(url, null);
    }

    /**
     * Retrieve configuration file from provisioning URL.
     * This method is blocking until configuration file is retrieved from the
     * network or if an exception happen
     *
     * @param url provisioning URL
     * @param parameters the already filled parameters if any.
     * @return Stream of provisioning data
     */
    private InputStream retrieveConfigurationFile(String url,
                                           List<NameValuePair> parameters)
    {
        try
        {
            String arg = null;
            String args[] = null;

            URL u = new URL(url);
            InetAddress ipaddr =
                ProvisioningActivator.getNetworkAddressManagerService().
                    getLocalHost(InetAddress.getByName(u.getHost()));

            // Get any system environment identified by ${env.xyz}
            Pattern p = Pattern.compile("\\$\\{env\\.([^\\}]*)\\}");
            Matcher m = p.matcher(url);
            StringBuffer sb = new StringBuffer();
            while(m.find())
            {
                String value = System.getenv(m.group(1));
                if(value != null)
                {
                    m.appendReplacement(sb, Matcher.quoteReplacement(value));
                }
            }
            m.appendTail(sb);
            url = sb.toString();

            // Get any system property variable identified by ${system.xyz}
            p = Pattern.compile("\\$\\{system\\.([^\\}]*)\\}");
            m = p.matcher(url);
            sb = new StringBuffer();
            while(m.find())
            {
                String value = System.getProperty(m.group(1));
                if(value != null)
                {
                    m.appendReplacement(sb, Matcher.quoteReplacement(value));
                }
            }
            m.appendTail(sb);
            url = sb.toString();

            if(url.indexOf("${home.location}") != -1)
            {
                url = url.replace("${home.location}",
                    ProvisioningActivator.getConfigurationService()
                        .getScHomeDirLocation());
            }

            if(url.indexOf("${home.name}") != -1)
            {
                url = url.replace("${home.name}",
                    ProvisioningActivator.getConfigurationService()
                        .getScHomeDirName());
            }

            if(url.indexOf("${uuid}") != -1)
            {
                url = url.replace("${uuid}",
                    (String)ProvisioningActivator.getConfigurationService()
                        .getProperty(PROVISIONING_UUID_PROP));
            }

            if(url.indexOf("${osname}") != -1)
            {
                url = url.replace("${osname}", System.getProperty("os.name"));
            }

            if(url.indexOf("${arch}") != -1)
            {
                url = url.replace("${arch}", System.getProperty("os.arch"));
            }

            if(url.indexOf("${resx}") != -1 || url.indexOf("${resy}") != -1)
            {
                Rectangle screen = ScreenInformation.getScreenBounds();

                if(url.indexOf("${resx}") != -1)
                {
                    url = url.replace("${resx}", String.valueOf(screen.width));
                }

                if(url.indexOf("${resy}") != -1)
                {
                    url = url.replace("${resy}", String.valueOf(screen.height));
                }
            }

            if(url.indexOf("${build}") != -1)
            {
                url = url.replace("${build}",
                        System.getProperty("sip-communicator.version"));
            }

            if(url.indexOf("${locale}") != -1)
            {
                String locale =
                    ProvisioningActivator.getConfigurationService().getString(
                        ResourceManagementService.DEFAULT_LOCALE_CONFIG);
                if(locale == null)
                    locale = "";

                url = url.replace("${locale}", locale);
            }

            if(url.indexOf("${ipaddr}") != -1)
            {
                url = url.replace("${ipaddr}", ipaddr.getHostAddress());
            }

            if(url.indexOf("${hostname}") != -1)
            {
                String name;
                if(OSUtils.IS_WINDOWS)
                {
                    // avoid reverse DNS lookup
                    name = System.getenv("COMPUTERNAME");
                }
                else
                {
                    name = ipaddr.getHostName();
                }
                url = url.replace("${hostname}", name);
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
                                    ProvisioningActivator.
                                        getNetworkAddressManagerService().
                                            getHardwareAddress(iface);

                                if(hw == null)
                                    continue;

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

            ArrayList<String> paramNames = null;
            ArrayList<String> paramValues = null;
            int usernameIx = -1;
            int passwordIx = -1;

            if(args != null && args.length > 0)
            {
                paramNames = new ArrayList<String>(args.length);
                paramValues = new ArrayList<String>(args.length);

                String usernameParam = "${username}";
                String passwordParam = "${password}";

                for(int i = 0; i < args.length; i++)
                {
                    String s = args[i];

                    int equalsIndex = s.indexOf("=");
                    String currentParamName = null;

                    if (equalsIndex > -1)
                    {
                        currentParamName = s.substring(0, equalsIndex);
                    }

                    // pre loaded value we will reuse.
                    String preloadedParamValue =
                        getParamValue(parameters, currentParamName);

                    // If we find the username or password parameter at this
                    // stage we replace it with an empty string.
                    // or if we have an already filled value we will reuse it.
                    if(s.indexOf(usernameParam) != -1)
                    {
                        if(preloadedParamValue != null)
                        {
                            s = s.replace(usernameParam, preloadedParamValue);
                        }
                        else
                            s = s.replace(usernameParam, "");
                        usernameIx = paramNames.size();
                    }
                    else if(s.indexOf(passwordParam) != -1)
                    {
                        if(preloadedParamValue != null)
                        {
                            s = s.replace(passwordParam, preloadedParamValue);
                        }
                        else
                            s = s.replace(passwordParam, "");
                        passwordIx = paramNames.size();
                    }

                    if (equalsIndex > -1)
                    {
                        paramNames.add(currentParamName);
                        paramValues.add(s.substring(equalsIndex + 1));
                    }
                    else
                    {
                        if(logger.isInfoEnabled())
                        {
                            logger.info(
                                    "Invalid provisioning request parameter: \""
                                    + s + "\", is replaced by \"" + s + "=\"");
                        }
                        paramNames.add(s);
                        paramValues.add("");
                    }
                }
            }

            HttpUtils.HTTPResponseResult res = null;
            Throwable errorWhileProvisioning = null;
            try
            {
                res =
                    HttpUtils.postForm(
                        url,
                        PROPERTY_PROVISIONING_USERNAME,
                        PROPERTY_PROVISIONING_PASSWORD,
                        paramNames,
                        paramValues,
                        usernameIx,
                        passwordIx,
                        new HttpUtils.RedirectHandler()
                        {
                            @Override
                            public boolean handleRedirect(
                                String location,
                                List<NameValuePair> parameters)
                            {
                                if(!hasParams(location))
                                    return false;

                                // if we have parameters proceed
                                retrieveConfigurationFile(location, parameters);

                                return true;
                            }

                            @Override
                            public boolean hasParams(String location)
                            {
                                return location.contains("${");
                            }
                        });
            }
            catch(Throwable t)
            {
                logger.error("Error posting form", t);
                errorWhileProvisioning = t;
            }

            // if there was an error in retrieving stop
            if(res == null)
            {
                // if canceled, lets check whether provisioning is
                // mandatory
                if(ProvisioningActivator.getConfigurationService().getBoolean(
                    PROPERTY_PROVISIONING_MANDATORY, false))
                {
                    String errorMsg;
                    if(errorWhileProvisioning != null)
                        errorMsg = errorWhileProvisioning.getLocalizedMessage();
                    else
                        errorMsg = "";

                    ErrorDialog ed = new ErrorDialog(
                        null,
                        ProvisioningActivator.getResourceService()
                            .getI18NString("plugin.provisioning.PROV_FAILED"),
                        ProvisioningActivator.getResourceService()
                            .getI18NString("plugin.provisioning.PROV_FAILED_MSG",
                                            new String[]{errorMsg}),
                        errorWhileProvisioning);
                    ed.setModal(true);
                    ed.showDialog();

                    // as shutdown service is not started and other bundles
                    // are scheduled to start, stop all of them
                    {
                        for(Bundle b : ProvisioningActivator.bundleContext
                                            .getBundles())
                        {
                            try
                            {
                                // skip our Bundle avoiding stopping us while
                                // starting and NPE in felix
                                if(ProvisioningActivator.bundleContext
                                    .equals(b.getBundleContext()))
                                {
                                    continue;
                                }
                                b.stop();
                            }
                            catch (BundleException ex)
                            {
                                logger.error(
                                    "Failed to being gentle stop " +
                                        b.getLocation(), ex);
                            }
                        }
                    }
                }

                // stop processing
                return null;
            }

            String userPass[] = res.getCredentials();
            if(userPass[0] != null && userPass[1] != null)
            {
                provUsername = userPass[0];
                provPassword = userPass[1];
            }

            InputStream in = res.getContent();

            // Skips ProgressMonitorInputStream wrapper on Android
            if(!OSUtils.IS_ANDROID)
            {
                // Chain a ProgressMonitorInputStream to the
                // URLConnection's InputStream
                final ProgressMonitorInputStream pin;
                pin = new ProgressMonitorInputStream(null, u.toString(), in);

                // Set the maximum value of the ProgressMonitor
                ProgressMonitor pm = pin.getProgressMonitor();
                pm.setMaximum((int)res.getContentLength());

                // Uses ProgressMonitorInputStream if available
                return pin;
            }

            return in;
        }
        catch (Exception e)
        {
            if (logger.isInfoEnabled())
                logger.info("Error retrieving provisioning file!", e);
            return null;
        }
    }

    /**
     * Search param value for the supplied name.
     * @param parameters the parameters can be null.
     * @param paramName the name to search.
     * @return the corresponding parameter value.
     */
    private static String getParamValue(List<NameValuePair> parameters,
                                        String paramName)
    {
        if(parameters == null || paramName == null)
            return null;

        for(NameValuePair nv : parameters)
        {
            if(nv.getName().equals(paramName))
                return nv.getValue();
        }

        return null;
    }

    /**
     * Update configuration with properties retrieved from provisioning URL.
     *
     * @param data Provisioning data
     */
    private void updateConfiguration(final InputStream data)
    {
        Properties fileProps = new OrderedProperties();
        InputStream in = null;

        try
        {
            in = new BufferedInputStream(data);
            fileProps.load(in);

            Iterator<Map.Entry<Object, Object> > it
                = fileProps.entrySet().iterator();

            while(it.hasNext())
            {
                Map.Entry<Object, Object> entry = it.next();
                String key = (String)entry.getKey();
                Object value = entry.getValue();

                // skip empty keys, prevent them going into the configuration
                if(key.trim().length() == 0)
                    continue;

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
                ProvisioningActivator.getConfigurationService().
                    storeConfiguration();
                ProvisioningActivator.getConfigurationService().
                    reloadConfiguration();
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
        if((value instanceof String) && value.equals("${null}"))
        {
            ProvisioningActivator.getConfigurationService().removeProperty(key);
        }
        else if(key.endsWith(".PASSWORD"))
        {
            /* password => credentials storage service */
            ProvisioningActivator.getCredentialsStorageService().storePassword(
                    key.substring(0, key.lastIndexOf(".")),
                    (String)value);

            if(logger.isInfoEnabled())
                logger.info(key +"=<password hidden>");

            return;
        }
        else if(key.startsWith(SYSTEM_PROP_PREFIX))
        {
            String sysKey = key.substring(
                SYSTEM_PROP_PREFIX.length(), key.length());

            System.setProperty(sysKey, (String)value);
        }
        else
        {
            ProvisioningActivator.getConfigurationService().setProperty(key,
                value);
        }

        if(logger.isInfoEnabled())
            logger.info(key + "=" + value);
    }

    /**
     * Walk through all properties and make sure all properties keys match
     * a specific set of prefixes defined in configuration.
     *
     * @param enforcePrefix list of enforce prefix.
     */
    private void checkEnforcePrefix(String enforcePrefix)
    {
        ConfigurationService config =
            ProvisioningActivator.getConfigurationService();
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
}
