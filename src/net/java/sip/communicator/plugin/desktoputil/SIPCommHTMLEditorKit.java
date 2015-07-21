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

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.ParagraphView;

/**
 * The <tt>SIPCommHTMLEditorKit</tt> is an <tt>HTMLEditorKit</tt> which uses
 * an extended <tt>ParagraphView</tt>.
 *
 * @author Yana Stamcheva
 */
public class SIPCommHTMLEditorKit extends HTMLEditorKit
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;


    private final JComponent container;

    /**
     * Creates an instance of <tt>SIPCommHTMLEditorKit</tt> by specifying the
     * container, where the editor kit would be used.
     *
     * @param container
     */
    public SIPCommHTMLEditorKit(JComponent container)
    {
        this.container = container;
    }

    /**
     * Returns the extended <tt>HTMLFactory</tt> defined here.
     *
     * @return the extended view factory
     */
    @Override
    public ViewFactory getViewFactory()
    {
        return new HTMLFactoryX();
    }

    /**
     * An extended <tt>HTMLFactory</tt> that uses the <tt>SIPCommImageView</tt>
     * to represent images and the <tt>ParagraphViewX</tt> to represent
     * paragraphs.
     */
    private class HTMLFactoryX extends HTMLFactory
        implements ViewFactory
    {
        @Override
        public View create(Element elem)
        {
            View view = super.create(elem);

            viewCreated(this, view);

            if (view instanceof ParagraphView)
            {
                return new ParagraphViewX(elem);
            }
            else if (view instanceof ComponentView)
            {
                return new MyComponentView(elem);
            }

            return view;
        }
    }

    /**
     * Inform extenders for the view creation.
     * @param view the newly created view.
     */
    protected void viewCreated(ViewFactory factory, View view){}

    /**
     * An extended component view, which provides horizontal and vertical
     * filling.
     */
    private class MyComponentView extends ComponentView
    {
        /**
         * Creates a new ComponentView object.
         *
         * @param elem the element to decorate
         */
        public MyComponentView(Element elem)
        {
            super(elem);
        }

        /**
         * Determines the preferred span for this view along an
         * axis.  This is implemented to return the value
         * returned by Component.getPreferredSize along the
         * axis of interest.
         *
         * @param axis may be either View.X_AXIS or View.Y_AXIS
         * @return   the span the view would like to be rendered into >= 0.
         *           Typically the view is told to render into the span
         *           that is returned, although there is no guarantee.
         *           The parent may choose to resize or break the view.
         * @exception IllegalArgumentException for an invalid axis
         */
        @Override
        public float getPreferredSpan(int axis)
        {
            if ((axis != X_AXIS) && (axis != Y_AXIS))
            {
                throw new IllegalArgumentException("Invalid axis: " + axis);
            }
            if (getComponent() != null)
            {
                Dimension size = getComponent().getPreferredSize();
                if (axis == View.X_AXIS)
                {
                    return container.getWidth();
                }
                else
                {
                    return size.height;
                }
            }
            return 0;
        }

        /**
         * Determines the maximum span for this view along an
         * axis.  This is implemented to return the value
         * returned by Component.getMaximumSize along the
         * axis of interest.
         *
         * @param axis may be either View.X_AXIS or View.Y_AXIS
         * @return   the span the view would like to be rendered into >= 0.
         *           Typically the view is told to render into the span
         *           that is returned, although there is no guarantee.
         *           The parent may choose to resize or break the view.
         * @exception IllegalArgumentException for an invalid axis
         */
        @Override
        public float getMaximumSpan(int axis)
        {
            if ((axis != X_AXIS) && (axis != Y_AXIS))
            {
                throw new IllegalArgumentException("Invalid axis: " + axis);
            }
            if (getComponent() != null)
            {
                Dimension size = getComponent().getMaximumSize();
                if (axis == View.X_AXIS)
                {
                    return container.getWidth();
                }
                else
                {
                    return size.height;
                }
            }
            return 0;
        }
    }

    /**
     * The <tt>ParagraphViewX</tt> is created in order to solve the following
     * problem (Bug ID: 4855207):
     * <p>
     * When a paragraph in a JTextPane has a large amount of text the
     * processing needed to layout the entire paragraph increases as the
     * paragraph grows.
     */
    static class ParagraphViewX extends ParagraphView
    {
        /**
         * Creates an instance of <tt>ParagraphViewX</tt>.
         *
         * @param elem the element that this view is responsible for
         */
        public ParagraphViewX(Element elem)
        {
            super(elem);
        }

        /**
         * Calculate requirements along the minor axis.  This
         * is implemented to forward the request to the logical
         * view by calling getMinimumSpan, getPreferredSpan, and
         * getMaximumSpan on it.
         *
         * @param axis the axis, for which we calculate size requirements
         * @param sizeRequirements the initial size requirements
         * @return the recalculated size requirements for the given axis
         */
        @Override
        protected SizeRequirements calculateMinorAxisRequirements (
                int axis, SizeRequirements sizeRequirements)
        {
            if (sizeRequirements == null)
            {
                sizeRequirements = new SizeRequirements();
            }

            float pref = layoutPool.getPreferredSpan(axis);
            float min = layoutPool.getMinimumSpan(axis);

            // Don't include insets, Box.getXXXSpan will include them.
            sizeRequirements.minimum = (int)min;
            sizeRequirements.preferred
                = Math.max(sizeRequirements.minimum, (int) pref);
            sizeRequirements.maximum = Short.MAX_VALUE;
            sizeRequirements.alignment = 0.5f;
            return sizeRequirements;
        }
    }
}
