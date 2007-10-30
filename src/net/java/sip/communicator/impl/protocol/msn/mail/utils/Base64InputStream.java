/*
 * Base64InputStream.java
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
 * A Base64 content transfer encoding filter stream.
 * <p>
 * From RFC 2045, section 6.8:
 * <p>
 * The Base64 Content-Transfer-Encoding is designed to represent
 * arbitrary sequences of octets in a form that need not be humanly
 * readable.  The encoding and decoding algorithms are simple, but the
 * encoded data are consistently only about 33 percent larger than the
 * unencoded data.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 */
public class Base64InputStream
  extends FilterInputStream
{

  private byte[] buffer;
  private int buflen;
  private int index;
  private byte[] decodeBuf;

  private static final char[] src =
  {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
    'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 
    'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 
    'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', 
    '8', '9', '+', '/'
  };
  private static final byte[] dst;
  
  private static final int LF = 10, CR = 13, EQ = 61;
  
  static
  {
    dst = new byte[256];
    for (int i = 0; i<255; i++)
      dst[i] = -1;
    for (int i = 0; i<src.length; i++)
      dst[src[i]] = (byte)i;
    
  }
  
  /**
   * Constructs an input stream that decodes an underlying Base64-encoded
   * stream.
   * @param in the Base64-encoded stream
   */
  public Base64InputStream(InputStream in)
  {
    super(in);
    decodeBuf = new byte[4];
    buffer = new byte[3];
  }
  
  /**
   * Reads the next byte of data from the input stream.
   */
  public int read()
    throws IOException
  {
    if (index>=buflen)
    {
      decode();
      if (buflen==0)
        return -1;
      index = 0;
    }
    return buffer[index++] & 0xff;
  }
  
  /**
   * Reads up to len bytes of data from the input stream into an array of 
   * bytes.
   */
  public int read(byte[] b, int off, int len)
    throws IOException
  {
    try
    {
      int l = 0;
      for (; l<len; l++)
      {
        int c;
        if ((c=read())==-1)
        {
          if (l==0)
            l = -1;
          break;
        }
        b[off+l] = (byte)c;
      }
      return l;
    }
    catch (IOException e)
    {
      return -1;
    }
  }

  /**
   * Returns the number of bytes that can be read(or skipped over) from this
   * input stream without blocking by the next caller of a method for this 
   * input stream.
   */
  public int available()
    throws IOException
  {
    return (in.available()*3)/4+(buflen-index);
  }
  
  private void decode()
    throws IOException
  {
    buflen = 0;
    int c;
    do
    {
      c = in.read();
      if (c==-1)
        return;
    }
    while (c==LF || c==CR);
    decodeBuf[0] = (byte)c;
    int j = 3, l;
    for (int k=1;(l=in.read(decodeBuf, k, j))!=j; k += l)
    {
      if (l==-1)
        throw new IOException("Base64 encoding error");
      j -= l;
    }
    
    byte b0 = dst[decodeBuf[0] & 0xff];
    byte b2 = dst[decodeBuf[1] & 0xff];
    buffer[buflen++] = (byte)(b0<<2 & 0xfc | b2>>>4 & 0x3);
    if (decodeBuf[2]!=EQ)
    {
      b0 = b2;
      b2 = dst[decodeBuf[2] & 0xff];
      buffer[buflen++] = (byte)(b0<<4 & 0xf0 | b2>>>2 & 0xf);
      if (decodeBuf[3]!=EQ)
      {
        b0 = b2;
        b2 = dst[decodeBuf[3] & 0xff];
        buffer[buflen++] = (byte)(b0<<6 & 0xc0 | b2 & 0x3f);
      }
    }
  }

}
