/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration.xml;

import org.w3c.dom.*;

import net.java.sip.communicator.util.xml.*;

/**
 * Common XML Tasks.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class XMLConfUtils extends XMLUtils
{

    /**
     * Returns the element which is at the end of the specified
     * String chain.  <great...grandparent>...<grandparent>.<parent>.<child>
     * @param parent the xml element that is the parent of the root of this
     * chain.
     * @param chain a String array containing the names of all the child's
     * parent nodes.
     * @return the node represented by the specified chain
     */
    public static Element getChildElementByChain(Element parent,
                                                 String[] chain)
    {
        if(chain == null)
            return null;
        Element e = parent;
        for(int i=0; i<chain.length; i++)
        {
            if(e == null)
                return null;
            e = findChild(e, chain[i]);
        }
        return e;
    }

    /**
     * Creates (only if necessary) and returns the element which is at the end
     * of the specified path.
     * @param doc the target document where the specified path should be created
     * @param path a dot separated string indicating the path to be created
     * @return the component at the end of the newly created path.
     */
    public static Element createLastPathComponent(Document doc, String[] path)
    {
        Element parent = (Element)doc.getFirstChild();
        if(   path   == null
           || parent == null
           || doc   == null)
            throw new IllegalArgumentException(
                "Document parent and path must not be null");

        Element e = parent;
        for(int i=0; i < path.length; i++)
        {
            Element newEl = findChild(e, path[i]);
            if(newEl == null)
            {
                newEl = doc.createElement(path[i]);
                e.appendChild(newEl);
            }
            e = newEl;
        }
        return e;
    }
}
