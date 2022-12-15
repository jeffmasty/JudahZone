package net.judah.controllers;

import static java.awt.event.KeyEvent.*;
import static net.judah.JudahZone.*;

import java.awt.KeyEventPostProcessor;
import java.awt.event.KeyEvent;

import lombok.RequiredArgsConstructor;
import net.judah.api.AudioMode;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.Instruments;
import net.judah.seq.Seq;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class Qwerty implements KeyEventPostProcessor {

	private static final int ASCII_ONE = 49;
	private final Seq seq;

	@Override
	public boolean postProcessKeyEvent(KeyEvent e) {
		// TODO knob mode
		if (e.getID() == KeyEvent.KEY_PRESSED) {
//			seq.keyPressed(e);
			return true;
		}
		
		if (e.getID() != KeyEvent.KEY_RELEASED) {
			return false;
		}
		
//		if (seq.keyReleased(e) == true)
//			return true;
		
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
            case VK_UP: return volume(true, focus); 
            case VK_DOWN: return volume(false, focus); 
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
            			loop.record(loop.isRecording() != AudioMode.RUNNING);
            	} else if (focus instanceof Instrument) {
            		Instrument line = (Instrument)focus;
            		line.setMuteRecord(!line.isMuteRecord());
            	}
            	return true;
            case VK_D: 
            	if (focus instanceof Loop) 
            		((Loop)focus).erase();
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

    private boolean volume(boolean up, Channel ch) {
        int vol = ch.getVolume();
        vol += up? 5 : -5;
        if (vol > 100) vol = 100;
        if (vol < 0) vol = 0;
        ch.getGain().setVol(vol);
        MainFrame.update(ch);
        return true;
    }

    private boolean nextChannel(boolean toRight) {
    	Looper looper = getLooper();
        Instruments channels = getInstruments();
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
