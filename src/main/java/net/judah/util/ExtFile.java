package net.judah.util;

import java.awt.Component;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

// Strips extenstions (.mid .pro .set)
public class ExtFile extends File implements ListCellRenderer<ExtFile> {
	String toString;
	public ExtFile(String pathname) {
		super(pathname);
	}
	
	@Override public String toString() {
		if (toString == null)
			toString = getName().substring(0, getName().lastIndexOf('.'));
		return toString;
	}
	
	@Override public Component getListCellRendererComponent(JList<? extends ExtFile> list, ExtFile value, int index, 
			boolean isSelected, boolean cellHasFocus) {
		return new JLabel(value == null ? "?" : value.toString());
	}
			
}
