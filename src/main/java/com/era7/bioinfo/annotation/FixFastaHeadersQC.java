/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.ohnosequences.util.Executable;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class FixFastaHeadersQC implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("This program expects three parameters: \n"
                    + "1. Input FASTA original file \n"
                    + "2. Input FASTA (headers fixed) file\n"
                    + "3. Quality control TXT output file\n");
        } else {

            String originalFileString = args[0];
            String fixedFileString = args[1];
            String outFileString = args[2];

            File originalFile = new File(originalFileString);
            File fixedFile = new File(fixedFileString);
            File outFile = new File(outFileString);

            BufferedWriter outBuff = null;
            boolean error = false;

            try {

                outBuff = new BufferedWriter(new FileWriter(outFile));

                String originalLine = null;
                String fixedLine = null;
                BufferedReader originalReader = new BufferedReader(new FileReader(originalFile));
                BufferedReader fixedReader = new BufferedReader(new FileReader(fixedFile));

                int originalLineCounter = 1;

                outBuff.write("Quality control check for files: " + originalFile.getName() + " & " + fixedFile.getName() + "\n");

                while ((originalLine = originalReader.readLine()) != null) {

                    fixedLine = fixedReader.readLine();
                                        
                    if (originalLine.startsWith(">")) {
                        if (!fixedLine.startsWith(">")) {
                            error = true;
                            outBuff.write("Error found for line: " + originalLineCounter + "\n");
                        } else {
                            
                            if (!originalLine.substring(1).equals(fixedLine.substring(fixedLine.indexOf('|') + 1))) {
                                error = true;
                                outBuff.write("Error found for header in line: " + originalLineCounter + "\n");
                            }

                        }
                    } else {
                        if (!fixedLine.equals(originalLine)) {
                            error = true;
                            outBuff.write("Error! line: " + originalLineCounter + " should be equal in both files\n");
                        }
                    }

                    originalLineCounter++;
                }

                originalReader.close();
                fixedReader.close();


            } catch (Exception e) {
                try {
                    outBuff.write("Something went wrong: \n" + e.getMessage());
                    error = true;
                    e.printStackTrace();
                } catch (IOException ex) {
                    Logger.getLogger(FixFastaHeadersQC.class.getName()).log(Level.SEVERE, null, ex);
                }
            } finally {
                try {
                    if (!error) {
                        outBuff.write("Quality control passed!! :) \n");
                    } else {
                        outBuff.write("Errors were found.... :( \n");
                    }
                    outBuff.close();
                    
                    System.out.println("Quality control file generated with name: " + outFileString);
                } catch (IOException ex) {
                    Logger.getLogger(FixFastaHeadersQC.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
