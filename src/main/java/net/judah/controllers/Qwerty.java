package net.judah.controllers;

import static java.awt.event.KeyEvent.*;
import static net.judah.JudahZone.*;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.widgets.ModalDialog;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.Zone;
import net.judah.seq.MidiTab;
import net.judah.song.Song;
import net.judah.song.SongTab;
import net.judah.util.RTLogger;

public class Qwerty extends JTabbedPane implements KeyListener, Size {

	private static final int ASCII_ONE = 49;

	public Qwerty(JPanel... tabs) {
		
		setMaximumSize(TAB_SIZE);
        setPreferredSize(TAB_SIZE);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        setFocusable(true);
        
        // setFocusTraversalKeysEnabled(false);
		// getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("LEFT"), null);
		// getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("RIGHT"), null);
        
        addKeyListener(this);	
		for (JPanel tab : tabs)
			if (tab != null)
				addTab(tab.getName(), tab);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		if (getSelectedComponent() instanceof MidiTab) {
			((MidiTab)getSelectedComponent()).getMusician().keyTyped(e);
		} // else mixer?
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
//		if (getSelectedComponent() instanceof MidiTab) {
//			((MidiTab)getSelectedComponent()).getMusician().keyPressed(e);
//		} // else mixer?
	}

	@Override
	public void keyReleased(KeyEvent e) {
//		if (getSelectedComponent() instanceof MidiTab) {
//			((MidiTab)getSelectedComponent()).getMusician().keyReleased(e);
//		} // else mixer?
	}

	public void updateSongTitle(Song current) {
		if (current == null || current.getFile() == null)
			return;
		for (int i = 0; i < getTabCount(); i++)
			if (getTabComponentAt(i) instanceof SongTab)
				setTitleAt(i, current.getFile().getName());
	}

	public void title(JPanel tab) {
		for (int i = 0; i < getTabCount(); i++) {
			if (getComponentAt(i) == tab)
				setTitleAt(i, tab.getName());
			}
	}
	
	public void tab(boolean fwd) {
		int idx = getSelectedIndex() + (fwd ? 1 : -1);
		if (idx >= getTabCount())
			idx = 0;
		if (idx < 0)
			idx = getTabCount() - 1;
		setSelectedIndex(idx);
	}

	@Override
	public void setSelectedIndex(int index) {
		if (getSelectedIndex() != index) 
			super.setSelectedIndex(index);
		requestFocusInWindow();
	}

	public void closeTab(Component tab) {
        remove(tab);
    }

	@SuppressWarnings("unused")
	private boolean keyEvent(KeyEvent e) {
		if (ModalDialog.getInstance() != null)
			return false;
		
//		// TODO knob mode
//		if (e.getID() == KeyEvent.KEY_PRESSED) {
//			if (main.getTab() instanceof MidiTab)
//				((MidiTab)main.getTab()).keyPressed(e);
//			return true;
//		}
//		else if (e.getID() != KeyEvent.KEY_RELEASED)  return false;
//		if (main.getTab() instanceof MidiTab)
//			((MidiTab)main.getTab()).keyReleased(e);
//		if (e.isConsumed()) return true;
		int code = e.getKeyCode();
        Channel focus = getFxRack().getChannel();
        Looper looper = getLooper();
        switch(code) {

            case VK_ESCAPE: getMains().setOnMute(
                    !getMains().isOnMute());return true;
            case VK_F1: MainFrame.setFocus(looper.get(0)); return true;
            case VK_F2: MainFrame.setFocus(looper.get(1)); return true;
            case VK_F3: MainFrame.setFocus(looper.get(2)); return true;
            case VK_F4: MainFrame.setFocus(looper.get(3)); return true;
            case VK_F5: MainFrame.setFocus(getGuitar()); return true;
            case VK_F6: MainFrame.setFocus(getMic()); return true;
            case VK_F7: MainFrame.setFocus(getDrumMachine()); return true;
            case VK_F8: MainFrame.setFocus(getSynth1()); return true;
            case VK_F9: MainFrame.setFocus(getSynth2()); return true;
            case VK_F10: MainFrame.setFocus(getCrave()); return true;
            case VK_F11: MainFrame.setFocus(getFluid()); return true;
//            case VK_UP: return volume(true, focus); 
//            case VK_DOWN: return volume(false, focus); 
            case VK_LEFT: return nextChannel(false); 
            case VK_RIGHT: return nextChannel(true); 

            case VK_SPACE: case VK_M: mute(); return true;
            case VK_DELETE: RTLogger.log(this, "DELETE!"); return true;
			case VK_ENTER: /* enterKey(); */return true;

            case VK_R: 
            	if (focus instanceof Loop) {
            		Loop loop = (Loop)focus;
            		if (loop == looper.getLoopA())
            			getLooper().getLoopA().trigger();
            		else 
            			loop.record(!loop.isRecording());
            	} else if (focus instanceof Instrument) {
            		Instrument line = (Instrument)focus;
            		line.setMuteRecord(!line.isMuteRecord());
            	}
            	return true;
            case VK_D: 
            	if (focus instanceof Loop) 
            		((Loop)focus).clear();
            	return true;
            case VK_X: 
            	if (focus instanceof Loop) 
            		((Loop)focus).clear();
            	return true;
        }

        int ch = e.getKeyChar(); // 1 to sampleCount pressed, focus on specific loop idx
        if (ch >= ASCII_ONE && ch < looper.size() + ASCII_ONE) {
            MainFrame.setFocus(looper.get(ch - (ASCII_ONE) ));
            return true;
        }
        
        RTLogger.log(this, "pressed: " + KeyEvent.getKeyText(e.getKeyCode()));
        return false;

    }

    private void mute() {
        getFxRack().getChannel().setOnMute(!getFxRack().getChannel().isOnMute());
    }

//    private boolean volume(boolean up, Channel ch) {
//        int vol = ch.getVolume();
//        vol += up? 5 : -5;
//        if (vol > 100) vol = 100;
//        if (vol < 0) vol = 0;
//        ch.getGain().setVol(vol);
//        MainFrame.update(ch);
//        return true;
//    }

    private boolean nextChannel(boolean toRight) {
    	Looper looper = getLooper();
        Zone channels = getInstruments();
        Channel bus = getFxRack().getChannel();
        if (bus instanceof Instrument) {
            int i = channels.indexOf(bus);
            if (toRight) {
                if (i == channels.size() -1) {
                    MainFrame.setFocus(looper.get(0));
                    return true;
                }
                MainFrame.setFocus(channels.get(i + 1));
                return true;
            } // else toLeft
            if (i == 0) {
                MainFrame.setFocus(looper.get(looper.size()-1));
                return true;
            }
            MainFrame.setFocus(channels.get(i - 1));
            return true;
        } 
        // else instanceof Sample
        int i = looper.indexOf(bus);
        if (toRight) {
            if (i == looper.size() - 1) {
                MainFrame.setFocus(channels.get(0));
                return true;
            }
            MainFrame.setFocus(looper.get(i + 1));
            return true;
        } // else toLeft
        if (i == 0) {
            MainFrame.setFocus(channels.get(channels.size() - 1));
            return true;
        }
        MainFrame.setFocus(looper.get(i - 1));
        return true;
    }



}
