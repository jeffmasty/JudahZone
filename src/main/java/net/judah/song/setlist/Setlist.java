package net.judah.song.setlist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.gui.widgets.FileChooser;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

/** Encapsulates a disk folder or a list of files */
@Getter @NoArgsConstructor
public class Setlist extends ArrayList<File> {
	@JsonIgnore @Setter private File source;
	
	public Setlist(File f) throws IOException {
		source = f;
		if (f.isDirectory()) {
			for (File song : Folders.sort(f))
				add(song);
		} else if (f.isFile()) {
			Setlist disk = (Setlist)JsonUtil.readJson(f, Setlist.class);
			addAll(disk);
		} else throw new IOException(f == null ? "null" : f.getAbsolutePath());
	}
	
	public boolean isCustom() {
		return source != null && source.isFile();
	}
	
	public boolean isDirectory() {
		return source != null && source.isDirectory();
	}

	@Override public String toString() {
		return source.getName();
	}
	
	public void save() {
		try {
			if (source == null)
				source = FileChooser.choose(Setlists.ROOT);
			if (source == null || source.isDirectory()) return;
			JsonUtil.writeJson(this, source); 
			RTLogger.log(this, "saved " + source.getName());
		} catch (IOException e) {RTLogger.warn(this, e.getMessage());}
	}

	public File[] array() {
		File[] result = new File[size()];
		for (int i = 0; i < size(); i++)
			result[i] = get(i);
		return result;
	}

}
