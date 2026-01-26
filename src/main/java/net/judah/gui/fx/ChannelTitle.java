package net.judah.gui.fx;

import java.awt.Dimension;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.widgets.Btn;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.LineIn;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.looper.Loop;
import net.judah.midi.MidiInstrument;

public class ChannelTitle extends JPanel {

	protected final JButton mute;
	protected final JButton lfo;
	private final Channel channel;
	private final JLabel name;
	private final JudahZone zone;

	// TODO mute when LineIn change to Red when looper is rec
	public ChannelTitle(Channel ch, JudahZone judahZone) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		this.channel = ch;
		this.zone = judahZone;
		name = new JLabel(standard(), JLabel.CENTER);
		name.setFont(Gui.BOLD13);

		if (channel instanceof LineIn in)
			mute = new Btn("tape", e->in.setMuteRecord(!in.isMuteRecord()));
		else
			mute = new Btn("mute", e->channel.setOnMute(!channel.isOnMute()));

		lfo = new Btn("lfo", e->{
			if (MainFrame.getKnobMode() == KnobMode.LFO)
				channel.toggle(channel.getLfo());
			else MainFrame.setFocus(KnobMode.LFO);});
		Btn wav = new Btn("wav", e->MainFrame.setFocus(KnobMode.Tuner));
		mute.setFont(Gui.FONT10);
		lfo.setFont(Gui.FONT10);
		wav.setFont(Gui.FONT10);

		Gui.resize(name, new Dimension(Size.WIDTH_KNOBS - 3 * Size.TINY.width, Size.STD_HEIGHT));

		add(name);
//		if (channel == judahZone.getBass()) // clocked
//			add(sync(judahZone.getBass()));
		add(Gui.resize(mute, Size.TINY));
		add(Gui.resize(lfo, Size.TINY));
		add(Gui.resize(wav, Size.TINY));
		update();
	}

	private String standard() {
		return channel instanceof Loop ? "" : channel.getName();
	}

	public void name(List<Channel> selected) {
		if (selected.size() == 1) {
			if (channel.getIcon() != null)
				name.setIcon(channel.getIcon());
			name.setText(standard());
			return;
		}
		name.setIcon(null);
		StringBuffer buf = null;
		for (Channel c : selected) {
			if (buf == null)
				buf = new StringBuffer(c.getName());
			else buf.append(" ").append(c.getName());
		}
		name.setText(buf.toString());
		repaint();
	}

	public void update() {
		updateMute();
		lfo.setBackground(channel.isActive(channel.getLfo()) ? Pastels.BLUE : null);
	}

	public void updateMute() {
		if (channel instanceof LineIn in) {
			if (in.isMuteRecord())
				mute.setBackground(null);
			else if (zone.getLooper().isOnCapture())
				mute.setBackground(Pastels.RED);
			else
				mute.setBackground(Pastels.ONTAPE);
		}
		else
			mute.setBackground(channel.isOnMute() ? Pastels.PURPLE : null);
	}

	private JToggleButton sync(MidiInstrument i) {
		JToggleButton sync = new JToggleButton("sync");
		sync.setFont(Gui.FONT10);
		sync.addActionListener(l->zone.getMidi().synchronize(i));
		return sync;
	}
}
