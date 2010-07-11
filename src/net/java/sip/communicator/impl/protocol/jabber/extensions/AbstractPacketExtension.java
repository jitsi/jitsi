/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import java.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * A generic implementation of <tt>PacketExtension</tt>. The purpose of this
 * class is quite similar to that of smack's {@link DefaultPacketExtension}
 * with the main difference being that this one is meant primarily for
 * extension rather than using as a fallback for unknown elements. We let for
 * example our descendants handle child elements and we automate attribute
 * handling instead.
 *
 * @author Emil Ivov
 */
public abstract class AbstractPacketExtension
    implements PacketExtension
{
    /**
     * The name space of this packet extension. Should remain <tt>null</tt> if
     * there's no namespace associated with this element.
     */
    private final String namespace;

    /**
     * The name space of this packet extension. Should remain <tt>null</tt> if
     * there's no namespace associated with this element.
     */
    private final String elementName;

    /**
     * A map of all attributes that this extension is currently using.
     */
    private final Map<String, Object> attributes
                                    = new LinkedHashMap<String, Object>();

    /**
     * Creates an {@link AbstractPacketExtension} instance for the specified
     * <tt>namespace</tt> and <tt>elementName</tt>.
     *
     * @param namespace the XML namespace for this element.
     * @param elementName the name of the element
     */
    protected AbstractPacketExtension(String namespace, String elementName)
    {
        this.namespace = namespace;
        this.elementName = elementName;
    }

    /**
     * Returns the name of the <tt>encryption</tt> element.
     *
     * @return the name of the <tt>encryption</tt> element.
     */
    public String getElementName()
    {
        return elementName;
    }

    /**
     * Returns the XML namespace for this element or <tt>null</tt> if the
     * element does not live in a namespace of its own.
     *
     * @return the XML namespace for this element or <tt>null</tt> if the
     * element does not live in a namespace of its own.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Returns an XML representation of this extension.
     *
     * @return an XML representation of this extension.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder("<" + getElementName() + " ");

        if(getNamespace() != null)
            bldr.append("xmlns='" + getNamespace() + "'");

        //add the rest of the attributes if any
        for(Map.Entry<String, Object> entry : attributes.entrySet())
        {
            bldr.append(" " + entry.getKey() + "=" + entry.getValue());
        }

        //add child elements if any
        List<PacketExtension> childElements = getChildElements();

        if(childElements == null || childElements.size() == 0)
            bldr.append("/>");
        else
        {
            for(PacketExtension packExt : childElements)
            {
                bldr.append(packExt.toXML());
            }

            bldr.append("</"+getElementName()+">");
        }

        return bldr.toString();
    }

    /**
     * Returns all sub-elements for this <tt>AbstractPacketExtension</tt> or
     * <tt>null</tt> if there aren't any.
     * <p>
     * Overriding extensions need to override this method if they have any child
     * elements.
     *
     * @return the {@link List} of elements that this packet extension contains.
     */
    public List<PacketExtension> getChildElements()
    {
        return null;
    }

    /**
     * Sets the value of the attribute named <tt>name</tt> to <tt>value</tt>.
     *
     * @param name the name of the attribute that we are setting.
     * @param value an {@link Object} whose <tt>toString()</tt> method returns
     * the XML value of the attribute we are setting.
     */
    public void setAttribtue(String name, Object value)
    {
        synchronized(attributes)
        {
            this.attributes.put(name, value);
        }
    }

    /**
     * Removes the attribute with the specified <tt>name</tt> from the list of
     * attributes registered with this packet extension.
     *
     * @param name the name of the attribute that we are removing.
     */
    public void removeAttribtue(String name)
    {
        synchronized(attributes)
        {
            attributes.remove(name);
        }
    }

    /**
     * Returns the attribute with the specified <tt>name</tt> from the list of
     * attributes registered with this packet extension.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     *
     * @return the value of the specified <tt>attribute</tt> or <tt>null</tt>
     * if no such attribute is currently registered with this extension.
     */
    public Object getAttribtue(String attribute)
    {
        synchronized(attributes)
        {
            return attributes.get(attribute);
        }
    }

    /**
     * Returns the string value of the attribute with the specified
     * <tt>name</tt>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     *
     * @return the String value of the specified <tt>attribute</tt> or
     * <tt>null</tt> if no such attribute is currently registered with this
     * extension.
     */
    public String getAttribtueString(String attribute)
    {
        synchronized(attributes)
        {
            Object attributeVal = attributes.get(attribute);

            return attributeVal == null ? null : attributeVal.toString();
        }
    }
}
