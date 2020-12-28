# JudahZone

A mixer/looper/live performance environment written in Java for the Jack sound system.

## Technologies

Technologies experimented with and will probably find their way into a finished product:

JNAJack (https://github.com/jaudiolibs/jnajack) Java bindings to the Jack sound system.  The checkout on my github fixes a bug with xruns callback. Get it on your classpath.

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port and dynamically modified plugins.

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.
Also using fluid-soundfont-gm.

Jackson (https://github.com/FasterXML/jackson) for JSON handling and
Apache Jena (https://jena.apache.org/) to open LV2 .ttl plugin descriptors.

Illposed OSC (http://www.illposed.com/software/javaosc.html) successfully communicated with an OSC server.


##   

A GUI is starting to come together:
![JudahZone logo](/resources/JudahZone.png)
 
