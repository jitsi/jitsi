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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;

import java.util.*;

/**
 * Represents <tt>ssrc-group</tt> elements described in XEP-0339.
 *
 * Created by gp on 07/08/14.
 * @author George Politis
 * @author Pawel Domas
 */
public class SourceGroupPacketExtension
        extends AbstractPacketExtension
{
    /**
     * The name of the "ssrc-group" element.
     */
    public static final String ELEMENT_NAME = "ssrc-group";

    /**
     * The namespace for the "ssrc-group" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:ssma:0";

    /**
     * The name of the payload <tt>id</tt> SDP argument.
     */
    public static final String SEMANTICS_ATTR_NAME = "semantics";

    /**
     * The constant used for signaling simulcast semantics.
     */
    public static final String SEMANTICS_SIMULCAST = "SIM";

    /**
     * The constant used for flow identification (see RFC5888).
     */
    public static final String SEMANTICS_FID = "FID";

    /**
     * Return new instance of <tt>SourceGroupPacketExtension</tt> with simulcast
     * semantics pre-configured.
     */
    public static SourceGroupPacketExtension createSimulcastGroup()
    {
        SourceGroupPacketExtension simulcastGroupPe
            = new SourceGroupPacketExtension();

        simulcastGroupPe.setSemantics(SEMANTICS_SIMULCAST);

        return simulcastGroupPe;
    }

    /**
     * Creates a new {@link SourceGroupPacketExtension} instance with the proper
     * element name and namespace.
     */
    public SourceGroupPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Gets the semantics of this source group.
     *
     * @return the semantics of this source group.
     */
    public String getSemantics()
    {
        return getAttributeAsString(SEMANTICS_ATTR_NAME);
    }

    /**
     * Sets the semantics of this source group.
     */
    public void setSemantics(String semantics)
    {
        this.setAttribute(SEMANTICS_ATTR_NAME, semantics);
    }

    /**
     * Gets the sources of this source group.
     *
     * @return the sources of this source group.
     */
    public List<SourcePacketExtension> getSources()
    {
        return getChildExtensionsOfType(SourcePacketExtension.class);
    }

    /**
     * Sets the sources of this source group.
     *
     * @param sources the sources of this source group.
     */
    public void addSources(List<SourcePacketExtension> sources)
    {
        if (sources != null && sources.size() != 0)
        {
            for (SourcePacketExtension source : sources)
                this.addChildExtension(source);
        }

    }

    /**
     * Returns deep copy of this <tt>SourceGroupPacketExtension</tt> instance.
     */
    public SourceGroupPacketExtension copy()
    {
        SourceGroupPacketExtension copy
            = AbstractPacketExtension.clone(this);

        copy.setSemantics(getSemantics());

        List<SourcePacketExtension> sources = getSources();

        List<SourcePacketExtension> sourcesCopy
            = new ArrayList<SourcePacketExtension>(sources.size());

        for (SourcePacketExtension source : sources)
        {
            sourcesCopy.add(source.copy());
        }

        copy.addSources(sourcesCopy);

        return copy;
    }
}
