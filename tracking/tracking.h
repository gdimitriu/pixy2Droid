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

#ifndef __TRACKING_H__
#define __TRACKING_H__

#include <Arduino.h>
#include <Wire.h>
#include <Pixy2I2C.h>
#include <PIDLoop.h>

extern bool isStopped;
extern bool isTracking;
extern bool isLampOn;
extern bool isEngineTracking;

extern float Kp;
extern float Kd;
extern float Ki;

extern int32_t lastError;
extern int32_t integration;

extern Pixy2I2C pixy;
extern PIDLoop panLoop;
extern PIDLoop tiltLoop;
extern int32_t panServoPos;

void initTracking();

void tracking();

#endif
