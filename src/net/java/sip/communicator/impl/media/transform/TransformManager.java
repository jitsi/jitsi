/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import javax.media.rtp.*;
import java.security.*;

import net.java.sip.communicator.impl.media.transform.dummy.*;
import net.java.sip.communicator.impl.media.transform.srtp.*;
import net.java.sip.communicator.impl.media.transform.zrtp.*;
import net.java.sip.communicator.impl.media.keyshare.*;
import net.java.sip.communicator.service.media.*;
import java.util.*;

import org.bouncycastle.jce.provider.*;

/**
 * TransformManager class encapsulate the logic of creating different kinds of
 * TransformConnectors. All TransformConnector objects should be created through
 * TransformManager class.
 * 
 * @author Bing SU (nova.su@gmail.com)
 * @author Emanuel Onica (eonica@info.uaic.ro)
 */
public class TransformManager
{
     
    /**
     * Map of supported cryptography services providers
     */
    public static HashMap cryptoProviders = null;
    
    /**
     * Initialize the supported cryptography services providers
     */
    public static void initializeProviders()
    {
        if (cryptoProviders == null)
        {
            cryptoProviders = new HashMap();
            
            Provider cryptoProvider = null ;
            
            try 
            {
                Class<?> c = Class
                        .forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                cryptoProvider = (Provider) c.newInstance();
            } 
            catch (ClassNotFoundException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            catch (InstantiationException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            catch (IllegalAccessException e) 
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            cryptoProviders.put("BouncyCastle", cryptoProvider);
        }
    }
    
    /**
     * Select a cryptography services provider to use from the supported ones 
     * 
     * @param cryptoProvider cryptography provider selection string
     * @return the actual cryptography provider to use
     */
    public static Provider selectProvider(String cryptoProvider)
    {
        return (Provider)cryptoProviders.get(cryptoProvider);
    }
    
    /**
     * Create a SRTP TransformConnector, which will provide SRTP encryption /
     * decryption functionality, using algorithms defined in RFC3711.
     * 
     * @param addr local RTP session listen address
     * @param masterKey master key of this SRTP session
     * @param masterSalt master salt of this SRTP session
     * @param srtpPolicy SRTP policy for this SRTP session
     * @param srtcpPolicy SRTCP policy for this SRTP session
     * @param cryptoProvider the cryptography services provider selection string
     *                       should be obtained from a resource file or by querying 
     * @return the TransformConnector used for SRTP encyption/decryption
     * @throws InvalidSessionAddressException if the local RTP session address
     * is invalid
     */ 
    public static TransformConnector createSRTPConnector(SessionAddress addr,
                                                         byte[] masterKey, 
                                                         byte[] masterSalt, 
                                                         SRTPPolicy srtpPolicy,
                                                         SRTPPolicy srtcpPolicy, 
                                                         String cryptoProvider)
    throws InvalidSessionAddressException 
    {
        SRTPTransformEngine engine = null;
        
        Provider cp = selectProvider(cryptoProvider);
        
        try 
        {
            engine = new SRTPTransformEngine(masterKey, 
                                             masterSalt, 
                                             srtpPolicy,
                                             srtcpPolicy, 
                                             cp);
        } 
        catch (GeneralSecurityException e) 
        {
            e.printStackTrace();
            return null;
        }

        TransformConnector connector = null;
        
        connector = new TransformConnector(addr, engine);

        return connector;
    }

    /**
     * Creates a connector specific for use in case of ZRTP key management
     * 
     * @param addr local RTP session listen address
     * @param cryptoProvider the cryptography services provider selection string
     *                       should be obtained from a resource file or by querying 
     * @return the TransformConnector used for SRTP encyption/decryption
     * @throws InvalidSessionAddressException
     */
    public static TransformConnector createZRTPConnector(SessionAddress addr,
                                                         String cryptoProvider,
                                                         CallSession callSession)
        throws InvalidSessionAddressException
    {
        //for adding multistream support the engine should be instantiated 
    	//once as a static variable of this class and passed to every ZRTP
    	//connector as a parameter
        ZRTPTransformEngine engine = new ZRTPTransformEngine(); 
        
        Provider cp = selectProvider(cryptoProvider);

        TransformConnector connector = null;
        
        connector = new ZrtpTransformConnector(addr, engine);

        //for adding multistream support this method should be replaced with 
        //an addConnector, which should add the connector to an internal engine 
        //connector array; supporting multistream mode by the engine implies
        //the proper management of this connector array - practically every 
        //stream has it's own connector
        engine.setConnector(connector);     
        
        //for adding multistream support also the SCCallback should be instantiates
        //only once as a static variable of this class and passed to the engine
        engine.setUserCallback(new SCCallback(callSession));
        engine.setCryptoProvider(cp);
        
        return connector;  
    }

    /**
     * Create a dummy TransformConnector. A dummy TransformConnector does no
     * modification (transformation) to RTP/RTCP packets. Its main purpose is to
     * test the TransformationConnector interface and provides a example code
     * of how to use the TransformConnector interfaces.
     * 
     * @param addr local RTP session address
     * @return A dummy TransformationConnector object
     * @throws InvalidSessionAddressException if the local RTP session address
     * is invalid
     */
    public static TransformConnector createDummyConnector(SessionAddress addr)
        throws InvalidSessionAddressException
    {
        DummyTransformEngine engine = new DummyTransformEngine();
        TransformConnector connector = new TransformConnector(addr, engine);
        return connector;
    }
}
