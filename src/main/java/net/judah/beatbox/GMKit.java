package net.judah.beatbox;

import lombok.RequiredArgsConstructor;
/** Kits: (subtract 1)
1   Standard Drum Kit
9   Room Drum Kit
17  Power Drum Kit
25  Electric Drum Kit
26  Rap TR808 Drums
33  Jazz Drum Kit
41  Brush Kit */
@RequiredArgsConstructor
public enum GMKit {

    Standard1(0), Standard2(1),
    TR808(25),
    Electric(24),
    Room1(8), Room2(9), Room3(10),
    Jazz1(32), Jazz2(33),
    Brushes1(40), Brushes2(42),
    Rock1(16), Rock2(17),
    Orchestra(48)
    ;

    final int progChange;

}
