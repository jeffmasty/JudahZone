package net.judah.tracker;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.Panic;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channels;
import net.judah.tracker.Track.Cue;
import net.judah.util.Click;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Pastels;
import net.judah.util.Slider;

// Name  File   midiOut Inst  Vol     xxxxxxxx
// ▶/■  pattern cycle  cust1 cust2   xxxxxxxx
@Getter
public class TrackView extends JPanel {

	public static final Dimension SLIDESZ = new Dimension(58, STD_HEIGHT);
	private static final Font font = Gui.FONT11;
	
	private final Track track;
	private final FileCombo filename; 
	private final ProgChange patch;
	@Getter private final MidiOut midiOut;
	private final JComboBox<String> pattern = new JComboBox<>();
	private ActionListener patternListener;
	private final JComboBox<Cue> cue = new JComboBox<>();
	private final JComboBox<String> custom2 = new JComboBox<>();
	private final DefaultListCellRenderer center = new DefaultListCellRenderer(); 
	private final Slider volume = new Slider(null);
	private final JComboBox<String> cycle;
	private JButton playWidget = new JButton(" Play"); // ▶/■ 
	private final ActionListener playListener = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			if (track.isActive() || track.isOnDeck())
				track.setActive(false);
			else 
				track.setActive(true);
		}
	};
	
	public TrackView(Track input) {
		
		this.track = input;
		setFont(font);
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		setLayout(new GridLayout(2, 5));
		filename = new FileCombo(track);
		midiOut = new MidiOut(track);
        Click outLbl = new Click(track.getName());
        outLbl.addActionListener( e -> {
            JackPort out = (JackPort)midiOut.getSelectedItem();
            MainFrame.setFocus(JudahZone.getChannels().byName(Channels.volumeTarget(out)));});
		volume.setPreferredSize(SLIDESZ);
		volume.addChangeListener(e -> {
			float gain = ((Slider)e.getSource()).getValue() * .01f;
			if (track.getGain() != gain)
				track.setGain(gain);
		});
		patch = new ProgChange(track);
		playWidget.addActionListener(playListener);
		
        add(outLbl);
		add(filename);
        add(midiOut);
        add(patch);
		add(volume);
		JPanel btns = new JPanel();
		btns.add(playWidget);
		JButton edit = new JButton("Edit");
		edit.addActionListener(e -> {
			Tracker.setCurrent(track);
			MainFrame.get().getBeatBox().changeTrack(track);			
			MainFrame.get().addOrShow(MainFrame.get().getBeatBox(), "BeatBox");
		});
		btns.add(edit);

		if (track.isSynth()) {
			JToggleButton mpk = new JToggleButton("MPK");
			mpk.addActionListener(e -> {
				track.setLatch(!track.isLatch());
				for (Track t : Tracker.getTracks()) {
					if (t.isLatch()) {
						Transpose.setActive(true);
						return;
					}
				}
				Transpose.setActive(false);
			});
			btns.add(mpk);
			mpk.setFont(font);
		}
		
		add(btns);
		
		setBorder(Constants.Gui.NONE);
		fillPatterns();
		add(pattern);
		cycle = track.getCycle().createComboBox();
		add(cycle);
		doCue();
			
		if (track.isDrums())
			Constants.timer(300, () -> 
				add(((DrumTrack)track).getCustomVol()));
		else 
			Constants.timer(300, () ->  
				add(((PianoTrack)track).getPortVol()));
		
		cycle.setFont(font);
		filename.setFont(font); 
		patch.setFont(font);
		midiOut.setFont(font);
		pattern.setFont(font);
		playWidget.setFont(font);
		edit.setFont(font);
		outLbl.setFont(font);

		update();
	}
	
	private void doCue() {
		cue.setRenderer(center);
		cue.setFont(font);
		for (Cue item : Track.Cue.values())
			cue.addItem(item);
		cue.setSelectedItem(track.getCue());
		cue.addActionListener( e -> { 
			Cue change = (Cue)cue.getSelectedItem();
			if (track.getCue() != change)
				track.setCue(change);});
		add(cue);
	}
	
	public void fillPatterns() {
		if (patternListener == null) {
			pattern.setRenderer(center);
			patternListener = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String name = "" + pattern.getSelectedItem();
				for (Pattern p : track)
					if (name.equals(p.getName())) {
						track.setCurrent(p);
						return;
					}
			}};
		}
		pattern.removeActionListener(patternListener);
		pattern.removeAllItems();
		for (Pattern p : track.toArray(new Pattern[track.size()]))
			pattern.addItem(p.getName());
		pattern.addActionListener(patternListener);
		filename.refresh();
	}

	public void update() {
		playWidget.setText(track.isActive() ? " Stop" : track.isOnDeck() ? " " + track.cue: " Play");
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? Pastels.YELLOW : Pastels.EGGSHELL);
		volume.setValue((int) (track.getGain() * 100f));
		
		if (midiOut.getSelectedItem() != track.getMidiOut()) {
			JackPort old = (JackPort) midiOut.getSelectedItem();
			// there is a change
			midiOut.setSelectedItem(track.getMidiOut());
			if (old != null)
				new Panic(old).start();
		}
		if (cycle.getSelectedIndex() != track.getCycle().getSelected())
			cycle.setSelectedIndex(track.getCycle().getSelected());
	}

	public boolean process(int knob, int data2) {
		switch (knob) {
			case 0: // file (settable)
				File[] folder = track.getFolder().listFiles();
				int idx = Constants.ratio(data2, folder.length + 1);
				filename.setSelectedIndex(idx);
				return true;
			case 1: // midiOut
				midiOut.setSelectedItem(Constants.ratio(data2, midiOut.getAvailable()));
				return true;
			case 2:  
				patch.setSelectedIndex(1 + Constants.ratio(data2, patch.getItemCount() - 2));
				return true;
			case 3: 
				track.setGain(data2 * 0.01f);
				return true;
			case 4: // pattern
				track.setCurrent(track.get(Constants.ratio(data2 -1, track.size())));
				return true;
			case 5: // cycle
				cycle.setSelectedIndex(Constants.ratio(data2 - 1, Cycle.CYCLES.length));
				return true;
			case 6: // GMDRUM 2
				if (track.isDrums()) {
					ArrayList<GMDrum> drumkit = ((DrumTrack)track).getKit();
					drumkit.set(drumkit.size() - 1, (GMDrum)Constants.ratio(data2, GMDrum.values()));
					((DrumEdit)track.getEdit()).fillKit();
				}
				return true;
			case 7: // vol2
				if (track.isDrums()) {
					ArrayList<Float> volume = ((DrumTrack)track).getVolume();
					volume.set(volume.size() - 1, data2 * 0.01f);
				}
				return true;
		}
		return false;		
	}


	
}
