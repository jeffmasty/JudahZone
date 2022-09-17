package net.judah;

import static net.judah.util.AudioTools.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import com.illposed.osc.OSCSerializeException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.BasicClient;
import net.judah.api.Service;
import net.judah.controllers.Jamstik;
import net.judah.effects.Fader;
import net.judah.effects.api.PresetsDB;
import net.judah.effects.gui.ControlPanel;
import net.judah.fluid.FluidSynth;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.looper.beats.JudahBeats;
import net.judah.looper.sampler.Sampler;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Path;
import net.judah.mixer.Channels;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Mains;
import net.judah.plugin.Carla;
import net.judah.songs.SmashHit;
import net.judah.synth.JudahSynth;
import net.judah.tracker.Track;
import net.judah.util.Constants;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;
import net.judah.util.Services;
import net.judah.util.SettableCombo;

/* my jack sound system settings:
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone extends BasicClient {

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static JudahZone instance;
    @Getter private static boolean initialized;

    @Getter private static final ArrayList<JackPort> inPorts = new ArrayList<>();
    @Getter private JackPort outL, outR;
    /** external reverb, for instance */
    @Getter private static JackPort reverbL1, reverbL2, reverbR1, reverbR2;

    @Getter private static final Services services = new Services();
    @Getter private static Mains mains;
    @Getter private static Looper looper;
    @Getter private static Sampler sampler;
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final Channels channels = new Channels();
    private static JudahMidi midi;
    @Getter private static Carla carla;
    private static FluidSynth fluid;
    @Getter private static SetList setlist = new SetList();
    @Getter private static SmashHit current;
    @Getter private static JudahSynth synth;
    @Getter private static JudahBeats beats;

    
    public static void main(String[] args) {
        try {
            new JudahZone();
            RTLogger.monitor();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private JudahZone() throws JackException, JudahException, IOException, InvalidMidiDataException, OSCSerializeException {
        super(JUDAHZONE);
        instance = this;
        MainFrame.startNimbus();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        fluid = new FluidSynth(Constants.sampleRate());
        midi = new JudahMidi("JudahMidi");
        services.addAll(Arrays.asList(new Service[] {fluid, midi})); // Jamstik added later

        start();
    }

    @Override
    protected void initialize() throws JackException {
    	
        JackPort port;
        for (Instrument ch : channels.getInstruments()) {
            port = jackclient.registerPort(ch.getLeftConnection().replace(JUDAHZONE + ":", ""),
                    AUDIO, JackPortIsInput);
            ch.setLeftPort(port);
            inPorts.add(port);
            if (ch.isStereo()) {
                port = jackclient.registerPort(ch.getRightConnection().replace(JUDAHZONE + ":", ""), AUDIO, JackPortIsInput);
                ch.setRightPort(port);
                inPorts.add(port);
            }
        }

        reverbR1 = jackclient.registerPort("reverbR1", AUDIO, JackPortIsOutput);
        reverbR2 = jackclient.registerPort("reverbR2", AUDIO, JackPortIsOutput);
        reverbL1 = jackclient.registerPort("reverbL1", AUDIO, JackPortIsOutput);
        reverbL2 = jackclient.registerPort("reverbL2", AUDIO, JackPortIsOutput);
        
        outL = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
        outR = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
        looper = new Looper(outL, outR);
        synth = new JudahSynth(outL, outR);
        sampler = new Sampler(looper);
        beats = new JudahBeats();
        channels.add(synth);
        // channels.add(beats);

        mains = new Mains(outL, outR);

        try { // Initialize the Carla lv2 plugin host (now that our ports are created)
            carla = new Carla(false);
            services.add(carla);
            Constants.timer(220, () -> looper.init(carla) );
        } catch (Exception e) { throw new JackException(e); }
    }

    @Override
    protected void makeConnections() throws JackException {

        if (mains == null) {
            RTLogger.log(this, "Initialization failed.");
            return;
        }
        
        // inputs
        for (Instrument ch : channels.getInstruments()) {
            if (ch.getLeftSource() != null && ch.getLeftConnection() != null)
                jack.connect(jackclient, ch.getLeftSource(), ch.getLeftConnection());
            if (ch.getRightSource() != null && ch.getRightConnection() != null)
                jack.connect(jackclient, ch.getRightSource(), ch.getRightConnection());
        }
        // Synth input has custom Reverb
        channels.getFluid().setReverb(fluid.getReverb());
                
        // outputs
        jack.connect(jackclient, outL.getName(), "system:playback_1");
        jack.connect(jackclient, outR.getName(), "system:playback_2");

        // Initialize presets
        channels.initVolume();
        channels.initMutes();
        
    }
    
    public void initializeGui() {
    	
    	Constants.sleep(600); // allow external synth & plug-in host to startup
    	new MainFrame(JUDAHZONE);
        ControlPanel.getInstance().setFocus(channels.getGuitar());
        Jamstik.setMidiOut(new Path(midi.getFluidOut(), channels.getFluid()));
        initialized = true;
        /////////////////////////////////////////////////////////////////////////
        //                    now the system is live                           //
        /////////////////////////////////////////////////////////////////////////
        justInTimeCompiler();
    }

    // put algorithms through their paces
    private void justInTimeCompiler() {
    	MainFrame.updateTime();
        Fader.execute(Fader.fadeIn());
        looper.getDrumTrack().toggle();
        looper.getLoopA().getReverb().setActive(true);
        looper.getLoopB().getReverb().setActive(true);
        Loop c = looper.getLoopC();
    	c.record(true);
        Instrument guitar = channels.getGuitar();
        guitar.getReverb().setActive(true);
        guitar.getDelay().setActive(true);
        guitar.getChorus().setActive(true);
        guitar.getLfo().setActive(true);
        guitar.getEq().setActive(true);
        guitar.getCutFilter().setActive(true);
        mains.setOnMute(false);
        int timer = 777;
        Constants.timer(timer, () ->{
        	c.record(false);
        	Jamstik.toggle();
        	guitar.getLfo().setActive(false);
        	guitar.getReverb().setActive(false);
	        guitar.getDelay().setActive(false);
	        guitar.getChorus().setActive(false);
	        guitar.getEq().setActive(false);
	        guitar.getCutFilter().setActive(false);
	        guitar.getGain().setPan(50);
	        MainFrame.setFocus(guitar);
        	MainFrame.update(guitar);
        });
        Constants.timer(timer * 2, () -> {
        		mains.setOnMute(true);
        		if (Jamstik.isActive())
        			Jamstik.toggle();
        		looper.reset();
        		looper.getLoopA().getReverb().setActive(false);
        		looper.getLoopB().getReverb().setActive(false);
        		System.gc();
        		Constants.timer(30, () -> mains.setOnMute(false));
        });

		Track hihats = JudahClock.getTracker().getDrum2();
		hihats.setFile("HiHats");
		hihats.setPattern("closed");
		SettableCombo.highlight(null);
		
    }
    
    private class ShutdownHook extends Thread {
        @Override public void run() {
            mains.setOnMute(true);
            for (Service s : services)
                s.close();
        }
    }
    
    public static void nextSong() {
    	JComboBox<?> setlist = midi.getGui().getSetlist();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong();
	}

    /** load currently selected song in Setlist drop down */
    public static void loadSong() {
    	new Thread(()->{
	    	SmashHit song = ((SmashHit)midi.getGui().getSetlist().getSelectedItem());
	    	if (current != null)
	    		current.teardown();
	    	current = song;
	    	looper.clear();
	    	current.startup(JudahClock.getTracker(), looper, channels);
    	}).start();
    }
    
    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

    @Override
    public boolean process(JackClient client, int nframes) {

    	FloatBuffer left = outL.getFloatBuffer();
    	FloatBuffer right = outR.getFloatBuffer();
        // channels and looper will be additive
        silence(left);
        silence(right);
        silence(reverbL1.getFloatBuffer());
        silence(reverbR1.getFloatBuffer());
        silence(reverbL2.getFloatBuffer());
        silence(reverbR2.getFloatBuffer());

        if (mains.isOnMute() || !initialized) return true;

        // mix the live streams
        for (LineIn ch : channels) {
            if (ch.isOnMute()) continue;
            ch.process(); // internal effects

            // line-in stereo pan left channel (0.5 pan = 1.0 gain)
            float gainL = (1 - ch.getPan()) * 2;
            float gainR = ch.getPan() * 2;
            mix(ch.getLeftPort().getFloatBuffer(), gainL, left);

            // pan and mono/stereo
            if (ch.isStereo())
                mix(ch.getRightPort().getFloatBuffer(), gainR, right);
            else
                mix(ch.getLeftPort().getFloatBuffer(), gainR, right);
        }
        
        // get loops and samples in on process()
        looper.process();
        sampler.process();
        

        // final mix bus effects:
        mains.process();

        return true;
    }

    public void recoverMidi() {
        new Thread(() -> {
        	if (midi != null) midi.close();
            Constants.sleep(100);
            try {
                midi = new JudahMidi("JudahMidi");
            } catch (Exception e) {
                RTLogger.warn(this, e);
            }
        }).start();
    }

}

