package net.judah.midi;

import static net.judah.util.Constants.max;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.sound.midi.ShortMessage;
import javax.swing.*;
import javax.swing.border.Border;

import org.jaudiolibs.jnajack.JackMidi;
import org.jaudiolibs.jnajack.JackPort;

import net.judah.JudahZone;
import net.judah.api.Midi;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.controllers.CircuitTracks;
import net.judah.controllers.Jamstik;
import net.judah.controllers.KnobMode;
import net.judah.fluid.FluidInstrument;
import net.judah.fluid.FluidSynth;
import net.judah.looper.LoopWidget;
import net.judah.tracks.MidiOut;
import net.judah.util.*;

//	Menu beat        3: Calf
//  1: sync 2: tempo 4: Fluid
//	5: trk1  6: trk2 7: Jamstik
//	Loop A  		 8: MPK	
//                   
public class MidiGui extends JPanel implements TimeListener {

//	@Getter @Setter private static JackPort circuit1;
//	@Getter @Setter private static JackPort circuit2;
//	@Getter @Setter private static int fluidProg;
//	@Getter @Setter private static int calfProg;
//	@Getter @Setter private static Path jamstik;
//	@Getter @Setter private static Path MPK;

	private final JudahMidi midi;
	private final JudahClock clock;
	
	private final Dimension MAX = new Dimension(120, 30);
	private final Border highlight = BorderFactory.createLineBorder(Pastels.BLUE, 4);
	private final Border none = BorderFactory.createLineBorder(Pastels.EGGSHELL, 4);
	
	private final JToggleButton start = new JToggleButton("Start");
    private final JLabel beatLbl = new JLabel("Beat:", JLabel.CENTER);
    private final JLabel beat = new JLabel("0", JLabel.CENTER);
   	private final JLabel tempoLbl = new JLabel("?", JLabel.CENTER);
    private final JComboBox<Integer> sync = new JComboBox<>();
	private final JComboBox<FluidInstrument> calf = new JComboBox<FluidInstrument>();
    private final JComboBox<FluidInstrument> fluid = new JComboBox<FluidInstrument>();
    private final MidiOut circuit1 = new MidiOut();
    private final MidiOut circuit2 = new MidiOut();
	private final MidiOut mpk = new MidiOut();
    
	public MidiGui(JudahMidi midi, JudahClock clock) {
    	this.midi = midi;
		this.clock = clock;
    	clock.addListener(this);
    	setBorder(none);
    	new Thread(() -> initialize()).start();
    }
	
