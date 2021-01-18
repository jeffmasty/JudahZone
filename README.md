# JudahZone

A mixer/looper/live performance environment written in Java for the Jack sound system. (https://jackaudio.org/)

Currently, 6 audio input channels are handled, constituting a mix between stereo and mono, as well as 3 dedicated and synchronized stereo recording loops. All are mixed into a master output channel.  All channels have separately controlled internal volume, EQ, Hi/Lo Cut, Overdrive, Compression, Delay, Reverb, an LFO, and stereo panning. There's an internal Song and Pattern Sequencer, Metronome, Midi Router, Midi Looper, Midi Transposer, Wav file Sample Player, Midi file player, and Midi integration with a BeatBuddy drum machine (Midi Clock). External LV2 plugins being hosted by Carla are controlled over OSC. 

Works great with an Akai MPK-Mini Midi Controller and/or a guitar/mic and midi foot switch controller.
A few recordings made: https://www.youtube.com/user/judahmu/videos

## Technologies

Technologies experimented with:

JNAJack (https://github.com/jaudiolibs/jnajack) crucial Java bindings to the Jack sound system without which this project wouldn't be possible. 
A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port to dynamically modify plugins.

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.
Also using the fluid-soundfont-gm.

Illposed OSC (http://www.illposed.com/software/javaosc.html) successfully communicated with an OSC server.

Jackson (https://github.com/FasterXML/jackson) for JSON handling and serialization
Apache Jena (https://jena.apache.org/) to open LV2 .ttl plugin descriptors.



##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
