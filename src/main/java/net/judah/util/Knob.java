package net.judah.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import lombok.Getter;
import lombok.Setter;

/**<a href="https://github.com/mploof/JKnobFancy">From Michael Ploof on Github</a><br/><br/>
* JFancyKnob.java -
*   A knob component. The knob can be rotated by dragging a
*   spot on the knob around in a circle. The positions of
*   the handles may reported in degrees or radians by getting
*   a handle and calling the appropriate method.
*   <br><br>
*   Setting the max and min position values of the knob
*   limits motion of handles to the range of positions
*   between those two values. Setting the knob directionality
*   determines which arc bounded by those two values is valid
*   for handle movement. For instance, if a min position of 180
*   degrees and max position of 0 degrees is set, setting the
*   knob directionality to CW means the handles can move in the
*   top half of the circular track. Setting the directionality
*   to CCW (i.e. setting CW movement false) means the handles
*   can move in the lower half of the circular track. <b>Be
*   careful when switching the directionality</b>, since handles
*   located in an invalid region will no longer be able to be
*   moved.
*
* @author Michael ploof
*/
public class Knob extends JComponent{
	private static final long serialVersionUID = -3331634859451614043L;

	public static interface KnobListener {
		void knobChanged(int val);
	}

	private static final double DEG_PER_ROT = 360;

	//~~~~~~~~ Knob Value Vars ~~~~~~~~//
	/** Minimum value the knob can report*/
	int minVal;
	/** Maximum value the knob can report */
	int maxVal;
	/** The angular position in degrees at which the knob's minimum value occurs */
	double minValPos;
	/** The angular position in degrees at which the knob's maximum value occurs */
	double maxValPos;
	/** If true, the value of a knob handle will increase as it moves clockwise
	 * from the minValPos, otherwise it will increase as it moves counterclockwise */
	boolean cwDirection;

	//~~~~~~~~ Background Image Vars ~~~~~~~~//
	/** Dimension of the knob's background image */
	Dimension backgroundSize = new Dimension();
	/** Icon used for the knob background */
	ImageIcon backgroundIcon;
	/** The scale by which the background ImageIcon is scaled when it is drawn. */
	float scale;

	//~~~~~~~~ Handle and Track Vars ~~~~~~~~//
	/** Relative radius as a percent (0.0-1.0) of the overall width of the background image */
	protected double relTrackRadius;
	/** Radius of the circle along which the handles will move */
	protected int trackRadius;
	/** Pixel location of the center of the handle track */
	protected Point center;
	/** The handle icon passed to the knob constructor. Different handle icons may be used
	 * for subsequently added handles, but this icon will be used if none is specified. */
	protected ImageIcon defaultHandleIcon;
	/** List containing all handles currently located on the knob object */
	protected List<JKnobHandle> handles = new ArrayList<>();

	//~~~~~~~~ Handle Class~~~~~~~~//
	/** This class describes handle objects that may be positioned
	 * on the JKnobFancy object. */
	public class JKnobHandle {

		/** The handle icon */
		@Setter @Getter ImageIcon icon;
		/** Reference to the knob object on which the handle is located */
		Knob thisKnob;
		/** Radius of clickable handle area */
		int radius;
		/** Handle location in radians */
		private double theta;
		/** Whether the handle is currently clicked */
		@Setter @Getter private boolean pressedOnSpot;

		private JKnobHandle(double theta, ImageIcon icon, Knob thisKnob){
			init(theta, icon, thisKnob);
		}

		private void init(double theta, ImageIcon icon, Knob thisKnob){
			this.theta = theta;
			this.icon = icon;
			this.pressedOnSpot = false;
			this.thisKnob = thisKnob;
			this.radius = this.icon.getIconWidth() / 2;
		}

		/**@param theta the new handle angular position in radians. Use
		 * negative values for positions more than Pi radians from 0.*/
		public void setAngle(double theta) {
			// Only set the new angle if it's within the valid positional range
			double deg = Math.toDegrees(theta);
			deg = deg < 0 ? deg + DEG_PER_ROT : deg;
			if(isInValidRange(deg))
				this.theta = theta;
		}

		/**@return the current angular position of the handle in radians.
		* Values more than Pi radians from 0 are reported as a negative values \
		* relative to 0 (i.e. 3/2 Pi radians would be reported as -1/2 Pi radians).*/
		public double getAngle() {
			return theta;
		}

		/**@param deg the new handle angular position in degrees*/
		public void setAngleDeg(double deg) {
			this.setAngle(Math.toRadians(deg));
		}

