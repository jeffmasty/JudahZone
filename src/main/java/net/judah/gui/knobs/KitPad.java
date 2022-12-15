package net.judah.gui.knobs;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;

import lombok.Getter;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumSample;
import net.judah.drumkit.DrumType;
import net.judah.drumkit.Pad;
import net.judah.gui.Pastels;
import net.judah.widgets.FxButton;
import net.judah.widgets.Knob;
import net.judah.widgets.Knob.KnobListener;

public class KitPad extends Pad implements KnobListener {
	
	@Getter private final DrumKit kit;
	private final KitKnobs view;
	@Getter private final Knob knob;
	@Getter private final DrumSample sample;
	
	public KitPad(KitKnobs view, DrumType type) {
		super(type);
		this.view = view;
		this.kit = view.getKit();
		this.sample = findSample();
		knob = new Knob(this);
		knob.setKnobColor(Pastels.RED);
		if (type == DrumType.OHat) {
			JCheckBox choke = new JCheckBox();
			choke.setToolTipText("OHat shuts off when CHat plays");
			choke.setSelected(kit.isChoked());
			top.add(choke);
			choke.addItemListener(e->kit.setChoked(!kit.isChoked()));
		}
		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				kit.play(getSample(), true, 100);
			}
		});
		
		bottom.add(knob);
 		bottom.add(new FxButton(sample));
		update();
	}

	private DrumSample findSample() {
		return kit.getSamples()[type.ordinal()];
	}

	@Override
	public void update() {
		top.setBackground(getSample().isActive() ? Pastels.MY_GRAY : Pastels.EGGSHELL);
	}

	@Override
	public void knobChanged(int val) {
		view.doKnob(type.ordinal(), val);
	}

}
