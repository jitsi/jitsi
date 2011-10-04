/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A utility interface whose only purpose is to represent a call peer that's
 * also capable of ZRTP control.
 *
 * @param <T> the implementation specific call extension
 * @param <U> the implementation specific protocol provider extension
 *
 * @author Emil Ivov
 */
public abstract class SrtpAwareCallPeer<T extends Call,
                                        U extends ProtocolProviderService>
    extends AbstractCallPeer<T, U>
    implements SrtpControl
{

}
