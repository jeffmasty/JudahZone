package net.judah.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.File;

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
* @author Michael Ploof
* @author Jeff Masty
*/
public class RainbowKnob extends JComponent{
	private static final long serialVersionUID = -3331634859451614043L;

	public static interface KnobListener {
		void knobChanged(int val);
	}

	public static final double DEG_PER_ROT = 360;
	public static final Color BACKGROUND = Pastels.BUTTONS;
	public static final Color BORDER = Color.GRAY;
	public static final int WIDTH = 30;
	protected int halfRadius = WIDTH / 4;
	public static final Dimension SIZE = new Dimension(WIDTH, WIDTH);

	protected static final int RADIUS = 14;
	
	//~~~~~~~~ Knob Value Vars ~~~~~~~~//
	/** Minimum value the knob can report*/
	@Setter @Getter protected int minVal = 0;
	/** Maximum value the knob can report */
	@Setter @Getter protected int maxVal = 100;
	/** The angular position in degrees at which the knob's minimum value occurs */
	@Setter @Getter protected double minPos = 225;
	/** The angular position in degrees at which the knob's maximum value occurs */
	@Setter @Getter protected double maxPos = 330;

	/** if true, a stronger border is painted*/
	@Setter @Getter protected boolean onMode;
	
	
	//~~~~~~~~ Background Image Vars ~~~~~~~~//
	/** Icon used for the knob background */
	protected final ImageIcon knobIcon = new ImageIcon(new File(Constants.ROOT, "knob.png").getAbsolutePath());
	protected final int width = knobIcon.getIconWidth();
	protected final int height = knobIcon.getIconHeight();

	//~~~~~~~~ Handle and Track Vars ~~~~~~~~//
	/** Relative radius as a percent (0.0-1.0) of the overall width of the background image */
	protected double relTrackRadius;
	/** Radius of the circle along which the handles will move */
	protected int trackRadius;
	/** Pixel location of the center of the handle track */
	protected Point center;
	/** The handle icon passed to the knob constructor. Different handle icons may be used
	 * for subsequently added handles, but this icon will be used if none is specified. */
	/** knob's main indicator */
	JKnobHandle handle;
	
	//~~~~~~~~ Handle Class~~~~~~~~//
	/** This class describes handle objects that may be positioned
	 * on the JKnobFancy object. */
	public class JKnobHandle {

		/** Reference to the knob object on which the handle is located */
		RainbowKnob thisKnob;
		/** Radius of clickable handle area */
		int radius;
		/** Handle location in radians */
		private double theta;
		/** Whether the handle is currently clicked */
		@Setter @Getter private boolean pressedOnSpot;

		private JKnobHandle(double theta, RainbowKnob thisKnob){
			this.theta = theta;
			this.pressedOnSpot = false;
			this.thisKnob = thisKnob;
			this.radius = 7;
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

			return handleTemp >= maxTemp;
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
				pct = 1 - ((handleTemp - maxTemp) / range);

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

	/**@param listener receives user knob changes */
	public RainbowKnob(KnobListener listener) {

		init(150, new Point2D.Double(0.5, 0.5), 0.5, 30);
		addMouseMotionListener(new MouseMotionAdapter() {
			 @Override public void mouseDragged(MouseEvent e) {
				 listener.knobChanged(handle.getVal());
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
	  * This method is called from the constructors or explicitly after creation of
	  * a JKnobFancy object using the empty no-args constructor to finish initialization. <br>
	  * See notes for {@link #JKnobFancy(double, Point2D, double, ImageIcon, int, ImageIcon)}
	  * for parameter details.
	  */
	 public void init(double initDeg, Point2D relCenter, double relTrackRadius,
			 int backgroundWidth)
	 {

		this.center = new Point((int)(relCenter.getX() * SIZE.width),
				(int)(relCenter.getY() * SIZE.width));
		this.setRelTrackRadius(relTrackRadius);

		handle = new JKnobHandle(Math.toRadians(initDeg), this);

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
				handle.setPressedOnSpot(handle.isOnSpot(mouseLoc));
			 }

			 /**When the button is released, the dragging of the spot is disabled.
			  * @param e reference to a MouseEvent object describing the mouse release. */
			 @Override
			 public void mouseReleased(MouseEvent e) {
					handle.setPressedOnSpot(false);
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
		 return handle.getVal();
	 }

	 public void setValue(int value) {
		 handle.setValue(value);
	 }

	 /**
	  * If the mouseDragged MouseMotionListener event is overridden,
	  * include this method in the new listener to ensure that the
	  * handles still move when dragged.
	  * @param e MouseEvent
	  */
	 public void moveHandles(MouseEvent e){
			if (handle.isPressedOnSpot()) {

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
			    handle.setAngle(Math.atan2(myp, mxp));

			    repaint();
		}
	 }

	 //~~~~~~~~ Public Methods ~~~~~~~~//

	 /**@return the ideal size that the knob would like to be. */
	 @Override
	public Dimension getPreferredSize() {
		 return SIZE;
	}

	 /**@return the minimum size that the knob would like to be.
	  * This is the same size as the preferred size so the
	  * knob will be of a fixed size. */
	 @Override
	public Dimension getMinimumSize() {
		 return SIZE;
	 }

	/**@return the size of the knob's value range (i.e. maxVal - minVal)*/
	public int getValRange(){
		return maxVal - minVal;
	}
	/**@return the range between max and min value positions in degrees. This is dependent upon the
	 * knob's directionality--i.e. if direction is CW, range is minValPos - maxValPos, otherwise
	 * it is maxValPos - minValPos. */
	public double getValPosRangeDeg(){
		if(minPos == maxPos)
			return DEG_PER_ROT;
		double ret;
		ret = minPos - maxPos;
		ret = ret < 0 ? ret + DEG_PER_ROT : ret;
		return ret;
	}

	 /**
	  * @param relTrackRadius relative radius as a percent (0.0-1.0) of the overall width of the
	  * 	background image
	  */
	 public void setRelTrackRadius(double relTrackRadius){
		 this.relTrackRadius = relTrackRadius;
		 this.trackRadius = (int)(this.relTrackRadius * SIZE.getWidth());
	 }

	 
	 private BasicStroke highlight = new BasicStroke(2.5f);
	 private BasicStroke basic = new BasicStroke(1.1f);
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
		g.drawImage(knobIcon.getImage(), 0, 0, width, height, this);
		
		if (onMode) { // draw a highlight knob border
			g.setColor(BORDER);
			g.drawOval(0, 0, WIDTH - 1, WIDTH - 1);

			((Graphics2D)g).setStroke(highlight);
			g.setColor(Color.BLACK);
			g.drawOval(2, 2, WIDTH-4, WIDTH-4);
			((Graphics2D)g).setStroke(basic);
		}
		
		// Find the center of the handle
		Point pt = handle.getSpotCenter();
		int handleX = (int)pt.getX() - halfRadius;
		int handleY = (int)pt.getY() - halfRadius;
		g.setColor(RainbowFader.chaseTheRainbow(handle.getVal()));
		g.fillOval(handleX, handleY, RADIUS, RADIUS);
		g.setColor(BORDER);
		g.drawOval(handleX, handleY, RADIUS, RADIUS);
			
	 }


}
