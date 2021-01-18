package net.judah;

import static net.judah.JudahZone.*;

import net.judah.api.Midi;
import net.judah.looper.Recorder;
import net.judah.midi.JudahMidi;
import net.judah.mixer.bus.Delay;
import net.judah.plugin.MPK;
import net.judah.plugin.Plugin;
import net.judah.util.Console;

public class StageCommands {

    /** if the midi msg is a hard-coded mixer setting, run the setting and return false */
    public boolean midiProcessed(Midi midi) {

        if (midi.isCC()) {
            int data1 = midi.getData1();
            int data2 = midi.getData2();

            // Master Volume
            if (data1 == MPK.KNOBS.get(0)) {
                getMasterTrack().setVolume(data2); return true;
            }
            // Toggle BeatBuddy / Drum Loop Volume
            if (data1 == MPK.KNOBS.get(1)) {
                if (getLooper().getDrumTrack().hasRecording())
                    getLooper().getDrumTrack().setVolume(midi.getData2());
                else
                    getChannels().getDrums().setVolume(midi.getData2());
                return true;
            }
            // Guitar Volume
            if (data1 == MPK.KNOBS.get(2)) {
                getChannels().getGuitar().setVolume(data2); return true;}
            // Mic Volume
            if (data1 == MPK.KNOBS.get(3)) {
                getChannels().getMic().setVolume(data2); return true;}
            // Loop A Volume
            if (data1 == MPK.KNOBS.get(4)) {
                getLooper().get(0).setVolume(data2); return true;}
            // Loop B Volume
            if (data1 == MPK.KNOBS.get(5)) {
                getLooper().get(1).setVolume(data2); return true;}
            // Combined Aux1 & 2 Volume
            if (data1 == MPK.KNOBS.get(6)) {
                getChannels().getAux1().setVolume(data2);
                getChannels().getAux2().setVolume(data2); return true;}
            // Synth Volume
            if (data1 == MPK.KNOBS.get(7)) {
                getChannels().getSynth().setVolume(data2); return true;}

            // first row of CC pads
            if (data1 == MPK.PRIMARY_CC.get(0)) {// record loop A cc pad
                ((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
                return true;
            }
            if (data1 == MPK.PRIMARY_CC.get(1)) {// record loop B cc pad
                ((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
                return true;
            }

            // TODO drum track record, double loop b length
            if (data1 == MPK.PRIMARY_CC.get(2) && midi.getData2() > 0) {// CC pad 2: slave loop B
                new Thread() {
                    @Override public void run() {
                        getLooper().slave(); }}.start();

            }
            if (data1 == MPK.PRIMARY_CC.get(3) && midi.getData2() > 0) { // clear loopers cc pad
                getLooper().stopAll();
                new Thread() {
                    @Override public void run() {
                        getLooper().clear();
                        getLooper().getDrumTrack().toggle();
                    }
                }.start();
                return true;
            }

            // 2nd row of CC pads
            if (data1 == MPK.PRIMARY_CC.get(4)) {// mute loop A cc pad
                getLooper().get(0).setOnMute(midi.getData2() != 0);
                return true;
            }
            if (data1 == MPK.PRIMARY_CC.get(5)) {// mute loop B cc pad
                getLooper().get(1).setOnMute(midi.getData2() != 0);
                return true;
            }

            // play beat buddy
            if (data1 == MPK.PRIMARY_CC.get(6) && midi.getData2() > 0) {
                getDrummachine().play();
                return true;
            }


            if (data1 == MPK.PRIMARY_CC.get(7) && midi.getData2() > 0) { // setup a drumtrack slave loop
                getLooper().getDrumTrack().toggle();
                return true;
            }

            if (midi.getPort().equals(JudahMidi.getInstance().getPedal().getShortName())) {
                // foot pedal[0] octaver effect
                if (data1 == MPK.PEDAL.get(0)) {
                    new Thread() {
                        @Override public void run() {
                            try { getCarla().octaver(midi.getData2() > 0);
                            } catch (Throwable t) { Console.warn(t);}
                        }}.start();
                }

                if (data1 == MPK.PEDAL.get(1)) { // mute Loop A foot pedal
                    getLooper().get(0).setOnMute(midi.getData2() > 0);
                    return true;
                }
                if (data1 == MPK.PEDAL.get(2) && midi.getData2() > 0) { // trigger only foot pedal
                    getDrummachine().transission(); // drummachine.send(BeatBuddy.CYMBOL, 100);
                    return true;
                }

                if (data1 == MPK.PEDAL.get(3) ) { // record Drum Track
                    Console.info("Record Drum Track Toggle");
                    getLooper().getDrumTrack().record(midi.getData2() > 0);
                }
                if (data1 == MPK.PEDAL.get(4)) { // record Loop B foot pedal
                    ((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
                    return true;
                }
                if (data1 == MPK.PEDAL.get(5)) { // record Loop A foot pedal
                    ((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
                    return true;
                }
            }

            if (data1 == MPK.JOYSTICK_DOWN_CC && midi.getPort().equals(JudahMidi.getInstance().getKeyboard().getShortName())) {
                if (data2 < 20) {
                    getMasterTrack().getOverdrive().setActive(false);
                }
                else {
                    getMasterTrack().getOverdrive().setActive(true);
                    getMasterTrack().getOverdrive().setDrive((data2 - 27) / 100f);
                }
                return true;
            }

            if (data1 == MPK.JOYSTICK_UP_CC) {
                if (data2 < 77)
                    getMasterTrack().getDelay().reset();
                else {
                    Delay d = getMasterTrack().getDelay();
                    d.setActive(true);
                    if (d.getDelay() < Delay.DEF_TIME) d.setDelay(Delay.DEF_TIME);
                    getMasterTrack().getDelay().setFeedback((data2 - 77) / 50f); // 77 to 127;
                }
            }

        } // end is CC

        else if (midi.isProgChange()) {
            int data1 = midi.getData1();

            boolean result = false;
            for (Plugin plugin : getPlugins())
                if (plugin.getDefaultProgChange() == data1) {
                    plugin.activate(getChannels().getGuitar());
                    result = true;
                }
            if (result) return true;

            if (data1 == MPK.PRIMARY_PROG[3]) { // up instrument
                new Thread() { @Override public void run() {
                    getSynth().instUp(0, true);
                }}.start();
                return true;
            }
            if (data1 == MPK.PRIMARY_PROG[7]) { // up instrument
                new Thread() { @Override public void run() {
                    getSynth().instUp(0, false);
                }}.start();
                return true;
            }

        }
        return false;
    }

}
