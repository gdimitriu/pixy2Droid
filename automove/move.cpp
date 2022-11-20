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

bool lightsOn = false;

static int8_t droidDirection = 0;

unsigned int navigationType = 0;

Pixy2I2C pixy;

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

void initMove(int type) {
  navigationType = type;
  switch(type) {
    case 0:
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
  /*
   * 0 is left
   * 4 is forward
   * 3 is bacward
   * 5 is right
   * 6 is delay
   */
  // Get latest data from Pixy, including main vector, new intersections and new barcodes.
  res = pixy.line.getMainFeatures();

  if (res&LINE_BARCODE)
  {
    //pixy.line.barcodes->print();
    // code==0 is our left-turn sign
    if (pixy.line.barcodes->m_code==0) {
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
    // code==5 is our right-turn sign
    else if (pixy.line.barcodes->m_code==5) {
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
    //code==6 is our stop sign for 10000 ms
    else if (pixy.line.barcodes->m_code==6) {
#ifdef BLE_DEBUG_MODE      
      BTSerial.println("stop");
#endif      
      go(0,0);
      delay(10000);
    }
    //code==4 is our forward sign
    else if (pixy.line.barcodes->m_code==4) {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("forward");
#endif      
      if (droidDirection != 1) {
        go(0,0);
      }
      droidDirection = 1;//forward
      go(currentPower,currentPower);
    }
    //code==3 is our backward sign
    else if (pixy.line.barcodes->m_code==3) {
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

void move() {
  switch(navigationType) {
    case 0:
      moveBareCode();
      break;
  }
}
