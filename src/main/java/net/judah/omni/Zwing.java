package net.judah.omni;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;


/** Common AWT and Swing methods */
public interface Zwing {

	int LINE_HEIGHT = 18; // STD_ or MENU_
	Insets BTN_MARGIN = new Insets(1,1,1,1);
	Insets ZERO_MARGIN = new Insets(0,0,0,0);

	Border RED = BorderFactory.createLineBorder(Color.RED, 1);
	Border HIGHLIGHT = BorderFactory.createLineBorder(Color.BLACK, 1);
	Border NO_BORDERS = new EmptyBorder(BTN_MARGIN);


	String FACE = "Arial";
	Font FONT9 = new Font(FACE, Font.PLAIN, 9);
	Font FONT10 = new Font(FACE, Font.PLAIN, 10);
	Font FONT11 = new Font(FACE, Font.PLAIN, 11);
	Font FONT12 = new Font(FACE, Font.PLAIN, 12);
	Font FONT13 = new Font(FACE, Font.PLAIN, 13);
	Font BOLD10 = new Font(FACE, Font.BOLD, 10);
	Font BOLD = new Font(FACE, Font.BOLD, 11);
	Font BOLD12 = new Font(FACE, Font.BOLD, 12);
	Font BOLD13 = new Font(FACE, Font.BOLD, 13);
	default JComponent font(JComponent c, Font f) {
		c.setFont(f);
		return c;
	}
	default JComponent font(JComponent c) {
		return font(c, FONT9);
	}

    float[] _DASH = {9.0f};  // Length of the dash
    BasicStroke DASHED_LINE = new BasicStroke(2.0f,                 // Line width
                                         BasicStroke.CAP_BUTT,      // End cap style
                                         BasicStroke.JOIN_MITER,    // Join style
                                         10.0f,                     // Miter limit
                                         _DASH,                		// Dash pattern
                                         0.0f);                     // Dash phase

    static JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(1, 2));
		result.add(left);
		result.add(right);
		return result;
	}

	default JPanel wrap(Component... items) {
		JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		for (Component p : items)
			result.add(p);
		return result;
	}

	default JComponent size(JComponent c, Dimension d) {
		c.setMaximumSize(d);
		c.setPreferredSize(d);
		return c;
	}

	static void infoBox(String infoMessage, String titleBar) {
	    JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
	}

	static String inputBox(String infoMessage) {
	    return JOptionPane.showInputDialog(infoMessage);
	}

	static final float slope = 0.04f;
    static Color chaseTheRainbow(int percent) {
    	if (percent < 0 || percent > 100) return Color.BLACK;
    	float red = 0;
    	float green = 0;
    	float blue = 0;

    	if (percent < 25) { // green up
    		blue = 1;
    		green = percent * slope ;
    	}
    	else if (percent < 50) { // blue down
    		green = 1;
    		blue = 2 - slope * percent;
    	}
    	else if (percent < 75) { // red up
    		green = 1;
    		red = (percent - 50) * slope;
    	} else { // green down
    		red = 1;
    		green = 4 - slope * percent;
    	}
    	return new Color(red, green, blue).darker();
    }

	static class LambdaMenu extends JMenuItem {
		public LambdaMenu(String lbl, ActionListener l) {
			super(lbl);
			addActionListener(l);
		}
	}


}
