/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;

public class SIPCommImageView extends ImageView implements ImageObserver {

    // --- Attribute Values ------------------------------------------
    
    public static final String
    	TOP = "top",
    	TEXTTOP = "texttop",
    	MIDDLE = "middle",
    	ABSMIDDLE = "absmiddle",
    	CENTER = "center",
    	BOTTOM = "bottom";
    

    // --- Construction ----------------------------------------------

    /**
     * Creates a new view that represents an IMG element.
     *
     * @param elem the element to create a view for
     */
    public SIPCommImageView(Element elem) {
    	
    	super(elem);
    	
    	initialize(elem);
	
    	StyleSheet sheet = getStyleSheet();
    	
    	attr = sheet.getViewAttributes(this);
    }
    
    
    private void initialize( Element elem ) {
		
    	synchronized(this) {
		    loading = true;
		    fWidth = fHeight = 0;
		}
    	
		int width = 0;
		int height = 0;
		boolean customWidth = false;
		boolean customHeight = false;
		
		try {
		    fElement = elem;
	        
		    // Request image from document's cache:
		    AttributeSet attr = elem.getAttributes();
	        
		    if (isURL()) {
		    
		    	URL src = getSourceURL();
		      
		    	if( src != null ) {
			  
		    		Dictionary cache = (Dictionary) getDocument().getProperty(IMAGE_CACHE_PROPERTY);
			  
		    		if( cache != null )
		    			fImage = (Image) cache.get(src);
		    		else
		    			fImage = Toolkit.getDefaultToolkit().getImage(src);
				  	}
	            }
	            else {
	
	              /******** Code to load from relative path *************/
	            	String src =
	            		(String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
	            		            		            	
	            	//fImage = Toolkit.getDefaultToolkit().createImage(src);
	            	try {
	            		fImage = ImageIO.read(ImageLoader.class
                                .getClassLoader().getResource(src));
	            	    
		            	try { waitForImage(); }
		            	catch (InterruptedException e) { fImage = null; }
		            	
	            	} catch (IOException e) {
	        			e.printStackTrace();
	        		} 

	              /******************************************************/
	
	            }
		
		    // Get height/width from params or image or defaults:
		    height = getIntAttr(HTML.Attribute.HEIGHT,-1);
		    
		    customHeight = (height>0);
		    
		    if( !customHeight && fImage != null )
		    	height = fImage.getHeight(this);
		    
		    if( height <= 0 )
		    	height = DEFAULT_HEIGHT;
			
		    width = getIntAttr(HTML.Attribute.WIDTH,-1);
		    
		    customWidth = (width>0);
		    
		    if( !customWidth && fImage != null )
		    	width = fImage.getWidth(this);
		    
		    if( width <= 0 )
		    	width = DEFAULT_WIDTH;
	
		    // Make sure the image starts loading:
		    if( fImage != null )
				if( customWidth && customHeight )
				    Toolkit.getDefaultToolkit().prepareImage(fImage,height,
									     width,this);
				else
				    Toolkit.getDefaultToolkit().prepareImage(fImage,-1,-1,
									     this);
		
		    /********************************************************
	            // Rob took this out. Changed scope of src.
		    if( DEBUG ) {
			if( fImage != null )
			    System.out.println("ImageInfo: new on "+src+
					       " ("+fWidth+"x"+fHeight+")");
			else
			    System.out.println("ImageInfo: couldn't get image at "+
					       src);
			if(isLink())
			    System.out.println("           It's a link! Border = "+
					       getBorder());
			//((AbstractDocument.AbstractElement)elem).dump(System.out,4);
		    }
	            ********************************************************/
		} finally {
			
		    synchronized(this) {
				loading = false;
				
				if (customWidth || fWidth == 0) {
				    fWidth = width;
				}
				if (customHeight || fHeight == 0) {
				    fHeight = height;
				}
		    }
		}
    }

    /** Determines if path is in the form of a URL */
    private boolean isURL() {
        
    	String src =
          (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
        
        return src.toLowerCase().startsWith("file") ||
               src.toLowerCase().startsWith("http");
    }    

    

    /** Added this guy to make sure an image is loaded - ie no broken 
        images. So far its used only for images loaded off the disk (non-URL). 
        It seems to work marvelously. By the way, it does the same thing as
        MediaTracker, but you dont need to know the component its being 
        rendered on. Rob */
    private void waitForImage() throws InterruptedException { 
    	
      int w = fImage.getWidth(this);
      int h = fImage.getHeight(this);

      while (true) {
        int flags = Toolkit.getDefaultToolkit().checkImage(fImage, w, h, this);

        if ( ((flags & ERROR) != 0) || ((flags & ABORT) != 0 ) )
          throw new InterruptedException();
        
        else if ((flags & (ALLBITS | FRAMEBITS)) != 0) 
          return;
        
        Thread.sleep(10);
        //System.out.println("rise and shine...");
      }
    }

    
    /**
     * Fetches the attributes to use when rendering.  This is
     * implemented to multiplex the attributes specified in the
     * model with a StyleSheet.
     */
    public AttributeSet getAttributes() {
    	
    	return attr;
    }

    /** Is this image within a link? */
    boolean isLink( ) {
        //! It would be nice to cache this but in an editor it can change
        // See if I have an HREF attribute courtesy of the enclosing A tag:
	
    	AttributeSet anchorAttr = (AttributeSet)
	    
    	fElement.getAttributes().getAttribute(HTML.Tag.A);
    	
		if (anchorAttr != null) {
		    return anchorAttr.isDefined(HTML.Attribute.HREF);
		}
		return false;
	    }
	    
	    /** Returns the size of the border to use. */
	    int getBorder( ) {
	        return getIntAttr(HTML.Attribute.BORDER, isLink() ?DEFAULT_BORDER :0);
	    }
	    
	    /** Returns the amount of extra space to add along an axis. */
	    int getSpace( int axis ) {
	    	return getIntAttr( axis==X_AXIS ?HTML.Attribute.HSPACE :HTML.Attribute.VSPACE,
	    			   0 );
	    }
	    
	    /** Returns the border's color, or null if this is not a link. */
	    Color getBorderColor( ) {
	    	StyledDocument doc = (StyledDocument) getDocument();
	        return doc.getForeground(getAttributes());
	    }
	    
	    /** Returns the image's vertical alignment. */
	    float getVerticalAlignment( ) {
		String align = (String) fElement.getAttributes().getAttribute(HTML.Attribute.ALIGN);
		if( align != null ) {
		    align = align.toLowerCase();
		    if( align.equals(TOP) || align.equals(TEXTTOP) )
		        return 0.0f;
		    else if( align.equals(this.CENTER) || align.equals(MIDDLE)
						       || align.equals(ABSMIDDLE) )
		        return 0.5f;
		}
		
		return 1.0f;		// default alignment is bottom
    }
    
    boolean hasPixels( ImageObserver obs ) {
        
    	return fImage != null && fImage.getHeight(obs)>0
			      && fImage.getWidth(obs)>0;
    }
    

    /** Return a URL for the image source, 
        or null if it could not be determined. */
    private URL getSourceURL( ) {
	 	
    	String src = (String) fElement.getAttributes().getAttribute(HTML.Attribute.SRC);
	 	if( src==null ) return null;
	
		URL reference = ((HTMLDocument)getDocument()).getBase();
	        try {
	 	    URL u = new URL(reference,src);
		    return u;
	        } catch (MalformedURLException e) {
		    return null;
	        }
    }
    
    /** Look up an integer-valued attribute. <b>Not</b> recursive. */
    private int getIntAttr(HTML.Attribute name, int deflt ) {
    	AttributeSet attr = fElement.getAttributes();
    	if( attr.isDefined(name) ) {		// does not check parents!
    	    int i;
 	    String val = (String) attr.getAttribute(name);
 	    if( val == null )
 	    	i = deflt;
 	    else
 	    	try{
 	            i = Math.max(0, Integer.parseInt(val));
 	    	}catch( NumberFormatException x ) {
 	    	    i = deflt;
 	    	}
	    return i;
	} else
	    return deflt;
    }
    

    /**
     * Establishes the parent view for this view.
     * Seize this moment to cache the AWT Container I'm in.
     */
    public void setParent(View parent) {
	super.setParent(parent);
	fContainer = parent!=null ?getContainer() :null;
	if( parent==null && fComponent!=null ) {
	    fComponent.getParent().remove(fComponent);
	    fComponent = null;
	}
    }

    /** My attributes may have changed. */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
if(DEBUG) System.out.println("ImageView: changedUpdate begin...");
    	super.changedUpdate(e,a,f);
    	float align = getVerticalAlignment();
    	
    	int height = fHeight;
    	int width  = fWidth;
    	
    	initialize(getElement());
    	
    	boolean hChanged = fHeight!=height;
    	boolean wChanged = fWidth!=width;
    	if( hChanged || wChanged || getVerticalAlignment()!=align ) {
    	    if(DEBUG) System.out.println("ImageView: calling preferenceChanged");
    	    getParent().preferenceChanged(this,hChanged,wChanged);
    	}
if(DEBUG) System.out.println("ImageView: changedUpdate end; valign="+getVerticalAlignment());
    }


    // --- Painting --------------------------------------------------------

    /**
     * Paints the image.
     *
     * @param g the rendering surface to use
     * @param a the allocated region to render into
     * @see View#paint
     */
    public void paint(Graphics g, Shape a) {
	Color oldColor = g.getColor();
	fBounds = a.getBounds();
        int border = getBorder();
	int x = fBounds.x + border + getSpace(X_AXIS);
	int y = fBounds.y + border + getSpace(Y_AXIS);
	int width = fWidth;
	int height = fHeight;
	int sel = getSelectionState();
	
	// Make sure my Component is in the right place:
/*
	if( fComponent == null ) {
	    fComponent = new Component() { };
	    fComponent.addMouseListener(this);
	    fComponent.addMouseMotionListener(this);
	    fComponent.setCursor(Cursor.getDefaultCursor());	// use arrow cursor
	    fContainer.add(fComponent);
	}
	fComponent.setBounds(x,y,width,height);
	*/
	// If no pixels yet, draw gray outline and icon:
	if( ! hasPixels(this) ) {
	    g.setColor(Color.lightGray);
	    g.drawRect(x,y,width-1,height-1);
	    g.setColor(oldColor);
	    loadIcons();
	    Icon icon = fImage==null ?sMissingImageIcon :sPendingImageIcon;
	    if( icon != null )
	        icon.paintIcon(getContainer(), g, x, y);
	}
		    
	// Draw image:
	if( fImage != null ) {
	    g.drawImage(fImage,x, y,width,height,this);
	    // Use the following instead of g.drawImage when
	    // BufferedImageGraphics2D.setXORMode is fixed (4158822).

	    //  Use Xor mode when selected/highlighted.
	    //! Could darken image instead, but it would be more expensive.
/*
	    if( sel > 0 )
	    	g.setXORMode(Color.white);
	    g.drawImage(fImage,x, y,
	    		width,height,this);
	    if( sel > 0 )
	        g.setPaintMode();
*/
	}
	
	// If selected exactly, we need a black border & grow-box:
	Color bc = getBorderColor();
	if( sel == 2 ) {
	    // Make sure there's room for a border:
	    int delta = 2-border;
	    if( delta > 0 ) {
	    	x += delta;
	    	y += delta;
	    	width -= delta<<1;
	    	height -= delta<<1;
	    	border = 2;
	    }
	    bc = null;
	    g.setColor(Color.black);
	    // Draw grow box:
	    g.fillRect(x+width-5,y+height-5,5,5);
	}

	// Draw border:
	if( border > 0 ) {
	    if( bc != null ) g.setColor(bc);
	    // Draw a thick rectangle:
	    for( int i=1; i<=border; i++ )
	        g.drawRect(x-i, y-i, width-1+i+i, height-1+i+i);
	    g.setColor(oldColor);
	}
    }

    /** Request that this view be repainted.
        Assumes the view is still at its last-drawn location. */
    protected void repaint( long delay ) {
    	if( fContainer != null && fBounds!=null ) {
	    fContainer.repaint(delay,
	   	      fBounds.x,fBounds.y,fBounds.width,fBounds.height);
    	}
    }
    
    /** Determines whether the image is selected, and if it's the only thing selected.
    	@return  0 if not selected, 1 if selected, 2 if exclusively selected.
    		 "Exclusive" selection is only returned when editable. */
    protected int getSelectionState( ) {
    	int p0 = fElement.getStartOffset();
    	int p1 = fElement.getEndOffset();
	if (fContainer instanceof JTextComponent) {
	    JTextComponent textComp = (JTextComponent)fContainer;
	    int start = textComp.getSelectionStart();
	    int end = textComp.getSelectionEnd();
	    if( start<=p0 && end>=p1 ) {
		if( start==p0 && end==p1 && isEditable() )
		    return 2;
		else
		    return 1;
	    }
	}
    	return 0;
    }
    
    protected boolean isEditable( ) {
    	return fContainer instanceof JEditorPane
    	    && ((JEditorPane)fContainer).isEditable();
    }
    
    /** Returns the text editor's highlight color. */
    protected Color getHighlightColor( ) {
    	JTextComponent textComp = (JTextComponent)fContainer;
    	return textComp.getSelectionColor();
    }

    // --- Progressive display ---------------------------------------------
    
    // This can come on any thread. If we are in the process of reloading
    // the image and determining our state (loading == true) we don't fire
    // preference changed, or repaint, we just reset the fWidth/fHeight as
    // necessary and return. This is ok as we know when loading finishes
    // it will pick up the new height/width, if necessary.
    public boolean imageUpdate( Image img, int flags, int x, int y,
    				int width, int height ) {
    	if( fImage==null || fImage != img )
    	    return false;
    	    
    	// Bail out if there was an error:
        if( (flags & (ABORT|ERROR)) != 0 ) {
            fImage = null;
            repaint(0);
            return false;
        }
        
        // Resize image if necessary:
	short changed = 0;
        if( (flags & ImageObserver.HEIGHT) != 0 )
            if( ! getElement().getAttributes().isDefined(HTML.Attribute.HEIGHT) ) {
		changed |= 1;
            }
        if( (flags & ImageObserver.WIDTH) != 0 )
            if( ! getElement().getAttributes().isDefined(HTML.Attribute.WIDTH) ) {
		changed |= 2;
            }
	synchronized(this) {
	    if ((changed & 1) == 1) {
		fWidth = width;
	    }
	    if ((changed & 2) == 2) {
		fHeight = height;
	    }
	    if (loading) {
		// No need to resize or repaint, still in the process of
		// loading.
		return true;
	    }
	}
        if( changed != 0 ) {
            // May need to resize myself, asynchronously:
            if( DEBUG ) System.out.println("ImageView: resized to "+fWidth+"x"+fHeight);
	    
	    Document doc = getDocument();
	    try {
	      if (doc instanceof AbstractDocument) {
		((AbstractDocument)doc).readLock();
	      }
	      preferenceChanged(this,true,true);
	    } finally {
	      if (doc instanceof AbstractDocument) {
		((AbstractDocument)doc).readUnlock();
	      }
	    }			
	      
	    return true;
        }
	
	// Repaint when done or when new pixels arrive:
	if( (flags & (FRAMEBITS|ALLBITS)) != 0 )
	    repaint(0);
	else if( (flags & SOMEBITS) != 0 )
	    if( sIsInc )
	        repaint(sIncRate);
        
        return ((flags & ALLBITS) == 0);
    }
					  /*        
    /**
     * Static properties for incremental drawing.
     * Swiped from Component.java
     * @see #imageUpdate
     */
    private static boolean sIsInc = true;
    private static int sIncRate = 100;

    // --- Layout ----------------------------------------------------------

    /**
     * Determines the preferred span for this view along an
     * axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return   the span the view would like to be rendered into.
     *           Typically the view is told to render into the span
     *           that is returned, although there is no guarantee.  
     *           The parent may choose to resize or break the view.
     */
    public float getPreferredSpan(int axis) {
//if(DEBUG)System.out.println("ImageView: getPreferredSpan");
        int extra = 2*(getBorder()+getSpace(axis));
		switch (axis) {
		case View.X_AXIS:
		    return fWidth+extra;
		case View.Y_AXIS:
		    return fHeight+extra;
		default:
		    throw new IllegalArgumentException("Invalid axis: " + axis);
		}
    }

    /**
     * Determines the desired alignment for this view along an
     * axis.  This is implemented to give the alignment to the
     * bottom of the icon along the y axis, and the default
     * along the x axis.
     *
     * @param axis may be either X_AXIS or Y_AXIS
     * @return the desired alignment.  This should be a value
     *   between 0.0 and 1.0 where 0 indicates alignment at the
     *   origin and 1.0 indicates alignment to the full span
     *   away from the origin.  An alignment of 0.5 would be the
     *   center of the view.
     */
    public float getAlignment(int axis) {
	switch (axis) {
	case View.Y_AXIS:
	    return getVerticalAlignment();
	default:
	    return super.getAlignment(axis);
	}
    }

    /**
     * Provides a mapping from the document model coordinate space
     * to the coordinate space of the view mapped to it.
     *
     * @param pos the position to convert
     * @param a the allocated region to render into
     * @return the bounding box of the given position
     * @exception BadLocationException  if the given position does not represent a
     *   valid location in the associated document
     * @see View#modelToView
     */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
	int p0 = getStartOffset();
	int p1 = getEndOffset();
	if ((pos >= p0) && (pos <= p1)) {
	    Rectangle r = a.getBounds();
	    if (pos == p1) {
		r.x += r.width;
	    }
	    r.width = 0;
	    return r;
	}
	return null;
    }

