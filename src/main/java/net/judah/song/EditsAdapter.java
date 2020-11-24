package net.judah.song;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import lombok.RequiredArgsConstructor;
import net.judah.util.PopupMenu;;

@RequiredArgsConstructor
public class EditsAdapter extends MouseAdapter {
	private final PopupMenu menu;
	
	@Override public void mouseClicked(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e)) {
			menu.setVisible(false);
			return;
		}
		menu.setLocation(e.getLocationOnScreen());
		menu.setVisible(true);
	}
	
}
