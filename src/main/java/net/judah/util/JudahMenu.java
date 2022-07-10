package net.judah.util;

import static java.awt.event.KeyEvent.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import lombok.extern.log4j.Log4j;
import net.judah.ControlPanel;
import net.judah.JudahZone;
import net.judah.Looper;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.clock.JudahClock;
import net.judah.clock.JudahClock.Mode;
import net.judah.controllers.KorgPads;
import net.judah.looper.Loop;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.sequencer.Sequencer;
import net.judah.settings.Channels;
import net.judah.song.Song;

@Log4j
public class JudahMenu extends JPopupMenu implements KeyListener {

    private static final int ASCII_ONE = 49;
    private static JudahMenu instance;
    private static ControlPanel mixer;
    private static Channels channels;
    private static Looper looper;

    JMenu fileMenu = new JMenu("Song");

    JMenuItem beatsMenu = new JMenuItem("BeatBox");
    JMenuItem sheetMusic = new JMenuItem("Sheet Music");
    JMenuItem loadMidi = new JMenuItem("Open Midi");
    JMenuItem load = new JMenuItem("Open...");
    JMenuItem create = new JMenuItem("New...");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save As...");
    JMenuItem close = new JMenuItem("Close Song");
    JMenuItem exit = new JMenuItem("Exit");

//    JMenuItem beatBox = new JMenuItem("BeatBox");
//    JMenuItem noteBox = new JMenuItem("NoteBox");

    public JudahMenu() {

        fileMenu.setMnemonic(KeyEvent.VK_F);
        exit.setMnemonic(KeyEvent.VK_E);
        fileMenu.add(load);
        fileMenu.add(create);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(close);

        add(fileMenu);
        add(sheetMusic);
        add(beatsMenu);
        add(exit);

        addKeyListener(this);

        load.addActionListener( e -> {load();});
        create.addActionListener( e -> {create();});
        save.addActionListener( e -> {save();});
        save.addActionListener( e -> {saveAs();});
        close.addActionListener( e -> {
            MainFrame.get().closeTab(Sequencer.getCurrent().getPage());});
        sheetMusic.addActionListener( e -> {MainFrame.get().sheetMusic();});
        beatsMenu.addActionListener( e -> {MainFrame.get().beatBox();});
//        loadMidi.addActionListener(e -> {MainFrame.get().getTracker().loadMidi());
        exit.addActionListener((event) -> System.exit(0));

        ToggleSwitch mode = new ToggleSwitch();
        mode.addActionListener(
				e -> JudahClock.setMode(mode.isActivated() ? Mode.Internal : Mode.Midi24));
        add(mode);
        
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

    }

    public void save() {
        Sequencer s = Sequencer.getCurrent();
        if (s == null) return;
        s.getPage().save(s.getSongfile());
    }

    public void saveAs() {
        File file = FileChooser.choose();
        if (file == null) return;
        Sequencer s = Sequencer.getCurrent();
        if (s == null) return;
        s.getPage().save(file);
    }

    public void load() {
        File file = FileChooser.choose();
        if (file == null) return;

        try {
            new Sequencer(file);
        } catch (Exception e) {
            log.error(e.getMessage() + " - " + file.getAbsolutePath());
            Constants.infoBox(file.getAbsolutePath() + ": " + e.getMessage(), "Error");
        }

    }

    public void create() {
        File file = FileChooser.choose();
        if (file == null) return;
        Song song = new Song();
        try {
            JsonUtil.saveString(JsonUtil.MAPPER.writeValueAsString(song), file);
            new Sequencer(file);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            Constants.infoBox(e.getMessage(), "Error");
        }
    }

    @Override public void keyTyped(KeyEvent e) {  }
    @Override public void keyReleased(KeyEvent e) {  }

    @Override public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        Channel focus = ControlPanel.getInstance().getChannel();

