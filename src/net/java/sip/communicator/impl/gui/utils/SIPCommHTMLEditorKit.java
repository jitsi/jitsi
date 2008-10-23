/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

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
     * Returns the extended <tt>HTMLFactory</tt> defined here.
     */
    public ViewFactory getViewFactory()
    {
        return new HTMLFactoryX();
    }

    /**
     * An extended <tt>HTMLFactory</tt> that uses the <tt>SIPCommImageView</tt>
     * to represent images and the <tt>ParagraphViewX</tt> to represent
     * paragraphs.
     */
    static class HTMLFactoryX extends HTMLFactory
        implements ViewFactory
    {
        public View create(Element elem)
        {
            View view = super.create(elem);

            if (view instanceof ParagraphView)
            {
                return new ParagraphViewX(elem);
            }

            return view;
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
        public ParagraphViewX(Element elem)
        {
            super(elem);
        }

        /**
         * Calculate equirements along the minor axis.  This
         * is implemented to forward the request to the logical
         * view by calling getMinimumSpan, getPreferredSpan, and
         * getMaximumSpan on it.
         */
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


    /**
     * Create an uninitialized text storage model
     * that is appropriate for this type of editor.
     *
     * @return the model.
     */
    public Document createDefaultDocument()
    {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();

        ss.addStyleSheet(styles);

        HTMLDocument doc = new HTMLDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);

        return doc;
    }
}
