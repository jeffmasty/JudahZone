package net.judah.gui.knobs.synth;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import judahzone.fx.op.Kernel.KernelParams;
import judahzone.gui.Gui;
import judahzone.synth.Synth;
import net.judah.gui.widgets.Slider;

/*
Atk/DK 	*--                -*    Sus/Rls
||  ||  |				    |	||  ||
||	|| 	|				    	||  ||
||	|| 	  		Graph			||  ||
% 	%  *-- 					|    %   %
*/
public class FourStepEnv extends JPanel implements KernelParams {

	private static Dimension DAT = new Dimension(50, 28);
	private static class DataLbl extends JLabel {
		public DataLbl(String txt) {
			super(txt, JLabel.LEFT);
			Gui.resize(this, DAT);
		}
	}

	private final class Pack extends Box {

		static Dimension SLIDE = new Dimension (DAT.width, 100);

		DataLbl data = new DataLbl("");
		Slider slider = new Slider();
		public Pack(String label, int param) {
			super(BoxLayout.Y_AXIS);
			slider.setOrientation(JSlider.VERTICAL);
			slider.setValue(synth.get(param));
			slider.addChangeListener(e-> synth.set(param, slider.getValue()));
			// Gui.resize(this, size);
			add(data);
			add(Gui.resize(slider, SLIDE));
			add(new DataLbl(label));
		}
		public void update(int val) {
			data.setText("" + val);
			if (slider.getValue() != val)
				slider.setValue(val);
		}
	}

	private final Synth synth;
	private final FourStepGraph graph;
	private final Pack atk;
	private final Pack dk;
	private final Pack sus;
	private final Pack rls;

	public FourStepEnv(Synth d) {
		synth = d;
		graph = new FourStepGraph();
		graph.setBorder(Gui.SUBTLE);

		atk = new Pack("Atk", ATK);
		dk = new Pack("Dk", DK);
		sus = new Pack("Sus", SUS);
		rls = new Pack("Rls", REL);

		Box graphPnl = new Box(BoxLayout.Y_AXIS);
		graphPnl.add(Box.createVerticalStrut(30));
		graphPnl.add(graph);
		graphPnl.add(Box.createVerticalStrut(35));

		// atk, dk, graph, sus, rls
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createHorizontalStrut(10));
		add(atk);
		add(dk);
		add(Box.createHorizontalStrut(4));
		add(graphPnl);
		add(Box.createHorizontalStrut(4));
		add(sus);
		add(rls);
		add(Box.createHorizontalStrut(10));

	}

	public void update() {
		int a = synth.get(ATK);
		int d = synth.get(DK);
		int s = synth.get(SUS);
		int r = synth.get(REL);
		atk.update(a);
		dk.update(d);
		sus.update(s);
		rls.update(r);
		// push synth knob values into the graph as fractions (0..1)
		graph.setAttackFraction(a * 0.01f);
		graph.setDecayFraction(d * 0.01f);
		graph.setSustainFraction(s * 0.01f);
		graph.setReleaseFraction(r * 0.01f);
	}


}