package net.judah.gui;

import static net.judah.api.Notification.Property.*;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.jaudiolibs.jnajack.JackTransportState;

import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.midi.JudahClock;
import net.judah.seq.MidiTrack;
import net.judah.seq.TrackList;
import net.judah.widgets.RainbowFader;
import net.judah.widgets.TrackButton;

public class MiniSeq extends JPanel implements TimeListener {
	private final Dimension TRX = new Dimension(Size.WIDTH_KNOBS / 2 - 15, 85);
	private final Border highlight = BorderFactory.createRaisedSoftBevelBorder();

	private final TrackList tracks;
	private final JudahClock clock;
	private final JToggleButton start = new JToggleButton("Play");
	private final JLabel current = new JLabel(".", SwingConstants.CENTER);
	private final ArrayList<TrackButton> btns = new ArrayList<>();
	
	public MiniSeq(TrackList tracks, JudahClock clock) {
		this.tracks = tracks;
		this.clock = clock;
		clock.addListener(this);
		start.setSelected(clock.isActive());
        start.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3) 
        			clock.reset(); // debug
            	else if (clock.isActive()) 
                    clock.end();
                else
                    clock.begin(); }});
                JPanel actives = new JPanel(); 
        actives.setBorder(new LineBorder(Pastels.MY_GRAY, 1));
        Dimension plusOne = new Dimension(TRX.width + 1, TRX.height + 1);
        actives.setPreferredSize(plusOne);
        actives.setMaximumSize(plusOne);
        actives.setLayout(new GridLayout(2, 5, 1, 1));
        actives.setOpaque(true);
        
        tracks.forEach(t->btns.add(new TrackButton(t)));

        // [d1 d2 d3 d4 s5]
        // [s1 s2 s3 s4 s6]
        for (int i = 0; i < 4; i++) 
        	actives.add(btns.get(i));
        actives.add(btns.get(8));
        for (int i = 4; i < 8; i++)
        	actives.add(btns.get(i));
        actives.add(btns.get(9));
        
        btns.get(8).setFont(btns.get(8).getFont().deriveFont(Font.ITALIC));
        btns.get(9).setFont(btns.get(9).getFont().deriveFont(Font.ITALIC));
        
        JPanel seqTitle = new JPanel(new GridLayout(0, 2));
        seqTitle.add(start);
        seqTitle.add(current);

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(seqTitle);
        add(actives);

	}

	public void update() {
		MidiTrack t = tracks.getCurrent();
		current.setText(t.getName() + "/" + t.getCurrent());
		current.setBackground(clock.isLooperSync() ? Pastels.YELLOW : null);
		btns.forEach(b->b.update());
		btns.forEach(b -> b.setBorder(t == b.getTrack() ? highlight : null));
	}

	@Override
	public void update(Property prop, Object value) {
		if (prop == STEP) {
			int step = 100 * (int)value / clock.getSteps();
			start.setBackground(RainbowFader.chaseTheRainbow(step));
		}
		else if (prop == TRANSPORT) {
			start.setText(value == JackTransportState.JackTransportStarting ? 
					"Stop" : "Play");
		}
	}

	
}
