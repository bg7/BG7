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

package com.era7.bioinfo.annotation.tests;

import com.era7.lib.bioinfo.bioinfoutil.ExecuteFromFile;
import com.era7.lib.bioinfo.bioinfoutil.seq.SeqUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 *
 * @author ppareja
 */
public class Pruebas {

    public static void main(String[] args) throws Exception {


        String seq = "ATGATTCGTTCGATTTTCGTGTTATATACAATTATCAATTGTGTTAGATCCCACGGATACTTGTCCTATCCGGCGGCGAGACAATACAAATGCTTTAGAGACAATAATTTCTGGTGGCCCGAGGACGGAGAGGCGATTCCCGATCCCGCCTGCAAACACGCATATCAAACAGTGTACGCCAAATACCGATCAGAAGGAGAGCCAGCCGGTGTTGCCGCCAATGCCGCACAGTACATGTTTCAGCAGTATTACGAGTACGCTGCCGTCGCCGGCGCCAACTATGAAGATTTCGATCACATCAAGCGAAATGTAGTATCGCATAATTTATGCGCGGCCGGTGCCGATTACCGTTCCGATCCTTTCGGTGACAAATCCGGCATCGACGAGCCTCATACAGAGTGGCGAGCCGATACTTTTTATGTAGATCCCAAGCAAAAATATAGTCGAGGCATCGAAACGGTTCTGCATTTCTGTCCGACCGCGGTTCATCAGCCCAGCTACTTCGAGGTCTTTATCTCTAAGCGCGAATATGACTATTCCCACGAATTGACTTGGAACGACCTCGAGTTTATAGGCGGTAACAACTCACAGTTGATCGACAACGACGGGTCGGATCCATTGTGCGCACACGAATCCATATACACTATCAACGTCGCCATACCCTTTAGAATTAGGAAATTCGTATTGTACGTTCGCTGGCAACGCAACGACATAGCAGGCGAAGGATTTTACAACTGTGCCGATGTCGTCTTTGACGATTATTTACTCACACACAGACGTGTCAGTAAGCGGAGAAAGGCATATAGAGATCTT";
        String translationSeq = "MIRSIFVLYTIINCVRSHGYLSYPAARQYKCFRDNNFWWPEDGEAIPDPACKHAYQTVYAKYRSEGEPAGVAANAAQYMFQQYYEYAAVAGANYEDFDHIKRNVVSHNLCAAGADYRSDPFGDKSGIDEPHTEWRADTFYVDPKQKYSRGIETVLHFCPTAVHQPSYFEVFISKREYDYSHELTWNDLEFIGGNNSQLIDNDGSDPLCAHESIYTINVAIPFRIRKFVLYVRWQRNDIAGEGFYNCADVVFDDYLLTHRRVSKRRKAYRDL";
        
        
        System.out.println(translationSeq.equals(SeqUtil.translateDNAtoProtein(seq, new File("Genetic_code.txt"))));
//        String[] array = {"executions.xml"};
//        ExecuteFromFile.main(array);

//        File file = new File("genesPredichos.xml");
//
//        BufferedReader reader = new BufferedReader(new FileReader(file));
//        String line = null;
//        StringBuilder buffer = new StringBuilder();
//        char[] charBuffer = new char[100];
//        boolean encontrada = false;
//
//        int contador = 0;
//
//        while(reader.read(charBuffer,0, 100) != -1){
//
//            line = new String(charBuffer);
//
//            //System.out.println("line = " + line);
//            //System.exit(-1);
//            
//            if(line.trim().indexOf("<predicted_gene>") != -1){
//                if(encontrada){
//                    System.out.println(buffer.toString());
//                    encontrada = false;
//                }
//                buffer.delete(0, buffer.length());
//            }else if(line.indexOf("contig00122")!= -1){
//                encontrada = true;
//                System.out.println("contig00122 encontrado!!");
//            }
////            }else if(line.indexOf("Q88W21")!= -1){
////                encontrada = true;
////                System.out.println("Q88W21 encontrada!!");
////            }else if(line.indexOf("C2FIG3")!= -1){
////                encontrada = true;
////                System.out.println("C2FIG3 encontrada!!");
////            }else if(line.indexOf("C6VQH8")!= -1){
////                encontrada = true;
////                System.out.println("C6VQH8 encontrada!!");
////            }else if(line.indexOf("Q88T69")!= -1){
////                encontrada = true;
////                System.out.println("Q88T69 encontrada!!");
////            }else if(line.indexOf("C6VL53")!= -1){
////                encontrada = true;
////                System.out.println("C6VL53 encontrada!!");
////            }
//            buffer.append(line);
//
//            contador++;
//
//            if(contador % 100 == 0){
//                System.out.println("caracteres leidos = " + (contador*100));
//            }
//            
//
//            
//
//        }
//        

    }

}
