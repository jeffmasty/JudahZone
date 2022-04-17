package net.judah.tracks;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.Border;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.JudahMidi;
import net.judah.settings.Channels;
import net.judah.tracks.Track.Type;
import net.judah.util.Click;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.Slider;

// Name  File   midiOut Inst  Vol     xxxxxxxx
// ▶/■  pattern cycle  cust1 cust2   xxxxxxxx
@Getter
public abstract class TrackView extends JPanel {

	protected final Track track;

	protected final FileCombo filename; 
	
	protected JComboBox<Integer> channel;
	protected final JComboBox<?> instruments;
	protected final MidiOut midiOut;

	protected JToggleButton playWidget = new JToggleButton(" Play", false); // ▶/■ 
	protected final Slider volume = new Slider(null);
	protected JComponent custom1;
	protected JComponent custom2;
	
	protected static final Dimension SLIDESZ = new Dimension(61, STD_HEIGHT);
	protected static final Dimension NAMESZ = new Dimension(107, STD_HEIGHT);

	public TrackView(Track track) {
		this.track = track;
		setLayout(new GridLayout(2, 5));
		filename = new FileCombo(track);
		midiOut = new MidiOut(track);
        Click outLbl = new Click(track.getName());
        outLbl.addActionListener( e -> {
            JackPort out = JudahMidi.getByName(midiOut.getSelectedItem().toString());
            MainFrame.setFocus(JudahZone.getChannels().byName(Channels.volumeTarget(out)));});
		volume.setPreferredSize(SLIDESZ);
		volume.addChangeListener(e -> {
			track.setGain(((Slider)e.getSource()).getValue() * .01f);
		});
		instruments = createInstrumentsCombo(track);

        add(outLbl);//new JLabel(track.getName(), JLabel.CENTER)); // TODO menu
        
		add(filename);
        add(midiOut);
		add(instruments);
		add(volume);
		add(playWidget);
		
		playWidget.addActionListener((e) -> track.setActive(!track.isActive()));

		// subclass controls follow...
		new Thread(() -> {
			Constants.sleep(750);
			update();
		}).start();
	}
	
	
	protected JComboBox<?> createInstrumentsCombo(Track track) {
		if (this instanceof StepTrackView) {
			return ((StepTrackView)this).createInstrumentCombo(0);
		}
		return new Instrument(track);
	}


	public void update() {
		playWidget.setText(track.isActive() ? " Stop" : " Play");
		if (track.isActive()) playWidget.setBackground(Pastels.GREEN);
		else playWidget.setBackground(Pastels.EGGSHELL);
		volume.setValue((int) (track.getGain() * 100f));
		if (midiOut.getSelectedItem() != track.getMidiOut()) {
			// there is a change
			midiOut.setSelectedItem(track.getMidiOut());
		}
	}

	@Override public void setBorder(Border border) {
		super.setBorder(border);
		if (track != null && track.getFeedback() != null)
			track.getFeedback().setBorder(border);
	}

	public static TrackView create(Track track) {
		if (track.getType() == Type.DRUM_KIT)
			return new KitView(track);
		if (track instanceof StepTrack)
			return new StepTrackView((StepTrack)track);
		if (track instanceof MidiTrack)
			return track.isDrums() ? new MidiDrums((MidiTrack)track) : new MidiView((MidiTrack)track);
		return null;
	}

	public void redoInstruments() {
		if (instruments instanceof Instrument == false) return;
		((Instrument)instruments).fillInstruments();
	}


	public void selectInstrument(int idx) {
		if (idx < 0) return;
		Instrument inst = (Instrument)instruments;
		instruments.removeActionListener(inst.getListener());
		instruments.setSelectedIndex(idx);
		instruments.addActionListener(inst.getListener());
	}
	
	//	private Component channelCombo() {
//		channel = new JComboBox<Integer>();
//		for (int i = 0; i < 16; i++)
//			channel.addItem(i);
//		if (isDrums()) {
//			channel.setSelectedItem(9);
//			channel.setEnabled(false);
//		}
//		return channel;
//	}
	
	

	
}
