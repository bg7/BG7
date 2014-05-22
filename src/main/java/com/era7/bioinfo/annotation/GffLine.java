/*
 * Copyright (C) 2010-2011  "BG7"
 *
 * This file is part of BG7
 *
 * BG7 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.era7.bioinfo.annotation;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GffLine implements Comparable<GffLine> {

    protected int startPosition = 1;
    protected String line = "";

    public GffLine(int pos, String l) {

        startPosition = pos;
        line = l;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }



    public String getLine() {
        return line;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public int compareTo(GffLine o) {
        if(this.startPosition < o.startPosition){
            return -1;
        }else if(this.startPosition > o.startPosition){
            return 1;
        }else{
            //--->> VERY IMPORTANT, THIS IS A TRICK FOR USING THIS
            // CLASS IN THE PROGRAM GenerateGffFile
            return 1;
        }
    }



}
