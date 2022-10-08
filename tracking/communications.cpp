/*
  Droid with tracking turret using pixy2
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
#include <NeoSWSerial.h>
#include <EnableInterrupt.h>
#include "communications.h"
#include "tracking.h"
#include "engines.h"

#define RxD 2
#define TxD 3

static int32_t tempIValue;

static float tempDValue;

//for communication
static bool isValidInput;
static char inData[20]; // Allocate some space for the string
static char inChar; // Where to store the character read
static byte index = 0; // Index into array; where to store the character

static NeoSWSerial BTSerial(RxD, TxD);

void neoSSerial1ISR()
{
    NeoSWSerial::rxISR(*portInputRegister(digitalPinToPort(RxD)));
}

void initCommunications() {
  BTSerial.begin(38400);
  enableInterrupt(RxD, neoSSerial1ISR, CHANGE); 
}

static void makeCleanup() {  
  for (index = 0; index < 20; index++) {
    inData[index] = '\0';
  }
  index = 0;
  inChar ='0';
}

static void sendOK() {
  BTSerial.println("OK");
  BTSerial.flush();
}

void printMenuOnBLE() {
  BTSerial.println( "MENU:" );
  BTSerial.println( "h# print menu");
  BTSerial.println( "S# Start" );
  BTSerial.println( "s# Stop" );
  BTSerial.println( "T# tracking on");
  BTSerial.println( "t# tracking off");
  BTSerial.println( "L# lamp on/off");
  BTSerial.println( "E# engine pan tracking on/off");
  BTSerial.println( "pxx# horizontal position of camera");
  BTSerial.println( "Pxx# set Kp");
  BTSerial.println( "P# get Kp");
  BTSerial.println( "Ixx# set Ki");
  BTSerial.println( "I# get Ki");
  BTSerial.println( "Dxx# set Kd");
  BTSerial.println( "D# get Kd");
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

static void makeMove(const char *data) {
  char *realData = data;
  if (strlen(realData) > 0) {
     realData[strlen(realData)-1] = '\0';
  } else {
     return;
  }
  if (strlen(realData) == 1) {
    if (realData[0] == 's') {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("stop");
#endif
      go(0,0);
      isStopped = true;
      sendOK();
    } else if (realData[0] == 'S') {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("Start");
#endif
      go(0,0);
      isStopped = false;
      sendOK();
    } else if(realData[0] == 't') {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("Tracking off");
#endif
      isTracking = false;
      sendOK();
    } else if( realData[0] == 'T') {
#ifdef BLE_DEBUG_MODE
      BTSerial.println("Tracking on");
#endif
      lastError = 0;
      integration = 0;
      isTracking = true;
      sendOK();
    } else if (realData[0] == 'h') {
      printMenuOnBLE();
    } else if (realData[0] =='L') {
      if (isLampOn) {
        // Turn off both laps upper and lower
        pixy.setLamp(0, 0);
      } else {
        pixy.setLamp(1,1);
      }
      isLampOn = !isLampOn;
      sendOK();
    } else if (realData[0] == 'E') {
      isEngineTracking = !isEngineTracking;
#ifdef BLE_DEBUG_MODE
      if (isEngineTracking)
        BTSerial.println("Engine tracking on");
      else
        BTSerial.println("Engine tracking off");
#endif
      sendOK();
    } else if ( realData[0] == 'P') {
      BTSerial.println(Kp);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 'I') {
      BTSerial.println(Ki);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 'D') {
      BTSerial.println(Kd);
      BTSerial.flush();
      return;
    }
  } else { //commands with data
    if (realData[0] == 'p') {
      //remove p from command
      realData++;
      if (!isValidNumber(realData)) {
        sendOK();
        return;
      }
      tempIValue = atoi(realData);
      if (tempIValue <= 1000 && tempIValue >=0) {
          pixy.setServos(tempIValue,500);
          panServoPos = tempIValue;
      } else {
        sendOK();
        return;
      }
    } else if (realData[0] == 'P') {
      //remove P from command
      realData++;
      tempDValue = atof(realData);
#ifdef BLE_DEBUG_MODE
      BTSerial.print("Kp=");BTSerial.print(Kp);BTSerial.print("->");BTSerial.println(tempDValue);
#endif
      Kp=tempDValue;
      lastError = 0;
      integration = 0;
      tiltLoop.reset();
      pixy.setServos(panServoPos, tiltLoop.m_command);
      sendOK();
    } else if (realData[0] == 'D') {
      //remove D from command
      realData++;
      tempDValue = atof(realData);
#ifdef BLE_DEBUG_MODE
      BTSerial.print("Kd=");BTSerial.print(Kd);BTSerial.print("->");BTSerial.println(tempDValue);
#endif
      Kd=tempDValue;
      lastError = 0.;
      integration = 0;
      tiltLoop.reset();
      pixy.setServos(panServoPos, tiltLoop.m_command);
      sendOK();
    } else if (realData[0] == 'I') {
      //remove I from command
      realData++;
      tempDValue = atof(realData);
#ifdef BLE_DEBUG_MODE
      BTSerial.print("Ki=");BTSerial.print(Ki);BTSerial.print("->");BTSerial.println(tempDValue);
#endif
      Ki=tempDValue;
      lastError = 0;
      integration = 0;
      tiltLoop.reset();
      pixy.setServos(panServoPos, tiltLoop.m_command);
      sendOK();
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
