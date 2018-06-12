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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import net.java.sip.communicator.util.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;

/**
 * Implements the Jitsi Videobridge <tt>stats</tt> extension within COnferencing
 * with LIghtweight BRIdging that will provide various statistics.
 *
 * @author Hristo Terezov
 * @author Boris Grozev
 */
public class ColibriStatsExtension
    extends AbstractPacketExtension
{
    /**
     * The logger instance used by this class.
     */
    private final static Logger logger
        = Logger.getLogger(ColibriConferenceIQ.class);

    /**
     * The XML element name of the Jitsi Videobridge <tt>stats</tt> extension.
     */
    public static final String ELEMENT_NAME = "stats";

    /**
     * The XML COnferencing with LIghtweight BRIdging namespace of the Jitsi
     * Videobridge <tt>stats</tt> extension.
     */
    public static final String NAMESPACE
        = "http://jitsi.org/protocol/colibri";

    /**
     * Tries to parse an object as an integer, returns null on failure.
     * @param obj the object to parse.
     */
    private static Integer getInt(Object obj)
    {
        if (obj == null)
        {
            return null;
        }
        if (obj instanceof Integer)
        {
            return (Integer) obj;
        }

        String str = obj.toString();
        try
        {
            return Integer.valueOf(str);
        }
        catch (NumberFormatException e)
        {
            logger.error("Error parsing an int: " + obj);
        }
        return null;
    }

    /**
     * Creates a deep copy of a {@link ColibriStatsExtension}.
     * @param source the {@link ColibriStatsExtension} to copy.
     * @return the copy.
     */
    public static ColibriStatsExtension clone(
        ColibriStatsExtension source)
    {
        ColibriStatsExtension destination
            = AbstractPacketExtension.clone(source);

        for (Stat stat : source.getChildExtensionsOfType(Stat.class))
        {
            destination.addStat(Stat.clone(stat));
        }

        return destination;
    }

    /**
     * Constructs new <tt>ColibriStatsExtension</tt>
     */
    public ColibriStatsExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Adds a specific {@link Stat} instance to the list of stats.
     * @param stat the {@link Stat} instance to add.
     */
    public void addStat(Stat stat)
    {
        addChildExtension(stat);
    }

    /**
     * Adds a new {@link Stat} instance with a specific name and a specific
     * value to the list of stats.
     * @param name the name.
     * @param value the value.
     */
    public void addStat(String name, Object value)
    {
        addStat(new Stat(name, value));
    }

    /**
     * @return the first {@link Stat}, if any, with a specific name.
     * @param name the name of the stat to match.
     */
    public Stat getStat(String name)
    {
        for (Stat stat : getChildExtensionsOfType(Stat.class))
        {
            if (stat.getName().equals(name))
            {
                return stat;
            }
        }

        return null;
    }

    /**
     * @return the value of the first {@link Stat}, if any, with a specific
     * name.
     * @param name the name of the stat to match.
     */
    public Object getValue(String name)
    {
        Stat stat = getStat(name);
        return stat == null ? null : stat.getValue();
    }

    /**
     * Tries to get the value of the stat with the given {@code name} as a
     * {@link String}. If there is no stat with the given name, or it has no
     * value, returns {@code null}. Otherwise, it returns the {@link String}
     * representation of the value.
     *
     * @param name the name of the stat.
     * @return a {@link String} which represents the value of the stat with the
     * given {@code name}, or {@code null}.
     */
    public String getValueAsString(String name)
    {
        Object o = getValue(name);
        if (o != null)
        {
            return (o instanceof String) ? (String) o : o.toString();
        }
        return null;
    }

    /**
     * Tries to get the value of the stat with the given {@code name} as an
     * {@link Integer}. If there is no stat with the given name, or it has no
     * value, returns {@code null}. Otherwise, it tries to parse the value as
     * an {@link Integer} and returns the result (or {@code null} if parsing
     * fails).
     *
     * @param name the name of the stat.
     * @return an {@link Integer} representation of the value of the stat with
     * the given {@code name}, or {@code null}.
     */
    public Integer getValueAsInt(String name)
    {
        return getInt(getValue(name));
    }

    @Override
    public List<? extends ExtensionElement> getChildExtensions()
    {
        return Collections.unmodifiableList(super.getChildExtensions());
    }

    public static class Stat
        extends AbstractPacketExtension
    {
        /**
         * The XML element name of a <tt>content</tt> of a Jitsi Videobridge
         * <tt>stats</tt> IQ.
         */
        public static final String ELEMENT_NAME = "stat";

        /**
         * The XML name of the <tt>name</tt> attribute of a <tt>stat</tt> of a
         * <tt>stats</tt> IQ which represents the <tt>name</tt> property of the
         * statistic.
         */
        public static final String NAME_ATTR_NAME = "name";

        /**
         * The XML name of the <tt>name</tt> attribute of a <tt>stat</tt> of a
         * <tt>stats</tt> IQ which represents the <tt>value</tt> property of the
         * statistic.
         */
        public static final String VALUE_ATTR_NAME = "value";

        public Stat()
        {
            super(NAMESPACE, ELEMENT_NAME);
        }

        /**
         * Constructs new <tt>Stat</tt> by given name and value.
         * @param name the name
         * @param value the value
         */
        public Stat(String name, Object value)
        {
            this();
            this.setName(name);
            this.setValue(value);
        }

        @Override
        public String getElementName()
        {
            return ELEMENT_NAME;
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return getAttributeAsString(NAME_ATTR_NAME);
        }

        @Override
        public String getNamespace()
        {
            return NAMESPACE;
        }

        /**
         * @return the value
         */
        public Object getValue()
        {
            return getAttribute(VALUE_ATTR_NAME);
        }

        /**
         * @param name the name to set
         */
        public void setName(String name)
        {
            setAttribute(NAME_ATTR_NAME, name);
        }

        /**
         * @param value the value to set
         */
        public void setValue(Object value)
        {
            setAttribute(VALUE_ATTR_NAME, value);
        }

        @Override
        public String toXML()
        {
            String name = getName();
            Object value = getValue();

            if ((name == null) || (value == null))
            {
                return "";
            }
            else
            {
                return
                    "<" + ELEMENT_NAME + " " + NAME_ATTR_NAME + "='"
                        + StringUtils.escapeForXml(name) + "' "
                        + VALUE_ATTR_NAME + "='"
                        + StringUtils.escapeForXml(value.toString()) + "' />";
            }
        }
    }
}
