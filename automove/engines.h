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

#ifndef __ENGINES_H__
#define __ENGINES_H__

#define MAX_POWER_ENGINE 255

#define MIN_POWER_ENGINE 150

void initializetEngines();

void go(int speedLeft, int speedRight);

extern unsigned int turn90;
extern int currentPower;

#endif
