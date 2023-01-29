package net.judah.gui.knobs;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.fx.CutFilter;
import net.judah.fx.Gain;
import net.judah.fx.Reverb;
import net.judah.gui.Pastels;
import net.judah.gui.knobs.KitKnobs.Modes;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;

public class KitPad extends JPanel implements KnobListener {
		protected static final Color borderColor = Pastels.PURPLE;

	private final DrumType type;
	private final KitKnobs view;
	private final Knob knob;
	private JCheckBox choke;
	private final JPanel top = new JPanel();
	private final JPanel bottom = new JPanel();
	
	public KitPad(KitKnobs view, DrumType t) {
		this.type = t;
		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(0, 1, 0, 0));
		JLabel nombre = new JLabel(type.name());
		top.add(nombre);
		top.setOpaque(true);
		bottom.setOpaque(true);
		add(top);
		add(bottom);
		setOpaque(true);
		
		this.view = view;
		knob = new Knob(this);
		knob.setKnobColor(Pastels.RED);
		if (type == DrumType.OHat) {
			choke = new JCheckBox();
			choke.setToolTipText("Stop OHat when CHat plays");
			top.add(choke);
			choke.addItemListener(e->view.getKit().setChoked(choke.isSelected()));
		}
		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				view.getKit().play(findSample(), true, 100);
			}
		});
		
		Btn fxWrapper = new Btn("", e->new FxButton(findSample()).doClick());
		fxWrapper.setIcon(FxButton.icon());
		
		bottom.add(knob);
 		bottom.add(fxWrapper);
	}

	public void update() {
		if (choke != null)
			if (view.getKit().isChoked() != choke.isSelected())
				choke.setSelected(view.getKit().isChoked());
		DrumSample sample = findSample();
		top.setBackground(sample.isActive() ? Pastels.DRUM_PAD : Pastels.EGGSHELL);
		int current = knob.getValue();
		switch(view.getMode()) {
			case Volume: 
				if (sample.getGain().getVol() != current)
					knob.setValue(sample.getGain().getVol()); 
				break;
			case Attack: 
				if (sample.getAttackTime() * 0.2f != current)
					knob.setValue((int) (sample.getAttackTime() * 0.2f));
				break;
			case Decay: 
				if (sample.getDecayTime() != current)
					knob.setValue(sample.getDecayTime()); 
				break;
			case HiCut: 
				if (sample.getHiCut().get(CutFilter.Settings.Frequency.ordinal()) != current)
					knob.setValue(sample.getCutFilter().get(CutFilter.Settings.Frequency.ordinal()));
				break;
			case Reverb: 
				if (sample.getReverb().get(Reverb.Settings.Wet.ordinal()) != current)
					knob.setValue(sample.getReverb().get(Reverb.Settings.Wet.ordinal()));
				break;
			case Pan: 
				if (sample.getGain().get(Gain.PAN) != current)
					knob.setValue(sample.getGain().getPan());
				break;
		}
	}

	@Override
	public void knobChanged(int val) {
		DrumSample sample = findSample();
		Modes mode = view.getMode();
		if (mode == Modes.Attack) {
			if (sample.getAttackTime() * 5 != val)
				sample.setAttackTime((int)(val * 0.2f)); 
		}
		else if (mode == Modes.Decay) {
			if (sample.getDecayTime() != val)
				sample.setDecayTime((val)); 
		}
		else if (mode == Modes.HiCut) {
			if (sample.getHiCut().get(CutFilter.Settings.Frequency.ordinal()) != val)
				sample.getHiCut().set(CutFilter.Settings.Frequency.ordinal(), val);
				sample.getHiCut().setActive(val < 99);
		}
		else if (mode == Modes.Reverb) {
			if (sample.getReverb().get(Reverb.Settings.Wet.ordinal()) != val)
				sample.getReverb().set(Reverb.Settings.Wet.ordinal(), val);
			sample.getReverb().setActive(val > 2); 
		}
		else if (mode == Modes.Volume) {
			if (sample.getGain().getVol() != val)
				sample.getGain().setVol(val); 
		}
		else if (mode == Modes.Pan) {
			if (sample.getGain().get(Gain.PAN) != val)
				sample.getGain().setPan(val);
		}
	}
	
	private DrumSample findSample() {
		return view.getKit().getSamples()[type.ordinal()];
	}

}
