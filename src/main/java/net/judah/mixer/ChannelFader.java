package net.judah.mixer;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.util.Constants;
import net.judah.widgets.RainbowFader;

/**Mixer view
 * <pre>
  each Channel has:
		Menu
		Volume Fader 
		LEDs  {fx status}
		Channel Icon </pre>
 * @author judah */
public abstract class ChannelFader extends JPanel implements Pastels {
	
	@Getter protected final Channel channel;
	@Getter protected final JPanel banner = new JPanel();

	@Getter protected final JToggleButton mute = new JToggleButton("mute");
	@Getter protected final JToggleButton fx = new JToggleButton("fx");
	@Getter protected final JToggleButton sync = new JToggleButton("sync");


	protected void mute() {
		boolean target = channel.isOnMute();
		boolean selected = mute.isSelected();
		if (target != selected)
			channel.setOnMute(selected);
        mute.setBackground(selected ? PURPLE : null);
	}

	
	protected RainbowFader volume;
	protected final JLabel title = new JLabel("", CENTER);
	protected JPanel sidecar = new JPanel(new GridLayout(3, 1));
	private final LEDs indicators;
	
	public ChannelFader(Channel channel) {
		this.channel = channel;
		setBorder(BorderFactory.createDashedBorder(Color.DARK_GRAY));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(BUTTONS);
		
		indicators = new LEDs(channel);
		volume = new RainbowFader(vol -> {
			channel.getGain().setVol(volume.getValue());
			MainFrame.update(channel);
		});
		volume.setOpaque(true);
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		title.setFont(Constants.Gui.BOLD13);
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
		mute.addActionListener(e -> mute());

	}
	
	protected abstract Color thisUpdate(); 
	
	protected Component font(Component c) {
		c.setFont(Constants.Gui.FONT9);
		return c;
	}

	
	public final void update() {
		Color bg = thisUpdate();
		if (false == banner.getBackground().equals(bg)) 
			banner.setBackground(bg);
		indicators.sync();		
		updateVolume();
		fx.setSelected(channel.isPresetActive());
		fx.setBackground(fx.isSelected() ? BLUE : null);
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
