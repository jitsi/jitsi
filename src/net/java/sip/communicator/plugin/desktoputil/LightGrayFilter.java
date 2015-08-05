/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
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
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;

/**
 * An image filter that "disables" an image by turning
 * it into a grayscale image, and brightening the pixels
 * in the image. Used by buttons to create an image for
 * a disabled button. Creates a more brighter image than
 * the javax.swing.GrayFilter.
 *
 * @author Yana Stamcheva
 */
public class LightGrayFilter extends GrayFilter
{
    /**
     * Creates an instance of a LightGrayFilter.
     * @param b  a boolean -- true if the pixels should be brightened
     * @param p  an int in the range 0..100 that determines the percentage
     *           of gray, where 100 is the darkest gray, and 0 is the lightest
     */
    public LightGrayFilter(boolean b, int p)
    {
        super(b, p);
    }

    /**
     * Creates a disabled image.
     * @param i The source image.
     * @return A disabled image based on the source image.
     */
    public static Image createDisabledImage(Image i)
    {
        LightGrayFilter filter = new LightGrayFilter(true, 50);
        ImageProducer prod = new FilteredImageSource(i.getSource(), filter);
        Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);

        return grayImage;
    }
}