    /**
     * Provides a mapping from the view coordinate space to the logical
     * coordinate space of the model.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param a the allocated region to render into
     * @return the location within the model that best represents the
     *  given point of view
     * @see View#viewToModel
     */
    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
	Rectangle alloc = (Rectangle) a;
	if (x < alloc.x + alloc.width) {
	    bias[0] = Position.Bias.Forward;
	    return getStartOffset();
	}
	bias[0] = Position.Bias.Backward;
	return getEndOffset();
    }

    /**
     * Set the size of the view. (Ignored.)
     *
     * @param width the width
     * @param height the height
     */
    public void setSize(float width, float height) {
    	// Ignore this -- image size is determined by the tag attrs and
    	// the image itself, not the surrounding layout!
    }
    
 
    // --- Static icon accessors -------------------------------------------

    private Icon makeIcon(final String gifFile) throws IOException {
        /* Copy resource into a byte array.  This is
         * necessary because several browsers consider
         * Class.getResource a security risk because it
         * can be used to load additional classes.
         * Class.getResourceAsStream just returns raw
         * bytes, which we can convert to an image.
         */
	InputStream resource = SIPCommImageView.class.getResourceAsStream(gifFile);

        if (resource == null) {
            System.err.println(SIPCommImageView.class.getName() + "/" + 
                               gifFile + " not found.");
            return null; 
        }
        BufferedInputStream in = 
            new BufferedInputStream(resource);
        ByteArrayOutputStream out = 
            new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        out.flush();

        buffer = out.toByteArray();
        if (buffer.length == 0) {
            System.err.println("warning: " + gifFile + 
                               " is zero-length");
            return null;
        }
        return new ImageIcon(buffer);
    }

    private void loadIcons( ) {
        try{
            if( sPendingImageIcon == null )
            	sPendingImageIcon = makeIcon(PENDING_IMAGE_SRC);
            if( sMissingImageIcon == null )
            	sMissingImageIcon = makeIcon(MISSING_IMAGE_SRC);
	}catch( Exception x ) {
	    System.err.println("ImageView: Couldn't load image icons");
	}
    }
    
    protected StyleSheet getStyleSheet() {
	HTMLDocument doc = (HTMLDocument) getDocument();
	return doc.getStyleSheet();
    }

    // --- member variables ------------------------------------------------

    private AttributeSet attr;
    private Element   fElement;
    private Image     fImage;
    private int       fHeight,fWidth;
    private Container fContainer;
    private Rectangle fBounds;
    private Component fComponent;
    private Point     fGrowBase;        // base of drag while growing image
    private boolean   fGrowProportionally;	// should grow be proportional?
    /** Set to true, while the receiver is locked, to indicate the reciever
     * is loading the image. This is used in imageUpdate. */
    private boolean   loading;
    
    // --- constants and static stuff --------------------------------

    private static Icon sPendingImageIcon,
    			sMissingImageIcon;
    private static final String
        PENDING_IMAGE_SRC = "icons/image-delayed.gif",  // both stolen from HotJava
        MISSING_IMAGE_SRC = "icons/image-failed.gif";
    
    private static final boolean DEBUG = false;
    
    //$ move this someplace public
    static final String IMAGE_CACHE_PROPERTY = "imageCache";
    
    // Height/width to use before we know the real size:
    private static final int
        DEFAULT_WIDTH = 32,
        DEFAULT_HEIGHT= 32,
    // Default value of BORDER param:      //? possibly move into stylesheet?
        DEFAULT_BORDER=  2;

}
