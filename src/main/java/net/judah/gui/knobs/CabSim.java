package net.judah.gui.knobs;

import static net.judah.fx.Convolution.Settings.Cabinet;
import static net.judah.fx.Convolution.Settings.Wet;

import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import net.judah.JudahZone;
import net.judah.fx.Convolution;
import net.judah.fx.MonoFilter;
import net.judah.fx.Convolution.Stereo;
import net.judah.fx.MonoFilter.Settings;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.widgets.DoubleSlider;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.util.Constants;

public class CabSim extends JPanel implements Updateable {

	private final JComboBox<String> cabinets = new JComboBox<String>(JudahZone.getIR().getNames());
	private static final int FREQUENCY = Settings.Frequency.ordinal();


	private final Instrument channel;
	private final Convolution ir;

	private final FxTrigger lbl;
	private final JSlider wet = new JSlider(0, 100, 90);
	private DoubleSlider filter;

	public CabSim(Channel ch) {
		this.channel = ch instanceof Instrument i ? i : null;
		this.ir = ch.getIR();
		lbl = new FxTrigger("Cabinet", ir, ch);
		lbl.setOpaque(true);
		((JLabel)cabinets.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		Gui.resize(wet, (ir instanceof Stereo ? Size.COMBO_SIZE : Size.MODE_SIZE));

		boolean isMono = !ch.isStereo() && ch instanceof Instrument;
		int cols = isMono ? 3 : 2;
		setLayout(new GridLayout(2, cols));

		add(lbl);
		add(Gui.wrap(wet));
		if (isMono) {
			filter = new DoubleSlider( ((Instrument)ch).getHp(), FREQUENCY, ((Instrument)ch).getLp(), FREQUENCY);
			filter.setColors(LFOWidget.RANGE);
			add(filter);
		}

		add(cabinets);
		add(new JLabel(" Dry / Wet ", JLabel.CENTER));

		if (isMono)
			add(new JLabel(" LoCut / HiCut ", JLabel.CENTER));

		update();
		wet.addChangeListener(e->ir.set(Wet.ordinal(), wet.getValue()));
		cabinets.addActionListener(e->ir.set(Cabinet.ordinal(), cabinets.getSelectedIndex()));

	}

	@Override public void update() {
		lbl.setBackground(ir.isActive() ? Pastels.YELLOW : null);
		if (wet.getValue() != ir.get(Wet.ordinal()))
			wet.setValue(ir.get(Wet.ordinal()));
		if (cabinets.getSelectedIndex() != ir.get(Cabinet.ordinal()))
			cabinets.setSelectedIndex(ir.get(Cabinet.ordinal()));
		if (filter != null)
			filter.update();
	}

	public void doKnob(int idx, int data2) {
		if (idx == 4)
			cabinets.setSelectedIndex(Constants.ratio(data2 - 1, cabinets.getItemCount()));
		else if (idx == 5)
			wet.setValue(data2);
		else if (filter != null) {
			if (idx == 6)
				channel.getHp().setFrequency(MonoFilter.knobToFrequency(data2));
			else if (idx == 7)
				channel.getLp().setFrequency(MonoFilter.knobToFrequency(data2));

			MainFrame.update(this);
		}
	}

}
