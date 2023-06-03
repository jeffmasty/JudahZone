package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.RainbowFader;

/**Mixer view
 * <pre>
  each Channel has:
		Menu
		Volume Fader 
		LEDs  {fx status}
		Channel Icon </pre>
 * @author judah */
public abstract class MixWidget extends JPanel implements Pastels {
	
	@Getter protected final Channel channel;
	@Getter protected final JPanel banner = new JPanel();

	@Getter protected final JToggleButton mute = new JToggleButton("mute");
	@Getter protected final JToggleButton fx = new JToggleButton("fx");
	@Getter protected final JToggleButton sync = new JToggleButton("sync");

	protected RainbowFader volume;
	protected final JLabel title = new JLabel("", CENTER);
	protected JPanel sidecar = new JPanel(new GridLayout(3, 1));
	private final LEDs indicators;
	
	public MixWidget(Channel channel) {
		this.channel = channel;
		//setBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);
		
		indicators = new LEDs(channel);
		volume = new RainbowFader(vol -> {
			channel.getGain().set(Gain.VOLUME, volume.getValue());
			MainFrame.update(channel);
		});
		volume.setOpaque(true);
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		title.setFont(Gui.BOLD13);
		banner.add(title);
		banner.add(sidecar);
		sidecar.setOpaque(false);
		
		add(banner);
		add(indicators);
		add(volume);
		addMouseListener(new MouseAdapter() {
		// TODO right/double click menu
		@Override public void mouseClicked(MouseEvent e) {
			MainFrame.setFocus(channel);}});
		
		fx.addActionListener(e -> channel.setPresetActive(!channel.isPresetActive())); 

	}
	
	protected abstract Color thisUpdate(); 
	
	protected Component font(Component c) {
		c.setFont(Gui.FONT9);
		return c;
	}

	
	protected boolean quiet() {
		return channel.getGain().getGain() < 0.05f;
	}
	
	public final void update() {
		Color bg = thisUpdate();
		if (false == banner.getBackground().equals(bg)) 
			banner.setBackground(bg);
		indicators.sync();		
		updateVolume();
		fx.setSelected(channel.isPresetActive());
		fx.setBackground(fx.isSelected() ? YELLOW : null);
	}
	
	public void updateVolume() {
		if (channel.getVolume() != volume.getValue()) {
			volume.setValue(channel.getVolume());
			volume.repaint();
		}
		if (channel.isOnMute())
			volume.setBackground(PURPLE);
		else 
			volume.setBackground(null);
	}
	
	
}
