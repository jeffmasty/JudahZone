package net.judah.midi;

import static net.judah.util.Constants.max;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.border.Border;

import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.controllers.CircuitTracks;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KnobMode;
import net.judah.looper.LoopWidget;
import net.judah.tracker.MidiOut;
import net.judah.util.*;

//	Menu beat        3: Calf
//  1: sync 2: tempo 4: Fluid
//	5: trk1  6: trk2 7: Jamstik
//	Loop A feedback	 8: MPK	
//                   
public class MidiGui extends JPanel implements TimeListener {

	private final JudahMidi midi;
	private final JudahClock clock;
	
	private final JButton menu = new JButton("Zone");
	private final JToggleButton start = new JToggleButton("Play");
    private final JLabel beatLbl = new JLabel("Beat:", JLabel.CENTER);
    private final JLabel beat = new JLabel("0", JLabel.CENTER);
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
    private final JComboBox<Integer> sync = new JComboBox<>();
    
    @Getter private final ProgChange calf;
    @Getter private final ProgChange fluid;
    
    private final MidiOut circuit1 = new MidiOut();
    private final MidiOut circuit2 = new MidiOut();
	private final MidiOut mpk = new MidiOut();
	private final JPanel mpkPanel = new JPanel();
	private Jamstik jam;
    
	private final Border NONE = BorderFactory.createLineBorder(Pastels.BUTTONS, 4);

	public MidiGui(JudahMidi midi, JudahClock clock) {
    	this.midi = midi;
		this.clock = clock;
    	clock.addListener(this);
    	
    	setBorder(NONE);
    	setOpaque(true);
    	setLayout(new GridLayout(1, 3));
		calf = new ProgChange(midi.getCalfOut());
		fluid = new ProgChange(midi.getFluidOut());

		new Thread(() -> initialize()).start();
    }
	
	private void initialize() {
		
        start.setSelected(clock.isActive());
        start.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3) 
        			clock.reset(); // debug
            	else if (clock.isActive()) 
                    clock.end();
                else
                    clock.begin(); }});
        
        
        
        JudahMenu popup = new JudahMenu();
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
                try { clock.setTempo(Float.parseFloat(input));
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

		
        // doCircuit
		JackPort[] outs = new JackPort[] { 
			midi.getCraveOut(), midi.getFluidOut(), midi.getCalfOut()
			/* midi.getUnoOut(), */ 
		};
		for (JackPort port : outs) {
			circuit1.addItem(port);
			circuit2.addItem(port);
			mpk.addItem(port);
		}
		circuit1.addActionListener(e -> CircuitTracks.setOut1((JackPort)circuit1.getSelectedItem()));
		circuit2.addActionListener(e -> CircuitTracks.setOut2((JackPort)circuit2.getSelectedItem()));
		
		
		mpk.addItem(midi.getCircuitOut());
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener( e -> midi.setKeyboardSynth((JackPort)mpk.getSelectedItem()));

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
        
        left4.add(new JLabel("T1", SwingConstants.CENTER));
        left4.add(circuit1);
        left4.add(new JLabel("T2", SwingConstants.CENTER));
        left4.add(circuit2);
        
        
        JPanel p1 = new JPanel(), p2 = new JPanel();
        JPanel jamPanel = new JPanel();
        jam = new Jamstik(jamPanel, JudahZone.getServices());

        p1.setBackground(Pastels.BUTTONS);
        p2.setBackground(Pastels.BUTTONS);
        jamPanel.setBackground(Pastels.BUTTONS);
        mpkPanel.setBackground(Pastels.BUTTONS);
        
        p1.add(new JLabel("Calf", SwingConstants.CENTER));
        p1.add(max(calf));
        p2.add(new JLabel("Fluid", SwingConstants.CENTER));
        p2.add(max(fluid));
        jamPanel.add(new JLabel("Jam", SwingConstants.CENTER));
        jamPanel.add(max(jam));
        
        
        
        mpkPanel.add(new JLabel("MPK", SwingConstants.CENTER));
        mpkPanel.add(max(mpk));
        
        right.add(p1); right.add(p2); right.add(jamPanel); right.add(mpkPanel);
	
	
	}

	
	
	/**@param idx knob 0 to 7
     * @param data2  user input */
    public void clockKnob(int idx, int data2) {
    	
    	switch(idx) {
    	case 0: // sync loop length 
			if (data2 == 0) 
				clock.setLength(1);
			else 
				clock.setLength((int) Constants.ratio(data2, JudahClock.LENGTHS));
			return;
    	case 1: 
    		// Tempo handled at MPK
    		return;
    	case 2: // calf inst
    		calf.setSelectedIndex(data2);
    		return;
    	case 3: // fluid inst
    		fluid.setSelectedIndex(data2);
    		return;
 	    case 4: 
 	    	circuit1.setSelectedIndex(Constants.ratio(data2 - 1, circuit1.getItemCount()));
 	    	// circuit midi 1 out
 	    	return;
    	case 5: // circuit midi 2 out
    		circuit2.setSelectedIndex(Constants.ratio(data2 - 1, circuit2.getItemCount()));
    		return;
    	case 6: // jamstik out
    		jam.setSelectedIndex(Constants.ratio(data2 - 1, midi.getPaths().size()));
    		return;
    	case 7: // mpk keys out
    		mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
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
			int step = 100 * (int)value / JudahClock.getSteps();
			menu.setBackground(RainbowFader.chaseTheRainbow(step));
		}

	}

	public void length(int bars) {
		sync.setSelectedItem(bars);
	}
	
	public void mode(KnobMode knobs) {
		setBorder(knobs == KnobMode.Clock ? Constants.Gui.HIGHLIGHT : NONE);
	}

	public void transpose(boolean active) {
		mpkPanel.setBackground(active ? Pastels.RED: Pastels.BUTTONS);
	}

}
