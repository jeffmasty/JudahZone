package net.judah.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import lombok.Getter;
import net.judah.MainFrame;

public class EditorDialog extends JDialog {

	@Getter private Boolean result;
	@Getter private JPanel content;
	private JPanel okCancel;

	public EditorDialog(String title) {
		super(MainFrame.get(), true);
		setTitle(title);
		okCancel = new JPanel();
		JButton ok = new JButton("ok");
		JButton cancel = new JButton("cancel");
		ok.addActionListener( (event) -> ok());
		cancel.addActionListener( (event) -> cancel());
		okCancel.add(ok);
		okCancel.add(cancel);
		getRootPane().registerKeyboardAction(new ActionListener() {
	        @Override public void actionPerformed(ActionEvent e) { cancel();} },
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(new ActionListener() {
	        @Override public void actionPerformed(ActionEvent e) { ok();} },
	            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		setMinimumSize(new Dimension(100, 100));
	}

	/**blocks until user closes
	 * @param content The form the user should interact with
	 * @return true if user clicks OK. */
	public boolean showContent(JPanel content) {
		this.content = content;
		setLayout(new BorderLayout());
		add(content, BorderLayout.CENTER);
		add(okCancel, BorderLayout.PAGE_END);
		Point p = MouseInfo.getPointerInfo().getLocation();
		setLocation(p.x - 100, p.y - 30);
		pack();
		setVisible(true);
		return result == null ? false : result;
	}
	
	public void ok() {
		result = true;
		dispose();
	}
	public void cancel() {
		result = false;
		dispose();
	}

}