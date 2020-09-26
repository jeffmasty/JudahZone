package net.judah.util;

import static net.judah.util.Constants.*;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

abstract public class Tab extends JComponent implements ActionListener {

	protected final JTextArea output;
	protected final JScrollPane listScroller;
	private String history = null;

	public Tab(boolean customLayout) {
		if (customLayout) {
			output = null;
			listScroller = null;
			return;
		}
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        output = new JTextArea();
        output.setEditable(false);
        listScroller = new JScrollPane(output);
        listScroller.setBorder(new EtchedBorder());
        listScroller.setPreferredSize(new Dimension(680, 350));
        listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScroller.setAlignmentX(LEFT_ALIGNMENT);
        add(listScroller);

		JTextField input = new JTextField(70);
		input.setMaximumSize(new Dimension(660, 75));
        add(input);
        input.addActionListener(this);
        input.grabFocus();
	}

	public Tab() {
		this(false);
	}

	abstract public String getTabName();

	abstract public void setProperties(Properties p);

	public void addText(Object o) {
		addText("" + o);
	}

	public void addText(String s) {
		if (output == null) {
			return;
		}
		if (s == null) {
			Logger.getLogger(Tab.class).info("null addText() to terminal");
			return;
		}
		if (false == s.endsWith(NL))
			s = s + NL;
		output.append(s);
		Rectangle r = output.getBounds();
		listScroller.getViewport().setViewPosition(new Point(0, r.height));
	}

	protected String parseInputBox(ActionEvent e) {
		if (e.getSource() instanceof JTextField == false) return null;
		JTextField widget = (JTextField)e.getSource();
		String text = widget.getText();
		widget.setText("");
		if (text.isEmpty() && history != null) {
			addText(history);
			return history;
		}
		addText("> " + text);
		history = text;
		return text;
	}

	
}
