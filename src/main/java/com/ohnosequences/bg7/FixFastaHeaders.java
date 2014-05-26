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

import com.ohnosequences.util.Executable;

import java.io.*;
import java.util.ArrayList;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class FixFastaHeaders implements Executable {

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
                    + "1. Input FASTA file \n"
                    + "2. Output FASTA file\n"
                    + "3. Project prefix\n");
        } else {

            String inFileString = args[0];
            String outFileString = args[1];
            String projectPrefix = args[2];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);

            try {

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String line;
                int idCounter = 1;
                while ((line = reader.readLine()) != null) {
                    
                    if(line.startsWith(">")){
                        
                        outBuff.write(">" + projectPrefix + addUglyZeros(idCounter) + " |" + line.substring(1) + "\n");
                        idCounter++;
                        
                    }else{
                        outBuff.write(line + "\n");
                    }
                }
                //closing input file reader
                reader.close();
                //closing output file writer
                outBuff.close();
                
                System.out.println("Output fasta file created successfully! :D");

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    
    private static String addUglyZeros(int value){
        
        String result = "";
        String valueSt = String.valueOf(value);
        
        for(int i=valueSt.length(); i<6; i++){
            result += "0";
        }
        result += valueSt;
        
        return result;
    }
}
