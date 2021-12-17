package net.judah.mixer;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import net.judah.MainFrame;
import net.judah.ControlPanel;
import net.judah.util.Constants;


// 45 x 30


public class Thumbnail extends MixWidget {

	public static final Dimension preferred = new Dimension(45, 34);
	
	public Thumbnail(Channel channel) {
		super(channel);
		setPreferredSize(preferred);
		if (channel.getIcon() == null) {
			
			setFont(Constants.Gui.BOLD);
            setText(channel.getName());
		}
		else 
            setIcon(channel.getIcon());
		
		addMouseListener(new MouseAdapter() {
		@Override public void mouseClicked(MouseEvent e) {
			MainFrame.setFocus(channel);
		}});
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		ControlPanel.getInstance().setFocus(channel);
	}

}
