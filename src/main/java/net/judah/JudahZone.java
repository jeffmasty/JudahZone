package net.judah;

import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.BasicClient;
import net.judah.api.Service;
import net.judah.effects.Fader;
import net.judah.effects.api.PresetsHandler;
import net.judah.fluid.FluidSynth;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.mixer.LineIn;
import net.judah.mixer.MasterTrack;
import net.judah.plugin.BeatBuddy;
import net.judah.plugin.Carla;
import net.judah.plugin.TalReverb;
import net.judah.sequencer.Sequencer;
import net.judah.settings.Channels;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.RTLogger;
import net.judah.util.Services;

/* my jack sound system settings:
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone extends BasicClient {

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static JudahZone instance;
    @Getter private static boolean initialized;

    @Getter private static final ArrayList<JackPort> inPorts = new ArrayList<>();
    @Getter private static final ArrayList<JackPort> outPorts = new ArrayList<>();
    @Getter private JackPort outL, outR;
    /** external reverb, for instance */
    @Getter private static JackPort effectsL, effectsR;

    @Getter private static final Services services = new Services();
    @Getter private static final CommandHandler commands = new CommandHandler();

    @Getter private static MasterTrack masterTrack;
    @Getter private static final Channels channels = new Channels();
    @Getter private static final Looper looper = new Looper(outPorts);
    @Getter private static final Plugins plugins = new Plugins();
    @Getter private static final PresetsHandler presets = new PresetsHandler();

    @Getter private static JudahMidi midi;
    @Getter private static Carla carla;
    @Getter private static BeatBuddy drummachine;
    @Getter private static FluidSynth synth;
    @Getter private static Metronome metronome;

    public static void main(String[] args) {
        try {
            new JudahZone();
            while (true)
                RTLogger.poll();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    JudahZone() throws Throwable {
        super(JUDAHZONE);
        instance = this;
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        synth = new FluidSynth(Constants.sampleRate());
        drummachine = new BeatBuddy();
        midi = new JudahMidi("JudahMidi", drummachine);
        metronome = new Metronome(drummachine, midi,
                Constants.resource("metronome/JudahZone.mid"));

        services.addAll(Arrays.asList(new Service[] {drummachine, synth, metronome, midi}));

        start();
    }

    @Override
    protected void initialize() throws JackException {
        JackPort port;
        for (LineIn ch : channels) {
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

        for (String name : new String[] { "left", "right" /* ,"auxL", "auxR" */})
            outPorts.add(jackclient.registerPort(name, AUDIO, JackPortIsOutput));
        outL = outPorts.get(LEFT_CHANNEL);
        outR = outPorts.get(RIGHT_CHANNEL);
        effectsL = jackclient.registerPort("effectsL", AUDIO, JackPortIsOutput);
        effectsR = jackclient.registerPort("effectsR", AUDIO, JackPortIsOutput);


        String debug = "channels: ";
        for (LineIn ch : channels)
            debug += ch.getName() + " ";
        log.debug(debug);

        try { // Initialize the Carla lv2 plugin host (now that our ports are created)
            carla = new Carla(true);
            Thread.sleep(950);
            services.add(carla);
            plugins.addAll(carla.getPlugins());

        } catch (Exception e) { throw new JackException(e); }
        masterTrack = new MasterTrack(outL, outR, effectsL, effectsR, carla.getReverb());
        masterTrack.setIcon(Icons.load("Speakers.png"));

        looper.init(carla);

    }

    @Override
    protected void makeConnections() throws JackException {
        // inputs
        for (LineIn ch : channels) {
            if (ch.getLeftSource() != null && ch.getLeftConnection() != null)
                jack.connect(jackclient, ch.getLeftSource(), ch.getLeftConnection());
            if (ch.getRightSource() != null && ch.getRightConnection() != null)
                jack.connect(jackclient, ch.getRightSource(), ch.getRightConnection());
        }
        // Synth input has custom Reverb
        channels.getSynth().setReverb(synth.getReverb());

        // outputs
        jack.connect(jackclient, outL.getName(), "system:playback_1");
        jack.connect(jackclient, outR.getName(), "system:playback_2");

        // Initialize command registry and presets, and start GUI
        commands.initializeCommands();

        new MainFrame(JUDAHZONE);
        channels.initVolume();
        new TalReverb(carla, carla.getPlugins().byName(TalReverb.NAME))
            .initialize(Constants.sampleRate(), Constants.bufSize());

        drummachine.setVolume(55);

        MixerPane.getInstance().setFocus(masterTrack);
        initialized = true;
        /////////////////////////////////////////////////////////////////////////
        //                    now the system is live                           //
        /////////////////////////////////////////////////////////////////////////
        // Open a default song
        Constants.timer(100, () ->{
            File file = new File("/home/judah/git/JudahZone/resources/Songs/BeatBox");
            try { new Sequencer(file);
            } catch (Exception e) {
                Console.warn(e.getMessage() + " " + file.getAbsolutePath(), e); }
        });
        Fader.execute(Fader.fadeIn());
        masterTrack.setOnMute(false);

    }

    private class ShutdownHook extends Thread {
        @Override public void run() {
            masterTrack.setOnMute(true);
            if (Sequencer.getCurrent() != null)
                Sequencer.getCurrent().close();

            for (Service s : services)
                s.close();
        }
    }

    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

    @Override
    public boolean process(JackClient client, int nframes) {

        // channels and looper will be additive
        processSilence(outL.getFloatBuffer());
        processSilence(outR.getFloatBuffer());
        processSilence(effectsL.getFloatBuffer());
        processSilence(effectsR.getFloatBuffer());

        if (masterTrack == null || masterTrack.isOnMute()) return true;

        // mix the live streams
        for (LineIn ch : channels) {
            if (ch.isOnMute()) continue;
            ch.process(); // internal effects

            // do line-in stereo pan here (0.5 pan = 1.0 gain)
            float gainL = (1 - ch.getPan()) * 2;
            float gainR = ch.getPan() * 2;
            processAdd(ch.getLeftPort().getFloatBuffer(), gainL, outL.getFloatBuffer());

            //line-in todo PAN
            if (ch.isStereo())
                processAdd(ch.getRightPort().getFloatBuffer(), gainR, outR.getFloatBuffer());
            else
                processAdd(ch.getLeftPort().getFloatBuffer(), gainR, outR.getFloatBuffer());
        }

        // get looper in on process()
        looper.process();

        // final mix bus effects:
        masterTrack.process();

        return true;
    }

}
