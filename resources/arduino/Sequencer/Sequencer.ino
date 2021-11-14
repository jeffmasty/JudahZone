/*************************************************** 
  This is an example for the Adafruit VS1053 Codec Breakout

  Designed specifically to work with the Adafruit VS1053 Codec Breakout 
  ----> https://www.adafruit.com/products/1381

  Adafruit invests time and resources providing this open source code, 
  please support Adafruit and open-source hardware by purchasing 
  products from Adafruit!

  Written by Limor Fried/Ladyada for Adafruit Industries.  
  BSD license, all text above must be included in any redistribution
 ****************************************************/
 
#include <SoftwareSerial.h>
  
// define the pins used
#define TO_VS1053  4 // This is the pin that connects to the RX pin on VS1053 shield
#define TRIGGER_PIN 3 // interrupt attached
#define RESET_PIN 2 // resets step count to 0
// Rotary Encoder Inputs
#define ROTARY_CLK 8
#define ROTARY_DAT 9
#define ROTARY_SW 10

// See http://www.vlsi.fi/fileadmin/datasheets/vs1053.pdf Pg 31
#define VS1053_BANK_DEFAULT 0x00
#define VS1053_BANK_DRUMS1 0x78
#define VS1053_BANK_DRUMS2 0x7F
#define VS1053_BANK_MELODY 0x79

#define MIDI_NOTE_ON  0x90
#define MIDI_NOTE_OFF 0x80
#define MIDI_CHAN_MSG 0xB0
#define MIDI_CHAN_BANK 0x00
#define MIDI_CHAN_VOLUME 0x07
#define MIDI_CHAN_PROGRAM 0xC0
#define GM_HARP 47

#define WAIT 266
#define STEPS 16
#define PATTERNS 6
#define INSTRUMENTS 4

#define BASS 36
#define SNARE 38
#define HIHAT 42
#define CLAVES 75

SoftwareSerial VS1053_MIDI(0, TO_VS1053); // TX only, do not use the 'rx' side

int pattern;
int velocity = 100;
int btnState;
int currentStateCLK;
int lastStateCLK;
unsigned long lastButtonPress = 0;

volatile int step;

void setup() {
  reset();
  VS1053_MIDI.begin(31250); // MIDI uses a 'strange baud rate'
    
  midiSetChannelBank(0, VS1053_BANK_MELODY);
  midiSetInstrument(0, GM_HARP);

  pinMode(ROTARY_CLK,INPUT);
  pinMode(ROTARY_DAT,INPUT);
  pinMode(ROTARY_SW, INPUT_PULLUP);
  
  midiSetChannelBank(9, VS1053_BANK_DRUMS2);
  pinMode(RESET_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(RESET_PIN), reset, RISING);
  pinMode(TRIGGER_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(TRIGGER_PIN), pulse, RISING);
}


bool bass[PATTERNS][16] = {
  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
  {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
  {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0},
  {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
  {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0},
  {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0} };

bool snare[PATTERNS][16] = {
  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
  {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
  {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1},
  {0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0},
  {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
  {1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0} };

bool hihat[PATTERNS][16] = {
  {0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
  {0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0},
  {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
  {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
  {0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1},
  {1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1} };

bool claves[PATTERNS][16] = {
  {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
  {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
  {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
  {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0} };

  
void reset() {
  step = -1;
}

void pulse() {
  step++;
  if (step == STEPS) step = 0;

  if (bass[pattern][step])
    drum(BASS);
  if (snare[pattern][step])
    drum(SNARE);
  if (hihat[pattern][step])
    drum(HIHAT);
  if (claves[pattern][step])
    drum(CLAVES);
  
}


void drum(uint8_t note) {
  if (note > 127) return;
  
  VS1053_MIDI.write(MIDI_NOTE_ON | 9);
  VS1053_MIDI.write(note);
  VS1053_MIDI.write(velocity);
}

void loop() {  
  encoderBtn();
  encoderPot();
  delay(1);
}

void encoderBtn() {
  // Read the button state
  btnState = digitalRead(ROTARY_SW);

  //If we detect LOW signal, button is pressed
  if (btnState == LOW) {
    //if 40ms have passed since last LOW pulse, it means that the
    //button has been pressed, released and pressed again
    if (millis() - lastButtonPress > 40) {
      pattern++;
      if (pattern == PATTERNS)
        pattern = 0;
    }

    // Remember last button press event
    lastButtonPress = millis();
  }
}


void encoderPot() {
    // Read the current state of CLK
  currentStateCLK = digitalRead(ROTARY_CLK);

  // If last and current state of CLK are different, then pulse occurred
  // React to only 1 state change to avoid double count
  if (currentStateCLK != lastStateCLK  && currentStateCLK == 1){

    // If the DT state is different than the CLK state then
    // the encoder is rotating CCW so decrement
    if (digitalRead(ROTARY_DAT) != currentStateCLK) {
      // Encoder is rotating CW so increment
      if (velocity != 127) velocity++;
    } else {
      if (velocity != 0)
      velocity--;
    }
  }
  // Remember last CLK state
  lastStateCLK = currentStateCLK;
}

void midiSetInstrument(uint8_t chan, uint8_t inst) {
  if (chan > 15) return;
  inst --; // page 32 has instruments starting with 1 not 0 :(
  if (inst > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_PROGRAM | chan);  
  VS1053_MIDI.write(inst);
}


void midiSetChannelVolume(uint8_t chan, uint8_t vol) {
  if (chan > 15) return;
  if (vol > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_MSG | chan);
  VS1053_MIDI.write(MIDI_CHAN_VOLUME);
  VS1053_MIDI.write(vol);
}

void midiSetChannelBank(uint8_t chan, uint8_t bank) {
  if (chan > 15) return;
  if (bank > 127) return;
  
  VS1053_MIDI.write(MIDI_CHAN_MSG | chan);
  VS1053_MIDI.write((uint8_t)MIDI_CHAN_BANK);
  VS1053_MIDI.write(bank);
}

void midiNoteOn(uint8_t chan, uint8_t n, uint8_t vel) {
  if (chan > 15) return;
  if (n > 127) return;
  if (vel > 127) return;
  
  VS1053_MIDI.write(MIDI_NOTE_ON | chan);
  VS1053_MIDI.write(n);
  VS1053_MIDI.write(vel);
}

void midiNoteOff(uint8_t chan, uint8_t n, uint8_t vel) {
  if (chan > 15) return;
  if (n > 127) return;
  if (vel > 127) return;
  
  VS1053_MIDI.write(MIDI_NOTE_OFF | chan);
  VS1053_MIDI.write(n);
  VS1053_MIDI.write(vel);
}
