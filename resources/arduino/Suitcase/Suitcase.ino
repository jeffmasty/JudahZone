#include <SoftwareSerial.h>

/* Listens to a foot switch and sends midi cc when pressed.
   Echos any incoming midi messages. */

// for ATtiny85: under preferences -> board mangers...
// https://raw.githubusercontent.com/damellis/attiny/ide-1.6.x-boards-manager/package_damellis_attiny_index.json 

#define IN 0
#define OUT 1
#define LED 2
#define LEFT 3
#define RIGHT 4

SoftwareSerial midiPort (IN, OUT);  // Rx, Tx pin

bool clear;
int countdown = 0;
int wait = 500;
  
byte velocity;
byte cmd;
byte note;
  
void setup() {
  
  // Set MIDI baud rate:
  midiPort.begin(31250);

  // set foot switch input with resistor
  pinMode(LEFT, INPUT_PULLUP);    
  pinMode(RIGHT, INPUT_PULLUP);   

  // play some octave midi notes (48, 60, 72) and blink the LED
  velocity = 0x45;
  byte off = 0x00;
  int lowC = 48;
  int middleC = 60;
  int highC = 72;
    
  digitalWrite(LED, HIGH);
  playnote(lowC, velocity);
  delay(wait);
  
  digitalWrite(LED, LOW);
  playnote(lowC, off);
  playnote(middleC, velocity);
  delay(wait);
  
  digitalWrite(LED, HIGH);
  playnote(middleC, off);
  playnote(highC, velocity);
  delay(wait);
  
  playnote(highC, off);
  digitalWrite(LED, LOW);
  delay(wait);
  digitalWrite(LED, LOW);
}

void loop() {
    if (countdown != 0) {
      if (countdown == 1) {
        digitalWrite(LED, LOW);
      }
      countdown--;
    }

    int left = digitalRead(LEFT);
    int right = digitalRead(RIGHT);

    if (left || right) {
      if (clear) {
        if (right) {
          cc(1, 127);
        }
        else if (left) {
          cc(2, 127);
        }
        clear = false;
      }
    }
    else {
      clear = true;    
    }

// echo any incoming Midi when at least three bytes are available
//    do {
//      cmd = midiPort.read();
//      note = midiPort.read();
//      velocity = midiPort.read();
//      midimsg(cmd, note, velocity);
//    } while (midiPort.available() > 2); 

  // delay(1); // for stability?
}

void midimsg(int cmd, int dat1, int dat2) {
    digitalWrite(LED, HIGH);
    midiPort.write(cmd);
    midiPort.write(dat1);
    midiPort.write(dat2);
    countdown = 1100;
}

void cc(int dat1, int dat2) {
  midimsg(0xB0, dat1, dat2);
}

// no LED
void playnote(int note, int velocity) {
    midiPort.write(0x90);
    midiPort.write(note);
    midiPort.write(velocity);
}



//// play notes from F#-0 (0x1E) to F#-5 (0x5A):
//  for (int note = 0x1E; note < 0x5A; note ++) {
//    //Note on channel 1 (0x90), some note value (note), middle velocity (0x45):
//    noteOn(0x90, note, 0x45);
//    delay(100);
//    //Note on channel 1 (0x90), some note value (note), silent velocity (0x00):
//    noteOn(0x90, note, 0x00);
//    delay(100);
//  }
//  delay(200);
//}
