package net.judah.gui.widgets;

import java.awt.Component;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class FileRender implements ListCellRenderer<File> {

	private final JLabel render = new JLabel();
	
	@Override
	public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected,
			boolean cellHasFocus) {
		render.setText(value == null ? "?" : value.getName());
		return render;
	}
	
}
