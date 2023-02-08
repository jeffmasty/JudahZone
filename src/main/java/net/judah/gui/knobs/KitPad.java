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
import net.judah.gui.Pastels;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.FxButton;
import net.judah.gui.widgets.Knob;
import net.judah.gui.widgets.Knob.KnobListener;

public class KitPad extends JPanel implements KnobListener {
	private static final Color borderColor = Pastels.PURPLE;
	private static final float ATK_SCALE = 0.2f;
		
		
	private final DrumType type;
	private final KitKnobs view;
	private final Knob knob;
	private JCheckBox choke;
	private final JPanel top = new JPanel();
	private final JPanel bottom = new JPanel();
	
	public KitPad(KitKnobs view, DrumType t) {
		this.type = t;
		this.view = view;

		setBorder(new LineBorder(borderColor, 1));
		setLayout(new GridLayout(0, 1, 0, 0));
		JLabel nombre = new JLabel(type.name());
		top.add(nombre);
		top.setOpaque(true);
		bottom.setOpaque(true);
		add(top);
		add(bottom);
		setOpaque(true);
		
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
				if (sample.getVolume() != current)
					knob.setValue(sample.getVolume()); 
				break;
			case Attack: 
				if (sample.getAttackTime() != current * ATK_SCALE)
					knob.setValue((int) (sample.getAttackTime() / ATK_SCALE));
				break;
			case Decay: 
				if (sample.getDecayTime() != current)
					knob.setValue(sample.getDecayTime()); 
				break;
			case pArTy: 
				if (sample.getParty().get(CutFilter.Settings.Frequency.ordinal()) != current)
					knob.setValue(sample.getParty().get(CutFilter.Settings.Frequency.ordinal()));
				break;
			case Dist: 
				if (sample.getOverdrive().get(0) != current)
					knob.setValue(sample.getOverdrive().get(0));
				break;
			case Pan: 
				if (sample.getPan() != current)
					knob.setValue(sample.getPan());
				break;
		}
	}

	@Override
	public void knobChanged(int value) {
		DrumSample sample = findSample();
		switch(view.getMode()) {
			case Volume: 
				if (sample.getVolume() != value)
					sample.getGain().set(Gain.VOLUME, value);
				break;
			case Attack: 
				if (sample.getAttackTime() != (int)(value * ATK_SCALE))
					sample.setAttackTime((int)(value * ATK_SCALE)); 
				break;
			case Decay: 
				if (sample.getDecayTime() != value)
					sample.setDecayTime(value);
				break;
			case pArTy: 
				if (sample.getParty().get(CutFilter.Settings.Frequency.ordinal()) != value) {
					sample.getParty().set(CutFilter.Settings.Frequency.ordinal(), value);
					sample.getParty().setActive(value < 97);
				}
				break;
			case Dist: 
				if (sample.getOverdrive().get(0) != value) {
					sample.getOverdrive().set(0, value);
					sample.getOverdrive().setActive(value > 3);
				}
				break;
			case Pan: 
				if (sample.getPan() != value)
					sample.getGain().set(Gain.PAN, value);
				break;
		}
	}
	
	private DrumSample findSample() {
		return view.getKit().getSamples()[type.ordinal()];
	}

}
