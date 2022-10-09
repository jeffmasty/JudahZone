package net.judah.util;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;

public class SettableCombo<T> extends CenteredCombo<T> {

	private static final Border red = BorderFactory.createSoftBevelBorder(
			BevelBorder.RAISED, Color.RED, Color.RED.darker());
	@Getter private static SettableCombo<?> focus;
	
	@Setter private Runnable action; 
	private Border old;
	
	public SettableCombo() {
		old = getBorder();
		addMouseListener(new MouseAdapter() { // right click = set
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3)
					set();
			}});
		addActionListener(e ->SettableCombo.highlight(this));
	}
	
	public SettableCombo(Runnable action) {
		this();
		this.action = action;
	}
	
	public static void highlight(SettableCombo<?> change) {
		if (JudahZone.isOverride()) {
			if (focus != null) {
				focus.setBorder(focus.old);
				focus = null;
			}
			return;
		}

		if (focus != null) {
			focus.setBorder(focus.old);
		}
		focus = change;
		if (focus != null) {
			focus.setBorder(red);
		}
	}
	
	public static void set() {
		if (focus != null)
			new Thread(() ->{
				focus.action.run();
				highlight(null);
			}).start();
		else {
			new Thread(()->{
				JudahZone.loadSong();
				highlight(null);
			}).start();
		}
	}
	
}
