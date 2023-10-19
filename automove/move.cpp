/*
  Droid which recognize barcodes and move accordingly.
  Copyright 2022 Gabriel Dimitriu

  This file is part of pixy2Droid

  pixy2Droid is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
  pixy2Droid is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
  You should have received a copy of the GNU General Public License
  along with pixy2Droid; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
*/

#include "move.h"
#include "engines.h"
#include "communications.h"

#define MAX_CAMERA_BRIGHTNESS 255

bool autoCalibrationDone = false;
bool isStopped = true;

static int8_t droidDirection = 0;

static unsigned int navigationType = 0;

Pixy2I2C pixy;

static unsigned int leftCode = 0;  // code==0 is our left-turn sign
static unsigned int rightCode = 5; // code==5 is our right-turn sign
static unsigned int stopCode = 6; //code==6 is our stop sign for 10000 ms
static unsigned int forwardCode = 4; //code==4 is our forward sign
static unsigned int backwardCode = 3; //code==3 is our backward sign

#define X_CENTER         (pixy.frameWidth/2)

float Kp = -0.25;
float Kd = 100.0;
float Ki = 0.0;

static volatile int32_t lastError = 0;
static volatile int32_t integration = 0;

void reset() {
  lastError = 0;
  integration = 0;
}

void setLeftCode(unsigned int value) {
  leftCode = value;
}

void setRightCode(unsigned int value) {
  rightCode = value;
}

void setStopCode(unsigned int value) {
  stopCode = value;
}

void setForwardCode(unsigned int value) {
  forwardCode = value;
}

void setBackwardCode(unsigned int value) {
  backwardCode = value;
}

unsigned int getLeftCode() {
  return leftCode;
}

unsigned int getRightCode() {
  return rightCode;
}

unsigned int getStopCode() {
  return stopCode;
}

unsigned int getForwardCode() {
  return forwardCode;
}

unsigned int getBackwardCode() {
  return backwardCode;
}

void initMove() {
  Wire.begin();
  pixy.init();
  pixy.setServos(380,010);
  // change to the line_tracking program.  Note, changeProg can use partial strings, so for example,
  // you can change to the line_tracking program by calling changeProg("line") instead of the whole
  // string changeProg("line_tracking")
  pixy.changeProg("line");
  autoCalibrationDone = false;
  // Turn off both laps upper and lower
  pixy.setLamp(0, 0);
}

void initMove(unsigned int type) {
  navigationType = type;
  switch(type) {
    case 0:
      pixy.setServos(380,010);
      pixy.changeProg("line");
      break;
    case 1:
      pixy.setServos(380,010);
      pixy.changeProg("line");
      break;
  }
}

/*
 * Autocalibration of the brightness and led based on the environment light to detect barcodes.
 */
void autocalibrationCamera() {
  int8_t res;
  int8_t newBrightness = 0;
  bool lowerLed = false;
  bool upperLed = false;
  bool lastTry = false;
  
#ifdef BLE_DEBUG_MODE
  BTSerial.println("Autocalibration start ...");
#endif  
  while(1) {
    res = pixy.line.getMainFeatures();
    //success get the barcode for forward move
    if (pixy.line.barcodes->m_code==4) {
      autoCalibrationDone = true;
      BTSerial.println("Autocalibration finish with success ...");
      isStopped = true;
      return;
    } else if (newBrightness < MAX_CAMERA_BRIGHTNESS) {
      newBrightness +=5;
    } 
    if (newBrightness >= MAX_CAMERA_BRIGHTNESS) {
      newBrightness = MAX_CAMERA_BRIGHTNESS;
      if (!lowerLed) {
        pixy.setLamp(0,1);
        lowerLed = true;
      } else if (lowerLed && !upperLed) {
        pixy.setLamp(1,1);
        upperLed = true;        
      } else if (upperLed && !lastTry) {
        newBrightness = 0;
        lastTry = true;
      } else {
        autoCalibrationDone = true;
#ifdef BLE_DEBUG_MODE        
        BTSerial.println("Autocalibration finished with no result keep full power led and maximum brightness ...");
#endif
        isStopped = true;
        return;
      }
    }
    pixy.setCameraBrightness(newBrightness);
    delay(10);
  }
}

