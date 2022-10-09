package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.midi.TimeSigGui;
import net.judah.tracker.Track.Cue;
import net.judah.util.*;

public abstract class TrackEdit extends JPanel implements ActionListener {
    
	private static final Dimension BTN_BOUNDS = new Dimension(WIDTH_BUTTONS, TABS.height);
	private static final Dimension SLIDER = new Dimension(75, STD_HEIGHT);
	
	protected final Track track;
	
	@Getter protected final JComboBox<String> pattern = new CenteredCombo<>();
	@Getter protected final JComboBox<Integer> trackNum = new CenteredCombo<>();
    protected final JPanel buttons = new JPanel();
    protected final JComboBox<String> file = new CenteredCombo<>();
    protected ActionListener fileListener = new ActionListener() {
		@Override public void actionPerformed(ActionEvent e) {
			String select = "" + file.getSelectedItem();
			if (select.equals("_clear")) 
				track.clearFile();
			else 
				track.setFile(select);
		}
	};
    protected final JComboBox<String> cycle;
    @Getter protected final JComboBox<Cue> cue = new CenteredCombo<>();
    @Getter protected final JComboBox<String> ratio = new CenteredCombo<>();
    
    @Getter protected final Slider trackVol = new Slider(null);
    @Getter protected final JButton playWidget = new JButton("Play"); // ▶/■ 
    @Getter protected final JButton record = new JButton("Rec");
    @Getter protected final JButton mpk = new JButton("MPK");

    protected boolean disable;
    
    protected final JComboBox<Integer> steps = new JComboBox<>();
    protected final JComboBox<Integer> div = new JComboBox<>();

    protected TrackEdit(Track t) {
    	setOpaque(true);
    	this.track = t;
    	cycle = t.getCycle().createComboBox();
    	buttons.setLayout(new GridLayout(0, 1));
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	genericButtons();
    	refreshFile();
    }
    
    public abstract void step(int step);
    
    public void update() {
    	playWidget.setBackground(track.isActive() ? Pastels.GREEN : track.isOnDeck() ? Pastels.YELLOW : Pastels.EGGSHELL);
    	record.setBackground(track.isRecord() ? Pastels.RED : Pastels.BUTTONS);
    	repaint();
    }
    
    public void setPattern(int idx) {
    	if (idx >= pattern.getItemCount()) fillPatterns();
    	if (idx >= pattern.getItemCount()) {
    		RTLogger.warn(this, track.getName() + " " + idx + " vs. " + pattern.getItemCount());
    		return;
    	}
		pattern.removeActionListener(this);
		pattern.setSelectedIndex(idx);
		pattern.addActionListener(this);
    }

    
	public final void refreshFile() {
		String selected = (track.getFile() == null) ? "_clear" : track.getFile().getName();
		file.removeActionListener(fileListener);
		file.removeAllItems();
		String[] sort = track.getFolder().list();
		Arrays.sort(sort);
		for (String name : sort) 
			file.addItem(name);
		file.addItem("_clear");
		file.setSelectedItem(selected);
		file.addActionListener(fileListener);
	}
	
	public final void fillPatterns() {
		disable = true;
		pattern.removeActionListener(this);
		pattern.removeAllItems();
		for (Pattern p : track)
			pattern.addItem(p.getName());
		pattern.addItem("new");
		pattern.setSelectedItem(track.getCurrent().getName());
		pattern.addActionListener(this);
		if (!track.isDrums())
			track.getCurrent().update();
		refreshFile();
		disable = false;
	}
	
	public final void fillTracks(int count) {
		trackNum.removeActionListener(this);
		new Thread(() -> {
			Constants.sleep(10);
			trackNum.removeAllItems();
			for (int i = 0; i < count; i++)
				trackNum.addItem(i + 1);
			if (track.getNum() < trackNum.getItemCount() - 1)
				trackNum.setSelectedIndex(track.getNum());
			trackNum.addActionListener(this);

		}).start();
	}
	private void genericButtons() {
		buttons.setLayout(new GridLayout(0, 1, 2, 3));
    	buttons.setMaximumSize(BTN_BOUNDS.getSize());

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
        		JudahZone.getTracker().get(track).fillPatterns();
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

        // File open/save/new
        pnl = new JPanel();
        pnl.add(file);
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener( e -> {
        	if (track.getFile() == null)
        		FileChooser.setCurrentDir(track.getFolder());
            else FileChooser.setCurrentFile(track.getFile());

            File f = FileChooser.choose();
            if (f != null) {
                track.write(f);
                refreshFile();
            }
        });
        pnl.add(saveBtn);
        buttons.add(pnl);
        // play rec mpk btns
        buttons.add(playRecMpk());
        // new copy del btns
        buttons.add(newCopyDelete());
        // cycle and cue
        buttons.add(cycleCue());
        // probablity and trackVol
        buttons.add(trackVol());
        // port  and portVol
        // buttons.add(port());
        // instrument

        // div and steps
        buttons.add(stepsDiv());
        // gate/octave in PianoEdit
        
        add(buttons);
	}

