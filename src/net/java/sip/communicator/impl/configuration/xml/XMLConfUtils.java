/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
     * @param path an array of <tt>String</tt> elements which represents the
     * path to be created. Each element of <tt>path</tt> up to and including the
     * index <code>pathLength - 1</code> must be valid XML (element) names
     * @param pathLength the length of the specified <tt>path</tt>
     * @return the component at the end of the newly created path.
     */
    public static Element createLastPathComponent(
            Document doc,
            String[] path, int pathLength)
    {
        if (doc == null)
            throw new IllegalArgumentException("doc must not be null");
        if (path == null)
            throw new IllegalArgumentException("path must not be null");

        Element parent = (Element)doc.getFirstChild();

        if (parent == null)
            throw new IllegalArgumentException("parentmust not be null");

        Element e = parent;

        for (int i = 0; i < pathLength; i++)
        {
            String pathEl = path[i];
            Element newEl = findChild(e, pathEl);

            if (newEl == null)
            {
                newEl = doc.createElement(pathEl);
                e.appendChild(newEl);
            }
            e = newEl;
        }
        return e;
    }
}
