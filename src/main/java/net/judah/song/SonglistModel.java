package net.judah.song;

import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;

class SonglistModel extends DefaultComboBoxModel<File> {
		
		SonglistModel(ArrayList<String> files) {
			for (String s : files) {
				addElement(new File(s));
			}
		}
	
		
		
		public ArrayList<String> getAll() {
			ArrayList<String> result = new ArrayList<String>();
			for (int i = 0; i < getSize(); i++) {
				result.add(getElementAt(i).getAbsolutePath());
			}
			return result;
		}
		
		
	}