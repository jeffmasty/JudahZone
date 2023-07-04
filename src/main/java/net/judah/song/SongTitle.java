package net.judah.song;

import javax.swing.*;

import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.SongsCombo;
import net.judah.gui.widgets.Btn;
import net.judah.midi.JudahClock;
import net.judah.midi.Signature;
import net.judah.seq.chords.Scale;
import net.judah.song.setlist.Setlists;
import net.judah.song.setlist.SetlistsCombo;

public class SongTitle extends JPanel {
	
	@Setter private Song song;
	private final JudahClock clock;
	private final JComboBox<Signature> timeSig = new JComboBox<>(Signature.values());
	private final JComboBox<Key> key = new JComboBox<>(Key.values());
	private final JComboBox<Scale> scale = new JComboBox<>(Scale.values());
	
	public SongTitle(JudahClock clock, Setlists setlists) {
		this.clock = clock;
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(new SongsCombo()); 
		add(new Btn(" 🔁 ", e->JudahZone.reload(), "Reload"));
		add(new Btn(UIManager.getIcon("FileView.floppyDriveIcon"), e->JudahZone.save()));
		add(Gui.resize(timeSig, Size.SMALLER_COMBO));
		add(Gui.resize(key, Size.MICRO));
		add(Gui.resize(scale, Size.MEDIUM_COMBO));
		add(new JLabel("Set:"));
		add(new SetlistsCombo(setlists));
		add(Box.createGlue());

		update();
		scale.addActionListener(e->JudahZone.getCurrent().setScale((Scale) scale.getSelectedItem()));
		key.addActionListener(e->JudahZone.getCurrent().setKey((Key) key.getSelectedItem()));
		timeSig.addActionListener(e->song.setTimeSig((Signature)timeSig.getSelectedItem()));
	}
	
	
	public void update() {
		if (timeSig.getSelectedItem() != clock.getTimeSig())
			timeSig.setSelectedItem(clock.getTimeSig());
		if (song == null) return;
		if (song.getKey() != null && key.getSelectedItem() != song.getKey())
			key.setSelectedItem(song.getKey());
		if (song.getScale() != null && scale.getSelectedItem() != song.getScale())
			scale.setSelectedItem(song.getScale());		
	}
	
}
