/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Loads the Dict provider factory and registers its services in the OSGI
 * bundle context.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cédric
 */
public class DictActivator
    implements BundleActivator
{
    private static final Logger logger = Logger.getLogger(DictActivator.class);

    /**
     * The currently valid bundle context.
     */
    private static BundleContext bundleContext = null;

    private ServiceRegistration dictPpFactoryServReg = null;
    private static ProtocolProviderFactoryDictImpl
                                 dictProviderFactory = null;

    /**
     * Called when this bundle is started. In here we'll export the
     * dict ProtocolProviderFactory implementation so that it could be
     * possible to register accounts with it in SIP Communicator.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        bundleContext = context;
        
        Hashtable hashtable = new Hashtable();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.DICT);
        
        dictProviderFactory = new ProtocolProviderFactoryDictImpl();
        
        //load all stored Dict accounts.
        dictProviderFactory.loadStoredAccounts();
        
        //reg the dict provider factory.
        dictPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    dictProviderFactory,
                    hashtable);

        logger.info("DICT protocol implementation [STARTED].");
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
        
        this.dictProviderFactory.stop();
        dictPpFactoryServReg.unregister();
        
        logger.info("DICT protocol implementation [STOPPED].");
    }
    
    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryDictImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryDictImpl getProtocolProviderFactory()
    {
        return dictProviderFactory;
    }
}
