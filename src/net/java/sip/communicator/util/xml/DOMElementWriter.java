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
 */
package net.java.sip.communicator.util.xml;

import java.io.*;

import org.w3c.dom.*;

/**
 * Writes a DOM tree to a given Writer.
 *
 * <p>Utility class used by {@link XMLUtils} and
 * {@link net.java.sip.communicator.slick.runner.SipCommunicatorSlickRunner}.
 * </p>
 *
 * @author Lubomir Marinov
 */
public class DOMElementWriter
{
    /**
     * The system-specific line separator as defined by the well-known system
     * property.
     */
    private static final String lSep = System.getProperty("line.separator");

    /**
     * Decodes an XML (element) name according to
     * http://www.w3.org/TR/xml/#NT-Name.
     *
     * @param name the XML (element) name to be decoded
     * @return a <tt>String</tt> which represents <tt>name</tt> decoded
     * according to http://www.w3.org/TR/xml/#NT-Name
     */
    public static String decodeName(String name)
    {
        int length = name.length();
        StringBuilder value = new StringBuilder(length);

        for (int i = 0; i < length;)
        {
            int start = name.indexOf('_', i);

            /*
             * If there's nothing else to decode, append whatever's left and
             * finish.
             */
            if (start == -1)
            {
                value.append(name, i, length);
                break;
            }

            /*
             * We may have to decode from start (inclusive). Append from i to
             * start (exclusive).
             */
            if (i != start)
                value.append(name, i, start);

            // Determine whether we'll actually decode.
            int end = start + 6 /* xHHHH_ */;

            if ((end < length)
                    && (name.charAt(start + 1) == 'x')
                    && (name.charAt(end) == '_')
                    && isHexDigit(name.charAt(start + 2))
                    && isHexDigit(name.charAt(start + 3))
                    && isHexDigit(name.charAt(start + 4))
                    && isHexDigit(name.charAt(start + 5)))
            {
                char c = (char) Integer.parseInt(name.substring(start + 2, end), 16);

                /*
                 * We've decoded a character. But is it really a character we'd
                 * have encoded in the first place? We don't want to
                 * accidentally decode a string just because it looked like an
                 * encoded character.
                 */
                if ((start == 0) ? !isNameStartChar(c) : !isNameChar(c))
                {
                    value.append(c);
                    i = end + 1;
                    continue;
                }
            }

            // We didn't really have to decode and the string was a literal.
            value.append(name.charAt(start));
            i = start + 1;
        }
        return value.toString();
    }

    /**
     * Encodes a specific <tt>String</tt> so that it is a valid XML (element)
     * name according to http://www.w3.org/TR/xml/#NT-Name.
     *
     * @param value the <tt>String</tt> to be encoded so that it is a valid XML
     * name
     * @return a <tt>String</tt> which represents <tt>value</tt> encoded so that
     * it is a valid XML (element) name
     */
    public static String encodeName(String value)
    {
        int length = value.length();
        StringBuilder name = new StringBuilder();

        for (int i = 0; i < length; i++)
        {
            char c = value.charAt(i);

            if (i == 0)
            {
                if (isNameStartChar(c))
                {
                    name.append(c);
                    continue;
                }
            }
            else if (isNameChar(c))
            {
                name.append(c);
                continue;
            }

            name.append("_x");
            if (c <= 0x000F)
                name.append("000");
            else if (c <= 0x00FF)
                name.append("00");
            else if (c <= 0x0FFF)
                name.append('0');
            name.append(Integer.toHexString(c).toUpperCase());
            name.append('_');
        }
        return name.toString();
    }

    /**
     * Determines whether a specific character represents a hex digit.
     *
     * @param c the character to be checked whether it represents a hex digit
     * @return <tt>true</tt> if the specified character represents a hex digit;
     * otherwise, <tt>false</tt>
     */
    private static boolean isHexDigit(char c)
    {
        return
            (('0' <= c) && (c <= '9'))
                || (('A' <= c) && (c <= 'F'))
                || (('a' <= c) && (c <= 'f'));
    }

    /**
     * Determines whether a specific characters is a <tt>NameChar</tt> as
     * defined by http://www.w3.org/TR/xml/#NT-Name.
     *
     * @param c the character which is to be determines whether it is a
     * <tt>NameChar</tt>
     * @return <tt>true</tt> if the specified character is a <tt>NameChar</tt>;
     * otherwise, <tt>false</tt>
     */
    private static boolean isNameChar(char c)
    {
        if (isNameStartChar(c))
            return true;
        else if ((c == '-') || (c == '.'))
            return true;
        else if (('0' <= c) && (c <= '9'))
            return true;
        else if (c == 0xB7)
            return true;
        else if (c < 0x0300)
            return false;
        else if (c <= 0x036F)
            return true;
        else if (c < 0x203F)
            return false;
        else if (c <= 0x2040)
            return true;
        else
            return false;
    }

    /**
     * Determines whether a specific characters is a <tt>NameStartChar</tt> as
     * defined by http://www.w3.org/TR/xml/#NT-Name.
     *
     * @param c the character to be determined whether it is a
     * <tt>NameStartChar</tt>
     * @return <tt>true</tt> if the specified character is a
     * <tt>NameStartChar</tt>; otherwise, <tt>false</tt>
     */
    private static boolean isNameStartChar(char c)
    {
        if ((c == ':') || (c == '_'))
            return true;
        else if (('A' <= c) && (c <= 'Z'))
            return true;
        else if (('a' <= c) && (c <= 'z'))
            return true;
        else if (c < 0xC0)
            return false;
        else if (c <= 0xD6)
            return true;
        else if (c < 0xD8)
            return false;
        else if (c <= 0xF6)
            return true;
        else if (c < 0xF8)
            return false;
        else if (c <= 0x2FF)
            return true;
        else if (c < 0x370)
            return false;
        else if (c <= 0x37D)
            return true;
        else if (c < 0x37F)
            return false;
        else if (c <= 0x1FFF)
            return true;
        else if (c < 0x200C)
            return false;
        else if (c <= 0x200D)
            return true;
        else if (c < 0x2070)
            return false;
        else if (c <= 0x218F)
            return true;
        else if (c < 0x2C00)
            return false;
        else if (c <= 0x2FEF)
            return true;
        else if (c < 0x3001)
            return false;
        else if (c <= 0xD7FF)
            return true;
        else if (c < 0xF900)
            return false;
        else if (c <= 0xFDCF)
            return true;
        else if (c < 0xFDF0)
            return false;
        else if (c <= 0xFFFD)
            return true;
//        else if (c < 0x10000)
//            return false;
//        else if (c <= 0xEFFFF)
//            return true;
        else
            return false;
    }

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
        for (int i = 0; i < len; i++)
        {
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
                if ((nextSemi < 0)
                        || !isReference(value.substring(i, nextSemi + 1)))
                    sb.append("&amp;");
                else
                    sb.append('&');
                break;
            default:
                if (isLegalCharacter(c))
                    sb.append(c);
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
     * @param ent the string whose nature we need to determine.
     * @return <tt>true</tt> if <tt>ent</tt> is an entity reference and
     * <tt>false</tt> otherwise.
     */
    public boolean isReference(String ent) {
        if (!(ent.charAt(0) == '&') || !ent.endsWith(";"))
            return false;

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
     * @param c the character whose nature we'd like to determine.
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
