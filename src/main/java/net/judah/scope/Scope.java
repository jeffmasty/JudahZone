package net.judah.scope;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.mixer.Channel;
import net.judah.util.AudioTools;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Memory;

public class Scope extends KnobPanel {

	public static enum Mode {Log, Tuner}; //, Vol, HZ}
    @Getter private final KnobMode knobMode = KnobMode.Tools;
    
    @Getter private final JPanel title = new JPanel();

	private final JComboBox<Mode> modeCombo = new JComboBox<>(Mode.values());
	private final HashMap<Mode, ScopeView> tools = new HashMap<>();
	private final List<Channel> channels;
	private final Memory mem;
//	private final Spectrum spectrum = new Spectrum();
//	private final ChannelVolume plot;

	public Scope(List<Channel> user, Memory mem) {
		this.channels = user;
		this.mem = mem;
		setLayout(new GridLayout(1, 1));

		tools.put(Mode.Log, Console.getInstance());
		tools.put(Mode.Tuner, new GuitarTuner(this));
		
		modeCombo.setSelectedItem(Mode.Log);
		modeCombo.addActionListener(e->changeScope());
		title.add(modeCombo);
		changeScope();
		validate();
	}

	private void changeScope() {
		removeAll();
		add(tools.get(modeCombo.getSelectedItem()));
		repaint();
	}
	
	@Override public boolean doKnob(int idx, int value) {
		if (idx == 0) {
			Mode result = (Mode)Constants.ratio(value, Mode.values());
			if (modeCombo.getSelectedItem() != result)
				modeCombo.setSelectedItem(result);
		} 
		// TODO  else FWD to current ScopeView for zoom in/out x/y, etc
		return true; 
	}
	
	@Override public void pad1() { /* increment view ? */ }
	@Override public void pad2() { /* trigger? */ }
	@Override public void update() { /* knobs */ }
	
	public void update(float[][] stereo) {
		tools.get(modeCombo.getSelectedItem()).process(stereo);
	} 

	public void process() {
		if (MainFrame.getKnobMode() != KnobMode.Tools) 
			return;
		
		if (modeCombo.getSelectedItem() == Mode.Log)
			return;
		
		float[][] stereo = mem.getFrame();
		for (Channel ch : channels) { // copy and merge selected channels real-time audio
			AudioTools.mix(ch.getLeft(), stereo[Constants.LEFT]);
			AudioTools.mix(ch.getRight(), stereo[Constants.RIGHT]);
		}
		MainFrame.update(stereo); // compute off thread
	}
	
}
