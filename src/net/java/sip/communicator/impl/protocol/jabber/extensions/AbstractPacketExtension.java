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
     * The text content of this packet extension, if any.
     */
    private String textContent;

    /**
     * A list of extensions registered with this element.
     */
    private List<PacketExtension> childExtensions
                                = new ArrayList<PacketExtension>();

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
            bldr.append(" " + entry.getKey() + "='" + entry.getValue() + "'");
        }

        //add child elements if any
        List<? extends PacketExtension> childElements = getChildExtensions();

        if(childElements == null || childElements.size() == 0)
        {
            bldr.append("/>");
            return bldr.toString();
        }
        else
        {
            bldr.append(">");
            for(PacketExtension packExt : childElements)
            {
                bldr.append(packExt.toXML());
            }
        }

        //text content if any
        if(getText() != null && getText().trim().length() > 0)
            bldr.append(getText());

        bldr.append("</"+getElementName()+">");

        return bldr.toString();
    }

    /**
     * Returns all sub-elements for this <tt>AbstractPacketExtension</tt> or
     * <tt>null</tt> if there aren't any.
     * <p>
     * Overriding extensions may need to override this method if they would like
     * to have anything more elaborate than just a list of extensions.
     *
     * @return the {@link List} of elements that this packet extension contains.
     */
    public List<? extends PacketExtension> getChildExtensions()
    {
        return childExtensions;
    }

    /**
     * Adds the specified <tt>childExtension</tt> to the list of extensions
     * registered with this packet.
     * <p/>
     * Overriding extensions may need to override this method if they would like
     * to have anything more elaborate than just a list of extensions (e.g.
     * casting separate instances to more specific.
     *
     * @param childExtension the extension we'd like to add here.
     */
    public void addChildExtension(PacketExtension childExtension)
    {
        childExtensions.add(childExtension);
    }

    /**
     * Sets the value of the attribute named <tt>name</tt> to <tt>value</tt>.
     *
     * @param name the name of the attribute that we are setting.
     * @param value an {@link Object} whose <tt>toString()</tt> method returns
     * the XML value of the attribute we are setting or <tt>null</tt> if we'd
     * like to remove the attribute with the specified <tt>name</tt>.
     */
    public void setAttribute(String name, Object value)
    {
        synchronized(attributes)
        {
            if(value != null)
                this.attributes.put(name, value);
            else
                this.attributes.remove(name);
        }
    }

    /**
     * Removes the attribute with the specified <tt>name</tt> from the list of
     * attributes registered with this packet extension.
     *
     * @param name the name of the attribute that we are removing.
     */
    public void removeAttribute(String name)
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
    public Object getAttribute(String attribute)
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
    public String getAttributeAsString(String attribute)
    {
        synchronized(attributes)
        {
            Object attributeVal = attributes.get(attribute);

            return attributeVal == null ? null : attributeVal.toString();
        }
    }

    /**
     * Returns the <tt>int</tt> value of the attribute with the specified
     * <tt>name</tt>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     *
     * @return the <tt>int</tt> value of the specified <tt>attribute</tt> or
     * <tt>-1</tt> if no such attribute is currently registered with this
     * extension.
     *
     * @throws ClassCastException if <tt>attribute</tt> is not registered as an
     * <tt>int</tt>.
     */
    public int getAttributeAsInt(String attribute)
        throws ClassCastException
    {
        synchronized(attributes)
        {
            Object attributeVal = attributes.get(attribute);

            return attributeVal == null
                ? -1
                : ((Integer)attributeVal).intValue();
        }
    }

    /**
     * Specifies the text content of this extension.
     *
     * @param text the text content of this extension.
     */
    public void setText(String text)
    {
        this.textContent = text;
    }

    /**
     * Returns the text content of this extension or <tt>null</tt> if no text
     * content has been specified so far.
     *
     * @return the text content of this extension or <tt>null</tt> if no text
     * content has been specified so far.
     */
    public String getText()
    {
        return textContent;
    }


}
