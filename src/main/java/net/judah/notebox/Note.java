package net.judah.notebox;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor @Getter
public class Note {
    final int step;
    @Setter float velocity = 1;
    @Setter float gate = 0.5f;

}
