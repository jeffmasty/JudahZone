package net.judah.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import lombok.RequiredArgsConstructor;

public class EditsPane extends JScrollPane {

	public EditsPane(JTable table, PopupMenu menu) {
		super(table);
		addMouseListener(new EditsAdapter(menu));
	}

	@RequiredArgsConstructor
	public static class EditsAdapter extends MouseAdapter {
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
}
