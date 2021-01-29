package net.judah.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import org.apache.commons.lang3.CharUtils;
import org.jaudiolibs.jnajack.JackPort;
import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.midi.MidiClock;
import net.judah.midi.ProgMsg;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/** listens to an incoming Midi Clock stream and knows how to communicate to a Beat Buddy drum machine */
@Log4j
public class BeatBuddy extends ArrayList<Command> implements MidiClock, Service {

    /** location where BeatBuddy's SD card's SONGS are mirrored locally */

    public static final File INSTALL_FOLDER = new File("/home/judah/SDCARD/SONGS");
    public static final String CONFIG_CSV = "CONFIG.CSV";
    /** a song/temp/drumset configuration string in a song file's settings <br/>
     * x = songNum, y = tempo, z = drumset */
    public static final String CONFIG_FORMAT = "folderX@YY:Z";
    private static final int CC = ShortMessage.CONTROL_CHANGE;

    // investigate beat buddy sent: 240.127.127/0  176.102.2/0
    public static enum Folder {
        Blues, Brazilian, BrushesBeats, Country, DrumAndBass, Funk,
        HipHop, JamSongs, Jazz, Latin, Marching, Metal, OddTime, Oldies,
        Pop, Punk, RnB, Reggae, Rock, Techno, Beatbox, World, Metronome
    }

    public static enum Drumset {
        _notUsed, Standard, Rock, Metal, Jazz, Brushes, Percussion, Latin,
        Dance, Ethereal, Voice, Liverpool, TR808, TR707, STAXStrings

    }

    public static final String PARAM_BUDDY = "beatbuddy";
    // Song select is prog change
    public static final int FOLDER_MSB = 0;
    public static final int FOLDER_LSB = 32;
    public static final int DATA_UP = 96;
    public static final int DATA_DOWN = 97;
    public static final int REG_DATA_1 = 98; // assign data up/down to tempo, data2 = 106
    public static final int REG_DATA_2 = 99; // assign data up/down to temp, data2 = 107

    public static final int NEXT_PART = 102;
    public static final int TEMPO_MSB = 106;
    public static final int TEMPO_LSB = 107;
    public static final int VOLUME = 108;
    public static final int CYMBOL = 110;
    public static final int CONTINUE = 111;
    public static final int DRUM_FILL = 112;
    public static final int TRANSITION = 113; // data2 = song part number, see notes
    public static final int START = 114;
    public static final int OUTRO = 115;
    public static final int DRUMSET = 116; // data2 = set number
    public static final int TAPT_MODE = 117; // enter tap tempo mode

    public static final int NORMAL_VELOCITY = 100;

    public static final Midi CYMBOL_HIT = Midi.create(CC, 9, CYMBOL, NORMAL_VELOCITY);
    public static final Midi PLAY_MIDI = Midi.create(CC, START, NORMAL_VELOCITY);
    public static final Midi PAUSE_MIDI = Midi.create(CC, CONTINUE, NORMAL_VELOCITY);
    public static final Midi STOP_MIDI = Midi.create(CC, OUTRO, NORMAL_VELOCITY);
    public static final Midi TAP_TEMPO = Midi.create(CC, TAPT_MODE, NORMAL_VELOCITY);

    static final Midi FOLDER_FIRST = Midi.create(CC, FOLDER_MSB, 0);

    @Setter JackPort out;
    ArrayList<TimeListener> listeners = new ArrayList<>();

    @Getter private float tempo = 90f;
    @Getter private int beat;
    @Getter private boolean play;
    @Getter @Setter
    private ConcurrentLinkedQueue<ShortMessage> queue =
            new ConcurrentLinkedQueue<>();
    private BeatBuddyGui gui;

    int pulse;
    long ticker;
    long delta;

    @Data class Dir {
        final File dir;
        final String name;
    }
    @Data class Track {
        final int idx;
        final String name;
    }

    // gui access
    final ArrayList<Dir> directories = new ArrayList<>();
    final HashMap<String, List<Track>> tracks = new HashMap<>();

