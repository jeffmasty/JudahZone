package net.judah.song;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.judah.JudahZone;

public class CellDialog extends JDialog {

	public static interface CallBack {
		void callback(boolean okClicked);
	}

	CallBack callback;
	
	public CellDialog(JComponent content, CallBack callback) {
		super(JudahZone.getFrame(), true);
		setLayout(new BorderLayout());
		this.callback = callback;
		add(content, BorderLayout.CENTER);
		
		JPanel okCancel = new JPanel();
		JButton ok = new JButton("ok");
		JButton cancel = new JButton("cancel");
		ok.addActionListener( (event) -> ok());
		cancel.addActionListener( (event) -> cancel());
		okCancel.add(ok);
		okCancel.add(cancel);
		add(okCancel, BorderLayout.PAGE_END);
		
		getRootPane().registerKeyboardAction(new ActionListener() {
	        @Override public void actionPerformed(ActionEvent e) { cancel();} },
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(new ActionListener() {
	        @Override public void actionPerformed(ActionEvent e) { ok();} },
	            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

		pack();
		setVisible(true);
	}
	
	public void ok() {
		if (callback != null)
			callback.callback(true);
		dispose();
	}
	public void cancel() {
		if (callback != null)
			callback.callback(false);
		dispose();
		
	}
	
	
}
