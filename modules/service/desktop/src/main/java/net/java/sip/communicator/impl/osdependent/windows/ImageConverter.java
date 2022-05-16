/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2000-2016 JetBrains s.r.o.
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package net.java.sip.communicator.impl.osdependent.windows;

import java.awt.image.*;
import java.nio.*;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef.*;

/**
 * Image conversion utilities.
 * 
 * Parts of this code are based on AppIcon.java from IntelliJ community.
 * Licensed under Apache 2.0, Copyright 2000-2016 JetBrains s.r.o.
 */
public class ImageConverter
{
    /**
     * Converts the <tt>BufferedImage</tt> to an ICONDIR structure.
     * @param src The source image to convert
     * @return an ICONDIR structure with the data of the passed source image
     */
    public static byte[] writeTransparentIcoImage(BufferedImage src)
    {
        int bitCount = 32;

        int scanline_size = (bitCount * src.getWidth() + 7) / 8;
        if ((scanline_size % 4) != 0)
            scanline_size += 4 - (scanline_size % 4); // pad scanline to 4 byte
                                                      // size.
        int t_scanline_size = (src.getWidth() + 7) / 8;
        if ((t_scanline_size % 4) != 0)
            t_scanline_size += 4 - (t_scanline_size % 4); // pad scanline to 4
                                                          // byte size.
        int imageSize = 40 + src.getHeight() * scanline_size
            + src.getHeight() * t_scanline_size;

        //   sizeof(ICONDIR)
        // + sizeof(ICONDIRENTRY)
        // + (sizeof(BITMAPINFOHEADER)+data)
        ByteBuffer bos = ByteBuffer.allocate(6 + 16 + imageSize);
        bos.order(ByteOrder.LITTLE_ENDIAN);

        // ICONDIR
        bos.putShort((short) 0); // reserved
        bos.putShort((short) 1); // 1=ICO, 2=CUR
        bos.putShort((short) 1); // count

        // ICONDIRENTRY
        int iconDirEntryWidth = src.getWidth();
        int iconDirEntryHeight = src.getHeight();
        if (iconDirEntryWidth > 255 || iconDirEntryHeight > 255)
        {
            iconDirEntryWidth = 0;
            iconDirEntryHeight = 0;
        }
        bos.put((byte) iconDirEntryWidth);
        bos.put((byte) iconDirEntryHeight);
        bos.put((byte) 0);
        bos.put((byte) 0); // reserved
        bos.putShort((short) 1); // color planes
        bos.putShort((short) bitCount);
        bos.putInt(imageSize);
        bos.putInt(22); // image offset

        // BITMAPINFOHEADER
        bos.putInt(40); // size
        bos.putInt(src.getWidth());
        bos.putInt(2 * src.getHeight());
        bos.putShort((short) 1); // planes
        bos.putShort((short) bitCount);
        bos.putInt(0); // compression
        bos.putInt(0); // image size
        bos.putInt(0); // x pixels per meter
        bos.putInt(0); // y pixels per meter
        bos.putInt(0); // colors used, 0 = (1 << bitCount) (ignored)
        bos.putInt(0); // colors important

        int bit_cache = 0;
        int bits_in_cache = 0;
        int row_padding = scanline_size - (bitCount * src.getWidth() + 7) / 8;
        for (int y = src.getHeight() - 1; y >= 0; y--)
        {
            for (int x = 0; x < src.getWidth(); x++)
            {
                int argb = src.getRGB(x, y);

                bos.put((byte) (0xff & argb));
                bos.put((byte) (0xff & (argb >> 8)));
                bos.put((byte) (0xff & (argb >> 16)));
                bos.put((byte) (0xff & (argb >> 24)));
            }

            for (int x = 0; x < row_padding; x++)
                bos.put((byte) 0);
        }

        int t_row_padding = t_scanline_size - (src.getWidth() + 7) / 8;
        for (int y = src.getHeight() - 1; y >= 0; y--)
        {
            for (int x = 0; x < src.getWidth(); x++)
            {
                int argb = src.getRGB(x, y);
                int alpha = 0xff & (argb >> 24);
                bit_cache <<= 1;
                if (alpha == 0)
                    bit_cache |= 1;
                bits_in_cache++;
                if (bits_in_cache >= 8)
                {
                    bos.put((byte) (0xff & bit_cache));
                    bit_cache = 0;
                    bits_in_cache = 0;
                }
            }

            if (bits_in_cache > 0)
            {
                bit_cache <<= (8 - bits_in_cache);
                bos.put((byte) (0xff & bit_cache));
                bit_cache = 0;
                bits_in_cache = 0;
            }

            for (int x = 0; x < t_row_padding; x++)
                bos.put((byte) 0);
        }

        byte[] result = new byte[bos.position()];
        System.arraycopy(bos.array(), 0, result, 0, bos.position());
        return result;
    }

    /**
     * Converts an ICONDIR ico to an HICON handle
     * @param ico the image data
     * @return A Windows HICON handle
     */
    public static HICON createIcon(byte[] ico)
    {
        Memory buffer = new Memory(ico.length);
        buffer.write(0, ico, 0, ico.length);
        int nSize = 100;
        int offset = User32Ex.INSTANCE.LookupIconIdFromDirectoryEx(buffer, true,
            nSize, nSize, 0);
        if (offset != 0)
        {
            return User32Ex.INSTANCE.CreateIconFromResourceEx(
                buffer.share(offset), new WinDef.DWORD(0), true,
                new WinDef.DWORD(0x00030000), nSize, nSize, 0);
        }

        return null;
    }
}