    public BeatBuddy() {
        try {
            loadSDCard();
            // addListener(JudahClock.getInstance());
        } catch (Throwable t) { Console.warn(t); }
        // add Commands: song, nextSong, partNum, nextPart, drumset, tempo, volume, start, pause, outro
    }

    private void loadSDCard() throws IOException {
        File config = new File(INSTALL_FOLDER, CONFIG_CSV);
        BufferedReader reader = new BufferedReader(new FileReader(config));
        List<String[]> list = new ArrayList<>();
        String str = null;
        while((str = reader.readLine())!=null) {
            list.add(str.split(","));
        }
        reader.close();
        for (String[] entry : list) {
            String[] suffix = entry[1].split("\\.");
            Dir dir = new Dir(new File(INSTALL_FOLDER, entry[0]), suffix[1]);
            directories.add(dir);
            tracks.put(dir.name, new ArrayList<>());
            File config2 = new File(dir.dir, CONFIG_CSV);
            reader = new BufferedReader(new FileReader(config2));
            String str2 = null;
            while((str2 = reader.readLine())!=null) {
                String[] hash = str2.split(",");
                String[] track = hash[1].split("\\.");
                tracks.get(dir.name).add(new Track(Integer.parseInt(track[0]), track[1].replaceAll(" ", "")));
            }
            reader.close();
        }
        for (Dir dir : directories) {
            log.trace(dir.getName());
            for (Track t : tracks.get(dir.name)) {
                log.trace("    " + t.getName() + " (" + t.idx + ")");
            }
        }
    }

    @Override public void addListener(TimeListener l) { if (!listeners.contains(l)) listeners.add(l); }
    @Override public void removeListener(TimeListener l) { listeners.remove(l); }
    @Override public int getMeasure() { return 0; /* no-op */}
    @Override public void setMeasure(int bpb) {/* no-op */ }
    @Override public List<Command> getCommands() { return this; }

    @Override public long getLastPulse() { return ticker; }

    public BeatBuddyGui getGui() {
        if (gui == null)
            gui = new BeatBuddyGui(this);
        return gui;
    }

    public void setVolume(int vol) {
        if (vol < 0 || vol > 127) throw new InvalidParameterException("" + vol);
        queue.offer(Midi.create(CC, BeatBuddy.VOLUME, vol));
    }

    public void play(boolean play) {
        this.play = play;
        if (play)
            queue.offer(PLAY_MIDI);
        else
            queue.offer(PAUSE_MIDI);
    }

    public void play() {
        if (play == false) {
            play = true;
            queue.offer(PLAY_MIDI);
            return;
        }
        queue.offer(PAUSE_MIDI);
    }

    public void send(int command, int value) {
        queue.offer(Midi.create(CC, command, value));
    }

    @Override
    public void close() {
        setVolume(0);
    }

    @Override
    public void properties(HashMap<String, Object> props) {
        if (props.containsKey(PARAM_BUDDY)) // load song in pedal
            parseConfig(props.get(PARAM_BUDDY).toString());
    }


    /** @param config beatbuddy config string format:  <br/>
     *  folderX@YY:Z  <br/>
     *  where X = song#, YY = tempo and Z = an optional drumset index*/
    public void parseConfig(String config) {
        String[] split = config.split("@");
        if (split.length != 2) {
            Console.info(config + " does not match " + CONFIG_FORMAT);
            return;
        }

        int song = 0;
        int choke = 0;
        for (int i = 0; i < split[0].length(); i++) {
            char c = split[0].charAt(i);
            if (CharUtils.isAsciiNumeric(c)) {
                if (song == 0) {
                    choke = i;
                    song = CharUtils.toIntValue(c);
                }
                else song = song * 10 + CharUtils.toIntValue(c);
            }
        }
        if (choke == 0) {
            Console.info(config + " does not match " + CONFIG_FORMAT);
            return;
        }
        String name = split[0].substring(0, choke);
        Folder folder = Folder.valueOf(name);

        if (song < 0 || song > 127 || folder == null) {
            Console.info(config + " does not match " + CONFIG_FORMAT);
            return;
        }
        song(folder.ordinal(), song);

        String[] suffix = split[1].split(":");
        int bpm = Integer.parseInt(suffix[0]);

        int temp = 0;
        if (suffix.length == 2)
            if (suffix[1].length()>3)
                temp = Drumset.valueOf(suffix[1]).ordinal();
            else
                temp = Integer.parseInt(suffix[1]);
        final int drumset = temp;
        new Thread() { @Override public void run() {
            try {Thread.sleep(450);} catch (Throwable t) { }
            setTempo(bpm);
            if (drumset <= 0 || drumset >= Drumset.values().length)
                return;
            drumset(drumset);
        }}.start();
    }

