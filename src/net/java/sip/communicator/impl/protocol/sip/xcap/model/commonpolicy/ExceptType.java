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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * The Authorization Rules except element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ExceptType
{
    /**
     * The domain attribute.
     */
    private String id;

    /**
     * The domain attribute.
     */
    private String domain;

    /**
     * Gets the value of the id property.
     *
     * @return the id property.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param id the id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Gets the value of the domain property.
     *
     * @return the domain property.
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * Sets the value of the domain property.
     *
     * @param domain the domain to set.
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }
}
