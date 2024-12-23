package net.judah.song;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.api.Key;
import net.judah.api.Signature;
import net.judah.api.TimeProvider;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.chords.Scale;
import net.judah.song.setlist.Setlists;
import net.judah.song.setlist.SetlistsCombo;

public class SongTitle extends JPanel {

	private final Overview overview;
	private final TimeProvider clock;
	private final JComboBox<Signature> timeSig = new JComboBox<>(Signature.values());
	private final JComboBox<Key> key = new JComboBox<>(Key.values());
	private final JComboBox<Scale> scale = new JComboBox<>(Scale.values());
	private final JLabel bar = new JLabel("0", JLabel.CENTER);

	public SongTitle(TimeProvider clock, Setlists setlists, Overview overview) {
		this.clock = clock;
		this.overview = overview;

		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(Box.createHorizontalStrut(3));
		add(Gui.resize(new SongCombo(), Size.TITLE_SIZE));
		add(new Btn(Icons.SAVE, e->overview.save()));
		add(new Btn(" 🔁 ", e->overview.reload(), "Reload"));
		add(Gui.resize(new SetlistsCombo(setlists), Size.MEDIUM_COMBO));
		add(new Btn(Icons.DETAILS_VEW, e->MainFrame.setFocus(KnobMode.SETLIST)));
		add(Box.createHorizontalGlue());
		add(Gui.resize(timeSig, Size.SMALLER_COMBO));
		add(Gui.resize(key, Size.MICRO));
		add(Gui.resize(scale, Size.MEDIUM_COMBO));
		add(Gui.resize(bar, Size.MICRO));

		setBackground(Pastels.BUTTONS);
		update();
		scale.addActionListener(e->overview.getSong().setScale((Scale) scale.getSelectedItem()));
		key.addActionListener(e->overview.getSong().setKey((Key) key.getSelectedItem()));
		timeSig.addActionListener(e->overview.getSong().setTimeSig((Signature)timeSig.getSelectedItem()));
	}

	public void update() {
		if (timeSig.getSelectedItem() != clock.getTimeSig())
			timeSig.setSelectedItem(clock.getTimeSig());
		Song song = overview.getSong();
		if (song == null) return;
		if (song.getKey() != null && key.getSelectedItem() != song.getKey())
			key.setSelectedItem(song.getKey());
		if (song.getScale() != null && scale.getSelectedItem() != song.getScale())
			scale.setSelectedItem(song.getScale());
	}

	public void updateBar(int value) {
		Threads.execute(()->bar.setText("" + (value + 1)));
	}

}
