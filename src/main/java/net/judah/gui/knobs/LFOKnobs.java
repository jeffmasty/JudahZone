package net.judah.gui.knobs;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.fx.Effect;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.FxButton;
import net.judah.mixer.Channel;
import net.judah.mixer.DJJefe;

public class LFOKnobs extends KnobPanel {

	@Getter private final KnobMode knobMode = KnobMode.LFOz;
    @Getter private final JPanel title;
    @Getter private final Channel channel;
    @Getter private final LFOWidget lfo1, lfo2;
    @Getter private final CompressorWidget compressor;

    public LFOKnobs(final Channel ch, DJJefe mixer) {
    	this.channel = ch;
    	title = Gui.wrap(mixer.getCombo(ch), new FxButton(ch));
    	lfo1 = new LFOWidget(ch, ch.getLfo(), 1);
    	lfo2 = new LFOWidget(ch, ch.getLfo2(), 2);
    	compressor = new CompressorWidget(ch);

    	setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    	add(lfo1);
    	add(lfo2);
    	add(compressor);
		update();
		validate();
    }

	@Override public void pad1() {
		lfo2.getLfo().setActive(!lfo2.getLfo().isActive());
	}

	@Override public void pad2() {
		//channel.getTuner().setActive(!channel.getTuner().isActive());
		channel.getCompression().setActive(!channel.getCompression().isActive());
		MainFrame.update(channel);
		// MainFrame.update(this);
	}

	@Override
	public final void update() {
		lfo1.update();
		lfo2.update();
		compressor.update();
	}

	@Override
	public boolean doKnob(int idx, int data2) {
		if (lfo1.doKnob(idx, data2))
			return false;
		compressor.doKnob(idx, data2);
		return false;
	}

	public void update(Effect fx) {
		if (fx == lfo2.getLfo())
			lfo2.update();
		else
			lfo1.update();
	}

}
