package net.judah.sampler;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import judahzone.gui.Gui;
import lombok.Getter;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.looper.SoloTrack;

public class SampleKnobs extends KnobPanel {

	public static enum TABS {Pads, MP3, Phone, Siren, Stenzel} // DB, grains
	@Getter private final KnobMode knobMode = KnobMode.Sample;
	@Getter private final JPanel title = Gui.wrap(new JLabel(""));

	private final SamplePads pads;
	private final BoomBox boombox;
	private final PhoneSynth phone;
	private final SirenSynth shepard;
	// private final VoiceBox stenzel;

	private final JTabbedPane content;

	public SampleKnobs(Sampler sampler, PhoneSynth phoneSynth, SirenSynth shepardRisset, SoloTrack s) {

		boombox = new BoomBox(sampler);
		phone = phoneSynth;
		shepard = shepardRisset;
		pads = new SamplePads(sampler);
		// stenzel = new VoiceBox(sampler);

		content = new JTabbedPane();
		content.addTab(TABS.Pads.name(), pads);
		content.addTab(TABS.MP3.name(), boombox);
		content.addTab(TABS.Phone.name(), phone);
		content.addTab(TABS.Siren.name(), shepard);
		// content.addTab(TABS.Stenzel.name(), stenzel);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(content);
		update();
		validate();

	}

	public void update(Sample samp) {
		pads.update(samp);
	}

	@Override public boolean doKnob(int idx, int value) {
		Component tab = content.getSelectedComponent();
		if (tab == pads)
			pads.doKnob(idx, value);
		else if (tab == boombox)
			boombox.doKnob(idx, value);
		else if (tab == phone)
			phone.doKnob(idx, value);
		else if (tab == shepard)
			shepard.doKnob(idx, value);
//		else if (tab == stenzel)
//			stenzel.doKnob(idx, value);
		return true;
	}

	@Override public void update() {
		// if mode == pads
		pads.update();
	}


	@Override public void pad1() { // fwd
		Component tab = content.getSelectedComponent();
		if (tab == phone)
			phone.dial(-1); // ringtone
		else if (tab == boombox)
			boombox.play(!boombox.isPlaying());

		else if (tab == shepard)
			shepard.toggle();
	}

	@Override public void pad2() { // next tab (circular)
		int idx = content.getSelectedIndex();
		idx = (idx + 1) % content.getTabCount();
		content.setSelectedIndex(idx);
	}


}
