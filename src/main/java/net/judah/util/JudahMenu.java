package net.judah.util;

import static java.awt.event.KeyEvent.*;
import static net.judah.JudahZone.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.controllers.KorgPads;
import net.judah.drumz.KitzView;
import net.judah.effects.gui.PresetsGui;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock.Mode;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.Instruments;
import net.judah.synth.SynthEngines;
import net.judah.tracker.Tracker;

public class JudahMenu extends JPopupMenu implements KeyListener {

    private static final int ASCII_ONE = 49;
    private static JudahMenu instance;

    JMenu fileMenu = new JMenu("Song");
    JMenuItem sheetMusic = new JMenuItem("Sheet Music");
    JMenuItem presets = new JMenuItem("Presets");
    JMenuItem beatBox = new JMenuItem("BeatBox");
    JMenuItem synths = new JMenuItem("Synths");
    JMenuItem tracker = new JMenuItem("Tracker");
    JMenuItem kits = new JMenuItem("Kits");
    JMenuItem exit = new JMenuItem("Exit");

    public JudahMenu() {
    	
        exit.setMnemonic(KeyEvent.VK_E);
        fileMenu.setMnemonic(KeyEvent.VK_F);
        add(tracker);
        add(synths);
        add(beatBox);
        add(kits);
        add(presets);
        add(sheetMusic);
        add(exit);
        addKeyListener(this);
        presets.addActionListener( e -> new PresetsGui(getPresets()));
        sheetMusic.addActionListener( e -> getFrame().sheetMusic(new File(Constants.SHEETMUSIC, "Four.png")));
        synths.addActionListener(e-> getFrame().addOrShow(SynthEngines.getInstance(), SynthEngines.NAME));
        beatBox.addActionListener( e -> getFrame().addOrShow(getBeatBox(), getBeatBox().getName()));
        kits.addActionListener(e ->getFrame().addOrShow(KitzView.getInstance(), KitzView.NAME));
        tracker.addActionListener(e-> {
        	getFrame().addOrShow(getTracker(), Tracker.NAME);
        });
        
        exit.addActionListener((event) -> System.exit(0));
        
        ToggleSwitch mode = new ToggleSwitch();
        mode.addActionListener(
				e -> getClock().setMode(mode.isActivated() ? Mode.Internal : Mode.Midi24));
        add(mode);


    }

    @Override public void keyTyped(KeyEvent e) {  }
    @Override public void keyReleased(KeyEvent e) {  }

    @Override public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        Channel focus = getFxRack().getChannel();
        Looper looper = getLooper();
        Instruments instruments = getInstruments();

        switch(code) {

            case VK_ESCAPE: getMains().setOnMute(
                    !getMains().isOnMute());return;
            case VK_F1: MainFrame.setFocus(looper.get(0)); return;
            case VK_F2: MainFrame.setFocus(looper.get(1)); return;
            case VK_F3: MainFrame.setFocus(looper.get(2)); return;
            case VK_F4: MainFrame.setFocus(looper.get(3)); return;
            case VK_F5: MainFrame.setFocus(instruments.get(0)); return;
            case VK_F6: MainFrame.setFocus(instruments.get(1)); return;
            case VK_F7: MainFrame.setFocus(getSynth1()); return;
            case VK_F8: return;
            case VK_F9: MainFrame.setFocus(getSynth2()); return;
            case VK_F10: MainFrame.setFocus(instruments.get(3)); return;
            case VK_F11: MainFrame.setFocus(instruments.get(4)); return;
            case VK_UP: volume(true); return;
            case VK_DOWN: volume(false); return;
            case VK_LEFT: nextChannel(false); return;
            case VK_RIGHT: nextChannel(true); return;

            case VK_SPACE: case VK_M: mute(); return;
            
			case VK_ENTER: /* enterKey(); */return;

            case VK_R: 
            	if (focus instanceof Loop) {
            		Loop loop = (Loop)focus;
            		if (loop == looper.getLoopA())
            			KorgPads.trigger(getLooper().getLoopA());
            		else 
            			loop.record(loop.isRecording() != AudioMode.RUNNING);
            	} else if (focus instanceof Instrument) {
            		Instrument line = (Instrument)focus;
            		line.setMuteRecord(!line.isMuteRecord());
            	}
            	return;
            case VK_D: 
            	if (focus instanceof Loop) 
            		((Loop)focus).erase();
            	return;
            case VK_X: 
            	if (focus instanceof Loop) 
            		((Loop)focus).clear();
            	return;
        }

