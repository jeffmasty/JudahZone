package net.judah.song.setlist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;

import judahzone.util.Folders;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.FileRender;

public class Setlists extends ArrayList<Setlist> {
	public static final File ROOT = Folders.getSetlistHome();
	public static final String SUFFIX = ".set";
	private final ArrayList<Setlist> scratch = new ArrayList<>();

	@Getter private Setlist current;
	private JComboBox<File> combo;

	public Setlists() {
		for (File f : ROOT.listFiles()) { try {
			add(new Setlist(f));
			} catch (Throwable t) { RTLogger.log(this, f.getAbsolutePath() + " " + t.getMessage()); }
		}
		if (!isEmpty())
			current = get(0);
	}

	public File getDefault() {
		for (Setlist l : this)
			if (l.isDirectory())
				return l.getSource();
		return null;
	}

	public void setCurrent(File list) {
		for (Setlist s : this)
			if (s.getSource().equals(list)) {
				if (current == s)
					return;
				current = s;
				if (getCombo().getSelectedItem() != list)
					getCombo().setSelectedItem(list);
				SongCombo.refill(current.array());
			}
	}

	public File[] array() {
		File[] result = new File[size()];
		for (int i = 0; i < result.length; i++)
			result[i] = get(i).getSource();
		return result;
	}

	public List<Setlist> getCustom() {
		scratch.clear();
		for (Setlist s : this)
			if (s.isCustom())
				scratch.add(s);
		return scratch;
	}

	public JComboBox<File> getCombo() {
		if (combo != null)
			return combo;
		combo = new JComboBox<File>(array());
    	combo.setSelectedItem(current.getSource());
    	combo.addActionListener(e->setCurrent((File)combo.getSelectedItem()));
		combo.setRenderer(new FileRender());
		return combo;
	}

}
