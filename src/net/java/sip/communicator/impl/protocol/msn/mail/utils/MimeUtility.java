/*
 * MimeUtility.java
 * Copyright (C) 2002, 2004, 2005 The Free Software Foundation
 *
 * This file is part of GNU JavaMail, a library.
 *
 * GNU JavaMail is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * GNU JavaMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * As a special exception, if you link this library with other files to
 * produce an executable, this library does not by itself cause the
 * resulting executable to be covered by the GNU General Public License.
 * This exception does not however invalidate any other reasons why the
 * executable file might be covered by the GNU General Public License.
 */

package net.java.sip.communicator.impl.protocol.msn.mail.utils;

import java.io.*;
import java.util.*;

/**
 * This is a utility class providing micellaneous MIME-related functionality.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.4
 */
public class MimeUtility
{

  /*
   * Uninstantiable.
   */
  private MimeUtility()
  {
  }

  /**
   * Decodes headers that are defined as '*text' in RFC 822.
   * @param etext the possibly encoded value
   * @return decoded text
   * @exception UnsupportedEncodingException if the charset conversion failed
   */
  public static String decodeText(String etext)
    throws UnsupportedEncodingException
  {
    String delimiters = "\t\n\r ";
    if (etext.indexOf("=?") == -1)
      {
        return etext;
      }
    StringTokenizer st = new StringTokenizer(etext, delimiters, true);
    StringBuffer buffer = new StringBuffer();
    StringBuffer extra = new StringBuffer();
    boolean decoded = false;
    while (st.hasMoreTokens())
      {
        String token = st.nextToken();
        char c = token.charAt(0);
        if (delimiters.indexOf(c) > -1)
          {
            extra.append(c);
          }
        else
          {
            try
              {
                token = decodeWord(token);
                if (!decoded && extra.length() > 0)
                  {
                    buffer.append(extra);
                  }
                decoded = true;
              }
            catch (Exception e)
            {
                if (extra.length() > 0)
                {
                    buffer.append(extra);
                }
                decoded = false;
            }
            buffer.append(token);
            extra.setLength(0);
          }
      }
    return buffer.toString();
  }

  /**
   * Decodes the specified string using the RFC 2047 rules for parsing an
   * "encoded-word".
   * @param text the possibly encoded value
   * @return decoded word
   * @exception Exception if the string is not an encoded-word
   * @exception UnsupportedEncodingException if the decoding failed
   */
  public static String decodeWord(String text)
    throws Exception, UnsupportedEncodingException
  {
        if (!text.startsWith("=?"))
        {
            throw new Exception();
        }
        int start = 2;
        int end = text.indexOf('?', start);
        if (end < 0)
        {
            throw new Exception();
        }
        String charset = text.substring(start, end);
        // Allow for RFC2231 language
        int si = charset.indexOf('*');
        if (si != -1)
        {
            charset = charset.substring(0, si);
        }

        start = end + 1;
        end = text.indexOf('?', start);
        if (end < 0)
        {
            throw new Exception();
        }
        String encoding = text.substring(start, end);
        start = end + 1;
        end = text.indexOf("?=", start);
        if (end < 0)
        {
            throw new Exception();
        }
        text = text.substring(start, end);
        try
        {
            // The characters in the remaining string must all be 7-bit clean.
            // Therefore it is safe just to copy them verbatim into a byte array.
            char[] chars = text.toCharArray();
            int len = chars.length;
            byte[] bytes = new byte[len];
            for (int i = 0; i < len; i++)
            {
                bytes[i] = (byte) chars[i];
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            InputStream is;
            if (encoding.equalsIgnoreCase("B"))
            {
                is = new Base64InputStream(bis);
            }
            else if (encoding.equalsIgnoreCase("Q"))
            {
                is = new QInputStream(bis);
            }
            else
            {
                throw new UnsupportedEncodingException("Unknown encoding: " +
                                                    encoding);
            }
            len = bis.available();
            bytes = new byte[len];
            len = is.read(bytes, 0, len);
            String ret = new String(bytes, 0, len, charset);
            if (text.length() > end + 2)
            {
                String extra = text.substring(end + 2);

                ret = ret + extra;
            }
            return ret;
        }
        catch (IOException e)
        {
            throw new Exception();
        }
        catch (IllegalArgumentException e)
        {
            throw new UnsupportedEncodingException();
        }
    }
}
