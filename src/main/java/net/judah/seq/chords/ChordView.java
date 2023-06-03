package net.judah.seq.chords;

import static net.judah.gui.Pastels.*;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.settable.ChordProFiles;
import net.judah.gui.widgets.Btn;
import net.judah.song.Song;

public class ChordView extends JPanel implements Updateable {
	
	private final ChordTrack chords;
	@Setter private Song song;
	private final JButton play;
	private final ChordProFiles folder = new ChordProFiles();
	@Getter private final ChordScroll scroll;
	
	public ChordView(ChordTrack trk) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.chords = trk;
		play = new Btn("▶️ Chords", e-> chords.toggle());
		play.setOpaque(true);
		scroll = new ChordScroll(chords);
		add(play);
		add(Gui.resize(folder, Size.COMBO_SIZE));
		add(scroll);
		add(Box.createGlue());
		update();
		setOpaque(true);
	}
	
	@Override
	public void update() {
		if (song != null && folder.getSelectedItem() != song.getChordpro())
			folder.setSelectedItem(song.getChordpro());
		play.setBackground(chords.isActive() ? GREEN : chords.isOnDeck() ? YELLOW : null);
	}
	
}
