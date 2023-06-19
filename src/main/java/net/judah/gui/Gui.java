package net.judah.gui;

import java.awt.*;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public interface Gui {

	int STD_HEIGHT = 18;
	Insets BTN_MARGIN = new Insets(1,1,1,1);
	Insets ZERO_MARGIN = new Insets(0,0,0,0);
	
	Border RED = BorderFactory.createLineBorder(Color.RED, 1);
	Border HIGHLIGHT = BorderFactory.createLineBorder(Color.BLACK, 1);
	Border SUBTLE = BorderFactory.createLineBorder(Pastels.MY_GRAY, 1);
	Border NO_BORDERS = new EmptyBorder(BTN_MARGIN);

	Font BOLD10 = new Font("Arial", Font.BOLD, 10);
	Font BOLD = new Font("Arial", Font.BOLD, 11);
	Font BOLD13 = new Font("Arial", Font.BOLD, 13);
	Font FONT13 = new Font("Arial", Font.PLAIN, 13);
	Font FONT12 = new Font("Arial", Font.PLAIN, 12);
	Font FONT11 = new Font("Arial", Font.PLAIN, 11);
	Font FONT10 = new Font("Arial", Font.PLAIN, 10);
	Border GRAY1 = new LineBorder(Color.GRAY, 1);
	Font FONT9 = new Font("Arial", Font.PLAIN, 9);
	
	static JPanel duo(Component left, Component right) {
		JPanel result = new JPanel();
		result.setLayout(new GridLayout(1, 2));
		result.add(left);
		result.add(right);
		return result;
	}
	
	static JPanel wrap(Component... items) {
		JPanel result = new JPanel();
		for (Component p : items)
			result.add(p);
		return result;
	}

	static JComponent resize(JComponent c, Dimension d) {
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
}

//	  List<String> colorKeys = new ArrayList<String>();
//    Set<Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults().entrySet();
//    for (Entry entry : entries) {
//      if (entry.getValue() instanceof Color)
//      { colorKeys.add((String) entry.getKey()); }}
//    // sort the color keys
//    Collections.sort(colorKeys);
//    // print the color keys
//    for (String colorKey : colorKeys)
//    {      System.out.println(colorKey);}
  
