/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

/**
 *
 * @author ppareja
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
