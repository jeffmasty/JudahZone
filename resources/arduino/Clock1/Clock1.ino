/*
 * 7-segment display with 74HC595 shift register
 * 4-Digit counter example.
 * Common anode 7-segment display is used.
 * This is a free software with NO WARRANTY.
 * https://simple-circuit.com/
 */

#define CLOCK16_PIN 11
#define RESET_PIN 12

// Rotary Encoder Inputs
#define ROTARY_CLK 8
#define ROTARY_DAT 9
#define ROTARY_SW 10

// shift register pin definitions
#define clockPin  7   // clock pin
#define dataPin   6   // data pin

// common pins of the four digits definitions
#define Dig1    5
#define Dig2    4
#define Dig3    3
#define Dig4    2

#define NADA 255
#define DOWNBEAT 109
#define CLOCKS_PER_BEAT 24 // MIDI Clock Ticks
#define STEPS_PER_BEAT 4
#define STEPS 16


// variable declarations
unsigned long last;

byte current_digit;

void disp(byte number, bool dec_point = 0);

// rotary encoder variables
byte bpm = 99;
bool playing = false;

//unsigned long initial = 0;

int digit;
int currentStateCLK;
int lastStateCLK;
unsigned long lastButtonPress = 0;
unsigned long interval;

volatile bool highlight = false;
volatile byte step = 0;
volatile byte countdown = 0;

void setup()
{

  pinMode(RESET_PIN, OUTPUT);
  pinMode(CLOCK16_PIN, OUTPUT);
  pinMode(Dig1, OUTPUT);
  pinMode(Dig2, OUTPUT);
  pinMode(Dig3, OUTPUT);
  pinMode(Dig4, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);

  // Set encoder pins as inputs
  pinMode(ROTARY_CLK,INPUT);
  pinMode(ROTARY_DAT,INPUT);
  pinMode(ROTARY_SW, INPUT_PULLUP);
  
  // Read the initial state of CLK
  lastStateCLK = digitalRead(ROTARY_CLK);
  interval = millisPerTick();

  disp_off();  // turn off the display

  // Timer1 module overflow interrupt configuration
  TCCR1A = 0;
  TCCR1B = 1;  // enable Timer1 with prescaler = 1 ( 16 ticks each 1 µs)
  TCNT1  = 0;  // set Timer1 preload value to 0 (reset)
  TIMSK1 = 1;  // enable Timer1 overflow interrupt
}

ISR(TIMER1_OVF_vect)   // Timer1 interrupt service routine (ISR)
{

  if (millis() - last >= interval) {
    if (playing) digitalWrite(CLOCK16_PIN, HIGH);
    
    last = millis();
    step++;
    if (step >= STEPS) step = 0;
    highlight = step % 4 == 0;
  }
  else {
    digitalWrite(CLOCK16_PIN, LOW);
  }
  // TODO
  // digitalWrite(CLOCK_OUT, highlight);

  disp_off();  // turn off the display
  switch (current_digit)
  {
    case 1:
      if (playing) {
        if (highlight) {
          shiftOut(dataPin, clockPin, MSBFIRST, DOWNBEAT);
        }
        else {
          shiftOut(dataPin, clockPin, MSBFIRST, NADA | true);
        }
      }
      else {
        shiftOut(dataPin, clockPin, MSBFIRST, NADA);
      }
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      digitalWrite(Dig1, LOW);  // turn on digit 1
      break;

    case 2:
      digit = (bpm / 100) % 10;
      if (digit != 0) {
        disp( digit );   // prepare to display digit 2
        digitalWrite(Dig2, LOW);     // turn on digit 2
      }
      break;

    case 3:
      disp( (bpm / 10) % 10 );   // prepare to display digit 3
      digitalWrite(Dig3, LOW);    // turn on digit 3
      break;

    case 4:
      disp(bpm % 10);   // prepare to display digit 4 (most right)
      digitalWrite(Dig4, LOW);  // turn on digit 4
  }

  current_digit = (current_digit % 4) + 1;
}

void pulse() {

}

extern void __attribute__((noreturn))
loop()
{
  encoderPot();
  encoderBtn();
  delay(1);
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
      if (bpm == 255) bpm = 26;
      else bpm++;
    } else {
      bpm--;
      if (bpm < 26) {
        bpm = 255;
      }
    }
    interval = millisPerTick();
  }
  // Remember last CLK state
  lastStateCLK = currentStateCLK;
}

void encoderBtn() {
  // Read the button state
  int btnState = digitalRead(ROTARY_SW);

  //If we detect LOW signal, button is pressed
  if (btnState == LOW) {
    //if 40ms have passed since last LOW pulse, it means that the
    //button has been pressed, released and pressed again
    if (millis() - lastButtonPress > 40) {
      playing = !playing;
      if (playing) { // start
            digitalWrite(RESET_PIN, HIGH);
            last = millis();
            step = 0;
            pulse();
            interval = millisPerTick();
            digitalWrite(RESET_PIN, LOW);
      }
      else {
        last == 0;
      }
    }

    // Remember last button press event
    lastButtonPress = millis();
  }
}


unsigned long millisPerTick() {
   return 60000 / (bpm * STEPS_PER_BEAT);
}

void disp(byte number, bool dec_point)
{
  switch (number)
  {
    
    case 0:  // print 0
      shiftOut(dataPin, clockPin, MSBFIRST, 0x02 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 1:  // print 1
      shiftOut(dataPin, clockPin, MSBFIRST, 0x9E | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 2:  // print 2
      shiftOut(dataPin, clockPin, MSBFIRST, 0x24 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 3:  // print 3
      shiftOut(dataPin, clockPin, MSBFIRST, 0x0C | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 4:  // print 4
      shiftOut(dataPin, clockPin, MSBFIRST, 0x98 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 5:  // print 5
      shiftOut(dataPin, clockPin, MSBFIRST, 0x48 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 6:  // print 6
      shiftOut(dataPin, clockPin, MSBFIRST, 0x40 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;
    
    case 7:  // print 7
      shiftOut(dataPin, clockPin, MSBFIRST, 0x1E | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 8:  // print 8
      shiftOut(dataPin, clockPin, MSBFIRST, !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
      break;

    case 9:  // print 9
      shiftOut(dataPin, clockPin, MSBFIRST, 0x08 | !dec_point);
      digitalWrite(clockPin, HIGH);
      digitalWrite(clockPin, LOW);
  }
}

void disp_off()
{
   digitalWrite(Dig1, HIGH);
   digitalWrite(Dig2, HIGH);
   digitalWrite(Dig3, HIGH);
   digitalWrite(Dig4, HIGH);
}
