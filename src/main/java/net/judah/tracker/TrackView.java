package net.judah.tracker;

import static net.judah.util.Size.STD_HEIGHT;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.Panic;
import net.judah.settings.Channels;
import net.judah.util.Click;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Pastels;
import net.judah.util.RTLogger;
import net.judah.util.Slider;

// Name  File   midiOut Inst  Vol     xxxxxxxx
// ▶/■  pattern cycle  cust1 cust2   xxxxxxxx
@Getter
public class TrackView extends JPanel {

	
	private static final Dimension SLIDESZ = new Dimension(58, STD_HEIGHT);
	
	private final Track track;

	private final FileCombo filename; 
	private final Instrument instruments;
	private final MidiOut midiOut;
	
	
	private final JComboBox<String> pattern = new JComboBox<>();
	private ActionListener patternListener;
	
	private JToggleButton playWidget = new JToggleButton(" Play", false); // ▶/■ 
	private final Slider volume = new Slider(null);
	private final JComboBox<String> cycle;
	
	public TrackView(Track track) {
		setFont(Constants.Gui.FONT10);
		this.track = track;
		setLayout(new GridLayout(2, 5));
		filename = new FileCombo(track);
		midiOut = new MidiOut(track);
        Click outLbl = new Click(track.getName());
        outLbl.addActionListener( e -> {
            JackPort out = (JackPort)midiOut.getSelectedItem();
            MainFrame.setFocus(JudahZone.getChannels().byName(Channels.volumeTarget(out)));});
		volume.setPreferredSize(SLIDESZ);
		volume.addChangeListener(e -> {
			track.setGain(((Slider)e.getSource()).getValue() * .01f);
		});
		
		instruments = new Instrument(track);

        add(outLbl);
		add(filename);
        add(midiOut);
		add(instruments);
		add(volume);
		JPanel btns = new JPanel();
		btns.add(playWidget);
		JButton rec = new JButton("Rec");
		rec.addActionListener(e -> RTLogger.log(this, "Midi Record not implemented yet"));
		
		btns.add(rec);
		JButton edit = new JButton("Edit");
		edit.addActionListener(e -> {
			track.getClock().getTracker().setCurrent(track);
			MainFrame main = MainFrame.get();
			main.getTabs().setSelectedComponent(main.getBeatBox());});
		btns.add(edit);
		add(btns);
		
		playWidget.addActionListener((e) -> track.setActive(!track.isActive()));
		setBorder(Constants.Gui.NONE);
		fillPatterns();
		add(pattern);
		cycle = track.getCycle().createComboBox();
		add(cycle);
		
		add(new JLabel("[latch]", JLabel.CENTER));
		add(new JLabel("[arp]", JLabel.CENTER));
		update();
		
		filename.setFont(Gui.FONT11); 
		instruments.setFont(Gui.FONT11);
		midiOut.setFont(Gui.FONT11);
		pattern.setFont(Gui.FONT11);
		playWidget.setFont(Gui.FONT11);
		edit.setFont(Gui.FONT11);
		rec.setFont(Gui.FONT11);
		outLbl.setFont(Gui.FONT11);
	}
	
	public void fillPatterns() {
		if (patternListener == null) {
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
		for (Pattern p : track)
			pattern.addItem(p.getName());
		pattern.addActionListener(patternListener);
	}

	public void update() {
		playWidget.setText(track.isActive() ? " Stop" : " Play");
		playWidget.setBackground(track.isActive() ? Pastels.GREEN : Pastels.EGGSHELL);
		volume.setValue((int) (track.getGain() * 100f));
		if (midiOut.getSelectedItem() != track.getMidiOut()) {
			JackPort old = (JackPort) midiOut.getSelectedItem();
			// there is a change
			midiOut.setSelectedItem(track.getMidiOut());
			if (old != null)
				new Panic(old).start();
		}
	}

//	public void redoInstruments() {
//		if (instruments instanceof Instrument == false) return;
//		instruments.fillInstruments();
//	}


	public boolean process(int knob, int data2) {
		switch (knob) {
			case 0: // file
				track.selectFile(data2);
			case 1: // midiOut
				JackPort port = (JackPort)Constants.ratio(data2, midiOut.getAvailable());
				JackPort old = track.getMidiOut();
				if (old == port)
					return false;
				track.setMidiOut(port);
				// redoInstruments();
				if (old != null) 
						new Panic(old).start();
				return true;
			case 2:   // TODO
					int idx = 1 + Constants.ratio(data2, getInstruments().getItemCount() - 2);
					
					//	Instrument.set(track, idx);
				//	selectInstrument(idx);
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
//					int idx = Constants.ratio(data2, JudahKit.values().length - 1);
//					int midi = GMDrum.values()[idx].getMidi();
//					// iterate patterns, changing track beats to new midi instrument
//					for (Grid pattern : track) 
//						pattern.setReference(new MidiBase(midi));
				}
				return true;
			case 7: // vol2
//				setVol2(data2 * 0.01f); 
				return true;
		}
		return false;		
	}


	
}
