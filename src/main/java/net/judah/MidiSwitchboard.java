package net.judah;

import java.util.HashMap;

import org.jaudiolibs.jnajack.JackPort;

import net.judah.api.Midi;
import net.judah.controllers.Controller;
import net.judah.controllers.KorgMixer;
import net.judah.controllers.KorgPads;
import net.judah.controllers.MPK;
import net.judah.controllers.MidiPedal;
import net.judah.midi.JudahMidi;
import net.judah.plugin.ArduinoPedal;

public class StageCommands {
	
	private final HashMap<JackPort, Controller> controllers = new HashMap<>();
	
	public StageCommands(JudahMidi midi) {
		if (midi.getMixer() != null) controllers.put(midi.getMixer(), new KorgMixer());
		if (midi.getPads() != null) controllers.put(midi.getPads(), new KorgPads());
		if (midi.getArduino() != null) controllers.put(midi.getArduino(), new ArduinoPedal());
		if (midi.getPedal() != null) controllers.put(midi.getPedal(), new MidiPedal());
		if (midi.getKeyboard() != null) controllers.put(midi.getKeyboard(), new MPK());
		
		if (controllers.isEmpty()) throw new NullPointerException("No Midi IN ports connected.");
    }

	/** Midi Input switchboard
     * @param midi midi bytes received
     * @param port received the midi message */
    public boolean midiProcessed(Midi midi, JackPort port) {
    	if (controllers.get(port) != null)
    		return controllers.get(port).midiProcessed(midi);
    	return false;
    }
    
//    private boolean oldProcessed(Midi midi, JackPort port) {
//    	
//        if (midi.isCC()) {
//        		int data1;
//        		int data2;
//        	data1 = midi.getData1();
//            data2 = midi.getData2();
////        	for (Controller c : controllers) 
////        		if (c.getChannel() == ch && c.midiProcessed(data1, data2))
////        			return true;
//        	
//            // Toggle BeatBuddy / Drum Loop Volume
////            if (data1 == MPKTools.KNOBS.get(1)) {
////                if (getLooper().getDrumTrack().hasRecording())
////                    getLooper().getDrumTrack().setVolume(midi.getData2());
////                else
////                    getChannels().getDrums().setVolume(midi.getData2());
////                return true;
////            }
////            
////            // Master Volume
////            if (data1 == MPKTools.KNOBS.get(0)) {
////                getMasterTrack().setVolume(data2); return true;
////            }
////
////            // Guitar Volume
////            if (data1 == MPKTools.KNOBS.get(2)) {
////                getChannels().getGuitar().setVolume(data2); return true;}
////            // Mic Volume
////            if (data1 == MPKTools.KNOBS.get(3)) {
////                getChannels().getMic().setVolume(data2); return true;}
////            // Loop A Volume
////            if (data1 == MPKTools.KNOBS.get(4)) {
////                getLooper().getLoopA().setVolume(data2); return true;}
////            // Loop B Volume
////            if (data1 == MPKTools.KNOBS.get(5)) {
////                getLooper().getLoopB().setVolume(data2); return true;}
////            // Combined Aux1 & 2 Volume
////            if (data1 == MPKTools.KNOBS.get(6)) {
////                getChannels().getAux1().setVolume(data2);
////                getChannels().getAux2().setVolume(data2); return true;}
////            // Synth Volume
////            if (data1 == MPKTools.KNOBS.get(7)) {
////                getChannels().getSynth().setVolume(data2); return true;}
//
//            // first row of CC pads
//            if (data1 == MPKTools.PRIMARY_CC.get(0)) {// record loop A cc pad
//                getLooper().getLoopA().record(midi.getData2() > 0);
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_CC.get(1)) {// record loop B cc pad
//                getLooper().getLoopB().record(midi.getData2() > 0);
//                return true;
//            }
//
//            // TODO drum track record, double loop b length
//            if (data1 == MPKTools.PRIMARY_CC.get(2) && midi.getData2() > 0) {// CC pad 2: slave loop B
//                new Thread() { @Override public void run() {
//                        getLooper().syncLoopB(); }}.start();
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_CC.get(3) && midi.getData2() > 0) { // clear loopers cc pad
//                getLooper().stopAll();
//                new Thread() {
//                    @Override public void run() {
//                        getLooper().clear();
//                        getLooper().getDrumTrack().toggle();
//                    }
//                }.start();
//                return true;
//            }
//
//            // 2nd row of CC pads
//            if (data1 == MPKTools.PRIMARY_CC.get(4)) {// mute loop A cc pad
//                getLooper().getLoopA().setOnMute(midi.getData2() != 0);
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_CC.get(5)) {// mute loop B cc pad
//                getLooper().getLoopB().setOnMute(midi.getData2() != 0);
//                return true;
//            }
//
//            // play beat buddy
//            if (data1 == MPKTools.PRIMARY_CC.get(6) && midi.getData2() > 0) {
//                getDrummachine().play();
//                return true;
//            }
//
//
//            if (data1 == MPKTools.PRIMARY_CC.get(7) && midi.getData2() > 0) { // setup a drumtrack slave loop
//                getLooper().getDrumTrack().toggle();
//                return true;
//            }
//
//
//            if (data1 == MPKTools.JOYSTICK_DOWN_CC && midi.getPort().equals(JudahMidi.getInstance().getKeyboard().getShortName())) {
//                if (data2 < 20) {
//                    getMasterTrack().getOverdrive().setActive(false);
//                    getMasterTrack().getChorus().setActive(false);
//                }
//                else {
//                    getMasterTrack().getOverdrive().setActive(true);
//                    getMasterTrack().getOverdrive().setDrive((data2 - 28) / 100f);
//                    getMasterTrack().getChorus().setActive(true);
//                    getMasterTrack().getChorus().setFeedback((data2 - 50) / 100f);
//                }
//                return true;
//            }
//
//            if (data1 == MPKTools.JOYSTICK_UP_CC) {
//                if (data2 < 77)
//                    getMasterTrack().getDelay().reset();
//                else {
//                    Delay d = getMasterTrack().getDelay();
//                    d.setActive(true);
//                    if (d.getDelay() < Delay.DEF_TIME) d.setDelay(Delay.DEF_TIME);
//                    getMasterTrack().getDelay().setFeedback((data2 - 77) / 50f); // 77 to 127;
//                }
//            }
//
//        } // end is CC
//
//        else if (midi.isProgChange()) {
//            int data1 = midi.getData1();
//
//            boolean result = false;
//            for (Plugin plugin : getPlugins())
//                if (plugin.getDefaultProgChange() == data1) {
//                    // octaver..
//                    plugin.activate(getChannels().getGuitar());
//                    result = true;
//                }
//            if (result) return true;
//
//            if (data1 == MPKTools.PRIMARY_PROG[3]) { // up instrument
//                new Thread() { @Override public void run() {
//                    getSynth().instUp(0, true);
//                }}.start();
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[7]) { // up instrument
//                new Thread(()->{getSynth().instUp(0, false);}).start();
//                return true;
//            }
//
//            if (data1 == MPKTools.PRIMARY_PROG[0]) { // I want bass
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 33));
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[1]) { // harp
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 46));
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[2]) { // piano
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 0));
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[4]) { // strings
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 44));
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[5]) { // church organ
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 19));
//                return true;
//            }
//            if (data1 == MPKTools.PRIMARY_PROG[6]) { // electric piano
//                JudahMidi.getInstance().queue(FluidSynth.getInstance().progChange(0, 5));
//                return true;
//            }
//
//
//
//
//
//        }
//        return false;
//    }

}
