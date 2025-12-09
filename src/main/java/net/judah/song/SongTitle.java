package net.judah.song;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Key;
import net.judah.api.Notification.Property;
import net.judah.api.Signature;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.SongCombo;
import net.judah.gui.widgets.Btn;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.seq.AddTrack;
import net.judah.seq.chords.ChordPlay;
import net.judah.seq.chords.Chords;
import net.judah.seq.chords.Scale;
import net.judah.seq.track.ChannelTrack;

public class SongTitle extends JPanel implements TimeListener {

	private final Overview overview;
	private final JComboBox<Signature> timeSig = new JComboBox<>(Signature.values());
	private final JComboBox<Key> key = new JComboBox<>(Key.values());
	private final JComboBox<Scale> scale = new JComboBox<>(Scale.values());
	private final JLabel bar = new JLabel("0", JLabel.CENTER);
	@Getter private final SongCombo songs = new SongCombo();

	private final TimeProvider clock = JudahZone.getClock();

	private final Chords chords = JudahZone.getChords();
	@Getter private final ChordTrack chordTrack = new ChordTrack(JudahZone.getChords());
	private JToggleButton chordsBtn = new JToggleButton(ChordPlay.FANCY);

	private final ChannelTrack funTimes = JudahZone.getSeq().getMains();
	@Getter private final SongTrack mains;
	private JToggleButton mainsBtn = new JToggleButton(JudahZone.getMains().getName());


	public SongTitle(Overview overview) {;
		this.overview = overview;
		mains = new SongTrack(funTimes);
		chordsBtn.addActionListener(l->showChords());
		mainsBtn.addActionListener (l->showMeta());

		Box top = Gui.box(
			Box.createHorizontalStrut(3),
			Gui.resize(songs, Size.TITLE_SIZE),
			new Btn(Icons.SAVE, e->overview.save()),
			new Btn(" 🔁 ", e->overview.reload(), "Reload"),
			new Btn(" + Track ", e->new AddTrack(JudahZone.getSeq())),
			mainsBtn, chordsBtn, Box.createHorizontalGlue(),
			Gui.resize(timeSig, Size.SMALLER_COMBO),
			Gui.resize(key, Size.MICRO),
			Gui.resize(scale, Size.MEDIUM_COMBO),
			Gui.resize(bar, Size.MICRO));
		top.setBackground(Pastels.BUTTONS);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(top);

		scale.addActionListener(e->overview.getSong().setScale((Scale) scale.getSelectedItem()));
		key.addActionListener(e->overview.getSong().setKey((Key) key.getSelectedItem()));
		timeSig.addActionListener(e->overview.getSong().setTimeSig((Signature)timeSig.getSelectedItem()));

		Gui.resize(chordTrack, Overview.CHORDS);
		clock.addListener(this);
	}

	public void update() {
		if (timeSig.getSelectedItem() != clock.getTimeSig())
			timeSig.setSelectedItem(clock.getTimeSig());
		Song song = overview.getSong();

		updateChords();

		if (song == null) return;
		if (song.getKey() != null && key.getSelectedItem() != song.getKey())
			key.setSelectedItem(song.getKey());
		if (song.getScale() != null && scale.getSelectedItem() != song.getScale())
			scale.setSelectedItem(song.getScale());
	}

	private void flush() {
		getParent().invalidate();
		getParent().doLayout();
		repaint();
	}

	void showChords() {
		boolean show = chordsBtn.isSelected();
		if (show)
			add(chordTrack);
		else
			remove(chordTrack);
		flush();
	}

	void showMeta() {
		boolean show = mainsBtn.isSelected();
		if (show) {
			add(mains);
			if (mains.isExpanded() == false)
				mains.expand();
			else flush();
		}
		else {
			remove(mains);
			flush();
		}
	}

	public boolean isMainsShowing() {
		return mainsBtn.isSelected();
	}

	void updateChords() {
		boolean show = !chords.isEmpty();
		if (show && chordsBtn.isSelected() == false)
			chordsBtn.doClick();
		else if (!show && chordsBtn.isSelected())
			chordsBtn.doClick();
	}

	public void updateBar(int value) {
		Threads.execute(()->bar.setText("" + (value + 1)));
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.BARS)
			updateBar((int)value);
	}

}