        switch(code) {

            case VK_ESCAPE: JudahZone.getMasterTrack().setOnMute(
                    !JudahZone.getMasterTrack().isOnMute());return;
            case VK_F1: mixer.setFocus(looper.get(0)); return;
            case VK_F2: mixer.setFocus(looper.get(1)); return;
            case VK_F3: mixer.setFocus(looper.get(2)); return;
            case VK_F4: mixer.setFocus(looper.get(3)); return;
            case VK_F5: mixer.setFocus(channels.get(0)); return;
            case VK_F6: mixer.setFocus(channels.get(1)); return;
            case VK_F7: mixer.setFocus(channels.get(2)); return;
            case VK_F8: mixer.setFocus(channels.get(3)); return;
            case VK_F9: mixer.setFocus(channels.get(4)); return;
            case VK_F10: mixer.setFocus(channels.get(5)); return;
            case VK_F11: mixer.setFocus(channels.get(6)); return;
            case VK_UP: volume(true); return;
            case VK_DOWN: volume(false); return;
            case VK_LEFT: nextChannel(false); return;
            case VK_RIGHT: nextChannel(true); return;

            case VK_SPACE: case VK_M: mute(); return;
            
            case VK_ENTER: enterKey(); return;

            case VK_R: 
            	if (focus instanceof Loop) {
            		Loop loop = (Loop)focus;
            		if (loop == looper.getLoopA())
            			KorgPads.trigger(JudahZone.getLooper().getLoopA());
            		else 
            			loop.record(loop.isRecording() != AudioMode.RUNNING);
            	} else if (focus instanceof LineIn) {
            		LineIn line = (LineIn)focus;
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
        }

        RTLogger.log(this, "typed: " + e.getKeyChar());

        
        int ch = e.getKeyChar(); // 1 to sampleCount pressed, focus on specific loop idx
        if (ch >= ASCII_ONE && ch < looper.size() + ASCII_ONE) {
            mixer.setFocus(looper.get(ch - (ASCII_ONE) ));
            return;
        }

    }

    private void enterKey() {
        Console.info("enter key handled");
        Channel ch = ControlPanel.getInstance().getChannel();
        if (ch instanceof Loop)
            ((Loop)ch).play(
                    ((Loop)ch).isPlaying() != AudioMode.RUNNING);
        else
            ((LineIn)ch).setMuteRecord(
                    !((LineIn)ch).isMuteRecord());
    }

    private void mute() {
        ControlPanel.getInstance().getChannel().setOnMute(!ControlPanel.getInstance().getChannel().isOnMute());
        // MixerPane.getInstance().getEffects().update();
    }

    private void volume(boolean up) {
        Channel bus = ControlPanel.getInstance().getChannel();
        int vol = bus.getVolume();
        vol += up? 5 : -5;
        if (vol > 100) vol = 100;
        if (vol < 0) vol = 0;
        bus.getGain().setVol(vol);
    }

    private void nextChannel(boolean toRight) {
        Channel bus = ControlPanel.getInstance().getChannel();
        if (bus instanceof LineIn) {
            int i = channels.indexOf(bus);
            if (toRight) {
                if (i == channels.size() -1) {
                    mixer.setFocus(looper.get(0));
                    return;
                }
                mixer.setFocus(channels.get(i + 1));
                return;
            } // else toLeft
            if (i == 0) {
                mixer.setFocus(looper.get(looper.size()-1));
                return;
            }
            mixer.setFocus(channels.get(i - 1));
            return;
        } 
        // else instanceof Sample
        int i = looper.indexOf(bus);
        if (toRight) {
            if (i == looper.size() - 1) {
                mixer.setFocus(channels.get(0));
                return;
            }
            mixer.setFocus(looper.get(i + 1));
            return;
        } // else toLeft
        if (i == 0) {
            mixer.setFocus(channels.get(channels.size() - 1));
            return;
        }
        ControlPanel.getInstance().setFocus(looper.get(i - 1));

    }

    public static void setMixerPane(ControlPanel mixerPane) {
        mixer = mixerPane;
        channels = JudahZone.getChannels();
        looper = JudahZone.getLooper();
    }

    public static JudahMenu getInstance() {
        if (instance == null) instance = new JudahMenu();
        return instance;
    }


}
