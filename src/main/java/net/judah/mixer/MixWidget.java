package net.judah.mixer;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import net.judah.util.Pastels;

public abstract class MixWidget extends JButton implements MouseListener {
	protected final Channel channel;
	
	public MixWidget(Channel channel) {
		super();
		setBackground(Pastels.MY_GRAY);
		this.channel = channel;
		addMouseListener(this);
		setBorder(BorderFactory.createRaisedSoftBevelBorder());
	}
	
	public boolean isInput() {
		return channel instanceof LineIn;
	}
	
    // public void mouseClicked(MouseEvent e) // subclass overrides
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
	
}
