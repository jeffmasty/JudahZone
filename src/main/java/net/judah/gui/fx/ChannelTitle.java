package net.judah.gui.fx;

import java.awt.Color;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.drumkit.Sample;
import net.judah.gui.Gui;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;

public class ChannelTitle extends JPanel {

	private final Mute mute;
	private final PresetCheckBox presetActive = new PresetCheckBox();
	private final Channel channel;
	private final JLabel name;
	
	public ChannelTitle(Channel channel) {
		JPanel main = new JPanel();
		
		this.channel = channel;
		name = new JLabel(standard(), JLabel.CENTER);
		name.setFont(Gui.BOLD13);
		main.add(name);
		
		main.add(new JLabel(" fx:"));
		main.add(presetActive);
		main.add(new JLabel(" mute:"));
		mute = new Mute();
		main.add(mute); 
		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		add(main);
	}

	private String standard() {
		return channel instanceof Sample ? "Sample " + channel.getName() : channel instanceof Loop ? "" : channel.getName();
	}
	
	public void name(MultiSelect selected) {
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
	}
	
	private class PresetCheckBox extends JCheckBox {
        PresetCheckBox() {
            addItemListener(e -> {
            	setBackground(isSelected() ? Color.GREEN : null);
                channel.setPresetActive(isSelected());
            });
        }
    }

	private class Mute extends JCheckBox {
		boolean inUpdate = false;
		Mute() {
            addItemListener(e -> {
            	if (inUpdate) return;
                if (channel instanceof Instrument)
                	((Instrument)channel).setMuteRecord(isSelected());
                else
                	channel.setOnMute(isSelected());
                update();
            });
            
		}

		void update() {
			
			boolean muted = channel instanceof Instrument ? 
					((Instrument)channel).isMuteRecord() :
						channel.isOnMute();
			setSelected(muted);
		}
		
		void ignore() {
			inUpdate = true;
		}
		void listen() {
			inUpdate = false;
		}
	}

	public void update() {
		presetActive.setSelected(channel.isPresetActive());
		mute.ignore();
		mute.update();
		mute.listen();
	}

	
}