		/** @return the current angular position of the handle in degrees.
		* Reported as values from 0-360 degrees (i.e. no negative values). */
		public double getAngleDeg(){
			double tempTheta = theta >= 0 ? theta : 2 * Math.PI + theta;
			double ret = Math.toDegrees(tempTheta);
			return ret;
		}

		private boolean isInValidRange(double newDeg){
			if(thisKnob.getMinPos() == thisKnob.getMaxPos())
				return true;

			double maxTemp = thisKnob.getMaxPos() - thisKnob.getMinPos();
			maxTemp = maxTemp < 0 ? maxTemp + DEG_PER_ROT : maxTemp;
			double handleTemp = newDeg - thisKnob.getMinPos();
			handleTemp = handleTemp < 0 ? handleTemp + DEG_PER_ROT : handleTemp;

			if((thisKnob.isCwDirection() && handleTemp >= maxTemp) || (!thisKnob.isCwDirection() && handleTemp <= maxTemp))
				return true;
			else
				return false;
		}

		 /** Determine if the mouse click was on the spot or
		  * not.  If it was return true, otherwise return false.
		  * @return true if x,y is on the spot and false if not. */
		 private boolean isOnSpot(Point pt) {
			return (pt.distance(getSpotCenter()) < this.radius);
		 }

		 /** Calculate the x, y coordinates of the center of the spot.
		  *  @return a Point containing the x,y position of the center of the spot. */
		 protected Point getSpotCenter() {
			// Calculate the center point of the spot RELATIVE to the
			// center of the of the circle.
			int r = thisKnob.trackRadius - this.radius;

			int xcp = (int)(r * Math.cos(theta));
			int ycp = (int)(r * Math.sin(theta));

			// Adjust the center point of the spot so that it is offset
			// from the center of the circle.  This is necessary because
			// 0,0 is not actually the center of the circle, it is  the
		     // upper left corner of the component!
			int xc = center.x + xcp;
			int yc = center.y - ycp;

			return new Point(xc,yc);
		 }

		 /** Calculate the x, y coordinates of the point on the edge of the
		  * handle's radius that is closes to the center of rotation. This is
		  * useful if drawing a line from the center of the handle's track to
		  * the edge of the handle icon.
		  * @return a Point containing the x,y position of the point on the
		  * 	handle closest to the center of ration */
		 protected Point getCenterEdgePoint(){
			 double tempTheta = this.getAngle();
			 int xOffset = (int)(this.radius * Math.cos(tempTheta));
			 int yOffset = (int)(this.radius * Math.sin(tempTheta));
			 Point center = this.getSpotCenter();
			 return new Point(center.x - xOffset, center.y + yOffset);
		}

		/** @return The radius in pixels of the clickable handle area */
		public int getRadius() {
			return radius;
		}

		/** @return The handle value scaled based upon the knob's min and max values */
		public int getVal(){

			double maxTemp = thisKnob.getMaxPos() - thisKnob.getMinPos();
			maxTemp = maxTemp < 0 ? maxTemp + DEG_PER_ROT : maxTemp;
			double handleTemp = this.getAngleDeg() - thisKnob.getMinPos();
			handleTemp = handleTemp < 0 ? handleTemp + DEG_PER_ROT : handleTemp;

			double pct = 0;
			double range = thisKnob.getValPosRangeDeg();
			if(thisKnob.isCwDirection())
				pct = 1 - ((handleTemp - maxTemp) / range);
			else{
				pct = 1 - ((handleTemp - maxTemp) / range) * -1;
				pct = thisKnob.getMaxPos() == thisKnob.getMinPos() ? pct - 1 : pct;
			}

			return (int) Math.round(pct * thisKnob.getValRange());
		}

		public void setValue(int value) {
			double range = thisKnob.getMaxPos() - thisKnob.getMinPos();
			double result = thisKnob.getMinPos() - (value / 40f) * range;
			setAngleDeg(result);
			repaint();
		}
	}

	//~~~~~~~~ Constructors and Initialization ~~~~~~~~//

