package net.judah.tracker;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.controllers.KorgMixer;
import net.judah.drumz.DrumType;
import net.judah.drumz.DrumzView;
import net.judah.drumz.GMKitView;
import net.judah.drumz.JudahDrumz;
import net.judah.midi.MidiCable;
import net.judah.midi.MidiPort;
import net.judah.midi.Panic;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channel;
import net.judah.mixer.Channels;
import net.judah.tracker.Track.Cue;
import net.judah.util.Click;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Pastels;
import net.judah.util.Size;
import net.judah.util.Slider;

@Getter
public class TrackView extends JPanel {

	public static final Dimension SLIDESZ = new Dimension(58, STD_HEIGHT);
	private static final Font font = Gui.FONT11;
	private static final Border normal = BorderFactory.
			createSoftBevelBorder(BevelBorder.RAISED, Pastels.MY_GRAY, Pastels.MY_GRAY.darker());
	private static final Border highlight = BorderFactory.
			createSoftBevelBorder(BevelBorder.RAISED, Pastels.GREEN, Pastels.GREEN.darker());
	
	private final Track track;
	private final FileCombo filename; 
	private final ProgChange patch;
	@Getter private final MidiCable midiOut;
	private final JComboBox<String> pattern = new JComboBox<>();
	private ActionListener patternListener;
	private final JComboBox<Cue> cue = new JComboBox<>();
	private final DefaultListCellRenderer center = new DefaultListCellRenderer(); 
	private final Slider volume = new Slider(null);
	private final JComboBox<String> cycle;
	@Getter JToggleButton transpose; // PianoTrack 
	private final JButton playWidget;
	private final ActionListener playListener = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			if (track.isActive() || track.isOnDeck())
				track.setActive(false);
			else 
				track.setActive(true);
	}};
	
	public TrackView(Track t) {
		this.track = t;
		setFont(font);
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		filename = new FileCombo(track);
		midiOut = new MidiCable(track);
        Click outLbl = new Click(track.getName());
        outLbl.addActionListener( e -> {
            MidiPort out = (MidiPort)midiOut.getSelectedItem();
            Channel focus = Channels.byName(out.toString(), JudahZone.getMixer().getChannels());
            MainFrame.setFocus(focus);});
        outLbl.setBorder(UIManager.getBorder("TitledBorder.border"));
		volume.setPreferredSize(SLIDESZ);
		volume.addChangeListener(e -> {
			float gain = ((Slider)e.getSource()).getValue() * .01f;
			if (track.getGain() != gain)
				track.setGain(gain);
		});
		patch = new ProgChange(track);

		playWidget = new JButton(track.getName()); // ▶/■ 
		playWidget.addActionListener(playListener);
		playWidget.setFont(Constants.Gui.BOLD);

		JButton edit = new JButton("Edit");
		edit.addActionListener(e -> {
			MainFrame main = JudahZone.getFrame();
			track.getTracker().setCurrent(track);
			JudahZone.getBeatBox().changeTrack(track);			
			main.addOrShow(JudahZone.getBeatBox(), JudahZone.getBeatBox().getName());
		});
		
		fillPatterns();
		cycle = track.getCycle().createComboBox();
		doCue();
			
		cycle.setFont(font);
		filename.setFont(font); 
		patch.setFont(font);
		midiOut.setFont(font);
		pattern.setFont(font);
		edit.setFont(font);
		outLbl.setFont(Constants.Gui.BOLD13);
		
		
		playWidget.setPreferredSize(new Dimension(150, 30));
		
		JPanel buttons = new JPanel();
		buttons.add(new JButton("Fx"));
		buttons.add(edit);
		buttons.add(new JButton("Rec"));

		JPanel top = new JPanel(new GridLayout(1, 3));
		top.add(Constants.wrap(playWidget));
		top.add(buttons);
		
		final JPanel knobs = new JPanel(new GridLayout(2, 4));
		knobs.add(filename);
		knobs.add(pattern);
		knobs.add(cycle);
		knobs.add(volume);
		knobs.add(cue);
		knobs.add(midiOut);
		knobs.add(patch);
		
		if (track.isSynth())  {
			doTranspose(buttons);
			Slider portVol = new Slider(e -> {
				Channel ch = Channels.byName(midiOut.toString(), JudahZone.getMixer().getChannels());
				int vol = ((Slider)e.getSource()).getValue();
				if (ch.getGain().getVol() != vol) {
					ch.getGain().setVol(vol);
					MainFrame.update(ch);
				}});
			knobs.add(portVol);
		}
		else {
			JButton viewKit = new JButton("Kit");
			final Dimension size = new Dimension((int)(Size.WIDTH_SONG / 3f * 2f), Size.HEIGHT_FRAME / 3);
			viewKit.addActionListener(e-> {
				JPanel view = track.getMidiOut().isExternal() ?
						new GMKitView(track) :
						new DrumzView((JudahDrumz)track.getMidiOut().getReceiver());
				KorgMixer.modalSet(view, size);});
			buttons.add(viewKit);
			ArrayList<Float> volume = ((DrumTrack)track).getVolume();
			Slider customVol = new Slider(e -> 
				volume.set(DrumType.Clap.ordinal(), ((Slider)e.getSource()).getValue() * .01f));
			knobs.add(customVol);
		}
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(top);
		add(knobs);
		
		update();
	}
	
	private void doTranspose(JPanel btns) {
		transpose = new JToggleButton("MPK");
		transpose.addActionListener(e -> {
			track.setLatch(!track.isLatch());
			for (Track t : track.getTracker().getTracks()) {
				if (t.isLatch()) {
					Transpose.setActive(true);
					return;
				}
			}
			Transpose.setActive(false);
		});
		transpose.setFont(font);
		btns.add(transpose);
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
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? Pastels.YELLOW : Pastels.EGGSHELL);
		volume.setValue((int) (track.getGain() * 100f));
		
		if (midiOut.getSelectedItem() != track.getMidiOut()) {
			MidiPort old = (MidiPort) midiOut.getSelectedItem();
			// there is a change
			midiOut.setSelectedItem(track.getMidiOut());
			if (old != null)
				new Panic(old).start();
		}
		if (cycle.getSelectedIndex() != track.getCycle().getSelected())
			cycle.setSelectedIndex(track.getCycle().getSelected());
		updateBorder(track.getTracker().getCurrent());
	}

	public boolean knob(int knob, int data2) {
		switch (knob) {
			case 0: // file (settable)
				File[] folder = track.getFolder().listFiles();
				int idx = Constants.ratio(data2, folder.length + 1);
				filename.setSelectedIndex(idx);
				return true;
			case 1: // pattern
				track.setCurrent(track.get(Constants.ratio(data2 -1, track.size())));
				return true;
			case 2:	 // cycle
				cycle.setSelectedIndex(Constants.ratio(data2 - 1, Cycle.CYCLES.length));
				return true;
  		case 3: 
				track.setGain(data2 * 0.01f);
				return true;
			case 4: 
				cue.setSelectedIndex(Constants.ratio(data2 -1, cue.getItemCount()));
			case 5: // midiOut
				midiOut.setSelectedItem(Constants.ratio(data2, midiOut.getItemCount()));
				return true;
			case 6: 
				patch.setSelectedIndex(1 + Constants.ratio(data2, patch.getItemCount() - 2));
				return true;
			case 7: // vol2?
				if (track.isDrums()) {
					ArrayList<Float> volume = ((DrumTrack)track).getVolume();
					volume.set(volume.size() - 1, data2 * 0.01f);
				} else {
					// ratio?
				}
				return true;
		}
		return false;		
	}

	public void updateBorder(Track current) {
		setBorder(track == current ? highlight : normal);
	}


	
}
