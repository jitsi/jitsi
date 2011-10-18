/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.osgi.framework.*;
import org.osgi.service.url.*;

/**
 * Implements {@link URLStreamHandlerService} for the &quot;reference&quot;
 * protocol used inside felix.client.run.properties in order to fix issue #647
 * (MalformedURLException in java-jml) by translating the URL to the
 * &quot;jar&quot; protocol which is natively supported by Java.
 *
 * @author Lubomir Marinov
 */
public class ReferenceURLStreamHandlerService
    extends AbstractURLStreamHandlerService
{

    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>ReferenceURLStreamHandlerService</tt> class and instances for logging
     * output.
     */
    private static final Logger logger
        = Logger.getLogger(ReferenceURLStreamHandlerService.class);

    /**
     * Registers a new <tt>ReferenceURLStreamHandlerService</tt> instance as an
     * implementation of {@link URLStreamHandlerService} in a specific
     * <tt>BundleContext</tt> for the &quot;reference&quot; protocol  if there is
     * no such registered implementation already. Otherwise, the existing
     * registered implementation is considered to be more complete in terms of
     * features than <tt>ReferenceURLStreamHandlerService</tt> and this method
     * does nothing.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which a new
     * <tt>ReferenceURLStreamHandlerService</tt> instance is to be registered
     */
    public static void registerService(BundleContext bundleContext)
    {
        ServiceReference[] serviceReferences;
        String clazz = URLStreamHandlerService.class.getName();
        String propertyName = URLConstants.URL_HANDLER_PROTOCOL;
        String propertyValue = "reference";

        try
        {
            serviceReferences
                = bundleContext
                    .getServiceReferences(
                        clazz,
                        "(" + propertyName + "=" + propertyValue + ")");
        }
        catch (InvalidSyntaxException ise)
        {
            logger
                .warn(
                    "Failed to determine whether there are registered "
                        + "URLStreamHandlerService implementations for the "
                        + "\"reference\" protocol",
                    ise);
            serviceReferences = null;
        }

        if ((serviceReferences != null) && (serviceReferences.length > 0))
            return;

        Dictionary<String, String> properties = new Hashtable<String, String>();

        properties.put(propertyName, propertyValue);
        bundleContext
            .registerService(
                clazz,
                new ReferenceURLStreamHandlerService(),
                properties);
    }

    /**
     * Implements <tt>AbstractURLStreamHandlerService#openConnection(URL)</tt>.
     * Opens a connection to the object referenced by the <tt>URL</tt> argument
     * by rewriting the &quot;reference&quot; protocol in it with the
     * &quot;jar&quot; protocol and then handing it for opening to
     * {@link JarURLConnection}.
     *
     * @param url the <tt>URL</tt> that <tt>this</tt> connects to
     * @return an <tt>URLConnection</tt> instance for the specified <tt>URL</tt>
     * @throws IOException if an I/O error occurs while opening the connection
     */
    public URLConnection openConnection(URL url)
        throws IOException
    {
        String referenceSpec = url.toString();
        String jarSpec = referenceSpec.replaceFirst("reference:", "jar:");
        String jarSeparator = "!/";

        // JarURLConnection mandates jarSeparator.
        if (!jarSpec.contains(jarSeparator))
            jarSpec += jarSeparator;

        return new URL(jarSpec).openConnection();
    }
}
