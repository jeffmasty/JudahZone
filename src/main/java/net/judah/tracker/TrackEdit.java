package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.border.LineBorder;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.settings.Channels;
import net.judah.util.*;

public abstract class TrackEdit extends JPanel implements ActionListener {
    
	private static final Dimension BTN_BOUNDS = new Dimension(WIDTH_BUTTONS, TABS.height);
	
	protected final Track track;
    protected final JudahClock clock = JudahClock.getInstance(); 
	
	@Getter protected final JComboBox<String> pattern = new CenteredCombo<>();
	@Getter protected final JComboBox<Integer> trackNum = new CenteredCombo<>();
    protected final JPanel buttons = new JPanel();
    protected final JComboBox<String> file = new CenteredCombo<>();
    protected final FileCombo filename; 
    protected final MidiOut midiOut;
    protected final JComboBox<String> cycle;
    protected final Slider portVol = new Slider(null);
    protected final Slider trackVol = new Slider(null);
    protected final JToggleButton playWidget = new JToggleButton("Play", false); // ▶/■ 
    protected final Instrument instrument;
    
    // TODO
    //    private final JComboBox<Integer> steps = new JComboBox<>();
    //    private final JComboBox<Integer> div;

    protected TrackEdit(Track t) {
    	this.track = t;
    	filename = new FileCombo(t);
    	midiOut = new MidiOut(t);
    	instrument = new Instrument(t);
    	cycle = t.getCycle().createComboBox();

    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBorder(new LineBorder(RainbowFader.chaseTheRainbow(14 * t.getNum())));
    	genericButtons();
    }
    
    public abstract void step(int step);
    
    public abstract void update();
    
    public void setPattern(int idx) {
    	if (getPattern().getSelectedIndex() != idx) {
			pattern.removeActionListener(this);
			pattern.setSelectedIndex(idx);
			pattern.addActionListener(this);
		}
    }

    
	public final void fillInstruments() {
		instrument.fillInstruments();
	}
	
	public final void fillFile1() {
		filename.refresh();
	}
	
	public final void fillPatterns() {
		track.getCycle().setCount(0); // TODO
		pattern.removeActionListener(this);
		pattern.removeAllItems();
		for (Pattern p : track)
			pattern.addItem(p.getName());
		pattern.addItem("new");
		pattern.setSelectedItem(track.getCurrent().getName());
		pattern.addActionListener(this);
		if (!track.isDrums())
			track.getCurrent().update();
		filename.refresh();
	}
	