	/**@author Jeff Masty
	 * @param listener receives user knob changes
	 */
	public Knob(KnobListener listener) {

		File dir = new File(System.getProperty("user.dir"));
		
		ImageIcon knobIcon = new ImageIcon(new File(dir, "knob.png").getAbsolutePath());

		ImageIcon knobHandle = new ImageIcon(new File(dir, "knobhandle.png").getAbsolutePath());
		init(200, new Point2D.Double(0.5, 0.5), 0.5, knobIcon, 30, knobHandle);
		setCwDirection(true);
		setMinPos(220);
		setMaxPos(320);
		setMinVal(0);
		setMaxVal(100);
		addMouseMotionListener(new MouseMotionAdapter() {
			 @Override public void mouseDragged(MouseEvent e) {
				 listener.knobChanged(getHandle(0).getVal());
				 moveHandles(e);
			 }
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				int target = 0;
				if (notches < 0) {
					target = getValue() + 5; // responsive
					if (target > getMaxVal()) target = getMaxVal();
				}
				else {
					target = getValue() - 2;
					if (target < getMinVal()) target = getMinVal();
				}
				listener.knobChanged(target);
			}
		});

	}

	/**
	 * No initial location constructor that initializes the position
	 * of the knob to 0 degrees (right).
	 *
	  * @param relCenter the center point around which the handles will rotate as fractional values relative to the overall size of the background image.
	  * 	<br><br>For instance, if the original background image is 100px W x 400 px H and the center of rotation should be at 50px, 100px, this parameter
	  * 	would be "new Point(0.5, 0,25)". The point of locating the center of rotation in this manner is to ensure that the center of rotation is always
	  * 	in the same place on the background image regardless of how it is scaled based upon the backgroundWidth parameter.<br>
	  * @param relTrackRadius the relative radius as a percent (0.0-1.0) of the overall width of the background image
	  * @param backgroundIcon the IconImage for the background
	  * @param backgroundWidth the width of the background. The background image will be scaled proportionally to fit this value
	  * @param handleIcon the default IconImage for the handles
	 */
	public Knob(Point2D relCenter, double relTrackRadius,
			ImageIcon backgroundIcon, int backgroundWidth,
			ImageIcon handleIcon) {
		this(0, relCenter, relTrackRadius, backgroundIcon, backgroundWidth,
				handleIcon);
	 }

	 /**
	  * Constructor to set initial handle position, relative track center and size, background
	  * ImageIcon, background width (height is scaled proportionally to maintain background image
	  * aspect ratio), and the initial handle IconImage.
	  * @param initDeg the initial angle of the pre-populated first handle <br>
	  * See {@link #JKnobFancy(Point2D, double, ImageIcon, int, ImageIcon)} for other parameters
	  */
	 public Knob(double initDeg, Point2D relCenter, double relTrackRadius,
			 ImageIcon backgroundIcon, int backgroundWidth,
			 ImageIcon handleIcon) {
		 init(initDeg, relCenter, relTrackRadius, backgroundIcon,
				 backgroundWidth, handleIcon);
	 }

