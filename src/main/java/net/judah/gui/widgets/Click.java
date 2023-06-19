package net.judah.gui.widgets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.gui.Pastels;

public class Click extends JLabel {
	static Border border = new LineBorder(Pastels.BLUE, 1, true);
	@Getter boolean right;
	
	public Click(String msg, ActionListener l) {
		this(msg);
		addActionListener(l);
	}
	
    public Click(String msg) {
    	super(msg, JLabel.CENTER);
    	setBorder(border);

    	addMouseListener(new MouseAdapter() {
        @Override public void mouseClicked(MouseEvent me) {
        	right = SwingUtilities.isRightMouseButton(me);
        	fireActionPerformed(new ActionEvent(Click.this, ActionEvent.ACTION_PERFORMED,
              getText()));}
    	});
    }

    public void addActionListener(ActionListener l) {
      listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
      listenerList.remove(ActionListener.class, l);
    }

    protected void fireActionPerformed(ActionEvent ae) {
      Object[] listeners = listenerList.getListeners(ActionListener.class);
      for (int i = 0; i < listeners.length; i++) {
        ((ActionListener) listeners[i]).actionPerformed(ae);
      }
    }
  }