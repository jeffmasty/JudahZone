package net.judah.song;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.midi.JudahClock;
import net.judah.midi.Signature;
import net.judah.seq.chords.Scale;
import net.judah.song.setlist.Setlists;
import net.judah.song.setlist.SetlistsCombo;
import net.judah.util.Constants;

public class SongTitle extends JPanel {
	
	@Setter private Song song;
	private final JudahClock clock;
	private final JComboBox<Signature> timeSig = new JComboBox<>(Signature.values());
	private final JComboBox<Key> key = new JComboBox<>(Key.values());
	private final JComboBox<Scale> scale = new JComboBox<>(Scale.values());
	private final JLabel bar = new JLabel("0", JLabel.CENTER);
	
	public SongTitle(JudahClock clock, Setlists setlists) {
		this.clock = clock;
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(Box.createHorizontalStrut(3));
		add(Gui.resize(new SongCombo(), Size.TITLE_SIZE)); 
		add(Gui.resize(new SetlistsCombo(setlists), Size.MEDIUM_COMBO));
		add(new Btn(Icons.SAVE, e->JudahZone.save()));
		add(new Btn(" 🔁 ", e->JudahZone.reload(), "Reload"));
		add(new Btn(Icons.DETAILS_VEW, e->MainFrame.setFocus(KnobMode.Setlist)));
		add(Box.createHorizontalGlue());
		add(Gui.resize(timeSig, Size.SMALLER_COMBO));
		add(Gui.resize(key, Size.MICRO));
		add(Gui.resize(scale, Size.MEDIUM_COMBO));
		add(Gui.resize(bar, Size.MICRO));
		
		setBackground(Pastels.BUTTONS);
		update();
		scale.addActionListener(e->JudahZone.getSong().setScale((Scale) scale.getSelectedItem()));
		key.addActionListener(e->JudahZone.getSong().setKey((Key) key.getSelectedItem()));
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

	public void updateBar(int value) {
		Constants.execute(()->bar.setText("" + (value + 1)));
	}
	
}
