/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.tests;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class DetectPotatos {

    public static void main(String[] args) throws Exception {



        if (args.length != 1) {
            System.out.println("The parameteres for this program are:" + "\n"
                    + "1.- Name of the file");
        } else {


            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            String line = null;

            int counter = 0;

            while ((line = reader.readLine()) != null) {
                counter++;
                if (line.trim().startsWith("<Iteration_query-def>")) {
                    char[] array = line.trim().toCharArray();
                    int barsCounter = 0;
                    for (char c : array) {
                        if (c == '|') {
                            barsCounter++;
                        }
                    }

                    if (barsCounter >= 3) {
                        System.out.println("Line: " + counter);
                    }
                }

            }


        }
    }
}
