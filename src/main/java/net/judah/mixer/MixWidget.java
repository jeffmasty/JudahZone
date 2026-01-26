package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import judahzone.widgets.RainbowFader;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.TogglePreset;
import net.judah.looper.Loop;

/**Mixer view <br/>
 * Each mixer channel has:
<pre>
	Icon/Btns
	FX LEDs
	Vol Fader/Gain
</pre>*/
public abstract class MixWidget extends JPanel implements Pastels, Updateable {
	private static final Dimension SIDECAR = new Dimension(50, 60);

	protected final Channel channel;
	protected final JLabel title = new JLabel("", CENTER);
	protected final JButton mute = new JButton("mute");
	protected final TogglePreset fx;
	protected final JToggleButton sync = new JToggleButton("sync");
	protected final JPanel banner = new JPanel();
	protected final JPanel sidecar = new JPanel(new GridLayout(3, 1, 0, 0));
	@Getter protected final FxLEDs indicators;
	protected final RMSIndicator rms;
	protected RainbowFader fader;
	protected final JComponent bottom;

	public MixWidget(Channel channel) {
		this.channel = channel;

		fx = new TogglePreset(channel);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);

		indicators = new FxLEDs(channel);
		fader = new RainbowFader(vol -> {
			channel.getGain().set(Gain.VOLUME, fader.getValue());
			MainFrame.update(channel);
		});
		rms = new RMSIndicator(channel);
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		title.setFont(Gui.BOLD13);
		if (channel.getIcon() == null) // TODO loops
			title.setText(channel.getName());
		else if (channel instanceof Loop == false)
            title.setIcon(channel.getIcon());
		banner.add(title);
		banner.add(sidecar);
		sidecar.setOpaque(false);
		Gui.resize(sidecar, SIDECAR);
		Gui.resize(title, SIDECAR);

		add(banner);
		add(indicators);
		add(Box.createVerticalStrut(1));

		bottom = Gui.wrap(rms, fader);
		add(bottom);
		add(Box.createVerticalStrut(1));

		addMouseListener(new MouseAdapter() {
			// TODO right/double click menu
			@Override public void mouseClicked(MouseEvent e) {
				MainFrame.setFocus(channel);}});
	}

	protected abstract Color thisUpdate();

	protected boolean quiet() {
		return channel.getGain().getGain() < 0.05f;
	}

	public void checkIcon() {
		if (channel.getIcon() != title.getIcon())
			title.setIcon(channel.getIcon());
	}

	@Override public final void update() {
		Color bg = thisUpdate();
		if (false == banner.getBackground().equals(bg))
			banner.setBackground(bg);
		fx.update(); // preset
		indicators.sync();
	}

	public void updateVolume() {
		if (channel.getVolume() != fader.getValue())
			fader.setValue(channel.getVolume());
		if (channel.isOnMute())
			fader.setBackground(Color.DARK_GRAY);
		else
			fader.setBackground(null);
	}

}
