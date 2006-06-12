/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import javax.swing.SizeRequirements;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.InlineView;
import javax.swing.text.html.ParagraphView;
import javax.swing.text.html.StyleSheet;

/**
 * The SIPCommHTMLEditorKit is a custom HTMLEditorKit which defines its own
 * image view in the html document. The image view used to represent the image
 * is the SIPCommImageView.
 * 
 * @author Yana Stamcheva
 */
public class SIPCommHTMLEditorKit extends HTMLEditorKit {

    public ViewFactory getViewFactory() {
        return new HTMLFactoryX();
    }

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
    
    static class ParagraphViewX extends ParagraphView {
        public ParagraphViewX(Element elem) {
            super(elem);
        }
        
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
     * @return the model
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
