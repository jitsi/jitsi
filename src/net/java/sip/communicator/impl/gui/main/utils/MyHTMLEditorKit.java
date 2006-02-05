/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.utils;

import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

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

}
