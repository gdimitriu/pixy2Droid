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

#ifndef __MOVE_H__
#define __MOVE_H__

#include <Arduino.h>
#include <Wire.h>
#include <Pixy2I2C.h>
#include "engines.h"

extern Pixy2I2C pixy;

extern bool autoCalibrationDone;
extern bool isStopped;

void initMove();

void setNavigationType(unsigned int type);

void move();

void autocalibrationCamera();

void setLeftCode(unsigned int value);
void setRightCode(unsigned int value);
void setStopCode(unsigned int value);
void setForwardCode(unsigned int value);
void setBackwardCode(unsigned int value);

unsigned int getLeftCode();
unsigned int getRightCode();
unsigned int getStopCode();
unsigned int getForwardCode();
unsigned int getBackwardCode();

extern float Kp;
extern float Kd;
extern float Ki;

void reset();
#endif
