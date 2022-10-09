# JudahZone

An open source GrooveBox (mixer/looper/sequencer) written in Java for the Jack sound system. (https://jackaudio.org/)

8 mono or stereo input channels are currently handled, as well as 4 synchronized stereo recording loops. A pair of polyphonic velocity-sensitive subtractive synth engines are supplied, each with 16 voices, 3 oscillators and the regular gamut of wave shapes, envelope settings, filters and presets. All audio channels have separately controlled internal gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Delay, Reverb, an LFO, and stereo panning. These effects are supplanted by external LV2 plugins controlled over OSC. Loops can be sync'd to each other and to the the clock for different song lengths and structures. There is an 8-track step and pattern sequencer, 4 tracks for drums and 4 piano rolls. The sample player holds 4 loops, 4 one-shots, and 32 drum samples (8 samples per drum sequencer track). There is also a guitar tuner, sheet music viewer, external midi routing and midi clock support.  Extend from the SmashHit class and compose music using POJOs to control all of the GrooveBox's equipment and get per beat updates as your masterpiece hums along.

Works great with an Akai MPK-Mini Midi controller and/or a guitar/mic and Midi foot switch controller, mixer and/or pads.
A few recordings made: https://www.youtube.com/user/judahmu/videos

## Technologies

Technologies experimented with:

JNAJack (https://github.com/jaudiolibs/jnajack) crucial Java bindings to the Jack sound system without which this project wouldn't be possible. 
A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port to dynamically modify plugins using the Illposed OSC library. (http://www.illposed.com/software/javaosc.html)

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.  Also using the fluid-soundfont-gm.

Using TarsosDSP (https://github.com/JorenSix/TarsosDSP) for a guitar tuner.

Synth Engine created by combining (https://github.com/michelesr/jack-oscillator) with (https://github.com/johncch/MusicSynthesizer).

## Build
Built with Lombok (https://projectlombok.org/) and Maven. 

## Running
The Jack sound system needs to be up and running.  
This project uses the "a2jmidid -e" ALSA midi bindings along with Jack.  This project uses FluidSynth (https://www.fluidsynth.org/) (with the fluid soundfount), the Carla plugin host (https://github.com/falkTX/Carla), DragonFly Reverbs (https://michaelwillis.github.io/dragonfly-reverb/) and Calf Lv2 plugins (https://calf-studio-gear.org/).  

This project is hardcoded to connect to the author's instrument ports. Fiddle around with the Channels class to get your particular system up and running. 

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
