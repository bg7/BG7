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
package com.ohnosequences.bioinfo.annotation;

/**
 * Utility class used in the semi-automatic gene annotation process
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class GeneEValuePair implements Comparable<GeneEValuePair> {

    public String id;
    public double eValue;

    public GeneEValuePair(String id, double eValue) {
        this.id = id;
        this.eValue = eValue;
    }

    public int compareTo(GeneEValuePair o) {
        if (this.eValue < o.eValue) {
            return -1;
        } else if (this.eValue > o.eValue) {
            return 1;
        } else {
            if(id.equals(o.id)){
                return 0;
            }else{
                return 1;
            }
        }
    }

    @Override
    public String toString(){
        return "(" + id + "," + eValue + ")\n";
    }

}
