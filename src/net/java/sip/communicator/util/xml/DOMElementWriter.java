package net.java.sip.communicator.util.xml;

/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * The code in this class was borrowed from the ant libs and included the
 * following copyright notice:

 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import java.io.*;

import org.w3c.dom.*;

/**
 * Writes a DOM tree to a given Writer.
 *
 * <p>Utility class used by net.java.sip.communicator.util.xml.XMLUtils
 * and the net.java.sip.communicator.slick.runner.SipCommunicatorSlickRunner.
 * </p>
 *
 */
public class DOMElementWriter {

    private static String lSep = System.getProperty("line.separator");

    /**
     * Don't try to be too smart but at least recognize the predefined
     * entities.
     */
    protected String[] knownEntities = {"gt", "amp", "lt", "apos", "quot"};


    /**
     * Writes a DOM tree to a stream in UTF8 encoding. Note that
     * it prepends the &lt;?xml version='1.0' encoding='UTF-8'?&gt;.
     * The indent number is set to 0 and a 2-space indent.
     * @param root the root element of the DOM tree.
     * @param out the outputstream to write to.
     * @throws IOException if an error happens while writing to the stream.
     */
    public void write(Element root, OutputStream out)
        throws IOException
    {
        Writer wri = new OutputStreamWriter(out, "UTF-8");
        wri.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lSep);
        write(root, wri, 0, "  ");
        wri.flush();
    }

    /**
     * Writes a DOM tree to a stream.
     *
     * @param element the Root DOM element of the tree
     * @param out where to send the output
     * @param indent number of
     * @param indentWith string that should be used to indent the corresponding tag.
     * @throws IOException if an error happens while writing to the stream.
     */
    public void write(Node element, Writer out, int indent,
                      String indentWith)
        throws IOException
    {
        // Write indent characters
        for (int i = 0; i < indent; i++) {
            out.write(indentWith);
        }

        if(element.getNodeType() == Node.COMMENT_NODE)
        {
            out.write("<!--");
            out.write(encode(element.getNodeValue()));
            out.write("-->");
        }
        else
        {
            // Write element
            out.write("<");
            out.write(((Element)element).getTagName());

            // Write attributes
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++)
            {
                Attr attr = (Attr) attrs.item(i);
                out.write(" ");
                out.write(attr.getName());
                out.write("=\"");
                out.write(encode(attr.getValue()));
                out.write("\"");
            }
            out.write(">");
        }
        // Write child elements and text
        boolean hasChildren = false;
        NodeList children = element.getChildNodes();
        for (int i = 0
             ; element.hasChildNodes()
               && i < children.getLength()
             ; i++)
        {
            Node child = children.item(i);

            switch (child.getNodeType()) {

            case Node.ELEMENT_NODE: case Node.COMMENT_NODE:
                if (!hasChildren) {
                    out.write(lSep);
                    hasChildren = true;
                }
                write(child, out, indent + 1, indentWith);
                break;

            case Node.TEXT_NODE:
                //if this is a new line don't print it as we print our own.
                if(child.getNodeValue() != null
                   && (   child.getNodeValue().indexOf("\n") == -1
                       || child.getNodeValue().trim().length() != 0))
                    out.write(encode(child.getNodeValue()));
                break;
            case Node.CDATA_SECTION_NODE:
                out.write("<![CDATA[");
                out.write(encodedata(((Text) child).getData()));
                out.write("]]>");
                break;

            case Node.ENTITY_REFERENCE_NODE:
                out.write('&');
                out.write(child.getNodeName());
                out.write(';');
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                out.write("<?");
                out.write(child.getNodeName());
                String data = child.getNodeValue();
                if (data != null && data.length() > 0) {
                    out.write(' ');
                    out.write(data);
                }
                out.write("?>");
                break;
            }
        }

        // If we had child elements, we need to indent before we close
        // the element, otherwise we're on the same line and don't need
        // to indent
        if (hasChildren) {
            for (int i = 0; i < indent; i++) {
                out.write(indentWith);
            }
        }

        // Write element close
        if(element.getNodeType() == Node.ELEMENT_NODE)
        {
            out.write("</");
            out.write(((Element)element).getTagName());
            out.write(">");
        }

        out.write(lSep);
        out.flush();
    }

    /**
     * Escape &lt;, &gt; &amp; &apos;, &quot; as their entities and
     * drop characters that are illegal in XML documents.
     *
     * @param value the value to encode
     *
     * @return a String containing the encoded element.
     */
    public String encode(String value) {
        StringBuffer sb = new StringBuffer();
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            case '\"':
                sb.append("&quot;");
                break;
            case '&':
                int nextSemi = value.indexOf(";", i);
                if (nextSemi < 0
                    || !isReference(value.substring(i, nextSemi + 1))) {
                    sb.append("&amp;");
                } else {
                    sb.append('&');
                }
                break;
            default:
                if (isLegalCharacter(c)) {
                    sb.append(c);
                }
                break;
            }
        }
        return sb.substring(0);
    }

    /**
     * Drop characters that are illegal in XML documents.
     *
     * <p>Also ensure that we are not including an <tt>]]&gt;</tt>
     * marker by replacing that sequence with
     * <tt>&amp;#x5d;&amp;#x5d;&amp;gt;</tt>.</p>
     *
     * <p>See XML 1.0 2.2 <a
     * href="http://www.w3.org/TR/1998/REC-xml-19980210#charsets">http://www.w3.org/TR/1998/REC-xml-19980210#charsets</a> and
     * 2.7 <a
     * href="http://www.w3.org/TR/1998/REC-xml-19980210#sec-cdata-sect">http://www.w3.org/TR/1998/REC-xml-19980210#sec-cdata-sect</a>.</p>
     *
     * @param value the value to encode
     *
     * @return a String containing the encoded value.
     */
    public String encodedata(final String value) {
        StringBuffer sb = new StringBuffer();
        int len = value.length();
        for (int i = 0; i < len; ++i) {
            char c = value.charAt(i);
            if (isLegalCharacter(c)) {
                sb.append(c);
            }
        }

        String result = sb.substring(0);
        int cdEnd = result.indexOf("]]>");
        while (cdEnd != -1) {
            sb.setLength(cdEnd);
            sb.append("&#x5d;&#x5d;&gt;")
                .append(result.substring(cdEnd + 3));
            result = sb.substring(0);
            cdEnd = result.indexOf("]]>");
        }

        return result;
    }

    /**
     * Is the given argument a character or entity reference?
     *
     *
     * @param ent the string whose nature we need to determine.
     *
     * @return true if ent is an entity reference and false otherwise.
     */
    public boolean isReference(String ent) {
        if (!(ent.charAt(0) == '&') || !ent.endsWith(";")) {
            return false;
        }

        if (ent.charAt(1) == '#') {
            if (ent.charAt(2) == 'x') {
                try {
                    Integer.parseInt(ent.substring(3, ent.length() - 1), 16);
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            } else {
                try {
                    Integer.parseInt(ent.substring(2, ent.length() - 1));
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
        }

        String name = ent.substring(1, ent.length() - 1);
        for (int i = 0; i < knownEntities.length; i++) {
            if (name.equals(knownEntities[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is the given character allowed inside an XML document?
     *
     * <p>See XML 1.0 2.2 <a
     * href="http://www.w3.org/TR/1998/REC-xml-19980210#charsets">
     * http://www.w3.org/TR/1998/REC-xml-19980210#charsets</a>.</p>
     *
     * @since 1.10, Ant 1.5
     *
     * @param c the character hose nature we'd like to determine.
     *
     * @return true if c is a legal character and false otherwise
     */
    public boolean isLegalCharacter(char c) {
        if (c == 0x9 || c == 0xA || c == 0xD) {
            return true;
        } else if (c < 0x20) {
            return false;
        } else if (c <= 0xD7FF) {
            return true;
        } else if (c < 0xE000) {
            return false;
        } else if (c <= 0xFFFD) {
            return true;
        }
        return false;
    }
}
