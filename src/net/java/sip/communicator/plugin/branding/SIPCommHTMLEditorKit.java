/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.branding;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.ParagraphView;

/**
 * The <tt>SIPCommHTMLEditorKit</tt> is an <tt>HTMLEditorKit</tt> which uses
 * the <tt>SIPCommImageView</tt> and an extended <tt>ParagraphView</tt>.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommHTMLEditorKit extends HTMLEditorKit {

    /**
     * Returns the extended <tt>HTMLFactory</tt> defined here.
     */
    public ViewFactory getViewFactory() {
        return new HTMLFactoryX();
    }

    /**
     * An extended <tt>HTMLFactory</tt> that uses the <tt>SIPCommImageView</tt>
     * to represent images and the <tt>ParagraphViewX</tt> to represent
     * paragraphs.
     */
    static class HTMLFactoryX extends HTMLFactory
        implements ViewFactory {

        public View create(Element elem) {

            View v=super.create(elem);

            if(v instanceof ImageView){
                return new SIPCommImageView(elem);
            }
            else if (v instanceof ParagraphView) {
                return new ParagraphViewX(elem);
            }            
            return v;
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
    static class ParagraphViewX extends ParagraphView {
        public ParagraphViewX(Element elem) {
            super(elem);
        }
        
        /**
         * Calculate equirements along the minor axis.  This
         * is implemented to forward the request to the logical
         * view by calling getMinimumSpan, getPreferredSpan, and
         * getMaximumSpan on it.
         */
        protected SizeRequirements calculateMinorAxisRequirements (
                int axis, SizeRequirements r) {
            if (r == null) {
                r = new SizeRequirements();
            }
            float pref = layoutPool.getPreferredSpan(axis);
            float min = layoutPool.getMinimumSpan(axis);
            // Don't include insets, Box.getXXXSpan will include them.
            r.minimum = (int)min;
            r.preferred = Math.max(r.minimum, (int) pref);
            r.maximum = Short.MAX_VALUE;
            r.alignment = 0.5f;
            return r;
        }
    }


    /**
     * Create an uninitialized text storage model
     * that is appropriate for this type of editor.
     *
     * @return the model.
     */
    public Document createDefaultDocument() {
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