    public void song(int folder, int songNum) {
        try {
            Midi songmidi = new ProgMsg(0, songNum);
            Console.info(PARAM_BUDDY + " " + folder + ":" + Folder.values()[folder] + " " + songNum);
            queue.offer(FOLDER_FIRST);
            queue.offer(Midi.create(CC, FOLDER_LSB, folder));
            queue.offer(songmidi);
            firstPart = true;
            if (gui != null) gui.song(folder, songNum);
        } catch (InvalidMidiDataException e) {
            Console.info(e.getMessage());
        }
    }

    /**TRANSITION (CC-113)
     * BeatBuddy triggers a transition when a custom transition message is received.
     * You can select which part to go after the transition by setting the value of
     * the command to the index of the part you want to jump. The transition will
     * continue playing in a loop until the value is changed to 0. <pre>
        ○ Value 1 → Part 1
        ○ Value 2 → Part 2
        ○ Value 3 → Part 3
        ○ Value 127 → Next Part
        ○ Value 0 → BeatBuddy ends transition and goes to the selected song part as
            specified in the original value, as specified above.</pre>
    */
    public void transission(int destination) {
        Midi target = Midi.create(CC, TRANSITION, destination);
        queue.offer(target);
        new Thread() { @Override public void run() {
                Constants.sleep(90);
                Midi enact = Midi.create(CC, TRANSITION, 0);
                queue.offer(enact);
            }
        }.start();
    }
    private boolean firstPart = true;
    public void transission() {
        transission( firstPart ? 2:1 );
        firstPart = !firstPart;
    }

    /** Tempo MSB & LSB: To directly set the tempo to a specific BPM,
      we need to use the Tempo MSB and Tempo LSB. The Beatbuddy will update its current tempo only
       when receiving the LSB message. So the order of the message should
       be: 1. MSB value (CC-106)  2. LSB value  (CC-107) <pre>
    MSB     LSB     Tempo
    (CC-106)(CC-107)
    0       40      40  // min
    0       127     127
    1       0       128
    1       25      153
    1       50      178
    2       0       256
    2       44      300 // max  </pre>*/
    @Override public boolean setTempo(float tempo) {
        int remainder = Math.round(tempo);
        int msb = 0;
        if (tempo - 127 > 0) {
            msb = 1;
            remainder -= 127;
        }
        if (tempo - 256 > 0) {
            msb = 2;
            remainder -= 127;
        }
        queue.offer(Midi.create(CC, TEMPO_MSB, msb));
        queue.offer(Midi.create(CC, TEMPO_LSB, remainder));
        return true;
    }

    public void drumset(int idx) {
        queue.offer(Midi.create(CC, BeatBuddy.DRUMSET, idx));
        if (gui != null) {
            gui.changingFolders = true;
            gui.drumsets.setSelectedIndex(idx - 1);
            gui.changingFolders = false;
        }
    }

    /** Tap-Tempo (CC-117)
    When BeatBuddy receives the CC-117 MIDI command, it enters Tap Tempo mode.
    This is a useful way to enter Tap Tempo mode hands free if you have an external
    device that can send this command (such as the Guitar Wing). Sending out the
    CC-117 MIDI command multiple times is like tapping out the tempo with the pedal. */
    public void tapTempo() {
        queue.offer(TAP_TEMPO);
    }

