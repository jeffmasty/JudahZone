package net.judah.util;

import static java.awt.event.KeyEvent.*;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.Looper;
import net.judah.MainFrame;
import net.judah.MixerPane;
import net.judah.api.AudioMode;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.sequencer.Sequencer;
import net.judah.settings.Channels;
import net.judah.song.Song;

@Log4j
public class MenuBar extends JMenuBar implements KeyListener {

    private static final int ASCII_ONE = 49;
    private static MenuBar instance;

    private MixerPane mixer;
    private Channels channels;
    private Looper looper;

    JMenu fileMenu = new JMenu("Song");

    JMenuItem load = new JMenuItem("Open...");
    JMenuItem create = new JMenuItem("New...");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save As...");
    JMenuItem close = new JMenuItem("Close Song");
    JMenuItem beatBox = new JMenuItem("BeatBox");
    JMenuItem noteBox = new JMenuItem("NoteBox");
    JMenuItem exit = new JMenuItem("Exit");

    public MenuBar() {

        fileMenu.setMnemonic(KeyEvent.VK_F);
        exit.setMnemonic(KeyEvent.VK_E);
        fileMenu.add(load);
        fileMenu.add(create);
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(close);
        fileMenu.add(beatBox);
        fileMenu.add(noteBox);
        fileMenu.add(exit);
        add(fileMenu);

        addKeyListener(this);

        load.addActionListener( e -> {load();});
        create.addActionListener( e -> {create();});
        save.addActionListener( e -> {save();});
        save.addActionListener( e -> {saveAs();});
        close.addActionListener( e -> {
            MainFrame.get().closeTab(Sequencer.getCurrent().getPage());});
        beatBox.addActionListener( e -> {MainFrame.get().beatBox();});
        noteBox.addActionListener( e -> {MainFrame.get().noteBox();});
        exit.addActionListener((event) -> System.exit(0));

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
        Channel focus = MixerPane.getInstance().getChannel();

        switch(code) {

            case VK_ESCAPE: JudahZone.getMasterTrack().setOnMute(
                    !JudahZone.getMasterTrack().isOnMute());return;
            case VK_F1: mixer.setFocus(channels.get(0)); return;
            case VK_F2: mixer.setFocus(channels.get(1)); return;
            case VK_F3: mixer.setFocus(channels.get(2)); return;
            case VK_F4: mixer.setFocus(channels.get(3)); return;
            case VK_F5: mixer.setFocus(channels.get(4)); return;
            case VK_F6: mixer.setFocus(channels.get(5)); return;
            case VK_F9: if (Sequencer.getCurrent() != null)
                    Sequencer.getCurrent().getPage().reload(); return;
            case VK_F10: /* used by MenuBar */ return;
            case VK_F11: Sequencer.transport(); return;
            case VK_UP: volume(true); return;
            case VK_DOWN: volume(false); return;
            case VK_LEFT: nextChannel(false); return;
            case VK_RIGHT: nextChannel(true); return;
            case VK_SPACE: case VK_M: mute(); return;
            case VK_ENTER: enterKey(); return;

//          case VK_Q: focus.getCutFilter().setActive(
//                  !focus.getCutFilter().isActive()); focus.update(); return;
//          case VK_C: focus.getCompression().setActive(
//                  !focus.getCompression().isActive()); focus.update(); return;
//          case VK_V: focus.getReverb().setActive(
//                  !focus.getReverb().isActive()); focus.update(); return;
            case VK_X: { // zero out loop or mute channel record
                if (focus instanceof LineIn)
                    ((LineIn)focus).setMuteRecord(!((LineIn)focus).isMuteRecord());
                else if (focus instanceof Sample) {
                    Sample s = (Sample)focus;
                    s.setRecording(new Recording(s.getRecording().size(),
                            s.getRecording().isListening()));
                }
                break; }
        }

        int ch = e.getKeyChar(); // 1 to sampleCount pressed, focus on specific loop idx
        if (ch >= ASCII_ONE && ch < looper.size() + ASCII_ONE) {
            mixer.setFocus(looper.get(ch - (ASCII_ONE) ));
            return;
        }

    }

    private void enterKey() {
        Console.info("enter key handled");
        Channel ch = MixerPane.getInstance().getChannel();
        if (ch instanceof Sample)
            ((Sample)ch).play(
                    ((Sample)ch).isPlaying() != AudioMode.RUNNING);
        else
            ((LineIn)ch).setMuteRecord(
                    !((LineIn)ch).isMuteRecord());
    }

    private void mute() {
        MixerPane.getInstance().getChannel().setOnMute(!MixerPane.getInstance().getChannel().isOnMute());
        // MixerPane.getInstance().getEffects().update();
    }

    private void volume(boolean up) {
        Channel bus = MixerPane.getInstance().getChannel();
        int vol = bus.getVolume();
        vol += up? 5 : -5;
        if (vol > 100) vol = 100;
        if (vol < 0) vol = 0;
        bus.setVolume(vol);
    }

    private void nextChannel(boolean toRight) {
        Channel bus = MixerPane.getInstance().getChannel();
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
        } // else instanceof Sample
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
        MixerPane.getInstance().setFocus(looper.get(i - 1));

    }

    public void setMixerPane(MixerPane mixerPane) {
        mixer = mixerPane;
        channels = JudahZone.getChannels();
        looper = JudahZone.getLooper();
    }

    public static MenuBar getInstance() {
        if (instance == null) instance = new MenuBar();
        return instance;
    }


}
