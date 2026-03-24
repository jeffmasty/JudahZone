package net.judah.gui.drums;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import judahzone.drums.Drum;
import judahzone.drums.DrumUpdate;
import judahzone.drums.DrumUpdate.Mode;
import judahzone.widgets.SingleRow;
import net.judah.gui.drums.views.DrumView;
import net.judah.gui.drums.views.DrumView.DataLbl;
import net.judah.gui.widgets.Slider;

/*
Attack: <---x--->  xxxx %
*--
|
	  	  Graph
						  |
						--*
Decay: <---x--->   xxxx %
     	(shell: Pan)
*/
public class TwoStepEnv extends JPanel implements ChangeListener {

	private final Drum drum;
	private final TwoStepGraph graph;
	private final Slider attack = new Slider(0, DrumView.SZ.SLIDER_MAX, this);
	private final Slider decay = new Slider(0, DrumView.SZ.SLIDER_MAX, this);
	private final DataLbl atkD = new DataLbl();
	private final DataLbl dkD = new DataLbl();

	public TwoStepEnv(Drum d) {
		drum = d;
		graph = new TwoStepGraph();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(new SingleRow("Attack", attack, atkD));
		add(graph);
		add(new SingleRow("Decay", decay, dkD));
	}

	@Override public void stateChanged(ChangeEvent e) {
		Object src = e.getSource();
		if (src == attack)
			drum.getEdit().set(new DrumUpdate(drum, Mode.BASE, 2, attack.getValue()));
		else if (src == decay)
			drum.getEdit().set(new DrumUpdate(drum, Mode.BASE, 3, decay.getValue()));
	}

	public void update() {
		int atk = drum.getEnv().getAttack();
		graph.setAttackKnob(atk);
		atkD.setText(atk + " %");
		if (attack.getValue() != atk)
			attack.setValue(atk);

		int dk = drum.getEnv().getDecay();
		graph.setDecayKnob(dk);
		dkD.setText(dk + " %");
		if (decay.getValue() != dk)
			decay.setValue(dk);

	}


}
