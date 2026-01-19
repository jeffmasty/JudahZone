package net.judah.gui.knobs;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import judahzone.api.FX;
import judahzone.gui.Gui;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.HQ;
import net.judah.gui.MainFrame;
import net.judah.midi.LFO;

public class LFOKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.LFO;
    @Getter private final JPanel title;
    @Getter private final Channel channel;
    @Getter private final LFOWidget lfo1, lfo2;
    @Getter private final CompressorWidget compressor;
    private final CabSim cabSim;
    private boolean upperKnobs = true;

    public LFOKnobs(final Channel ch, JComboBox<Channel> channels) {
    	this.channel = ch;
    	title = Gui.wrap(channels);
    	lfo1 = new LFOWidget(ch, ch.getLfo(), 1);
    	lfo2 = new LFOWidget(ch, ch.getLfo2(), 2);
    	compressor = new CompressorWidget(ch);
    	cabSim = new CabSim(ch);

    	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    	add(lfo1);
    	add(lfo2);
    	add(compressor);

    	add(cabSim);

		update();
		validate();
    }

	@Override public void pad1() {
		LFO lfo = upperKnobs ? channel.getLfo() : channel.getLfo2();
			channel.toggle(lfo);
	}

	@Override public void pad2() {
		channel.toggle(channel.getIR());
		MainFrame.update(cabSim);
	}

	@Override
	public final void update() {
		lfo1.update();
		lfo2.update();
		compressor.update();
		lfo1.bold(upperKnobs);
		lfo2.bold(!upperKnobs);
		cabSim.update();
	}

	@Override
	public boolean doKnob(int idx, int data2) {
		if (upperKnobs && lfo1.doKnob(idx, data2))
				return false;

		if (!upperKnobs && lfo2.doKnob(idx, data2))
				return false;

		if (HQ.isShift())
			cabSim.doKnob(idx, data2);
		else
			compressor.doKnob(idx, data2);
		return false;
	}

	public void update(FX fx) {
		if (fx == lfo2.getLfo())
			lfo2.update();
		else if (fx == lfo1.getLfo())
			lfo1.update();
		else if (fx == channel.getIR())
			cabSim.update();
		else if (fx == channel.getCompression())
			compressor.update();
	}

	public void upperLower() {
		upperKnobs = !upperKnobs;
		MainFrame.update(this);
	}

	/** toggle on/off LFO that has knob focus */
	public void toggle() {
		LFO target = upperKnobs ? channel.getLfo() : channel.getLfo2();
		channel.toggle(target);
	}

}
