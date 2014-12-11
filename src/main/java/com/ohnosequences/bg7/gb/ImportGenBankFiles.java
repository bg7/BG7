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
package com.ohnosequences.bg7.gb;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ohnosequences.util.Executable;
import com.ohnosequences.xml.api.util.XMLUtil;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

import javax.xml.transform.TransformerException;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class ImportGenBankFiles implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("This program expects one parameter: \n"
                    + "1. File/folder name with the genbank file(s) that must be imported \n");
        } else {

            File file = new File(args[0]);

            try{
                if (file.isDirectory()) {

                    File[] files = file.listFiles();
                    for (File subFile : files) {
                        if (subFile.isFile() && subFile.getName().endsWith(GBCommon.GEN_BANK_FILE_EXTENSION)) {
                            Annotation annotation = importGenBankFile(subFile);
                            writeFile(annotation,file);
                        }
                    }

                } else {

                    if (file.getName().endsWith(GBCommon.GEN_BANK_FILE_EXTENSION)) {
                        Annotation annotation = importGenBankFile(file);
                        writeFile(annotation,file);
                    } else {
                        System.out.println("The file provided does not have " + GBCommon.GEN_BANK_FILE_EXTENSION + " extension");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    private static void writeFile(Annotation annotation, File file) throws TransformerException {
        BufferedWriter writer = null;
        try {
            String outFileName = file.getName().split("\\"+ GBCommon.GEN_BANK_FILE_EXTENSION)[0] + ".xml";
            writer = new BufferedWriter(new FileWriter(new File(outFileName)));
            writer.write(XMLUtil.prettyPrintXML(annotation.toString(), 5));
            writer.close();
            System.out.println("File " + outFileName + " created successfully! :)");
        } catch (IOException ex) {
            Logger.getLogger(ImportGenBankFiles.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(ImportGenBankFiles.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static Annotation importGenBankFile(File file) {
        try {

            System.out.println("file.getName() = " + file.getName());

            int predictedGeneIdCounter = 1;
            int predictedRnaIdCounter = 1;

            //xml elems
            Annotation annotation = new Annotation();
            PredictedGenes predictedGenes = new PredictedGenes();
            PredictedRnas predictedRnas = new PredictedRnas();
            HashMap<String, ContigXML> contigGenesMap = new HashMap<String, ContigXML>();
            HashMap<String, ContigXML> contigRnasMap = new HashMap<String, ContigXML>();

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;

            while((line = reader.readLine()) != null){

                ContigXML contigGenes = new ContigXML();
                predictedGenes.addChild(contigGenes);
                ContigXML contigRnas = new ContigXML();
                predictedRnas.addChild(contigRnas);

                //------------------------------LOCUS line------------------------------------
                //line = reader.readLine();
                String[] locusSplit = line.split(" |\t");

                int currentIndex = 1;
                boolean found = false;
                //contig ids loop
                for (; currentIndex < locusSplit.length && !found; currentIndex++) {
                    if (!locusSplit[currentIndex].equals("")) {
                        //System.out.println("locusSplit[currentIndex] = " + locusSplit[currentIndex]);
                        String contigId = locusSplit[currentIndex];
                        contigGenes.setId(contigId);
                        contigRnas.setId(contigId);
                        contigGenesMap.put(contigId, contigGenes);
                        contigRnasMap.put(contigId, contigRnas);
                        found = true;
                    }
                }
                found = false;
                //contig length loop
                for (; currentIndex < locusSplit.length && !found; currentIndex++) {
                    if (!locusSplit[currentIndex].equals("")) {
                        //System.out.println("locusSplit[currentIndex] = " + locusSplit[currentIndex]);
                        contigGenes.setLength(Integer.parseInt(locusSplit[currentIndex]));
                        found = true;
                    }
                }
                //------------------------------------------------------------------------------------

                //------------------------------SOURCE line------------------------------------
                found = false;
                while ((line = reader.readLine()) != null && !found) {
                    if (line.startsWith(GBCommon.SOURCE_STR)) {
                        contigGenes.setOrganism(line.split(GBCommon.SOURCE_STR)[1].trim());
                        found = true;
                    }
                }
                //------------------------------------------------------------------------------------


                //------------------------------------------------------------------------------------
                //------------------------------genes & rnas lines----------------------------------
                found = false;
                //--------first I have to skip some lines till reaching the features line
                while ((line = reader.readLine()) != null && !found) {
                    if (line.startsWith(GBCommon.FEATURES_STR)) {
                        found = true;
                    }
                }

                //now more lines must be skipped till I reach the first gene
                do {
                    line = reader.readLine();
                } while (!line.trim().startsWith(GBCommon.GENE_STR) && !line.trim().startsWith(GBCommon.ORIGIN_STR));

                while (!line.trim().startsWith(GBCommon.ORIGIN_STR)) {

                    boolean newGeneReached = false;

                    if (line.trim().startsWith(GBCommon.GENE_STR) &&
                            checkStringContainsWhiteSpacesAtTheBeginning(line, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_GENE)) {

                        System.out.println(line);



                        //-------------GET STRAND & START/END POSITION--------------------
                        //---------------------------------------------------------------

                        boolean strandIsNegative = line.indexOf("complement(") >= 0;
                        String positionSt = "";
                        if (strandIsNegative) {
                            positionSt = line.split("complement\\(")[1].split("\\)")[0];
                        } else {
                            positionSt = line.split(GBCommon.GENE_STR)[1].trim();
                        }

                        boolean positionsIncludeJoin = positionSt.indexOf("join(") >= 0;


                        boolean startIsCanonical = true;
                        boolean endIsCanonical = true;
                        int startPosition1, endPosition1, startPosition2 = 0, endPosition2 = 0;
                        String geneName = "";

                        if(positionsIncludeJoin){

                            System.out.println("join!");

                            positionSt = line.split("join\\(")[1].split("\\)")[0];
                            String[] cols = positionSt.split(",");
                            positionSt = cols[0];
                            String position2St = cols[1];

//                        System.out.println("positionSt = " + positionSt);
//                        System.out.println("position2St = " + position2St);

                            String pos2_1St, pos2_2St;
                            pos2_1St = position2St.split("\\.\\.")[0];
                            pos2_2St = position2St.split("\\.\\.")[1];

                            //---------------Retrieving positions of second gene in the join----------------
                            if (pos2_1St.charAt(0) == '<') {
                                if (strandIsNegative) {
                                    endIsCanonical = false;
                                } else {
                                    startIsCanonical = false;
                                }
                                pos2_1St = pos2_1St.substring(1);
                            }
                            if (pos2_2St.charAt(0) == '>') {
                                if (strandIsNegative) {
                                    startIsCanonical = false;
                                } else {
                                    endIsCanonical = false;
                                }
                                pos2_2St = position2St.substring(1);
                            }
                            int pos1, pos2;
                            pos1 = Integer.parseInt(pos2_1St);
                            pos2 = Integer.parseInt(pos2_2St);
                            if (strandIsNegative) {
                                startPosition2 = pos2;
                                endPosition2 = pos1;
                            } else {
                                startPosition2 = pos1;
                                endPosition2 = pos2;
                            }
                        }

                        String pos1St, pos2St;
                        pos1St = positionSt.split("\\.\\.")[0];
                        pos2St = positionSt.split("\\.\\.")[1];

                        //Now I have to figure out if start/end are canonical or not
                        if (pos1St.charAt(0) == '<') {
                            if (strandIsNegative) {
                                endIsCanonical = false;
                            } else {
                                startIsCanonical = false;
                            }
                            pos1St = pos1St.substring(1);
                        }
                        if (pos2St.charAt(0) == '>') {
                            if (strandIsNegative) {
                                startIsCanonical = false;
                            } else {
                                endIsCanonical = false;
                            }
                            pos2St = pos2St.substring(1);
                        }

                        int pos1, pos2;
                        pos1 = Integer.parseInt(pos1St);
                        pos2 = Integer.parseInt(pos2St);
                        if (strandIsNegative) {
                            startPosition1 = pos2;
                            endPosition1 = pos1;
                        } else {
                            startPosition1 = pos1;
                            endPosition1 = pos2;
                        }



                        //---------------------------------------------------------------
                        //---------------------------------------------------------------

                        boolean cDSFound = false;
                        boolean rnaFound = false;

                        //----SKIP LINES TILL CDS /xRNA IS FOUND--------------
                        do {
                            line = reader.readLine();
                            cDSFound = line.trim().startsWith(GBCommon.CDS_STR) && checkStringContainsWhiteSpacesAtTheBeginning(line, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_CDS);
                            rnaFound = line.trim().startsWith(GBCommon.RNA_STR) && checkStringContainsWhiteSpacesAtTheBeginning(line, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_RNA);
                            if(line.trim().startsWith("/gene=")){
                                //System.out.println(line);
                                geneName = line.trim().split("=")[1].split("\"")[1];

                            }
                        } while (!cDSFound && !rnaFound);
                        //-------------------------------------------------------------

                        String translationSt = "";

                        //----SKIP LINES TILL PRODUCT IS FOUND--------------
                        boolean productFound = false;
                        do {
                            line = reader.readLine();
                            productFound = line.trim().startsWith("/product=");
                        } while ( !productFound
                                && !(line.trim().startsWith(GBCommon.GENE_STR)));
                        //-------------------------------------------------------------

                        newGeneReached = !productFound;

                        String productSt = "";
                        //----I'm already in the product line--------------
                        if(productFound){
                            productSt = line.trim().split("/product=\"")[1];
                            if (productSt.indexOf("\"") >= 0) {
                                //the product must be completed in this line
                                productSt = productSt.split("\"")[0];
                            } else {
                                do {
                                    line = reader.readLine();
                                    productSt += line.trim() + "\n";
                                } while (!line.trim().endsWith("\""));
                                productSt = productSt.split("\"")[0];
                            }
                            //System.out.println("productSt = " + productSt);
                        }
                        //-------------------------------------------------------------


                        //----READING LINES TILL FINDING EITHER TRANSLATION OR OTHER GENE--------------
                        boolean translationFound = false;
                        if(!newGeneReached){
                            do {
                                line = reader.readLine();
                                translationFound = line.trim().startsWith("/translation=");
                            } while (!translationFound
                                    && !(line.trim().startsWith(GBCommon.GENE_STR))
                                    && !(line.trim().startsWith(GBCommon.ORIGIN_STR)));
                            //-------------------------------------------------------------
                        }

                        if(!translationFound){
                            newGeneReached = true;
                        }

                        //---------------translation line-------------------------
                        if (line.trim().startsWith("/translation=")) {
                            translationSt = line.trim().split("/translation=\"")[1];
                            if (productSt.indexOf("\"") >= 0) {
                                //the product should be already complete in this line
                                translationSt = translationSt.split("\"")[0];
                            } else {
                                do {
                                    line = reader.readLine();
                                    translationSt += line.trim();
                                } while (!line.trim().endsWith("\""));
                                translationSt = translationSt.split("\"")[0];
                            }
                        }
                        //-------------------------------------------------------------

                        System.out.println(translationSt);

                        //System.out.println("isRna = " + isRna);

                        //-it's time to create the gene/rna
                        if (rnaFound) {
                            PredictedRna tempRna = new PredictedRna();
                            tempRna.setId(String.valueOf(predictedRnaIdCounter));
                            predictedRnaIdCounter++;
                            tempRna.setStartPosition(startPosition1);
                            tempRna.setEndPosition(endPosition1);
                            if (strandIsNegative) {
                                tempRna.setStrand(PredictedRna.NEGATIVE_STRAND);
                            } else {
                                tempRna.setStrand(PredictedRna.POSITIVE_STRAND);
                            }
                            tempRna.setRnaName(productSt);
                            contigRnas.addPredictedRna(tempRna);
                            //System.out.println("tempRna = " + tempRna);

                        } else {

                            //---------creating second gene in case there was a join-----------------
                            if(positionsIncludeJoin){
                                PredictedGene tempGene = new PredictedGene();
                                tempGene.setId(String.valueOf(predictedGeneIdCounter));
                                predictedGeneIdCounter++;
                                tempGene.setStartPosition(startPosition2);
                                tempGene.setEndPosition(endPosition2);
                                tempGene.setStartIsCanonical(startIsCanonical);
                                tempGene.setEndIsCanonical(endIsCanonical);
                                tempGene.setGeneNames(geneName);
                                if (strandIsNegative) {
                                    tempGene.setStrand(PredictedGene.NEGATIVE_STRAND);
                                } else {
                                    tempGene.setStrand(PredictedGene.POSITIVE_STRAND);
                                }
                                tempGene.setProteinNames(productSt);
                                if (translationSt.length() > 0) {
                                    tempGene.setProteinSequence(translationSt);
                                }
                                contigGenes.addPredictedGene(tempGene);
                            }



                            PredictedGene tempGene = new PredictedGene();
                            tempGene.setId(String.valueOf(predictedGeneIdCounter));
                            predictedGeneIdCounter++;
                            tempGene.setStartPosition(startPosition1);
                            tempGene.setEndPosition(endPosition1);
                            tempGene.setStartIsCanonical(startIsCanonical);
                            tempGene.setEndIsCanonical(endIsCanonical);
                            tempGene.setGeneNames(geneName);
                            if (strandIsNegative) {
                                tempGene.setStrand(PredictedGene.NEGATIVE_STRAND);
                            } else {
                                tempGene.setStrand(PredictedGene.POSITIVE_STRAND);
                            }
                            tempGene.setProteinNames(productSt);
                            if (translationSt.length() > 0) {
                                tempGene.setProteinSequence(translationSt);
                            }
                            contigGenes.addPredictedGene(tempGene);
                        }

                    }

                    if(!newGeneReached){
                        line = reader.readLine();
                    }

                }

                //----------------------CONTIG SEQUENCE PART (ORIGIN STR)------------------------------
                //------------------------------------------------------------------------------------
//            System.out.println("line = " + line);
                if (line.trim().startsWith(GBCommon.ORIGIN_STR)) {
                    StringBuilder seqStBuilder = new StringBuilder();
                    while (!(line = reader.readLine()).trim().equals("//")) {
                        String[] columns = line.trim().split(" ");
                        for (int i = 1; i < columns.length; i++) {
                            seqStBuilder.append(columns[i]);
                        }
                    }

                    String contigSequence = seqStBuilder.toString();

                    //------------NOW IT'S TIME TO FILL UP GENE/RNA SEQUENCES---------------------
                    List<Element> genesList = contigGenes.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                    List<Element> rnasList = contigRnas.asJDomElement().getChildren(PredictedRna.TAG_NAME);

                    //-------------rnas------------------
                    for (Element element : rnasList) {
                        PredictedRna tempRna = new PredictedRna(element);
                        int startPos, endPos;
                        if (tempRna.getStrand().equals(PredictedRna.POSITIVE_STRAND)) {
                            startPos = tempRna.getStartPosition() - 1;
                            endPos = tempRna.getEndPosition();
                        } else {
                            startPos = tempRna.getEndPosition() - 1;
                            endPos = tempRna.getStartPosition();
                        }
                    System.out.println("contigSequence.length() = " + contigSequence.length());
                    System.out.println("startPos = " + startPos);
                    System.out.println("endPos = " + endPos);
                        tempRna.setSequence(contigSequence.substring(startPos, endPos));
                    }

                    //-------------genes------------------
                    for (Element element : genesList) {
                        PredictedGene tempGene = new PredictedGene(element);
                        int startPos, endPos;
                        if (tempGene.getStrand().equals(PredictedRna.POSITIVE_STRAND)) {
                            startPos = tempGene.getStartPosition() - 1;
                            endPos = tempGene.getEndPosition();
                        } else {
                            startPos = tempGene.getEndPosition() - 1;
                            endPos = tempGene.getStartPosition();
                        }
                        System.out.println(tempGene.getGeneNames());
                        System.out.println(startPos + "," + endPos);
                        tempGene.setSequence(contigSequence.substring(startPos, endPos));
                    }

                    //------------------------------------------------------------------------------------

                }
                //------------------------------------------------------------------------------------
                //------------------------------------------------------------------------------------



                //------------------------------------------------------------------------------------
                //------------------------------------------------------------------------------------
            }

            reader.close();

            annotation.setPredictedGenes(predictedGenes);
            annotation.setPredictedRnas(predictedRnas);
            return annotation;

        } catch (Exception ex) {
            Logger.getLogger(ImportGenBankFiles.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        return null;

    }

    public static boolean checkStringContainsWhiteSpacesAtTheBeginning(String str, int numberOfWhiteSpaces){
        if(str.length() > numberOfWhiteSpaces){
            for(int i=0;i < numberOfWhiteSpaces; i++){
                if(str.charAt(i) != ' '){
                    return false;
                }
            }
            if(str.charAt(numberOfWhiteSpaces) != ' '){
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
}
