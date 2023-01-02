# JudahZone

An open source GrooveBox (mixer/effects/looper/sequencer) written in Java for the Jack sound system. (https://jackaudio.org/)

8 mono or stereo input channels are currently handled, as well as 4 synchronized stereo recording loops. All audio channels have separately controlled internal Gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Delay, Reverb, Compression, Stereo panning and an LFO. Loop recordings may be sync'd to each other or to the clock for different song lengths and structures. There is a 10-track song/pattern Midi editor/sequencer, 4 tracks for drums and 6 piano rolls. These tracks can be played through external gear or through the internal synth engines (subtractive, with 16 voices, 3 oscillators and the regular gamut of wave shapes, envelopes, filters, detuning/harmonics and presets). The sample player holds 4 loops, 4 one-shots and 32 drum samples (4 drum kits with 8 samples per kit). There is also a guitar tuner, sheet music viewer, external midi routing and midi clock support.  Currently, the following Midi controllers are integrated in the live system: Akai MPKmini, Korg NanoPad2 and Kontrol2, Line6 FBV Shortboard, Jamstik MIDI guitar, an old Arturia BeatStep, a Behringer Crave synth and their UMC1820 digital interface. A few recordings made: https://www.youtube.com/user/judahmu/videos  User generated material is stored in a separate project: https://github.com/jeffmasty/Setlist

## Technologies

JNAJack (https://github.com/jaudiolibs/jnajack) are crucial Java bindings to the Jack sound system without which this project wouldn't be possible. A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

FluidSynth (https://github.com/FluidSynth/fluidsynth) a sample-based general-midi synthesizer controlled over STDIN/STDOUT and Midi.  Also using the fluid-soundfont-gm.

Using TarsosDSP (https://github.com/JorenSix/TarsosDSP) for a guitar tuner.

Synth Engine created by combining (https://github.com/michelesr/jack-oscillator) with (https://github.com/johncch/MusicSynthesizer).

Wav File handling provided by Dr. Andrew Greensted (http://www.labbookpages.co.uk/audio/javaWavFiles.html)

Midi24 clock provided by (https://github.com/widdly/midiclock) and controlled with the Illposed OSC library (http://www.illposed.com/software/javaosc.html)

## Build
Built with Lombok (https://projectlombok.org/) and Maven. 

## Running
The Jack sound system needs to be up and running.  
This project uses the "a2jmidid -e" ALSA midi bindings along with Jack.  This project uses FluidSynth (https://www.fluidsynth.org/) (with the fluid soundfount). This project is hardcoded to connect to the author's instrument ports, fiddle around with MidiSetup and Channels to get your particular system up and running. 

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
