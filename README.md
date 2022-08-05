# JudahZone

A mixer/looper/live performance environment written in Java for the Jack sound system. (https://jackaudio.org/)

6 mono or stereo input channels are currently handled, as well as 4 synchronized stereo recording loops. All are mixed into a master stereo output bus.  All channels have separately controlled internal gain, EQ, Hi/Lo Cut, Chorus, Overdrive, Delay, Reverb, an LFO, and stereo panning. These internal effects are supplanted by LV2 plugins hosted externally and controlled over OSC. There's also an internal Song and Pattern/Step Sequencer, Guitar Tuner, Midi Router, 

Works great with an Akai MPK-Mini Midi Controller and/or a guitar/mic and midi foot switch controller.
A few recordings made: https://www.youtube.com/user/judahmu/videos

## Technologies

Technologies experimented with:

JNAJack (https://github.com/jaudiolibs/jnajack) crucial Java bindings to the Jack sound system without which this project wouldn't be possible. 
A big shout out to Neil C Smith, many of his Digital Sound Processing units have found their way into this project. (https://github.com/jaudiolibs/audioops)

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port to dynamically modify plugins using the Illposed OSC library. (http://www.illposed.com/software/javaosc.html)

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.  Also using the fluid-soundfont-gm.

Jackson (https://github.com/FasterXML/jackson) for JSON handling and serialization
Apache Jena (https://jena.apache.org/) to open LV2 .ttl plugin descriptors.

Using TarsosDSP (https://github.com/JorenSix/TarsosDSP) for a guitar tuner.

## Build
Built with Maven and Lombok. 

## Running
You need to have Jack up and running.  This project uses the "a2jmidid -e" ALSA midi bindings along with Jack.  This project uses FluidSynth (with the fluid soundfount) and the Carla plugin host.  

This project is hardcoded to connect to the author's instrument ports.  Fiddle around with the Channels class to get your particular system up and running. 

##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
