# JudahZone

An open source GrooveBox (mixer/effects/looper/sequencer) written in Java for the Jack realtime sound system. (https://jackaudio.org/)

8 mono or stereo instrument channels are currently handled as well as 4 recording loops. All audio channels have separately controlled internal Gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Reverb, Delay, Compression, Stereo panning, Automation, a Spectrometer, a pair of LFOs and an IR CabSim. Loops may be synchronized for different song lengths and structures. A MIDI sequencer and feature-rich editor is provided consisting of 4 drum tracks and unlimited piano rolls. The sequencer generates chords, arpeggios and bass lines from ChordPro files. The sample player holds 8 loops/one-shots and 32 drum samples (8 samples per drum track). Melodic tracks may be played through external gear or through TacoSynth, a built-in subtractive synthesizer with the regular gamut of wave shapes, envelopes, resonant filters, detuning/harmonics and presets. Also included: a song editor, sheet music and chord views, guitar tuner, RMS/spectrogram, lossless audio recording and MIDI routing/clock support.  The following controllers are integrated in the live system: Akai MPKmini, Korg NanoPad2 and Kontrol2, Line6 FBV Shortboard, Jamstik MIDI guitar, an old Arturia BeatStep, a Behringer Crave synth and their UMC1820 digital interface. Performance material is stored in a separate project: https://github.com/jeffmasty/Setlist  Some live looping: https://www.youtube.com/user/judahmu/videos  

## Build
Built with Java 25, Lombok (https://projectlombok.org/) and Maven.

This project depends on [a2j](https://github.com/jackaudio/a2jmidid)

This project depends on [FluidSynth](https://www.fluidsynth.org/) (with the FluidR3_GM soundfont). 

This project depends on the author's [Setlist](https://github.com/jeffmasty/Setlist) maven project.


## Running
The Jack sound system needs to be up and running. 
This project boots "a2jmidid -e" ALSA midi bindings and fluidsynth at startup.  
This project is hardcoded to connect to the author's instrument ports, fiddle around with JudahMidi and JudahZone.initialize() to get your particular system up and running. 

## Credits 
JNAJack (https://github.com/jaudiolibs/jnajack) provides crucial Java bindings to the Jack sound system without which this project wouldn't be possible. 

Delay, MonoFilter, FreeVerb, Chorus and the 'Smith' OverDrive gratefully adapted from 
	Neil C Smith's [JAudioLibs](https://github.com/jaudiolibs/audioops/). 
Additional Overdrive algorithms ported from [JUCEGuitarAmpBasic](https://github.com/martinpenberthy/JUCEGuitarAmpBasic).
	
Compressor ported from [Rakarrack](https://github.com/ssj71/rkrlv2).

Filters/EQ ported from [JackIIR](https://github.com/adiblol/jackiir).

Using [TarsosDSP](https://github.com/JorenSix/TarsosDSP) for FFT, an audio file I/O and guitar tuner. 

TacoSynth created by combining [Jack-Oscillator](https://github.com/michelesr/jack-oscillator) 
with [MusicSynthesizer](https://github.com/johncch/MusicSynthesizer).

[SongPro.org](https://github.com/SongProOrg/songpro-java) adapted to process ChordPro files.

Racman sequence provided by [ybalcanci](https://github.com/ybalcanci/Sequence-Player)

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
![JudahZone logo2](/resources/JudahZone3.png)
 