	public final void fillTracks() {
		trackNum.removeActionListener(this);
		new Thread(() -> {
			Constants.sleep(10);
			trackNum.removeAllItems();
			for (int i = 0; i < clock.getTracks().length; i++)
				trackNum.addItem(i + 1);
			trackNum.setSelectedIndex(track.getNum());
			trackNum.addActionListener(this);

		}).start();
	}
	private void genericButtons() {
		buttons.setLayout(new GridLayout(0, 1, 2, 3));
    	buttons.setMaximumSize(BTN_BOUNDS.getSize());

		fillInstruments();
		fillPatterns();
		
        JLabel trklbl = new JLabel("Track", JLabel.CENTER);
        trklbl.setFont(Constants.Gui.BOLD);
        JLabel patlbl = new JLabel("Pattern", JLabel.CENTER);
        patlbl.setFont(Constants.Gui.BOLD);
        patlbl.addMouseListener(new MouseAdapter() {
        	@Override
        	public void mouseClicked(MouseEvent e) {
        		String name = Constants.inputBox("Pattern Name:");
        		if (name == null || name.isEmpty()) return;
        		track.getCurrent().setName(name);
        		fillPatterns();
        		track.getView().fillPatterns();
        	}
		});
		
        JPanel pnl;
		pnl = new JPanel(new GridLayout(1, 2));
        pnl.add(trklbl);
        pnl.add(patlbl);
        buttons.add(pnl);

		pnl = new JPanel(new GridLayout(1, 2));
        trackNum.setFont(Constants.Gui.BOLD);
        pnl.add(trackNum);
        pnl.add(pattern);
        buttons.add(pnl);        
        createTrackNav();
        
        pnl = new JPanel();
        JButton file = new JButton("clear");
        file.addActionListener(e -> track.setFile(null));
        pnl.add(new JLabel("File", JLabel.CENTER));
        pnl.add(file);
        pnl.add(filename);
        buttons.add(pnl);
        
        pnl = new JPanel();
        pnl.add(new JLabel("Cycle"), JLabel.CENTER);
        pnl.add(cycle);
        buttons.add(pnl);
        
        pnl = new JPanel();
        Dimension SLIDER = new Dimension(85, STD_HEIGHT);

        JLabel vol1 = new JLabel("Trak Vol ", JLabel.CENTER);
        pnl.add(vol1);
        trackVol.setPreferredSize(SLIDER);
        pnl.add(trackVol);
        buttons.add(pnl);
        
        pnl = new JPanel();
        JLabel vol2 = new JLabel("Port Vol ", JLabel.CENTER);
        pnl.add(vol2);
        portVol.setPreferredSize(SLIDER);
        pnl.add(portVol);
        buttons.add(pnl);
        
        Click outLbl = new Click("  Port  ");
        outLbl.addActionListener( e -> {
            JackPort out = JudahMidi.getByName(
                midiOut.getSelectedItem().toString());
            MainFrame.setFocus(
                    JudahZone.getChannels().byName(Channels.volumeTarget(out)));});

        JPanel midi = new JPanel();
        midi.setLayout(new BoxLayout(midi, BoxLayout.X_AXIS));
        midi.add(outLbl);
        midi.add(midiOut);
        buttons.add(midi);
        
        pnl = new JPanel();
        pnl.add(new JLabel(" Patch ", JLabel.CENTER));
        pnl.add(instrument);
        buttons.add(pnl);
        
        createPlayButtons();

        JButton create = new JButton("New");
        create.addActionListener(e -> track.newPattern());
        JButton copy = new JButton("Copy");
        copy.addActionListener(e -> track.copyPattern());
        JButton delete = new JButton("Del");
        delete.addActionListener(e -> track.deletePattern());

        pnl = new JPanel(new GridLayout(1, 3));
        pnl.add(create);
        pnl.add(copy);
        pnl.add(delete);
        buttons.add(pnl);
        
        pnl = new JPanel(new GridLayout(1, 2));
        pnl.add(new JLabel("Steps: " + track.getSteps()), JLabel.CENTER);
        pnl.add(new JLabel("Div: " + track.getDiv()), JLabel.CENTER);
        buttons.add(pnl);
        
        add(buttons);
	}

	private void createTrackNav() {

        JPanel result = new JPanel(new GridLayout(1, 4, 3, 3));

        Click chBack = new Click("<");
        chBack.addActionListener(e -> {
            int channel = trackNum.getSelectedIndex() - 1;
            if (channel < 0) channel = trackNum.getItemCount() - 1;
            clock.getTracker().setCurrent(clock.getTracks()[channel]);});

        Click chNext = new Click(">");
        chNext.addActionListener(e -> {
            int channel = trackNum.getSelectedIndex() + 1;
            if (channel == trackNum.getItemCount()) channel = 0;
            clock.getTracker().setCurrent(clock.getTracks()[channel]);
        });
        Click patternBack = new Click("<");
        patternBack.addActionListener(e -> {track.next(false);});
        Click patternNext = new Click(">");
        patternNext.addActionListener(e -> {track.next(true);});

        result.add(chBack);
        result.add(chNext);
        result.add(patternBack);
        result.add(patternNext);
        buttons.add(result);
    }	

	private void createPlayButtons() {

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener( e -> {
            FileChooser.setCurrentDir(track.getFolder());
            File f = FileChooser.choose();
            if (f != null)
                track.write(f);
        });
        
        playWidget.addActionListener((e) -> track.setActive(!track.isActive()));
        JPanel pnl = new JPanel(new GridLayout(1, 3));
        pnl.add(saveBtn);
        pnl.add(new JButton("Rec"));
        pnl.add(playWidget);
        buttons.add(pnl);

    }

	public boolean handled(ActionEvent e) {
		if (e.getSource() == pattern) 
			patternAction();
		else 
		if (e.getSource() == trackNum) 
			trackAction();
		else 
			return false;
		return true;
	}
	
	private void trackAction() {
		int idx = trackNum.getSelectedIndex(); 
		if (track.getNum() != idx)
			clock.getTracker().setCurrent(clock.getTracks()[idx]);
		trackNum.removeActionListener(this);
		trackNum.setSelectedIndex(track.getNum());
		trackNum.addActionListener(this);
	}

	private void patternAction() {
		if ("new".equals("" + pattern.getSelectedItem())) {
			track.newPattern();
		}
		else 
			track.setCurrent(track.get(pattern.getSelectedIndex()));
			update();
	}

}
