/*
 * QPInputStream.java
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
 * A Quoted-Printable decoder stream.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 */
public class QPInputStream
  extends FilterInputStream
{

  protected byte[] buf;

  /**
   * The number of times read() will return a space.
   */
  protected int spaceCount;

  private static final int LF = 10;
  private static final int CR = 13;
  private static final int SPACE = 32;
  private static final int EQ = 61;

  /**
   * Constructor.
   * @param in the underlying input stream.
   */
  public QPInputStream(InputStream in)
  {
    super(new PushbackInputStream(in, 2));
    buf = new byte[2];
  }

  /**
   * Read a character from the stream.
   */
  public int read()
    throws IOException
  {
    if (spaceCount>0)
    {
      spaceCount--;
      return SPACE;
    }
    
    int c = in.read();
    if (c==SPACE)
    {
      while ((c = in.read())==SPACE) 
        spaceCount++;
      if (c==LF || c==CR || c==-1)
        spaceCount = 0;
      else
      {
       ((PushbackInputStream)in).unread(c);
        c = SPACE;
      }
      return c;
    }
    if (c==EQ)
    {
      int c2 = super.in.read();
      if (c2==LF)
        return read();
      if (c2==CR)
      {
        int peek = in.read();
        if (peek!=LF)
         ((PushbackInputStream)in).unread(peek);
        return read();
      }
      if (c2==-1)
        return c2;
      
      buf[0] = (byte)c2;
      buf[1] = (byte)in.read();
      try
      {
        return Integer.parseInt(new String(buf, 0, 2), 16);
      }
      catch (NumberFormatException e)
      {
       ((PushbackInputStream)in).unread(buf);
      }
      return c;
    }
    else
      return c;
  }

  /**
   * Reads from the underlying stream into the specified byte array.
   */
  public int read(byte[] bytes, int off, int len)
    throws IOException
  {
    int pos = 0;
    try
    {
      while (pos<len)
      {
        int c = read();
        if (c==-1)
        {
          if (pos==0)
            pos = -1;
          break;
        }
        bytes[off+pos] = (byte)c;
        pos++;
      }

    }
    catch (IOException e)
    {
      pos = -1;
    }
    return pos;
  }

  /**
   * Mark is not supported.
   */
  public boolean markSupported()
  {
    return false;
  }

  /**
   * Returns the number of bytes that can be read without blocking.
   */
  public int available()
    throws IOException
  {
    return in.available();
  }
  
}
