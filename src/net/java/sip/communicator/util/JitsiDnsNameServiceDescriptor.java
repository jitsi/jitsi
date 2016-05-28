/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2016 Atlassian Pty Ltd
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
package net.java.sip.communicator.util;

import sun.net.spi.nameservice.*;

/**
 * A name service descriptor for Jitsi's DNS service. It is instantiated by the
 * JRE when DNSSEC is enabled in Jitsi's options.
 * 
 * @author Ingo Bauersachs
 */
public class JitsiDnsNameServiceDescriptor
    implements NameServiceDescriptor
{
    /**
     * Gets and creates an instance of the Jitsi's name service.
     * 
     * @return an instance of the Jitsi's name service.
     */
    public NameService createNameService()
    {
        return new JitsiDnsNameService();
    }

    /**
     * Gets the type of this name service.
     * @return the string <tt>dns</tt>
     */
    public String getType()
    {
        return "dns";
    }

    /**
     * Gets the name of this name service.
     * @return the string <tt>jitsi</tt>
     */
    public String getProviderName()
    {
        return "jitsi";
    }
}