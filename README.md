# JudahZone

A rough proof-of-concept using Java to connect to the Jack sound system and provide a live looping environment. Check back in a few months, there may be a more useful, general-purpose product available.

## Technologies

Technologies experimented with and will probably find their way into a finished product:

JNAJack (https://github.com/jaudiolibs/jnajack) Java bindings to the Jack sound system.  The checkout on my github fixes a bug with xruns callback. Get it on your classpath.

JVstHost (https://github.com/mhroth/jvsthost) After removing some deprecated and re-compiling the shared library, was able to open and exercise VST plugins.  However, I probably will not pursue VST, opting for LV2 plugins hosted through Carla and Mod-Host instead.  

Carla (https://github.com/falkTX/Carla) is an impressive LV2 plugin host.  Successfully connected to its OSC port and dynamically modified plugins.

mod-host (https://github.com/moddevices/mod-host) another LV2 plugin host. Successfully controlled modhost by taking over STDIN/STDOUT.  

FluidSynth (https://github.com/FluidSynth/fluidsynth) a synthesizer controlled over STDIN/STDOUT and Midi.
Also using fluid-soundfont-gm.

Apache Jena (https://jena.apache.org/) to open LV2 plugin descriptors.

Illposed OSC (http://www.illposed.com/software/javaosc.html) successfully connected to an OSC server.


##   
 
