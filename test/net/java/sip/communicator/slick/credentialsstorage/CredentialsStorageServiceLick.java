/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.credentialsstorage;

import java.util.*;

import junit.framework.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * @author Dmitri Melnikov
 */
public class CredentialsStorageServiceLick 
    extends TestSuite 
    implements BundleActivator 
{
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static CredentialsStorageService credentialsService = null;
    protected static BundleContext bc = null;
    public static TestCase tcase = new TestCase(){};

    /**
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
    	CredentialsStorageServiceLick.bc = bundleContext;
        setName("CredentialsStorageServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        addTestSuite(TestCredentialsStorageService.class);
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
    }
}
