# JudahZone

An open source GrooveBox (mixer/effects/looper/sequencer) written in Java for the Jack realtime sound system. (https://jackaudio.org/)

8 mono or stereo instrument channels are currently handled as well as 4 recording loops. All audio channels have separately controlled internal Gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Reverb, Delay, Compression, Stereo panning and a pair of LFOs. Loops may be synchronized for different song lengths and structures. A 10-track MIDI sequencer and feature-rich editor is provided consisting of 4 drum tracks and 6 piano rolls. Melodic tracks may be played through external gear or through TacoSynth, a built-in subtractive synthesizer (24 voices, 3 oscillators with the regular gamut of wave shapes, envelopes, resonant filters, detuning/harmonics and presets). The sequencer generates chords, arpeggios and bass lines from ChordPro files. The sample player holds 8 loops or one-shots and 32 drum samples (8 samples per drum track). But wait, also included: a song editor, sheet music and chord views, guitar tuner, lossless audio recording, waveform analysis and MIDI routing/clock support.  The following controllers are integrated in the live system: Akai MPKmini, Korg NanoPad2 and Kontrol2, Line6 FBV Shortboard, Jamstik MIDI guitar, an old Arturia BeatStep, a Behringer Crave synth and their UMC1820 digital interface. Performance material is stored in a separate project: https://github.com/jeffmasty/Setlist  Some live looping: https://www.youtube.com/user/judahmu/videos  

## Credits 

JNAJack (https://github.com/jaudiolibs/jnajack) are crucial Java bindings to the Jack sound system without which this project wouldn't be possible. A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

TacoSynth created by combining (https://github.com/michelesr/jack-oscillator) with (https://github.com/johncch/MusicSynthesizer).

Wav File handling provided by [Dr. Andrew Greensted](http://www.labbookpages.co.uk/audio/javaWavFiles.html).

Using [TarsosDSP](https://github.com/JorenSix/TarsosDSP) for a guitar tuner. 

[SongPro.org\](https://github.com/SongProOrg/songpro-java) adapted to process ChordPro files.

Racman sequence provided by (https://github.com/ybalcanci/Sequence-Player)

## Build
Built with Java 21, Lombok (https://projectlombok.org/) and Maven.

This project depends on [a2j](https://github.com/jackaudio/a2jmidid)

This project depends on [FluidSynth](https://www.fluidsynth.org/) (with the FluidR3_GM soundfont). 

This project depends on the author's [Setlist](https://github.com/jeffmasty/Setlist) maven project.


## Running
The Jack sound system needs to be up and running. 
This project boots "a2jmidid -e" ALSA midi bindings and fluidsynth at startup.  
This project is hardcoded to connect to the author's instrument ports, fiddle around with JudahMidi and JudahZone.initialize() to get your particular system up and running. 

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
![JudahZone logo2](/resources/JudahZone3.png)
 
