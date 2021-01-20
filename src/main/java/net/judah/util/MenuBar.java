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
import net.judah.effects.gui.EffectsRack;
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
	private EffectsRack focus;
	private Channels channels;
	private Looper looper;

    JMenu fileMenu = new JMenu("Song");

	JMenuItem load = new JMenuItem("Open...");
    JMenuItem create = new JMenuItem("New...");
    JMenuItem save = new JMenuItem("Save");
    JMenuItem saveAs = new JMenuItem("Save As...");
    JMenuItem close = new JMenuItem("Close Song");
    JMenuItem exit = new JMenuItem("Exit");
    JMenuItem metronome = new JMenuItem("Metronome");

	public MenuBar() {

		fileMenu.setMnemonic(KeyEvent.VK_F);
        load.addActionListener( (event) -> load());
        fileMenu.add(load);
        create.addActionListener( (event) -> create());
        fileMenu.add(create);
        save.addActionListener( (event) -> save());
        fileMenu.add(save);
        save.addActionListener( (event) -> saveAs());
        fileMenu.add(saveAs);
        close.addActionListener( (event) -> MainFrame.get().closeTab(Sequencer.getCurrent().getPage()));
        fileMenu.add(close);
        metronome.addActionListener( (event) -> JudahZone.getMetronome().openGui());
        fileMenu.add(metronome);

		exit.setMnemonic(KeyEvent.VK_E);
        exit.setToolTipText("Exit application");
        exit.addActionListener((event) -> System.exit(0));
        fileMenu.add(exit);
        add(fileMenu);

        addKeyListener(this);

//		editMenu.setMnemonic(KeyEvent.VK_E);
//		copy.addActionListener( (event) -> copy() );
//		editMenu.add(copy);
//		cut.addActionListener( (event) -> cut() );
//		editMenu.add(cut);
//		paste.addActionListener( (event) -> paste() );
//		editMenu.add(paste);
//		add.addActionListener( (event) -> add() );
//		editMenu.add(add);
//		delete.addActionListener( (event) -> delete() );
//		editMenu.add(delete);
//		add(editMenu);

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

			case VK_Q: focus.getCutFilter().setActive(
			        !focus.getCutFilter().isActive()); focus.update(); return;
			case VK_C: focus.getFocus().getCompression().setActive(
			        !focus.getFocus().getCompression().isActive()); focus.update(); return;
			case VK_V: focus.getFocus().getReverb().setActive(
			        !focus.getFocus().getReverb().isActive()); focus.update(); return;
			case VK_F: focus.lfo(); return;
			case VK_X: { // zero out loop or mute channel record
				Channel bus = focus.getFocus();
				if (bus instanceof LineIn)
					((LineIn)bus).setMuteRecord(!((LineIn)bus).isMuteRecord());
				else if (bus instanceof Sample) {
					Sample s = (Sample)bus;
					s.setRecording(new Recording(s.getRecording().size(),
							s.getRecording().isListening()));
				}
				focus.update();
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
		if (focus.getFocus() instanceof Sample)
			((Sample)focus.getFocus()).play(
					((Sample)focus.getFocus()).isPlaying() != AudioMode.RUNNING);
		else
			((LineIn)focus.getFocus()).setMuteRecord(
					!((LineIn)focus.getFocus()).isMuteRecord());
	}

	private void mute() {
		focus.getFocus().setOnMute(!focus.getFocus().isOnMute());
		focus.update();
	}

	private void volume(boolean up) {
		Channel bus = focus.getFocus();
		int vol = bus.getVolume();
		vol += up? 5 : -5;
		if (vol > 100) vol = 100;
		if (vol < 0) vol = 0;
		bus.setVolume(vol);
	}

	private void nextChannel(boolean toRight) {
		Channel bus = focus.getFocus();
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
		focus.setFocus(looper.get(i - 1));
	}

	public void setMixerPane(MixerPane mixerPane) {
		mixer = mixerPane;
		channels = JudahZone.getChannels();
		looper = JudahZone.getLooper();
		focus = EffectsRack.getInstance();
	}

	public static MenuBar getInstance() {
		if (instance == null) instance = new MenuBar();
		return instance;
	}


}
