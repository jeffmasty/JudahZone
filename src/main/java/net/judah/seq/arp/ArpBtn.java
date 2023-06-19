package net.judah.seq.arp;

import java.awt.Color;

import javax.swing.JButton;

public class ArpBtn extends JButton {
	
	private final Arp arp;
	private final Mode mode;
	private final Color color;

	// super(" ◉ "); super(" 🔁 ");
	public ArpBtn(Arp arp, String txt, Mode mode, Color active) {
		super(txt);
		this.arp = arp;
		this.mode = mode;
		this.color = active;
		addActionListener(e->arp.setMode(arp.getMode() == mode ? Mode.Off : mode));
		setOpaque(true);
	}
	
	public void update() {
		setBackground(arp.getMode() == mode ? color : null);
	}

}