        RTLogger.log(this, "typed: " + e.getKeyChar());

        
        int ch = e.getKeyChar(); // 1 to sampleCount pressed, focus on specific loop idx
        if (ch >= ASCII_ONE && ch < looper.size() + ASCII_ONE) {
            MainFrame.setFocus(looper.get(ch - (ASCII_ONE) ));
            return;
        }

    }

    private void mute() {
        getFxRack().getChannel().setOnMute(!getFxRack().getChannel().isOnMute());
        // MixerPane.getInstance().getEffects().update();
    }

    private void volume(boolean up) {
        Channel bus = getFxRack().getChannel();
        int vol = bus.getVolume();
        vol += up? 5 : -5;
        if (vol > 100) vol = 100;
        if (vol < 0) vol = 0;
        bus.getGain().setVol(vol);
    }

    private void nextChannel(boolean toRight) {
    	Looper looper = getLooper();
        Instruments channels = getInstruments();
        Channel bus = getFxRack().getChannel();
        if (bus instanceof Instrument) {
            int i = channels.indexOf(bus);
            if (toRight) {
                if (i == channels.size() -1) {
                    MainFrame.setFocus(looper.get(0));
                    return;
                }
                MainFrame.setFocus(channels.get(i + 1));
                return;
            } // else toLeft
            if (i == 0) {
                MainFrame.setFocus(looper.get(looper.size()-1));
                return;
            }
            MainFrame.setFocus(channels.get(i - 1));
            return;
        } 
        // else instanceof Sample
        int i = looper.indexOf(bus);
        if (toRight) {
            if (i == looper.size() - 1) {
                MainFrame.setFocus(channels.get(0));
                return;
            }
            MainFrame.setFocus(looper.get(i + 1));
            return;
        } // else toLeft
        if (i == 0) {
            MainFrame.setFocus(channels.get(channels.size() - 1));
            return;
        }
        MainFrame.setFocus(looper.get(i - 1));

    }

    public static JudahMenu getInstance() {
        if (instance == null) instance = new JudahMenu();
        return instance;
    }


}
//    JMenuItem loadMidi = new JMenuItem("Open Midi");
//    JMenuItem load = new JMenuItem("Open...");
//    JMenuItem create = new JMenuItem("New...");
//    JMenuItem save = new JMenuItem("Save");
//    JMenuItem saveAs = new JMenuItem("Save As...");
//    JMenuItem close = new JMenuItem("Close Song");
//    JMenuItem beatBox = new JMenuItem("BeatBox");
//    JMenuItem noteBox = new JMenuItem("NoteBox");
//        fileMenu.add(load);
//        fileMenu.add(create);
//        fileMenu.add(save);
//        fileMenu.add(saveAs);
//        fileMenu.add(close);
//        add(fileMenu);
//        load.addActionListener( e -> load());
//        create.addActionListener( e -> create());
//        save.addActionListener( e -> save());
//        save.addActionListener( e -> saveAs());
//        close.addActionListener( e -> {
//        	if (Sequencer.getCurrent() != null)
//        		MainFrame.get().closeTab(Sequencer.getCurrent().getPage());
//        });
//        loadMidi.addActionListener(e -> {MainFrame.get().getTracker().loadMidi());
        
        //  editMenu.setMnemonic(KeyEvent.VK_E);
        //  copy.addActionListener( (event) -> copy() );
        //  editMenu.add(copy);
        //  cut.addActionListener( (event) -> cut() );
        //  editMenu.add(cut);
        //  paste.addActionListener( (event) -> paste() );
        //  editMenu.add(paste);
        //  add.addActionListener( (event) -> add() );
        //  editMenu.add(add);
        //  delete.addActionListener( (event) -> delete() );
        //  editMenu.add(delete);
        //  add(editMenu);

//          case VK_Q: focus.getCutFilter().setActive(
//                  !focus.getCutFilter().isActive()); focus.update(); return;
//          case VK_C: focus.getCompression().setActive(
//                  !focus.getCompression().isActive()); focus.update(); return;
//          case VK_V: focus.getReverb().setActive(
//                  !focus.getReverb().isActive()); focus.update(); return;
//            case VK_X: { // zero out loop or mute channel record
//                if (focus instanceof LineIn)
//                    ((LineIn)focus).setMuteRecord(!((LineIn)focus).isMuteRecord());
//                else if (focus instanceof Loop) {
//                    Loop s = (Loop)focus;
//                    s.setRecording(new Recording(s.getRecording().size()));
//                }
//                break; }