	private JPanel playRecMpk() {
		playWidget.addActionListener((e) -> track.setActive(!track.isActive()));
		playWidget.setOpaque(true);
        record.addActionListener(e ->track.toggleRecord());
        if (track.isSynth()) {
			mpk.addActionListener(e -> {
				track.setLatch(!track.isLatch());
				((JudahNotez)track.getTracker()).checkLatch();
			});
        }		
        JPanel pnl = new JPanel(new GridLayout(1, 3));
        pnl.setOpaque(true);
        pnl.add(playWidget);
        pnl.add(record);
        pnl.add(mpk);
        return pnl;
	}
	
	private JPanel newCopyDelete() {
		JButton create = new JButton("New");
        create.addActionListener(e -> track.newPattern());
        JButton copy = new JButton("Copy");
        copy.addActionListener(e -> track.copyPattern());
        JButton delete = new JButton("Del");
        delete.addActionListener(e -> track.deletePattern());
        JPanel pnl = new JPanel(new GridLayout(1, 3));
        pnl.add(create);
        pnl.add(copy);
        pnl.add(delete);
        return pnl;
	}
	
	private JPanel cycleCue() {
		for (Cue item : Track.Cue.values())
			cue.addItem(item);
		cue.addActionListener( e -> {
			Cue select = (Cue)cue.getSelectedItem();
			if (track.getCue() != select)
				track.setCue(select);});
		JPanel pnl = new JPanel();
		pnl.add(cycle);
		pnl.add(cue);
		return pnl;
	}
	
	private Component trackVol() {
		trackVol.addChangeListener(e -> {
			float gain = ((Slider)e.getSource()).getValue() * .01f;
			if (track.getGain() != gain)
				track.setGain(gain);
		});

        trackVol.setPreferredSize(SLIDER);
		JPanel pnl = new JPanel();
        pnl.add(new JLabel("Vol/Rat", JLabel.CENTER));
		pnl.add(trackVol);
		ratio.addItem("1:1");
		ratio.addItem("1:2");
		ratio.addItem("1:4");
		ratio.setSelectedIndex(0);
		ratio.addActionListener(e ->{ track.setRatio(2 * ratio.getSelectedIndex()); });
		pnl.add(ratio);
        return pnl;
	}
	
	private JPanel stepsDiv() {
		JLabel stp = new JLabel("Steps", JLabel.CENTER);
		for (int i = 2; i <= 32; i++)
			steps.addItem(i);
		steps.setSelectedItem(track.getSteps());
		steps.addActionListener(this);
		JLabel dv = new JLabel("Div", JLabel.CENTER);
		for (int i : TimeSigGui.DIVS)
            div.addItem(i);
		div.setSelectedItem(track.getDiv());
		div.addActionListener(this);

		JPanel pnl = new JPanel(new GridLayout(1, 4));
		pnl.add(stp);
		pnl.add(steps);
		pnl.add(dv);
		pnl.add(div);
		return pnl;
	}
	
	private void createTrackNav() {
        JPanel result = new JPanel(new GridLayout(1, 4, 3, 3));

        Click chBack = new Click("<");
        chBack.addActionListener(e -> {
            int channel = trackNum.getSelectedIndex() - 1;
            if (channel < 0) channel = trackNum.getItemCount() - 1;
            JudahZone.getTracker().setCurrent(channel);});

        Click chNext = new Click(">");
        chNext.addActionListener(e -> {
            int channel = trackNum.getSelectedIndex() + 1;
            if (channel == trackNum.getItemCount()) channel = 0;
            JudahZone.getTracker().setCurrent(channel);
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

	
	public boolean handled(ActionEvent e) {
		Object src = e.getSource();
		if (src == pattern) 
			patternAction();
		else if (src == trackNum) 
			trackAction();
		else if (src == div) {
			divAction();
		}
		else if (src == steps) {
			stepsAction();
		}
		else 
			return false;
		return true;
	}
	
	private void trackAction() {
		int idx = trackNum.getSelectedIndex(); 
		if (track.getNum() != idx)
			JudahZone.getTracker().setCurrent(idx);
		trackNum.removeActionListener(this);
		trackNum.setSelectedIndex(track.getNum());
		trackNum.addActionListener(this);
	}

	private void patternAction() {
		if ("new".equals("" + pattern.getSelectedItem())) {
			track.newPattern();
		}
		else if (pattern.getSelectedIndex() != -1)
			track.setCurrent(track.get(pattern.getSelectedIndex()));
			update();
	}

	private void divAction() {
		int change = (int)div.getSelectedItem();
		if (track.getDiv() == change)
			return;
		track.setDiv(change);
		if (track.isDrums()) {
			((DrumEdit)this).getGrid().measure();
			invalidate();
		}
		else {
			((PianoEdit)this).refresh(track.getCurrent());
		}
		
	}
	
	private void stepsAction() {
		int change = (int)steps.getSelectedItem();
		if (track.getSteps() == change) 
			return;
		track.setSteps(change);
		if (track.isDrums()) {
			((DrumEdit)this).getGrid().measure();
			invalidate();
		}
		else {
			((PianoEdit)this).refresh(track.getCurrent());
		}
	}
	
}

//private JPanel port2() {
//	JPanel pnl = new JPanel();
//	pnl.add(new JLabel("MidiOut"));
//	// pnl.add(midiOut);
//	JButton panic = new JButton("!");
//	panic.addActionListener(e->new Panic(track.getMidiOut().getMidiPort(), track.getCh()).start());
//	pnl.add(panic);
//	return pnl;
//}
	
