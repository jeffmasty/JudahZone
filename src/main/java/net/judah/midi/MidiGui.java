package net.judah.midi;

import static net.judah.util.Constants.max;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KnobMode;
import net.judah.looper.LoopWidget;
import net.judah.songs.SmashHit;
import net.judah.tracker.Track;
import net.judah.util.*;

/** clock tempo, loop length, setlist, midi cables */
public class MidiGui extends JPanel implements TimeListener {

	private final JudahMidi midi;
	private final JudahClock clock;
	private final Jamstik jamstik;
	
	private final JButton menu = new JButton("Zone");
	private final JToggleButton start = new JToggleButton("Play");
    private final JLabel beatLbl = new JLabel("Beat:", JLabel.CENTER);
    private final JLabel beat = new JLabel("0", JLabel.CENTER);
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
    private final JComboBox<Integer> sync = new JComboBox<>();
    @Getter private final ProgChange calf;
    @Getter private final ProgChange fluid;
    @Getter private final SettableCombo<SmashHit> setlist = new SettableCombo<>(()->JudahZone.loadSong());
	@Getter private final CenteredCombo<MidiPort> mpk;
    
	private final JPanel mpkPanel = new JPanel();
	@Getter private final JudahMenu popup = new JudahMenu();
	private final Border NONE = BorderFactory.createLineBorder(Pastels.BUTTONS, 4);

	public MidiGui(JudahMidi midi, JudahClock clock, Jamstik jamstik) {
    	this.midi = midi;
		this.clock = clock;
    	clock.addListener(this);
    	this.jamstik = jamstik;
    	jamstik.setFrame(this);
    	
    	setBorder(NONE);
    	setOpaque(true);
    	setLayout(new GridLayout(1, 3));
		calf = new ProgChange(midi.getCalfOut(), 0);
		fluid = new ProgChange(midi.getFluidOut(), 0);
		mpk = new CenteredCombo<>();
		
		new Thread(()-> 
			initialize()).start();
    }
	
	private void doSetlist(JPanel pnl) {
		for (SmashHit song : JudahZone.getSetlist())
			setlist.addItem(song);
		pnl.add(setlist);
        JButton load = new JButton("▶");
        load.addActionListener(e->JudahZone.loadSong());
        pnl.add(load);
	}
	
