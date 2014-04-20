/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An <tt>OperationSet</tt> that allows access to connection information used 
 * by the protocol provider.
 *
 * @author Markus Kilas
 */
public interface OperationSetConnectionInfo
    extends OperationSet
{
    String getServerAddress();
    int getServerPort();
}
