/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import java.util.ArrayList;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class Export5ColumnsGenBankFiles implements Executable{
    
    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("This program expects 4 parameters: \n"
                    + "1. Gene annotation XML result filename \n"
                    + "2. Contig external general info XML filename\n"
                    + "3. FNA file with both header and contig sequence\n"
                    + "4. Prefix string for output files\n");
        } else {
            
            
            
        }
    }
    
}