//	 /** Empty public constructor available so the object may be
//	  * instantiated before being fully initialized. */
//	 public Knob(){
//	 }

    /**
	  * This method is called from the constructors or explicitly after creation of
	  * a JKnobFancy object using the empty no-args constructor to finish initialization. <br>
	  * See notes for {@link #JKnobFancy(double, Point2D, double, ImageIcon, int, ImageIcon)}
	  * for parameter details.
	  */
	 public void init(double initDeg, Point2D relCenter, double relTrackRadius,
			 ImageIcon backgroundIcon, int backgroundWidth,
			 ImageIcon handleIcon){

		this.setMinPos(0);
		this.setMaxPos(0);
		this.setMinVal(0);
		this.setMaxVal(0);
		this.defaultHandleIcon = handleIcon;
		this.backgroundIcon = backgroundIcon;
		this.setWidth(backgroundWidth);
		this.center = new Point((int)(relCenter.getX() * backgroundIcon.getIconWidth() * scale),
				(int)(relCenter.getY() * backgroundIcon.getIconHeight() * scale));
		this.setRelTrackRadius(relTrackRadius);


		handles.add(new JKnobHandle(Math.toRadians(initDeg), this.defaultHandleIcon, this));

		addMouseListener(new MouseAdapter() {
			 /**
			  * When the mouse button is pressed, the dragging of the
			  * spot will be enabled if the button was pressed over
			  * the spot.
			  *
			  * @param e reference to a MouseEvent object describing
			  *          the mouse press.
			  */
			 @Override
			 public void mousePressed(MouseEvent e) {

				Point mouseLoc = e.getPoint();
				boolean alreadySelected = false;
				for(JKnobHandle thisHandle : handles){
					// This prevents multiple spots from being simultaneously selected
					if(alreadySelected){
						thisHandle.setPressedOnSpot(false);
					}
					else{
						thisHandle.setPressedOnSpot(thisHandle.isOnSpot(mouseLoc));
					}
					if(thisHandle.isPressedOnSpot()){
						alreadySelected = true;
					}
				}
			 }

			 /**When the button is released, the dragging of the spot is disabled.
			  * @param e reference to a MouseEvent object describing the mouse release. */
			 @Override
			 public void mouseReleased(MouseEvent e) {
				for(JKnobHandle thisHandle : handles){
					thisHandle.setPressedOnSpot(false);
				}
			 }
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			 /**
			  * Compute the new angle for the spot and repaint the
			  * knob.  The new angle is computed based on the new
			  * mouse position.
			  *
			  * @param e reference to a MouseEvent object describing
			  *          the mouse drag.
			  */
			 @Override
			 public void mouseDragged(MouseEvent e) {
				 moveHandles(e);
			 }
		});

	 }

	 public int getValue() {
		 return getHandle(0).getVal();
	 }

	 public void setValue(int value) {
		 getHandle(0).setValue(value);
	 }

	 /**
	  * If the mouseDragged MouseMotionListener event is overridden,
	  * include this method in the new listener to ensure that the
	  * handles still move when dragged.
	  * @param e MouseEvent
	  */
	 public void moveHandles(MouseEvent e){
		 for(JKnobHandle thisHandle : handles){
			if (thisHandle.isPressedOnSpot()) {

			    int mx = e.getX();
			    int my = e.getY();

			    // Compute the x, y position of the mouse RELATIVE
			    // to the center of the knob.
			    int mxp = mx - center.x;
			    int myp = center.y - my;

			    // Compute the new angle of the knob from the
			    // new x and y position of the mouse.
			    // Math.atan2(...) computes the angle at which
			    // x,y lies from the positive y axis with cw rotations
			    // being positive and ccw being negative.
			    thisHandle.setAngle(Math.atan2(myp, mxp));

			    repaint();
			}
		}
	 }

	 //~~~~~~~~ Public Methods ~~~~~~~~//
	 /**
	  * Sets the knob background image width. The background image is always scaled with
	  * width / height proportionality, so this will cause the the width of the image
	  * to change to accommodate the requested width.
	  * @param backgroundWidth width in pixels
	  */
	 public void setWidth(int backgroundWidth){
		this.scale = (float) backgroundWidth / (float) backgroundIcon.getIconWidth();
		int height = (int)(backgroundIcon.getIconHeight() * this.scale);
		this.backgroundSize.setSize(backgroundWidth, height);
		repaint();
	 }

	 /**
	  * Sets the knob background image height. The background image is always scaled with
	  * width / height proportionality, so this will cause the the width of the image
	  * to change to accommodate the requested height.
	  * @param backgroundHeight background image height in pixels
	  */
	 public void setHeight(int backgroundHeight){
		 this.scale = (float) backgroundHeight / (float) backgroundIcon.getIconHeight();
		 int width = (int)(backgroundIcon.getIconWidth() * this.scale);
		 backgroundSize.setSize(width, backgroundHeight);
		 repaint();
	 }

	 /**
	  * @return Dimension of the current background image size
	  */
	 public Dimension getBackgroundSize(){
		 return backgroundSize;
	 }

	 /**
	  * @param relTrackRadius relative radius as a percent (0.0-1.0) of the overall width of the
	  * 	background image
	  */
	 public void setRelTrackRadius(double relTrackRadius){
		 this.relTrackRadius = relTrackRadius;
		 this.trackRadius = (int)(this.relTrackRadius * backgroundSize.getWidth());
	 }

	 /**
	  * Adds a new handle to the knob
	  * @param initDeg starting position of the new handle in degrees
	  * @param icon ImageIcon to use for the new handle
	  */
	 public void addHandle(double initDeg, ImageIcon icon){
		 handles.add(new JKnobHandle(Math.toRadians(initDeg), icon, this));
	 }

	 /**
	  * Adds a new handle to the knob. This uses the handle icon that
	  * was set in the knob constructor
	  * @param initDeg starting position of the new handle in degrees
	  */
	 public void addHandle(double initDeg){
		 handles.add(new JKnobHandle(Math.toRadians(initDeg), this.defaultHandleIcon, this));
	 }

	 /**
	  * Retrieves a handle object currently located on the knob
	  * @param which the element of the handle list that should be returned
	  * @return a the selected handle
	  */
	 public JKnobHandle getHandle(int which){
		 return handles.get(which);
	 }

	 /**
	  * Paint the JKnob on the graphics context given.  The knob
	  * is a filled circle with a small filled circle offset
	  * within it to show the current angular position of the
	  * knob.
	  *
	  * @param g The graphics context on which to paint the knob.
	  */
	 @Override
	public void paint(Graphics g) {

		// Draw background
		g.drawImage(backgroundIcon.getImage(), 0, 0, Math.round(backgroundIcon.getIconWidth()*scale), Math.round(backgroundIcon.getIconHeight()*scale), this);

		// Draw handles
		for(int i = 0; i < handles.size(); i ++){
			JKnobHandle thisHandle = handles.get(i);

			// Find the center of the handle
			Point pt = thisHandle.getSpotCenter();
			int xc = (int)pt.getX();
			int yc = (int)pt.getY();

			ImageIcon thisIcon = thisHandle.getIcon();
			g.drawImage(thisIcon.getImage(), xc-thisIcon.getIconWidth()/2, yc-thisIcon.getIconHeight()/2, thisIcon.getIconWidth(), thisIcon.getIconHeight(), this);
		}
	 }

	 /**
	  * Return the ideal size that the knob would like to be.
	  *
	  * @return the preferred size of the JKnob.
	  */
	 @Override
	public Dimension getPreferredSize() {
		 return new Dimension(Math.round(backgroundIcon.getIconWidth()*scale), Math.round(backgroundIcon.getIconHeight()*scale));
	 }

	 /**
	  * Return the minimum size that the knob would like to be.
	  * This is the same size as the preferred size so the
	  * knob will be of a fixed size.
	  *
	  * @return the minimum size of the JKnob.
	  */
	 @Override
	public Dimension getMinimumSize() {
		 return new Dimension(backgroundIcon.getIconWidth(), backgroundIcon.getIconHeight());
	 }

	/**
	 * @return the minimum value a knob handle may have
	 * 		(note: this is different than angular position)
	 */
	public int getMinVal() {
		 return minVal;
	}
	/**
	 * @param minVal the minimum value a knob handle may have
	 * 		(note: this is different than angular position)
	 */
	public void setMinVal(int minVal) {
		this.minVal = minVal;
	}
	/**
	 * @return maxVal the maximum value a knob handle may have
	 * 		(note: this is different than angular position)
	 */
	public int getMaxVal() {
		return maxVal;
	}
	/**
	 * @param maxVal the maximum value a knob handle may have
	 * 		(note: this is different than angular position)
	 */
	public void setMaxVal(int maxVal) {
		this.maxVal = maxVal;
	}
	/**
	 * @return the size of the knob's value range (i.e. maxVal - minVal)
	 */
	public int getValRange(){
		return maxVal - minVal;
	}
	/**
	 * @return the angular position in degrees at which the knob's minimum value occurs
	 */
	public double getMinPos(){
		return minValPos;
	}
	/**
	 * @param minPosDeg the angular position in degrees at which the knob's minimum value occurs
	 */
	public void setMinPos(double minPosDeg){
		this.minValPos = minPosDeg;
	}
	/**
	 * @return the angular position in degrees at which the knob's maximum value occurs
	 */
	public double getMaxPos(){
		return maxValPos;
	}
	/**
	 * @param maxPosDeg the angular position in degrees at which the knob's maximum value occurs
	 */
	public void setMaxPos(double maxPosDeg){
		this.maxValPos = maxPosDeg;
	}
	/**
	 * Gets the range between max and min value positions in degrees. This is dependent upon the
	 * knob's directionality--i.e. if direction is CW, range is minValPos - maxValPos, otherwise
	 * it is maxValPos - minValPos.
	 * @return range in degrees
	 */
	public double getValPosRangeDeg(){
		if(minValPos == maxValPos)
			return DEG_PER_ROT;
		double ret;
		if(this.isCwDirection())
			ret = minValPos - maxValPos;
		else
			ret = maxValPos - minValPos;
		ret = ret < 0 ? ret + DEG_PER_ROT : ret;
		return ret;
	}

	/**
	 * Determines whether knob is directionally clockwise
	 * @return  boolean value indicating whether knob value directionality. If it
	 * 		is true, the value of a knob handle will increase as it moves clockwise
	 * 		from the minValPos, otherwise it will increase as it moves counterclockwise.
	 */
	public boolean isCwDirection() {
		return cwDirection;
	}

	/**
	 * Sets whether knob is directionally clockwise. See return note
	 * for {@link #isCwDirection()} for more details.
	 * @param cwDirection boolean value indicating clockwise direction
	 */
	public void setCwDirection(boolean cwDirection) {
		this.cwDirection = cwDirection;
	}
}
