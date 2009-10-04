/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import net.java.sip.communicator.impl.media.transform.*;

import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.SessionAddress;

/**
 * ZRTP specific Transform Connector.
 * 
 * This class just adds ZRTP specific functions.
 * 
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
public class ZrtpTransformConnector extends TransformConnector 
{
    public ZrtpTransformConnector(SessionAddress localAddr,
                                  ZRTPTransformEngine engine) 
        throws InvalidSessionAddressException 
    {
        super(localAddr, engine);
    }

    public ZRTPTransformEngine getEngine() 
    {
        return (ZRTPTransformEngine) engine;
    }
}