	private void initialize() {
		mpk.setRenderer(MidiCable.STYLE);
		MidiCable.fillItems(false, mpk);
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener(e-> midi.setKeyboardSynth((MidiPort)mpk.getSelectedItem() ));

        start.setSelected(clock.isActive());
        start.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3) 
        			clock.reset(); // debug
            	else if (clock.isActive()) 
                    clock.end();
                else
                    clock.begin(); }});
        
        menu.addActionListener(e -> 
            popup.show(menu, menu.getLocation().x, menu.getLocation().y));
		
		JPanel left = new JPanel();
		left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
		add(left);
		
		JPanel right = new JPanel(new GridLayout(4, 1));
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		add(right, c);

		left.setBackground(Pastels.MY_GRAY);
		right.setBackground(Pastels.MY_GRAY);

		// doTempo
		TapTempo tapButton = new TapTempo("Tempo", msec -> {
            clock.setTempo(60000 / msec);
        });
		tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String input = Constants.inputBox("Tempo:");
                if (input == null || input.isEmpty()) return;
                try { 
                	clock.writeTempo((int)Float.parseFloat(input));
                } catch (Throwable t) { 
                	RTLogger.log(this, t.getMessage() + " -> " + input); 
                }
            }});
        tempoLbl.setFont(Constants.Gui.BOLD);

        HashSet<Integer> noDups = new HashSet<>();
		for (Integer i : JudahClock.LENGTHS) 
			if (noDups.contains(i)) continue;
			else {
				sync.addItem(i);
				noDups.add(i);
			}		
		sync.setSelectedItem(JudahClock.getLength());
		sync.addActionListener(e -> {
			clock.setLength((int)sync.getSelectedItem());
			MainFrame.update(JudahZone.getLooper().getLoopA());
		});
		
		JPanel left1 = new JPanel();
		JPanel left2 = new JPanel();
		JPanel left3 = new JPanel();
		JPanel left4 = new JPanel();
		left.add(left3); left.add(left4); 
		left.add(left2); left.add(left1);  
		left1.setBackground(Pastels.BUTTONS);
        left2.setBackground(Pastels.BUTTONS);
		left3.setBackground(Pastels.BUTTONS);
        left4.setBackground(Pastels.BUTTONS);

		menu.setMargin(Constants.Gui.ZERO_MARGIN);
		start.setMargin(Constants.Gui.ZERO_MARGIN);
		
        left1.add(menu);
		left1.add(start);
		left1.add(beatLbl);
		left1.add(beat);

        left2.add(new LoopWidget());

		left3.add(new JLabel("Sync", SwingConstants.CENTER));
        left3.add(sync);
        left3.add(tapButton);
        left3.add(tempoLbl);
        
        doSetlist(left4);
        
        JPanel p1 = new JPanel(), p2 = new JPanel();
        JPanel jamPanel = new JPanel();

        p1.setBackground(Pastels.BUTTONS);
        p2.setBackground(Pastels.BUTTONS);
        jamPanel.setBackground(Pastels.BUTTONS);
        mpkPanel.setBackground(Pastels.BUTTONS);
        
        p1.add(new JLabel("Calf", SwingConstants.CENTER));
        p1.add(max(calf));
        p2.add(new JLabel("Fluid", SwingConstants.CENTER));
        p2.add(max(fluid));
        jamPanel.add(new JLabel("Jam", SwingConstants.CENTER));
        jamPanel.add(max(jamstik));
        
        mpkPanel.add(new JLabel("MPK", SwingConstants.CENTER));
        mpkPanel.add(max(mpk));
        
        right.add(p1); right.add(p2); right.add(jamPanel); right.add(mpkPanel);
	
	}

	
	
	/**@param idx knob 0 to 7
     * @param data2  user input */
    public void clockKnobs(int idx, int data2) {
    	
    	switch(idx) {
    	case 0: // sync loop length 
			if (data2 == 0) 
				clock.setLength(1);
			else 
				clock.setLength((int) Constants.ratio(data2, JudahClock.LENGTHS));
			return;
    	case 2: // calf inst
    		calf.setSelectedIndex(data2);
    		return;
    	case 3: // fluid inst
    		fluid.setSelectedIndex(data2);
    		return;
 	    case 4: // Load song
 	    	setlist.setSelectedIndex(Constants.ratio(data2 - 1, setlist.getItemCount()));
 	    	return;
    	case 5: // change sequencer track focus
    		Track[] tracks = JudahZone.getTracker().getTracks();
    		JudahZone.getTracker().setCurrent((Track)Constants.ratio(data2 - 1, tracks));
    		return;
    	case 6: // jamstik out
    		JudahZone.getJamstik().setSelectedIndex(Constants.ratio(data2 - 1, midi.getPaths().size()));
    		return;
    	case 7: // mpk keys out
    		mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
    		return;
    	case 1: 
    		// Tempo handled at MPK
    		return;
    	}   
    }
    
	public void blink(boolean on) {
		beatLbl.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
		beat.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.TEMPO)
			tempoLbl.setText("" + value);
		if (prop == Property.TRANSPORT) {
			start.setText(value == JackTransportState.JackTransportStarting ? 
					"Stop" : "Play");
		}
		if (prop == Property.STEP) {
			int step = 100 * (int)value / clock.getSteps();
			menu.setBackground(RainbowFader.chaseTheRainbow(step));
		}

	}

	public void length(int bars) {
		sync.setSelectedItem(bars);
	}
	
	public void mode(KnobMode knobs) {
		setBorder(knobs == KnobMode.Clock ? Constants.Gui.HIGHLIGHT : NONE);
	}

	public void record(boolean active) {
		mpkPanel.setBackground(active ? Pastels.RED : Pastels.BUTTONS);
	}
	
	public void transpose(boolean active) {
		mpkPanel.setBackground(active ? Pastels.PINK: Pastels.BUTTONS);
	}

}