    /** in RealTime */
    @Override
    public void processTime(byte[] midi) {
        int stat;
        if (midi.length == 1)
            stat = midi[0] & 0xFF;
        else if (midi.length == 2) {
            stat = midi[1] & 0xFF;
            stat = (stat << 8) | (midi[0] & 0xFF);
        }
        else {
            stat = 0;
            RTLogger.log(this, midi.length + " " + new Midi(midi));
        }

        if (ShortMessage.TIMING_CLOCK == stat) {
            pulse++;
            if (pulse == 1) {
                ticker = System.currentTimeMillis();
            }
            if (pulse == 25) {
                listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
            }
            if (pulse == 49) { // hopefully 2 beats will be more accurate than 1
                listeners.forEach(listener -> {listener.update(Property.BEAT, ++beat);});
                delta = System.currentTimeMillis() - ticker;
                float temptempo = Constants.toBPM(delta, 2);
                if (Math.round(temptempo) != tempo) {
                    tempo = Math.round(temptempo);

                    listeners.forEach(l -> {l.update(Property.TEMPO, tempo); });
                    if (gui != null) gui.tempo((int)tempo);
                    // RTLogger.log(this, "TEMPO : " + tempo);
                }
                pulse = 0;
            }
        }

        else if (ShortMessage.START == stat) {
            RTLogger.log(this, "MIDI START!");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportStarting); });
            beat = 0;
            pulse = 0;
        }

        else if (ShortMessage.STOP == stat) {
            RTLogger.log(this, "MIDI STOP");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportStopped); });
        }

        else if (ShortMessage.CONTINUE == stat) {
            RTLogger.log(this, "MIDI CONTINUE");
            listeners.forEach(l -> {l.update(Property.TRANSPORT,
                    JackTransportState.JackTransportRolling); });

        }
        else
            RTLogger.log(this, "unknown beat buddy " + new Midi(midi));
    }

    @Override
    public void begin() { play(true); }

    @Override
    public void end() { play(false); }


}

/* from BeatBuddy Midi Manual pdf:
Tempo control
Since the BeatBuddy’s tempo ranges from 40BPM - 300BPM, we can’t
use just one CC command to cover the whole range because a CC
command can only have 128 values. There are two ways of changing the
tempo. With INC/DEC (increase/decrease) messages which adjust the
tempo up or down by 1 BPM, or by skipping directly to a specific tempo
BPM using the MSB/LSB system, like in the Song Selection system
(Program Change). However, it’s a bit more complicated because unlike
the Song Selection system where you have dedicated CC commands to
respresent the MSB (CC-0) and LSB (CC-32), there is no dedicated CC
commands for Tempo. So we use the “NRPN Register” (Non-Registered
Parameter Number) which is a general purpose MSB (CC-99) and LSB
(CC-98) that can be used to control tempo or any other parameter, or
multiple parameters at once. Currently we’re only using it to control
tempo, but we follow the MIDI Standard protocol to leave room for
further control in the future. Because of this capability for multiple
parameter control, the steps below are followed.
INC/DEC  (see. https://www.midi.org/specifications) ​
 ​
Here are the common steps to do to control the BeatBuddy’s tempo. It
follows the Data INC/DEC specification of the MIDI protocol:
Step to increment tempo
Step
 Message
 Details
s
1*
 CC–99 / 106
 Set the NRPN MSB register to Tempo MSB
2*
 CC–98 / 107
 Set the NRPN LSB register to Tempo LSB
3
 CC–96 / 1
 Increment the tempo by one
4*
 CC–99 / 127
 Clear the NRPN MSB register
 5*
 CC–98 / 127
 Clears the NRPN LSB register
Step to decrement tempo
Step
 Message
 Details
s
1*
 CC–99 / 106
 Set the NRPN MSB register to Tempo MSB
2*
 CC–98 /107
 Set the NRPN LSB register to Tempo LSB
3
 CC–97 / 1
 Decrement the tempo by one
4*
 CC–99 / 127
 Clear the NRPN MSB register
5*
 CC–98 / 127
 Clears the NRPN LSB register
Steps with a * are optional if the only value control by Inc/Dec is the
Tempo. By default, the Beatbuddy will increment / decrement the tempo
when receiving a INC/DEC message.

CC-0 [0-127]  Bank (Song folder) Select MSB
CC-32 [0-127] Bank (Song folder) Select LSB
CC-96 [1-127] Data increment (+1) – INC
CC-97  [1-127]  Data decrement (-1) – DEC
*/

