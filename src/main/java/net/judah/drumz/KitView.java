package net.judah.drumz;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import lombok.Setter;
import net.judah.effects.CutFilter;
import net.judah.tracker.DrumTrack;
import net.judah.util.CenteredCombo;
import net.judah.util.FxButton;
import net.judah.util.Knob;
import net.judah.util.Pastels;
import net.judah.util.Slider;

public class KitView extends JPanel {

	@Setter @Getter private static KitView current;
	private boolean override;
	
	public static enum Modes {
		Volume, Pan, Attack, Decay, Reverb, HiCut
	}
	private class ModeButton extends JButton {
		final Modes mode;
		ModeButton(Modes mode) {
			super(mode.name());
			setOpaque(true);
			this.mode = mode;
			setBackground(null);
			addActionListener((e)-> {
				setMode(mode);
				updateMode();	
			}); 
			buttons.add(this);
		}
	}

	@Getter private final DrumKit drumz;
	@Setter @Getter volatile private static Modes mode = Modes.Pan;
	JComboBox<String> preset = new CenteredCombo<>();
	private ArrayList<DrumPad> pads = new ArrayList<>(DrumKit.TRACKS);
	private ArrayList<ModeButton> buttons = new ArrayList<>();
	
	public KitView(DrumKit beats, DrumTrack associated) {
		KitView.current = this;
		this.drumz = beats;
		setBorder(BorderFactory.createTitledBorder(beats.getName()));
		JPanel upper = new JPanel(new GridLayout(1, 4, 3, 3));
    	JPanel lower = new JPanel(new GridLayout(1, 4, 3, 3));
    	setOpaque(true);
    	for(int i = 0; i < 4; i++)
    		pad(i, upper);
    	for(int i = 4; i < 8; i++)
    		pad(i, lower);
    	
    	JPanel knobs = new JPanel();
		knobs.setLayout(new BoxLayout(knobs, BoxLayout.PAGE_AXIS));
    	knobs.add(upper);
    	knobs.add(lower);
    	
    	for (String s : DrumDB.getKits())
    		preset.addItem(s);

    	if (associated == null)
    		verticalButtons(knobs);
    	else 
    		trackView(associated, knobs);
    	
    	update();
    	preset.addActionListener(e->beats.setKit("" + preset.getSelectedItem()));
	}

	private void verticalButtons(JPanel knobs) {
		JPanel btns = new JPanel();
    	btns.setLayout(new GridLayout(4, 2, 5, 5));
    	for (Modes m : Modes.values()) 
    		btns.add(new ModeButton(m));
    	JPanel buttons = new JPanel();
    	buttons.setLayout(new BoxLayout(buttons, BoxLayout.PAGE_AXIS));
    	buttons.add(preset);
    	buttons.add(btns);
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	add(knobs);
    	add(buttons);
	}

	private void trackView(DrumTrack track, JPanel knobs) {
		JPanel trackWidget = new JPanel();
		
		// TODO track file / pattern / cue
		trackWidget.add(preset);
		trackWidget.add(new FxButton(drumz));
		Slider volume = new Slider(null);
		volume.setValue((int)(track.getGain() * 100));
		volume.addChangeListener(e -> {;
			float gain = volume.getValue() * .01f;
			if (track.getGain() != gain)
				track.setGain(gain);
		});
		trackWidget.add(volume);
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(trackWidget);
		add(knobs);
		
		
		// TODO   knobHL/kit/trackFile/pattern
	}
	
	private void pad(int track, JPanel panel) {
		DrumPad pad = new DrumPad(this, DrumType.values()[track]);
		pads.add(pad);
		panel.add(pad);
	}
	
	public void update() {
		if (drumz.getKit() != null)
			if (preset.getSelectedItem() != drumz.getKit().getFolder().getName()) 
				preset.setSelectedItem(drumz.getKit().getFolder().getName());
			
		for (ModeButton b : buttons) 
			b.setBackground(mode == b.mode ? Pastels.GREEN : null);
	}

	public void update(DrumSample o) {
		for (DrumPad p : pads) 
			if (p.getSample() == o) 
				p.update();
		
	}
	
	void updateMode() {
		for (DrumPad p : pads) {
			Knob knob = p.getKnob();
			DrumSample s = p.getSample();
			override = true;
			switch (mode) {
				case Volume: 
					knob.setValue(s.getGain().getVol()); 
					break;
				case Pan: 
					knob.setValue(s.getGain().getPan());
					break;
				case Attack: 
					knob.setValue(s.getAttackTime() * 5);
					break;
				case Decay: 
					knob.setValue(s.getDecayTime());
					break;
				case HiCut: 
					knob.setValue(CutFilter.frequencyToKnob(s.getHiCut().getFrequency()));
					break;
				case Reverb: 
					knob.setValue((int)(s.getReverb().getWet() * 100));
					break;
			}		
		}
		update();
		override = false;
	}
	
	public void knob(int idx, int data2) {

		// phase 1: possibly feed Knob gui from Midi Controller
		if (pads.get(idx).getKnob().getValue() != data2) {
			pads.get(idx).getKnob().setValue(data2);
			return;
		}
		
		if (override) return;
		
		// phase 2: update model
		DrumSample s = drumz.getSamples()[idx]; 
		switch(mode) {
			case Volume: 
				s.getGain().setVol(data2); 
				break;
			case Attack: 
				s.setAttackTime((int)(data2 * 0.2f)); 
				break;
			case Decay: 
				s.setDecayTime((data2)); 
				break;
			case HiCut: 
				float hz = CutFilter.knobToFrequency(data2);
				s.getHiCut().setFrequency(hz); 
				s.getHiCut().setActive(data2 < 98);
				break;
			case Reverb: 
				s.getReverb().setWet(data2 * 0.01f); 
				s.getReverb().setActive(data2 > 2); 
				break;
			case Pan: 
				s.getGain().setPan(data2);
				break;
		}
	}

}