static void moveBareCode() {
  int8_t res;
  // Get latest data from Pixy, including main vector, new intersections and new barcodes.
  res = pixy.line.getMainFeatures();

  if (res&LINE_BARCODE)
  {
    //pixy.line.barcodes->print();
    if ( pixy.line.barcodes->m_code == leftCode ) {
#ifdef BLE_DEBUG_MODE      
      BTSerial.println("left");
#endif      
      go(-currentPower,currentPower);
      delay(turn90);
      go(0,0);
      if (droidDirection == 1) {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println("resuming forward");
#endif        
        go(currentPower,currentPower);
      } else if (droidDirection == 2) {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println("resuming backward");
#endif        
        go(-currentPower,-currentPower);
      } else {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println(droidDirection);
#endif        
      }
    }    
    else if ( pixy.line.barcodes->m_code == rightCode ) {
#ifdef BLE_DEBUG_MODE      
      BTSerial.println("right");
#endif      
      go(currentPower,-currentPower);
      delay(turn90);
      go(0,0);
      if (droidDirection == 1) {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println("resuming forward");
#endif        
        go(currentPower,currentPower);
      } else if (droidDirection == 2) {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println("resuming backward");
#endif        
        go(-currentPower,-currentPower);
      } else {
#ifdef BLE_DEBUG_MODE        
        BTSerial.println(droidDirection);
#endif        
      }
    }
    
    else if ( pixy.line.barcodes->m_code == stopCode ) {
#ifdef BLE_DEBUG_MODE      
      BTSerial.println("stop");
#endif      
      go(0,0);
      delay(10000);
    }    
    else if ( pixy.line.barcodes->m_code == forwardCode ) {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("forward");
#endif      
      if (droidDirection != 1) {
        go(0,0);
      }
      droidDirection = 1;//forward
      go(currentPower,currentPower);
    }    
    else if ( pixy.line.barcodes->m_code == backwardCode ) {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("backward");
#endif      
      if (droidDirection != 2) {
        go(0,0);
      }
      droidDirection = 2;//backward
      go(-currentPower,-currentPower);
    }
  }
}

static void moveFollowLine() {
  int8_t res;
  int32_t error; 
  int left, right;
  char buf[96];
  double command;
  int32_t P,D;

  // Get latest data from Pixy, including main vector, new intersections and new barcodes.
  res = pixy.line.getMainFeatures();
  
  // If error or nothing detected, stop motors
  if (res<=0) {
    go(0, 0);
    return;
  }

  // We found the vector...
  if ( res & LINE_VECTOR ) {
    // Calculate heading error with respect to m_x1, which is the far-end of the vector,
    // the part of the vector we're heading toward.
    error = (int32_t)pixy.line.vectors->m_x1 - (int32_t)X_CENTER;

    integration += error;
    P = error;
    if (integration > 255)
      integration = 255;
    if (integration < -255)
      integration = -255;
    D = error - lastError;
    if ( D > 255 )
      D = 255;
    if ( D < -255 )
      D = -255;
    lastError = error;
    command = Kp*P+Ki*integration+Kd*D;
    if ( error < 0 ) {
      left = command;
      right = -command;
    } else {
      left = -command;
      right = command;
    }

    // If vector is heading away from us (arrow pointing up), things are normal.
    if ( pixy.line.vectors->m_y0 > pixy.line.vectors->m_y1 ) {
      if ( pixy.line.vectors->m_flags & LINE_FLAG_INTERSECTION_PRESENT ) {
        left += MAX_POWER_ENGINE/4;
        right += MAX_POWER_ENGINE/4;
      } else {
        // otherwise, pedal to the metal!
        left += MAX_POWER_ENGINE/2;
        right += MAX_POWER_ENGINE/2;
      }    
    } else {
      // If the vector is pointing down, or down-ish, we need to go backwards to follow.
      left -= MAX_POWER_ENGINE/2;
      right -= MAX_POWER_ENGINE/2;  
    }
    if ( left > 0 ) {
      if ( left < MIN_POWER_ENGINE )
        left = MIN_POWER_ENGINE;
      else if ( left > MAX_POWER_ENGINE )
        left = MAX_POWER_ENGINE;
    } else {
      if ( left > -MIN_POWER_ENGINE )
        left = -MIN_POWER_ENGINE;
      else if ( left < -MAX_POWER_ENGINE )
        left = -MAX_POWER_ENGINE;
    }
    if ( right > 0 ) {
      if ( right < MIN_POWER_ENGINE )
        right = MIN_POWER_ENGINE;
      else if ( right > MAX_POWER_ENGINE )
        right = MAX_POWER_ENGINE;     
    } else {
      if ( right > -MIN_POWER_ENGINE )
        right = -MIN_POWER_ENGINE;
      else if ( right < -MAX_POWER_ENGINE )
        right = -MAX_POWER_ENGINE;
    }
    go(left, right);
  }
}

void move() {
  switch(navigationType) {
    case 0:
      moveBareCode();
      break;
    case 1:
      moveFollowLine();
      break;
  }
}
