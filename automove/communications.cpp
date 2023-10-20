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
static char outData[20];

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
  memset(inData, 0, sizeof(char) * 20);
  memset(outData, 0, sizeof(char) * 20);
  index = 0;
  inChar ='0';
}

void printMenuOnBLE() {
  BTSerial.println("h# print menu");
  BTSerial.println("s# stop");
  BTSerial.println("S# start");
  BTSerial.println("l[1/0]# lights on/off");
  BTSerial.println("a# autocalibration");
  BTSerial.println("L# left with turn90 delay");
  BTSerial.println("R# right with turn90 delay");
  BTSerial.println("txxx# set turn90");
  BTSerial.println("t# get turn90");
  BTSerial.println("sxxx,xxx# set servo tilt,pan");
  BTSerial.println("vxxx# set the current power");
  BTSerial.println("v# get the current power");
  BTSerial.println("m[b/l]# set barecode or line");
  BTSerial.println("S[l,r,s,f,b] set code for barcodes");
  BTSerial.println("G[l,r,s,f,b] get code for barcodes");
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

static void sendBLEOK() {
  char buff[10];
   for (int i = 0; i < 10; i++) {
     buff[i] = '\0';
  }
  sprintf(buff,"OK\r\n");
  BTSerial.print(buff);
  BTSerial.flush();
}

static void makeMove(char *data) {
  char *realData = data;
  if (strlen(realData) > 0) {
    realData[strlen(realData) -1] = '\0';
  } else {
    sendBLEOK();
    return;
  }
  if ( strlen(realData) == 1 ) {
    char buff[10];
    for (int i = 0; i < 10; i++) {
      buff[i] = '\0';
    }
    if ( realData[0] == 's' ) {
      isStopped = true;
      go(0,0);
    } else if ( realData[0] == 'S' ) {
      isStopped = false;
      reset();
    } else if ( realData[0] == 'a' ) {
      autocalibrationCamera();
    } else if ( realData[0] == 'L' ) {
      go(-currentPower,currentPower);
      delay(turn90);
      go(0,0);
    } else if ( realData[0] == 'R' ) {
      go(currentPower,-currentPower);
      delay(turn90);
      go(0,0);
    } else if ( realData[0] == 'h' ) {
      printMenuOnBLE();
    } else if ( realData[0] == 'D' ) {
      BTSerial.println(Kd);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 'I' ) {
      BTSerial.println(Ki);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 'P' ) {
      BTSerial.println(Kp);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 'v' ) {
      BTSerial.println(currentPower);
      BTSerial.flush();
      return;
    } else if ( realData[0] == 't' ) {
      BTSerial.println(turn90);
      BTSerial.flush();
      return;
    }
  } else {
    if (realData[0] == 'v') {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
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
          sendBLEOK();
          return;
        }
        if (atoi(realData) > 0) {
          turn90 = atoi(realData);
#ifdef BLE_DEBUG_MODE          
          BTSerial.print(" to ");BTSerial.print(turn90);
#endif          
        } else {
          makeCleanup();
          sendBLEOK();
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
    } else if (realData[0] == 'S') {      
      realData++;
      if ( realData[0] == 'l' ) {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
          return;
        }
        setLeftCode(atoi(realData));
      } else if ( realData[0] == 'r' ) {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
          return;
        }
        setRightCode(atoi(realData));
      } else if ( realData[0] == 's' ) {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
          return;
        }
        setStopCode(atoi(realData));
      } else if ( realData[0] == 'f' ) {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
          return;
        }
        setForwardCode(atoi(realData));
      } else if ( realData[0] == 'b' ) {
        realData++;
        if (!isValidNumber(realData)) {
          sendBLEOK();
          return;
        }
        setBackwardCode(atoi(realData));
      } else {
        BTSerial.print("0\r\n");
        BTSerial.flush();
        return;
      }
    } else if (realData[0] == 'G') {
      memset(outData, 0, sizeof(char) * 20);
      realData++;
      if ( realData[0] == 'l' ) {
        sprintf(outData,"%i\r\n", getLeftCode());
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      } else if ( realData[0] == 'r' ) {
        sprintf(outData,"%i\r\n", getRightCode());
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      } else if ( realData[0] == 's' ) {
        sprintf(outData,"%i\r\n", getStopCode());
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      } else if ( realData[0] == 'f' ) {
        sprintf(outData,"%i\r\n", getForwardCode());
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      } else if ( realData[0] == 'b' ) {
        sprintf(outData,"%i\r\n", getBackwardCode());
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      } else {
        sprintf(outData, "0\r\n");
        BTSerial.print(outData);
        BTSerial.flush();
        return;
      }
    } else if ( realData[0] == 'm') {
      realData++;
      if ( realData[0] == 'b' ) {
        initMove(0);
      } else if ( realData[1] == 'l' ) {
        initMove(1);
      }
    } else if ( realData[0] == 'D' ) {
      realData++;
      if (!isValidNumber(realData)) {
        sendBLEOK();
        return;
      }
      Kd = atof(realData);
    } else if ( realData[0] == 'I' ) {
      realData++;
      if (!isValidNumber(realData)) {
        sendBLEOK();
        return;
      }
      Ki = atof(realData);
    } else if ( realData[0] == 'P' ) {
      realData++;
      if (!isValidNumber(realData)) {
        sendBLEOK();
        return;
      }
      Kp = atof(realData);
    }  else if ( realData[0] == 'l' ) {
      realData++;
      if ( realData[0] == '1' )
        pixy.setLamp(1, 1);
      else if ( realData[0] == '0' )
        pixy.setLamp(0, 0);
    }
  }
  sendBLEOK();
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
