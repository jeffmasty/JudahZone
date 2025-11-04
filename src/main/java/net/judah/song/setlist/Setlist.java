package net.judah.song.setlist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.judah.omni.JsonUtil;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

/** Encapsulates a disk folder (genre) OR a list of files (mix-n-match) */
@NoArgsConstructor
public class Setlist extends ArrayList<File> {
	@JsonIgnore @Setter @Getter private File source;

	public Setlist(File f) throws IOException {
		source = f;
		// initial validation
		if (f.isFile()) {
			Setlist disk = (Setlist)JsonUtil.readJson(f, Setlist.class);
			addAll(disk);
		} else if (f.isDirectory() == false)
			throw new IOException(f == null ? "null" : f.getAbsolutePath());
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
				source = Folders.choose(Setlists.ROOT);
			if (source == null || source.isDirectory()) return;
			JsonUtil.writeJson(this, source);
			RTLogger.log(this, "saved " + source.getName());
		} catch (IOException e) {RTLogger.warn(this, e.getMessage());}
	}

	public ArrayList<File> list() {
		if (source.isDirectory()) {
			ArrayList<File> result = new ArrayList<File>();
			for (File song : Folders.sort(source)) // fresh updates (new songs)
				result.add(song);
			return result;
		}
		return this;
	}

	public File[] array() {
		ArrayList<File> list = list();
		File[] result = new File[list.size()];
		for (int i = 0; i < list.size(); i++)
			result[i] = list.get(i);
		return result;
	}


}
