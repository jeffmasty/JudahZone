package net.judah.util;

import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.apache.commons.io.FilenameUtils;

public class FileCellRenderer extends JLabel implements ListCellRenderer<File> {
	
	@Override
	public Component getListCellRendererComponent(JList<? extends File> list, File file, int index,
			boolean isSelected, boolean cellHasFocus) {
	          
		setText(" " + FilenameUtils.removeExtension(file.getName()));
		if (isSelected) {
			setBorder(Constants.Gui.GRAY1);
		    setForeground(Color.BLACK);
		} else {
			setBorder(null);
		    setForeground(Color.GRAY);
		}
		return this;
	}
	     
}

