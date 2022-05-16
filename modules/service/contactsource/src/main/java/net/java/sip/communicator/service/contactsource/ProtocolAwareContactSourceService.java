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
package net.java.sip.communicator.service.contactsource;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ProtocolAwareContactSourceService</tt> extends the basic
 * <tt>ContactSourceService</tt> interface to provide a protocol aware contact
 * source. In other words a preferred <tt>ProtocolProviderService</tt> can be
 * set for a given <tt>OperationSet</tt> class that would affect the query
 * result by excluding source contacts that has a preferred provider different
 * from the one specified as a preferred provider.
 *
 * @author Yana Stamcheva
 */
public interface ProtocolAwareContactSourceService
    extends ContactSourceService
{
    /**
     * Sets the preferred protocol provider for this contact source. The
     * preferred <tt>ProtocolProviderService</tt> set for a given
     * <tt>OperationSet</tt> class would affect the query result by excluding
     * source contacts that has a preferred provider different from the one
     * specified here.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, for which the
     * preferred provider is set
     * @param protocolProvider the <tt>ProtocolProviderService</tt> to set
     */
    public void setPreferredProtocolProvider(
                                    Class<? extends OperationSet> opSetClass,
                                    ProtocolProviderService protocolProvider);
}
