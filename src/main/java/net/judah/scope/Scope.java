package net.judah.scope;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.mixer.Channel;
import net.judah.omni.AudioTools;
import net.judah.omni.Threads;
import net.judah.scope.waveform.WaveformKnobs;
import net.judah.util.Constants;
import net.judah.util.Memory;

public class Scope extends KnobPanel {

	public static enum Mode {Tuner, Wave}; //, OSC, HZ/FFT}
    @Getter private final KnobMode knobMode = KnobMode.TOOLS;

    @Getter private final JPanel title = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

	private final JComboBox<Mode> modeCombo = new JComboBox<>(Mode.values());
	private final HashMap<Mode, ScopeView> tools = new HashMap<>();
	private final List<Channel> channels;
	private final Memory mem;
	private ScopeView view;

//	private final Spectrum spectrum = new Spectrum();

	public Scope(List<Channel> user, Memory mem) {
		this.channels = user;
		this.mem = mem;
		setLayout(new GridLayout(1, 1));

		tools.put(Mode.Tuner, new GuitarTuner());
		tools.put(Mode.Wave, new WaveformKnobs(Size.KNOB_PANEL));

		modeCombo.addActionListener(e->Threads.virtual(()->changeScope()));
		modeCombo.setSelectedItem(Mode.Tuner);
		title.add(modeCombo);
	}

	private void changeScope() {
		removeAll();
		invalidate();
		view = tools.get(modeCombo.getSelectedItem());
		add(view);
		doLayout();
		repaint();
		view.repaint();
	}

	@Override public boolean doKnob(int idx, int value) {
		switch (idx) {
		case 0:
				final Mode result = (Mode)Constants.ratio(value, Mode.values());
				if (modeCombo.getSelectedItem() != result)
					Threads.execute(()->modeCombo.setSelectedItem(result));
			break;
		case 1: // TODO change channel, volume, filter?
			break;
		case 2:
			break;
		case 3:
			break;
		case 4: case 5: case 6: case 7:
			Threads.execute(()->tools.get(modeCombo.getSelectedItem()).knob(idx, value));
			break;
		default:
		}

		// TODO  else FWD to current ScopeView for zoom in/out x/y, etc
		return true;
	}

	@Override public void pad1() { /* increment view ? */ }
	@Override public void pad2() { /* trigger? */ }
	@Override public void update() { /* knobs */ }

	public void update(float[][] stereo) {
		view.process(stereo);
		view.repaint();
	}

	public void process() {
		if (MainFrame.getKnobMode() != KnobMode.TOOLS)
			return;

		float[][] stereo = mem.getFrame();
		for (Channel ch : channels) { // copy and merge selected channels real-time audio
			AudioTools.mix(ch.getLeft(), stereo[Constants.LEFT]);
			AudioTools.mix(ch.getRight(), stereo[Constants.RIGHT]);
		}
		MainFrame.update(stereo); // compute off thread
	}

//	public void channelSelected(Channel ch) {
//		if (MainFrame.getKnobMode() != KnobMode.Tools)
//			return;
//		tools.get(modeCombo.getSelectedItem()).channelSelected(ch);
//
//	}

}
