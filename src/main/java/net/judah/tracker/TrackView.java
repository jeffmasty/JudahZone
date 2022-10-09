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
import net.judah.controllers.KnobMode;
import net.judah.drumz.DrumKit;
import net.judah.drumz.GMKitView;
import net.judah.drumz.KitView;
import net.judah.midi.MidiPort;
import net.judah.midi.ProgChange;
import net.judah.mixer.Channel;
import net.judah.mixer.Instruments;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthView;
import net.judah.tracker.Track.Cue;
import net.judah.util.*;
import net.judah.util.Constants.Gui;

@Getter
public class TrackView extends JPanel {

	public static final Dimension SLIDESZ = new Dimension(58, STD_HEIGHT);
	private static final Font font = Gui.FONT11;
	private static final Border normal = BorderFactory.
			createSoftBevelBorder(BevelBorder.RAISED, Pastels.MY_GRAY, Pastels.MY_GRAY.darker());
	private static final Border highlight = BorderFactory.
			createSoftBevelBorder(BevelBorder.RAISED, Pastels.GREEN, Pastels.GREEN.darker());
	
	private final Track track;
	private final Tracker tracker;
	private final FileCombo filename; 
	private final ProgChange progChange;
	private final JComboBox<String> receiver;
	private final JComboBox<String> pattern = new JComboBox<>();
	private ActionListener patternListener;
	private final JComboBox<Cue> cue = new JComboBox<>();
	private final DefaultListCellRenderer center = new DefaultListCellRenderer(); 
	private final Slider volume = new Slider(null);
	private final JComboBox<String> cycle;
	@Getter private JButton transpose; // PianoTrack 
	private JButton rec = new JButton("Rec");
	private final JButton playWidget;
	private final ActionListener playListener = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			if (track.isActive() || track.isOnDeck())
				track.setActive(false);
			else 
				track.setActive(true);
		}};
	
	public TrackView(Track t, Tracker parent) {
		this.track = t;
		this.tracker = parent;
		setFont(font);
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		filename = new FileCombo(track);
        Click outLbl = new Click(track.getName());
        outLbl.addActionListener( e -> {
            MidiPort out = track.getMidiOut().getMidiPort();
            Channel focus = Instruments.byName(out.toString(), JudahZone.getMixer().getChannels());
            MainFrame.setFocus(focus);});
        outLbl.setBorder(UIManager.getBorder("TitledBorder.border"));
		volume.setPreferredSize(SLIDESZ);
		volume.addChangeListener(e -> {
			float gain = volume.getValue() * .01f;
			if (track.getGain() != gain)
				track.setGain(gain);
		});
	
		receiver = t.isDrums() ? new JComboBox<String>() : JudahZone.getNotes().createMidiCable(this);
		progChange = new ProgChange(track.getMidiOut(), track.getCh());

		playWidget = new JButton(track.getName()); // ▶/■ 
		playWidget.addActionListener(playListener);
		playWidget.setFont(Constants.Gui.BOLD);
		playWidget.setOpaque(true);
		playWidget.setPreferredSize(new Dimension(150, 25));

		JButton edit = new JButton("Edit");
		edit.addActionListener(e -> {
			MainFrame main = JudahZone.getFrame();
			JudahZone.getTracker().setCurrent(track);
			JudahZone.getBeatBox().changeTrack(track);			
			main.addOrShow(JudahZone.getBeatBox(), JudahZone.getBeatBox().getName());
		});
		rec.setOpaque(true);
		rec.addActionListener(e->{
		track.setRecord(!track.isRecord());
		update();
		});
		
		fillPatterns();
		cycle = track.getCycle().createComboBox();
		doCue();
			
		cycle.setFont(font);
		filename.setFont(font); 
		progChange.setFont(font);
		pattern.setFont(font);
		receiver.setFont(font);
		edit.setFont(font);
		outLbl.setFont(Constants.Gui.BOLD13);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(new FxButton((Channel)track.getMidiOut()));
		buttons.add(edit);
		buttons.add(rec);

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(Box.createHorizontalGlue());
		top.add(playWidget);
		top.add(Box.createHorizontalGlue());
		top.add(buttons);
		
		final JPanel knobs = new JPanel(new GridLayout(2, 4));
		knobs.add(filename);
		knobs.add(pattern);
		knobs.add(cycle);
		knobs.add(volume);
		knobs.add(cue);
		knobs.add(receiver); // TODO
		knobs.add(progChange);
		
		if (track.isSynth())  {
			doSynth(buttons);
		}
		
		else {
			JButton viewKit = new JButton("Kit");
			final Dimension size = new Dimension((int)(Size.WIDTH_SONG / 3f * 2f), Size.HEIGHT_FRAME / 3);
			viewKit.addActionListener(e-> {
				JPanel view = track.getMidiOut().getMidiPort().isExternal() ?
						new GMKitView(track) :
						new KitView((DrumKit)track.getMidiOut(), null);
				new ModalDialog(view, size, KnobMode.Kit);
			
			});
			buttons.add(viewKit);
//			ArrayList<Float> volume = ((DrumTrack)track).getVolume();
//			customVol = new Slider(e -> 
//				volume.set(DrumType.Clap.ordinal(), ((Slider)e.getSource()).getValue() * .01f));
			knobs.add(new JLabel(" ")); // TODO
			}
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(top);
		add(knobs);
		
		update();
	}
	
	private void doSynth(JPanel btns) {
		
		transpose = new JButton("MPK");
		transpose.addActionListener(e -> {
			track.setLatch(!track.isLatch());
			for (Track t : Tracker.getAll()) {
				if (t.isLatch()) {
					Transpose.setActive(true);
					update();
					return;
				}
			}
			Transpose.setActive(false);
			update();
		});
		transpose.setFont(font);
		
		btns.add(transpose);

		if (track.getMidiOut() instanceof JudahSynth) {
			JButton synthView = new JButton(Icons.load("SynthView.png"));
			synthView.addActionListener(e-> {
				SynthView view = new SynthView((JudahSynth)track.getMidiOut());
				new ModalDialog(view, new Dimension(500, 300), KnobMode.Synth);
			});
			btns.add(synthView);
		}


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
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? Pastels.YELLOW : null);
		
		volume.setValue((int) (track.getGain() * 100f));
		if (cycle.getSelectedIndex() != track.getCycle().getSelected())
			cycle.setSelectedIndex(track.getCycle().getSelected());
		updateBorder(tracker.getCurrent());
		if (transpose != null)
			transpose.setBackground(((PianoTrack)track).isLatch() ? Pastels.PINK : null);
		rec.setBackground(track.isRecord() ? Pastels.RED : null);
		repaint();
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
			case 5: // TODO

				
				return true;
			case 6: 
				progChange.setSelectedIndex(1 + Constants.ratio(data2, progChange.getItemCount() - 2));
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
