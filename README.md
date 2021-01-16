# JudahZone

A mixer/looper/live performance environment written in Java for the Jack sound system. (https://jackaudio.org/)

A few recordings made: https://www.youtube.com/user/judahmu/videos

## Technologies

Technologies experimented with and will probably find their way into a finished product:

JNAJack (https://github.com/jaudiolibs/jnajack) crucial Java bindings to the Jack sound system. 

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port to dynamically modify plugins.

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.
Also using the fluid-soundfont-gm.

Illposed OSC (http://www.illposed.com/software/javaosc.html) successfully communicated with an OSC server.

Apache Jena (https://jena.apache.org/) to open LV2 .ttl plugin descriptors.

Jackson (https://github.com/FasterXML/jackson) for JSON handling and serialization




##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)

![JudahZone logo2](/resources/JudahZone2.png)
 
