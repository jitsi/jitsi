/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.utils;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class MyHTMLEditorKit extends HTMLEditorKit {
    
	  public ViewFactory getViewFactory() {
		    return new HTMLFactoryX();
	  }

	  public static class HTMLFactoryX extends HTMLFactory
	    implements ViewFactory {
	    
	    public View create(Element elem) {
	    	
	      Object o = 
	        elem.getAttributes().getAttribute(StyleConstants.NameAttribute);
	      
	      if (o instanceof HTML.Tag) {
	    	  
	    	  HTML.Tag kind = (HTML.Tag) o;
	    	  
	    	  if (kind == HTML.Tag.IMG) 
	    		  return new MyImageView(elem);
	      }
	      
	      return super.create( elem );
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
