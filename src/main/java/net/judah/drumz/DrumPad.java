package net.judah.drumz;

import java.awt.event.MouseAdapter;

import javax.swing.JCheckBox;

import lombok.Getter;
import net.judah.util.FxButton;
import net.judah.util.Knob;
import net.judah.util.Knob.KnobListener;
import net.judah.util.Pastels;

public class DrumPad extends Pad implements KnobListener {
	
	@Getter private final DrumKit playa;
	private final KitView view;
	@Getter private final Knob knob;
	@Getter private final DrumSample sample;
	
	public DrumPad(KitView view, DrumType type) {
		super(type);
		this.view = view;
		this.playa = view.getDrumz();
		this.sample = findSample();
		knob = new Knob(this);
		if (type == DrumType.OHat) {
			JCheckBox choke = new JCheckBox();
			choke.setToolTipText("OHat shuts off when CHat plays");
			choke.setSelected(playa.isChoked());
			top.add(choke);
			choke.addItemListener(e->playa.setChoked(!playa.isChoked()));
		}
		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent e) {
				playa.play(getSample(), true, 100);
			}
		});
		
		bottom.add(knob);
		bottom.add(new FxButton(sample));
		update();
	}

	private DrumSample findSample() {
		return playa.getSamples()[type.ordinal()];
	}

	@Override
	public void update() {
		top.setBackground(getSample().isActive() ? Pastels.MY_GRAY : Pastels.EGGSHELL);
	}

	@Override
	public void knobChanged(int val) {
		view.knob(type.ordinal(), val);
	}

}
