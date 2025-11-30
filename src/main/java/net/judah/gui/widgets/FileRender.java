package net.judah.gui.widgets;

import java.awt.Component;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class FileRender implements ListCellRenderer<File> {

	private final JLabel render = new JLabel("");

	public static String defix(File value) {
		if (value == null)
			return "?";
		if (value.getName().indexOf('.') > 1)
			return value.getName().substring(0, value.getName().lastIndexOf('.'));
		else return value.getName();
	}

	@Override public Component getListCellRendererComponent(JList<? extends File> list, File value,
			int index, boolean isSelected, boolean cellHasFocus) {
		render.setText(defix(value));
		return render;
	}

}
