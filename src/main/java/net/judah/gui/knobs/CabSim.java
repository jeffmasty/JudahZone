package net.judah.gui.knobs;

import static net.judah.fx.Convolution.Settings.Cabinet;
import static net.judah.fx.Convolution.Settings.Wet;

import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import judahzone.util.Constants;
import judahzone.util.Folders;
import net.judah.fx.Convolution;
import net.judah.fx.Convolution.Stereo;
import net.judah.fx.MonoFilter;
import net.judah.fx.MonoFilter.Settings;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.fx.FxTrigger;
import net.judah.gui.widgets.DoubleSlider;
import net.judah.mixer.Channel;
import net.judah.mixer.IRDB;
import net.judah.mixer.Instrument;
import net.judahzone.gui.Gui;
import net.judahzone.gui.Pastels;
import net.judahzone.gui.Updateable;

public class CabSim extends JPanel implements Updateable {

	private static final IRDB IR = new IRDB(Folders.getIR());
	public static IRDB getDB() { return IR; }

	private final JComboBox<String> cabinets = new JComboBox<String>(IR.getNames());
	private static final int FREQUENCY = Settings.Frequency.ordinal();

	private final Channel channel;
	private final Convolution ir;

	private final FxTrigger lbl;
	private final JSlider wet = new JSlider(0, 100, 90);
	private DoubleSlider filter;

	public CabSim(Channel ch) {
		this.channel = ch;
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
		if (ch instanceof Instrument in && !ch.isStereo()) {
			filter = new DoubleSlider( in.getHp(), FREQUENCY, in.getLp(), FREQUENCY);
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
		lbl.setBackground(channel.isActive(ir) ? Pastels.YELLOW : null);
		if (wet.getValue() != ir.get(Wet.ordinal()))
			wet.setValue(ir.get(Wet.ordinal()));
		if (cabinets.getSelectedIndex() != ir.get(Cabinet.ordinal()))
			cabinets.setSelectedIndex(ir.get(Cabinet.ordinal()));
		if (filter != null)
			filter.update();
	}

	public void doKnob(int idx, int data2) {
		if (idx == 4) {
			cabinets.setSelectedIndex(Constants.ratio(data2 - 1, cabinets.getItemCount()));
			MainFrame.updateChannel(channel, ir);
		}
		else if (idx == 5) {
			wet.setValue(data2);
			MainFrame.updateChannel(channel, ir);
		}
		else if (filter != null) {
			if (idx == 6) {
				MonoFilter fx = ((Instrument)channel).getHp();
				fx.setFrequency(MonoFilter.knobToFrequency(data2));
				MainFrame.updateChannel(channel, fx);
			}
			else if (idx == 7) {
				MonoFilter fx = ((Instrument)channel).getLp();
				fx.setFrequency(MonoFilter.knobToFrequency(data2));
				MainFrame.updateChannel(channel, fx);
			}

		}
	}

}
