/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

        if (args.length != 3) {
            System.out.println("This program expects 3 parameters: \n"
                    + "1. Gene annotation XML result filename \n"
                    + "2. GenomeProject ID\n"
                    + "3. Locus tag prefix\n") ;
        } else {
            
            String annotationFileSt = args[0];
            String genomeProjectIdSt = args[1];
            String locusTagPrefixSt = args[2];
            
            
            File annotationFile = new File(annotationFileSt);
            
            try {

                //-----READING XML FILE WITH ANNOTATION DATA------------
                BufferedReader reader = new BufferedReader(new FileReader(annotationFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Closing file
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());
                //-------------------------------------------------------------
                
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        }
    }
    
}
