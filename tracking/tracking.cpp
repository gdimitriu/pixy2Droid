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

#include "tracking.h"
#include "engines.h"

bool isStopped = true;
bool isTracking = false;
bool isLampOn = false;
bool isEngineTracking = false;

float Kp = -0.25;
float Kd = 100;
float Ki = 0;

int32_t lastError = 0;
int32_t integration = 0;

int32_t panServoPos = 380;

Pixy2I2C pixy;
PIDLoop panLoop(-300, 0, -5, true);
PIDLoop tiltLoop(-400, 0, -5, true);

void initTracking() {
  Wire.begin();
  pixy.init();
  isTracking = false;
  isLampOn = false;
  isStopped = true;
  // Turn off both laps upper and lower
  pixy.setLamp(0, 0);
  pixy.changeProg("color_connected_components");
}

void tracking() {
  int32_t panOffset, tiltOffset;
  int32_t left, right;
  // Use color connected components program for the pan tilt to track 
  pixy.changeProg("color_connected_components");
  
  // get active blocks from Pixy
  pixy.ccc.getBlocks();
  
  if (pixy.ccc.numBlocks) {        
    // calculate pan and tilt "errors" with respect to first object (blocks[0]), 
    // which is the biggest object (they are sorted by size).  
    panOffset = (int32_t)pixy.frameWidth/2 - (int32_t)pixy.ccc.blocks[0].m_x;
    tiltOffset = (int32_t)pixy.ccc.blocks[0].m_y - (int32_t)pixy.frameHeight/2;  
  
    // update loops    
    tiltLoop.update(tiltOffset);
    if (isEngineTracking) {
      double command;
      int32_t P,D;
      integration += panOffset;
      P = panOffset;
      if (integration > 255)
        integration = 255;
      if (integration < -255)
        integration = -255;
      D = panOffset - lastError;
      if (D > 255)
        D = 255;
      if (D < -255)
        D = -255;
      lastError = panOffset;
      command = Kp*P+Ki*integration+Kd*D;
      if (panOffset < 0) {
        left = command;
        if (left < MIN_ENGINE_POWER)
          left = MIN_ENGINE_POWER;
        right = -command;
        if (right > -MIN_ENGINE_POWER)
          right = - MIN_ENGINE_POWER;      
      } else {
        left = - command;
        if (left > - MIN_ENGINE_POWER)
          left = - MIN_ENGINE_POWER;
        right = command;
        if (right < MIN_ENGINE_POWER)
          right = MIN_ENGINE_POWER;      
      }
      pixy.setServos(panServoPos, tiltLoop.m_command);
      if (left < -MAX_ENGINE_POWER)
        left = -MAX_ENGINE_POWER;
      else if (left > MAX_ENGINE_POWER)
        left = MAX_ENGINE_POWER;
      if (right < -MAX_ENGINE_POWER)
        right = -MAX_ENGINE_POWER;
      else if (right > MAX_ENGINE_POWER)
        right = MAX_ENGINE_POWER;
      go(left,right);
    } else {
      panLoop.update(panOffset);
      // set pan and tilt servos  
      pixy.setServos(panLoop.m_command, tiltLoop.m_command);
    }
  } else {
    // no object detected, go into reset state
    go(0,0);
    panLoop.reset();
    tiltLoop.reset();
    pixy.setServos(panLoop.m_command, tiltLoop.m_command);
  }  
}
