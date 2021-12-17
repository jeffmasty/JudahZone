package net.judah.effects.gui;

import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.mixer.Channel;

public class ChannelTitle extends JPanel {

	
	private final PresetCheckBox presetActive = new PresetCheckBox();
	
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel channel) {
		this.channel = channel;
		// BorderLayout with loop buttons on left, title in center and mutes/presets on right?
		//setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		name = new JLabel(channel.getName(), JLabel.CENTER);
		// add(Box.createHorizontalStrut(getWidth() / 4));
		if (channel.getIcon() != null)
			add(new JLabel(channel.getIcon(), JLabel.CENTER));
		add(name);
		
		// add(Box.createHorizontalStrut(getWidth() / 4));
		add(presetActive);
		
	}

	private class PresetCheckBox extends JCheckBox {
        public PresetCheckBox() {
            addItemListener(e -> {
                if (isSelected()) {
                    setBackground(Color.GREEN);
                    setOpaque(true);
                }
                else
                    setOpaque(false);
           //     if (inUpdate) return;
                if (channel.getPreset() == null) return;
                channel.getPreset().applyPreset(channel, isSelected());
            });
        }
    }

	public void update() {
		presetActive.setSelected(channel.isPresetActive());
	}

	
}
