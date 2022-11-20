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

#include <Arduino.h>
#include <EnableInterrupt.h>
#include "communications.h"
#include "engines.h"
#include "move.h"

#define RxD 2
#define TxD 3

static int32_t tempIValue;

static float tempDValue;

//for communication
static bool isValidInput;
static char inData[20]; // Allocate some space for the string
static char inChar; // Where to store the character read
static byte index = 0; // Index into array; where to store the character

NeoSWSerial BTSerial(RxD, TxD);

void neoSSerial1ISR()
{
    NeoSWSerial::rxISR(*portInputRegister(digitalPinToPort(RxD)));
}

void initCommunications() {
  BTSerial.begin(38400);
  enableInterrupt(RxD, neoSSerial1ISR, CHANGE); 
}

static void makeCleanup() {
  BTSerial.println("OK");
  BTSerial.flush();
  for (index = 0; index < 20; index++) {
    inData[index] = '\0';
  }
  index = 0;
  inChar ='0';
}

void printMenuOnBLE() {
  BTSerial.println("h# print menu");
  BTSerial.println("s# stop/start");
  BTSerial.println("l# lights on");
  BTSerial.println("a# autocalibration");
  BTSerial.println("L# left with turn90 delay");
  BTSerial.println("R# right with turn90 delay");
  BTSerial.println("txxx# set turn90");
  BTSerial.println("sxxx,xxx# set servo tilt,pan");
  BTSerial.println("vxxx# set the current power");
}


static boolean isValidNumber(char *data)
{
  if (strlen(data) == 0 ) return false;
   if(!(data[0] == '+' || data[0] == '-' || isDigit(data[0]))) return false;

   for(byte i=1;i<strlen(data);i++) {
       if(!(isDigit(data[i]) || data[i] == '.')) return false;
   }
   return true;
}

static void makeMove(char *data) {
  char *realData = data;
  if (strlen(realData) > 0) {
    realData[strlen(realData) -1] = '\0';
  } else {
    return;
  }
  if (strlen(realData) == 1) {
    if (realData[0] == 's') {
      isStopped = !isStopped;
#ifdef BLE_DEBUG_MODE
      if (isStopped)
        BTSerial.println("stop");
      else
        BTSerial.println("start again");
#endif
      go(0,0);
    } else if (realData[0] == 'l') {
      lightsOn = !lightsOn;
      if (lightsOn)
        pixy.setLamp(1, 1);
      else
        pixy.setLamp(0, 0);
    } else if (realData[0] == 'a') {
      autocalibrationCamera();
    } else if (realData[0] == 'L') {
      go(-currentPower,currentPower);
      delay(turn90);
      go(0,0);
    } else if (realData[0] == 'R') {
      go(currentPower,-currentPower);
      delay(turn90);
      go(0,0);
    } else if (realData[0] == 'h') {
      printMenuOnBLE();
    }
  } else {
    if (realData[0] == 'v') {
        realData++;
        if (!isValidNumber(realData)) {
          return;
        }
        if (atoi(realData) > 0 && atoi(realData) < 256)
          currentPower = atoi(realData);
    } else if (realData[0] == 't') {
#ifdef BLE_DEBUG_MODE      
        BTSerial.print("Turn90 ms");BTSerial.print(turn90);
#endif        
        realData++;
        if (!isValidNumber(realData)) {
          return;
        }
        if (atoi(realData) > 0) {
          turn90 = atoi(realData);
#ifdef BLE_DEBUG_MODE          
          BTSerial.print(" to ");BTSerial.print(turn90);
#endif          
        } else {
          makeCleanup();
          return;
        }
    } else if (realData[0] == 's') {
      realData++;
      //realData[strlen(realData)] = '\0';
      int position;
      for (uint8_t i = 0; i < strlen(realData); i++) {
        if (realData[i] == ',') {
           position = i;
           break;
        }
      }
      char buf[10];
      for (int i = 0; i < 10; i++) {
        buf[i] = '\0';
      }
      for (int i = 0 ; i < position; i++) {
        buf[i] = realData[i];
      }
      int moveData = atoi(buf);
      for (int i = 0; i < 10; i++) {
        buf[i] = '\0';
      }
      int idx = 0;
      for (int i = position + 1; i < strlen(realData); i++) {
        buf[idx] = realData[i];
        idx++;
      }
      int rotateData = atoi(buf);
      pixy.setServos(moveData,rotateData);
    }
  }
}


void receiveCommand() {
  while(BTSerial.available() > 0) // Don't read unless there you know there is data
  {
     if(index < 19) // One less than the size of the array
     {
        inChar = BTSerial.read(); // Read a character
        if (inChar == '\r' || inChar == '\n' || inChar == '\0' || inChar < 35 || inChar > 122) {
          continue;
        }
        //commands start with a letter capital or small
        if (index == 0 && !((inChar > 64 && inChar < 91) || (inChar > 96 && inChar < 123))) {
          continue;
        }
        inData[index++] = inChar; // Store it
        inData[index] = '\0'; // Null terminate the string
     } else {
        makeCleanup();
     }
 }
 if (inData[index-1] == '#') {
    makeMove(inData);
    makeCleanup();
 }
}
