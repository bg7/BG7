/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
