# JudahZone

An open source GrooveBox (mixer/effects/looper/sequencer) written in Java for the Jack realtime sound system. (https://jackaudio.org/)

8 mono or stereo instrument channels are currently handled as well as 4 recording loops. All audio channels have separately controlled internal Gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Reverb, Delay, Compression, Stereo panning, Automation, a Spectrometer, an IR CabSim and a pair of LFOs. Loops may be synchronized for different song lengths and structures. A MIDI sequencer and feature-rich editor is provided consisting of 4 drum tracks and unlimited piano rolls. The sequencer generates chords, arpeggios and bass lines from ChordPro files. The sample player holds 8 loops/one-shots and 32 drum samples (8 samples per drum track). Melodic tracks may be played through external gear or through TacoSynth, a built-in subtractive synthesizer with the regular gamut of wave shapes, envelopes, resonant filters, detuning/harmonics and presets. Also included: a song editor, sheet music and chord views, guitar tuner, RMS/spectrogram, lossless audio recording and MIDI routing/clock support.  The following controllers are integrated in the live system: Akai MPKmini, Korg NanoPad2 and Kontrol2, Line6 FBV Shortboard, Jamstik MIDI guitar, an old Arturia BeatStep, a Behringer Crave synth and their UMC1820 digital interface. Performance material is stored in a separate project: https://github.com/jeffmasty/Setlist  Some live looping: https://www.youtube.com/user/judahmu/videos  

###Prerequisites

- Java 21 or newer
- Maven 3.8+
- Jack Audio 
- a2j
- FluidSynth
- songs/samples/presets https://github.com/jeffmasty/Setlist

## Build

Recommended (build from the aggregator `meta-zone` root):

1. Clone the aggregator and enter it:

	git clone https://github.com/jeffmasty/meta-zone.git  
	cd meta-zone
	
2. Build the `JudahZone` module (produces the shaded jar and copies the final `JudahZone.jar` to the `JudahZone` project root):	

	mvn -pl :JudahZone -am -DskipTests clean package
	
   Alternative: build only inside the `JudahZone` module (works if `../meta-zone/pom.xml` is present):	
   
	cd JudahZone  
	mvn -DskipTests clean package

Artifacts
- Slim jar in module target: `JudahZone/target/` (e.g. `JudahZone-<version>.jar`)
- Shaded jar in module target: `JudahZone/target/` (e.g. `JudahZone-<version>-shaded.jar`)
- Copied full jar at module root: `JudahZone/JudahZone.jar` (ready to run)

## Run

From the `JudahZone` module directory (or point to the copied `JudahZone.jar`):

	cd JudahZone 
	java -jar JudahZone.jar
	
### Notes
- JACK server must be up and running. 
- The app launches `a2jmidid` and Fluidsynth where configured; ensure those native deps are available on your system.
- This project connects to the author's instrument ports, see JudahMidi and JudahZone.initialize() to get your particular system up and running. 
- App ties with Setlist project (https://github.com/jeffmasty/Setlist)
- Further modularization coming soon...

## Credits 
JNAJack (https://github.com/jaudiolibs/jnajack) provides crucial Java bindings to the Jack sound system without which this project wouldn't be possible. 

Delay, MonoFilter, FreeVerb, Chorus and the 'Smith' OverDrive gratefully adapted from 
	Neil C Smith's [JAudioLibs](https://github.com/jaudiolibs/audioops/). 
Additional Overdrive algorithms ported from [JUCEGuitarAmpBasic](https://github.com/martinpenberthy/JUCEGuitarAmpBasic).
	
Compressor ported from [Rakarrack](https://github.com/ssj71/rkrlv2).

Filters/EQ ported from [JackIIR](https://github.com/adiblol/jackiir).

Using [TarsosDSP](https://github.com/JorenSix/TarsosDSP) for FFT, audio file I/O and guitar tuner. 

TacoSynth created by combining [Jack-Oscillator](https://github.com/michelesr/jack-oscillator) 
with [MusicSynthesizer](https://github.com/johncch/MusicSynthesizer).

[SongPro.org](https://github.com/SongProOrg/songpro-java) adapted to process ChordPro files.

Racman sequence provided by [ybalcanci](https://github.com/ybalcanci/Sequence-Player)

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
![JudahZone logo2](/resources/JudahZone3.png)
 