	private void initialize() {
		
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		beatLbl.setOpaque(true);
		beat.setOpaque(true);
		
        start.setSelected(clock.isActive());
        start.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON3) 
        			clock.reset(); // debug
            	else if (clock.isActive()) 
                    clock.end();
                else
                    clock.begin(); }});
        
        JButton menu = new JButton("Zone");
        JudahMenu popup = new JudahMenu();
        menu.addActionListener(e -> 
            popup.show(menu, menu.getLocation().x, menu.getLocation().y));
		
		JPanel left = new JPanel(/* new GridLayout(3, 4) */);
		left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
		
		JPanel right = new JPanel(new GridLayout(4, 1));
		
		setLayout(new GridLayout(1, 3));
		
		add(left);
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 2;
		
		add(right, c);

		// doTempo
		TapTempo tapButton = new TapTempo("Tempo", msec -> {
            clock.setTempo(60000 / msec);
        });
		tempoLbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String input = Constants.inputBox("Tempo:");
                if (input == null || input.isEmpty()) return;
                try { clock.setTempo(Float.parseFloat(input));
                } catch (Throwable t) { Console.info(t.getMessage() + " " + input); }
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

		// doSynths
		for (FluidInstrument i : FluidSynth.getInstruments().getInstruments())
        	if (i.index <= 100) 
        		calf.addItem(i);
        for (FluidInstrument i : FluidSynth.getInstruments().getInstruments())
        	if (i.index <= 100) 
        		fluid.addItem(i);
        calf.addActionListener(e -> progChange((FluidInstrument)calf.getSelectedItem(), midi.getCalfOut()));
        fluid.addActionListener(e -> progChange((FluidInstrument)fluid.getSelectedItem(), midi.getFluidOut()));
		
        // doCircuit
        JackPort[] outs = new JackPort[] {midi.getCraveOut(), midi.getCalfOut(), midi.getFluidOut()};
		for (JackPort port : outs) {
			circuit1.addItem(port);
			circuit2.addItem(port);
		}
		circuit1.addActionListener(e -> CircuitTracks.setOut1((JackPort)circuit1.getSelectedItem()));
		circuit2.addActionListener(e -> CircuitTracks.setOut2((JackPort)circuit2.getSelectedItem()));
		
		// doRouting
		Jamstik jam = Jamstik.getInstance(); 
		
		mpk.addItem(midi.getFluidOut());
		mpk.addItem(midi.getCalfOut());
		mpk.addItem(midi.getCraveOut());
		mpk.addItem(midi.getCircuitOut());
		mpk.setSelectedItem(midi.getKeyboardSynth());
		mpk.addActionListener( e -> midi.setKeyboardSynth((JackPort)mpk.getSelectedItem()));
		
		JPanel left1 = new JPanel();
		JPanel left2 = new JPanel();
		JPanel left3 = new JPanel();
		JPanel left4 = new JPanel();
		left.add(left1); left.add(left2); left.add(left3); left.add(left4); 
		
		
        left1.add(menu);
		left1.add(start);
		left1.add(beatLbl);
		left1.add(beat);

        left2.add(new JLabel("Sync", SwingConstants.CENTER));
        left2.add(sync);
        left2.add(tapButton);
        left2.add(tempoLbl);
        
        left3.add(new JLabel("Trk1", SwingConstants.CENTER));
        left3.add(circuit1);
        left3.add(new JLabel("Trk2", SwingConstants.CENTER));
        left3.add(circuit2);
        
        left4.add(new LoopWidget(JudahZone.getLooper().getLoopA()));
        
        JPanel p1 = new JPanel(), p2 = new JPanel(), p3 = new JPanel(), p4 = new JPanel();
        
       
        p1.add(new JLabel("Calf", SwingConstants.CENTER));
        p1.add(max(calf));
        p2.add(new JLabel("Fluid", SwingConstants.CENTER));
        p2.add(max(fluid));
        p3.add(new JLabel("Jam", SwingConstants.CENTER));
        p3.add(max(jam));
        p4.add(new JLabel("MPK", SwingConstants.CENTER));
        p4.add(max(mpk));
        
        right.add(p1); right.add(p2); right.add(p3); right.add(p4);
        validate();
	}
	
	public void progChange(FluidInstrument i, JackPort out) {
		try {
			Midi m = new Midi(ShortMessage.PROGRAM_CHANGE, 0, i.index);  
			JackMidi.eventWrite(out, midi.ticker(), 
					m.getMessage(), m.getLength());
		} catch (Exception  e) {
			RTLogger.warn(this, e);
		}
	}
	
	/**
     * @param idx knob 0 to 7
     * @param data2  user input
     */
    public void clockKnob(int idx, int data2) {
    	
    	switch(idx) {
    	case 0: // sync loop length 
			if (data2 == 0) 
				clock.setLength(1);
			else 
				clock.setLength((int) Constants.ratio(data2, JudahClock.LENGTHS));
			return;
    	case 1: // set Tempo
    		clock.setTempo( (data2 + 40) * 1.25f); return; 
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
    		Jamstik.getInstance().setSelectedIndex(Constants.ratio(data2 - 1, midi.getPaths().size()));
    		return;
    	case 7: // mpk keys out
    		mpk.setSelectedIndex(Constants.ratio(data2 - 1, mpk.getItemCount()));
    		return;
    	}   
    }
    
	public void blink(boolean on) {
		beatLbl.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
		beat.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
	//	bpmPnl.setBackground(on ? clock.isActive() ? Pastels.GREEN : Pastels.BLUE : Pastels.EGGSHELL);
	}

	
	
	@Override
	public void update(Property prop, Object value) {
		if (prop == Property.TEMPO)
			tempoLbl.setText("" + value);
	}

	public void length(int bars) {
		sync.setSelectedItem(bars);
	}
	
	
	
	public void mode(KnobMode knobs) {
		setBorder(knobs == KnobMode.Clock ? highlight : none);
	}
	
}
