# JudahZone

An open source GrooveBox (mixer/effects/looper/sequencer) written in Java for the Jack realtime sound system. (https://jackaudio.org/)

8 mono or stereo instrument channels are currently handled as well as 4 stereo recording loops. All audio channels have separately controlled internal Gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Reverb, Delay, Compression, Stereo panning and an LFO. Loops may be synchronized for different song lengths and structures. A 10-track MIDI sequencer and feature-rich editor is provided consisting of 4 drum tracks and 6 piano rolls. Synth tracks may be played through external gear or through built-in subtractive synth engines (24 voices, 3 oscillators and the regular gamut of wave shapes, envelopes, resonant filters, detuning/harmonics and presets). The sequencer generates chords, arpeggios and bass lines from ChordPro files. The sample player holds 4 loops, 4 one-shots and 32 drum samples (8 samples per drum track). A guitar tuner, loss-less audio recording (.wav), sheet music and chords viewer, song editor and MIDI clock/routing support is also provided.  The following controllers are integrated in the live system: Akai MPKmini, Korg NanoPad2 and Kontrol2, Line6 FBV Shortboard, Jamstik MIDI guitar, an old Arturia BeatStep, a Behringer Crave synth and their UMC1820 digital interface. Performance  material is stored in a separate project: https://github.com/jeffmasty/Setlist  Some live looping: https://www.youtube.com/user/judahmu/videos  

## Credits 

JNAJack (https://github.com/jaudiolibs/jnajack) are crucial Java bindings to the Jack sound system without which this project wouldn't be possible. A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

Synth Engine created by combining (https://github.com/michelesr/jack-oscillator) with (https://github.com/johncch/MusicSynthesizer).

Wav File handling provided by Dr. Andrew Greensted. (http://www.labbookpages.co.uk/audio/javaWavFiles.html)

Using TarsosDSP for a guitar tuner. (https://github.com/JorenSix/TarsosDSP)

SongPro.org adapted to process ChordPro files (https://github.com/SongProOrg/songpro-java)

Midi24 clock provided by (https://github.com/widdly/midiclock) and controlled with the Illposed OSC library (http://www.illposed.com/software/javaosc.html)

FluidSynth (https://github.com/FluidSynth/fluidsynth) is a sample-based general-midi synthesizer controlled over STDIN/STDOUT and Midi.  Also using the fluid-soundfont-gm.

Signal Processing/Visualization provided by Christian d'Heureuse/Inventec Informatik AG (https://www.source-code.biz/dsp/java/)

Racman sequence provided by (https://github.com/ybalcanci/Sequence-Player)

## Build
Built with Java 11+, Lombok (https://projectlombok.org/) and Maven. 

## Running
The Jack sound system needs to be up and running.  
This project boots "a2jmidid -e" ALSA midi bindings at startup.  
A Midi24 clock source must be connected to the project's "Tempo" Jack MIDI port.
This project uses FluidSynth (https://www.fluidsynth.org/) (with the fluid soundfount). This project is hardcoded to connect to the author's instrument ports, fiddle around with JudahMidi and JudahZone.initialize() to get your particular system up and running. 

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
![JudahZone logo2](/resources/JudahZone3.png)
 