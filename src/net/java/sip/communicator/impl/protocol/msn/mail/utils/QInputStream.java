/*
 * QInputStream.java
 * Copyright(C) 2002 The Free Software Foundation
 * 
 * This file is part of GNU JavaMail, a library.
 * 
 * GNU JavaMail is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 *(at your option) any later version.
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

/**
 * Provides RFC 2047 "B" transfer encoding.
 * See section 4.2:
 * <p>
 * The "Q" encoding is similar to the "Quoted-Printable" content-
 * transfer-encoding defined in RFC 2045.  It is designed to allow text
 * containing mostly ASCII characters to be decipherable on an ASCII
 * terminal without decoding.
 * <ol>
 * <li>Any 8-bit value may be represented by a "=" followed by two
 * hexadecimal digits.  For example, if the character set in use
 * were ISO-8859-1, the "=" character would thus be encoded as
 * "=3D", and a SPACE by "=20". (Upper case should be used for
 * hexadecimal digits "A" through "F".)
 * <li>The 8-bit hexadecimal value 20(e.g., ISO-8859-1 SPACE) may be
 * represented as "_"(underscore, ASCII 95.). (This character may
 * not pass through some internetwork mail gateways, but its use
 * will greatly enhance readability of "Q" encoded data with mail
 * readers that do not support this encoding.)  Note that the "_"
 * always represents hexadecimal 20, even if the SPACE character
 * occupies a different code position in the character set in use.
 * <li>8-bit values which correspond to printable ASCII characters other
 * than "=", "?", and "_"(underscore), MAY be represented as those
 * characters. (But see section 5 for restrictions.)  In
 * particular, SPACE and TAB MUST NOT be represented as themselves
 * within encoded words.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 */
public class QInputStream
  extends QPInputStream
{

  private static final int SPACE = 32;
  private static final int EQ = 61;
  private static final int UNDERSCORE = 95;

  /**
   * Constructor.
   * @param in the underlying input stream.
   */
  public QInputStream(InputStream in)
  {
    super(in);
  }

  /**
   * Read a character.
   */
  public int read()
    throws IOException
  {
    int c = in.read();
    if (c==UNDERSCORE)
      return SPACE;
    if (c==EQ)
    {
      buf[0] = (byte)in.read();
      buf[1] = (byte)in.read();
      try
      {
        return Integer.parseInt(new String(buf, 0, 2), 16);
      }
      catch (NumberFormatException e)
      {
        throw new IOException("Quoted-Printable encoding error: "+
            e.getMessage());
      }
    }
    return c;
  }

}
