/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
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
                    System.out.println("El archivo proporcionado no tiene extension '.gb'");
                }
            }
        }
    }

    private static void writeFile(Annotation annotation, File file){
        BufferedWriter writer = null;
        try {
            String outFileName = file.getName().split("\\.gb")[0] + ".xml";
            writer = new BufferedWriter(new FileWriter(new File(outFileName)));
            writer.write(annotation.toString());
            writer.close();
            System.out.println("Archivo " + outFileName + " creado con exito! :)");
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

            //xml elems
            Annotation annotation = new Annotation();
            PredictedGenes predictedGenes = new PredictedGenes();
            PredictedRnas predictedRnas = new PredictedRnas();
            ContigXML contigGenes = new ContigXML();
            predictedGenes.addChild(contigGenes);
            ContigXML contigRnas = new ContigXML();
            predictedRnas.addChild(contigRnas);

            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;

            //------------------------------LOCUS line------------------------------------
            line = reader.readLine();
            String[] locusSplit = line.split(" |\t");

            int currentIndex = 1;
            boolean found = false;
            //contig ids loop
            for (; currentIndex < locusSplit.length && !found; currentIndex++) {
                if (!locusSplit[currentIndex].equals("")) {
                    //System.out.println("locusSplit[currentIndex] = " + locusSplit[currentIndex]);
                    contigGenes.setId(locusSplit[currentIndex]);
                    contigRnas.setId(locusSplit[currentIndex]);
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

                //System.out.println("line1 = " + line);

                if (line.trim().startsWith(GBCommon.GENE_STR)) {

                    //-------------GET STRAND & START/END POSITION--------------------
                    //---------------------------------------------------------------

                    boolean strandIsNegative = line.indexOf("complement(") >= 0;
                    String positionSt = "";
                    if (strandIsNegative) {
                        positionSt = line.split("complement\\(")[1].split("\\)")[0];
                    } else {
                        positionSt = line.split(GBCommon.GENE_STR)[1].trim();
                    }

                    boolean startIsCanonical = true;
                    boolean endIsCanonical = true;
                    int startPosition, endPosition;
                    //Now I have to figure out if start/end are canonical or not
                    if (positionSt.charAt(0) == '<') {
                        if (strandIsNegative) {
                            endIsCanonical = false;
                        } else {
                            startIsCanonical = false;
                        }
                        positionSt = positionSt.substring(1);
                    }
                    if (positionSt.charAt(positionSt.length() - 1) == '>') {
                        if (strandIsNegative) {
                            startIsCanonical = false;
                        } else {
                            endIsCanonical = false;
                        }
                        positionSt = positionSt.substring(0, positionSt.length() - 1);
                    }
                    int pos1, pos2;
                    pos1 = Integer.parseInt(positionSt.split("\\.\\.")[0]);
                    pos2 = Integer.parseInt(positionSt.split("\\.\\.")[1]);
                    if (strandIsNegative) {
                        startPosition = pos2;
                        endPosition = pos1;
                    } else {
                        startPosition = pos1;
                        endPosition = pos2;
                    }

                    //---------------------------------------------------------------
                    //---------------------------------------------------------------

                    //----SKIP LINES TILL CDS /xRNA IS FOUND--------------
                    do {
                        line = reader.readLine();
                    } while (!line.trim().startsWith(GBCommon.CDS_STR)
                            && (line.trim().split(" |\t")[0].toUpperCase().indexOf("RNA") < 0));
                    //-------------------------------------------------------------

                    boolean isRna = false;
                    if (line.trim().split(" |\t")[0].toUpperCase().indexOf("RNA") >= 0) {
                        isRna = true;
                    }

                    String translationSt = "";

                    //----SKIP LINES TILL FINDIND PRODUCT--------------
                    do {
                        line = reader.readLine();
                    } while (!line.trim().startsWith("/product="));
                    //-------------------------------------------------------------

                    //----I'm already in the product line--------------
                    String productSt = line.trim().split("/product=\"")[1];
                    if (productSt.indexOf("\"") >= 0) {
                        //the product must be completed in this line
                        productSt = productSt.split("\"")[0];
                    } else {
                        do {
                            line = reader.readLine();
                            productSt += line.trim();
                        } while (!line.trim().endsWith("\""));
                        productSt = productSt.split("\"")[0];
                    }
                    //System.out.println("productSt = " + productSt);
                    //-------------------------------------------------------------


                    //----READING LINES TILL FINDING EITHER TRANSLATION OR OTHER GENE--------------
                    do {
                        line = reader.readLine();
                    } while (!line.trim().startsWith("/translation=")
                            && !(line.trim().startsWith(GBCommon.GENE_STR))
                            && !(line.trim().startsWith(GBCommon.ORIGIN_STR)));
                    //-------------------------------------------------------------

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
                        //System.out.println("translationSt = " + productSt);
                    }
                    //-------------------------------------------------------------

                    //System.out.println("isRna = " + isRna);

                    //-it's time to create the gene/rna
                    if (isRna) {
                        PredictedRna tempRna = new PredictedRna();
                        tempRna.setStartPosition(startPosition);
                        tempRna.setEndPosition(endPosition);
                        if (strandIsNegative) {
                            tempRna.setStrand(PredictedRna.NEGATIVE_STRAND);
                        } else {
                            tempRna.setStrand(PredictedRna.POSITIVE_STRAND);
                        }
                        tempRna.setRnaName(productSt);
                        contigRnas.addPredictedRna(tempRna);
                        //System.out.println("tempRna = " + tempRna);

                    } else {
                        PredictedGene tempGene = new PredictedGene();
                        tempGene.setStartPosition(startPosition);
                        tempGene.setEndPosition(endPosition);
                        tempGene.setStartIsCanonical(startIsCanonical);
                        tempGene.setEndIsCanonical(endIsCanonical);
                        if (strandIsNegative) {
                            tempGene.setStrand(PredictedRna.NEGATIVE_STRAND);
                        } else {
                            tempGene.setStrand(PredictedRna.POSITIVE_STRAND);
                        }
                        tempGene.setProteinNames(productSt);
                        if (translationSt.length() > 0) {
                            tempGene.setProteinSequence(translationSt);
                        }
                        contigGenes.addPredictedGene(tempGene);
                    }

                }

                if (!line.trim().startsWith(GBCommon.ORIGIN_STR) && !line.trim().startsWith(GBCommon.GENE_STR)) {
                    line = reader.readLine();
                }

            }

            //----------------------CONTIG SEQUENCE PART (ORIGIN STR)------------------------------
            //------------------------------------------------------------------------------------
//            System.out.println("line = " + line);
            if (line.trim().startsWith(GBCommon.ORIGIN_STR)) {
                StringBuilder seqStBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.trim().split(" ");
                    for (int i = 1; i < columns.length; i++) {
                        seqStBuilder.append(columns[i]);
                    }
                }
                reader.close();
                String contigSequence = seqStBuilder.toString();

                //------------NOW IT'S TIME TO FILL UP GENE/RNA SEQUENCES---------------------
                List<Element> genesList = predictedGenes.asJDomElement().getChild(ContigXML.TAG_NAME).getChildren(PredictedGene.TAG_NAME);
                List<Element> rnasList = predictedRnas.asJDomElement().getChild(ContigXML.TAG_NAME).getChildren(PredictedRna.TAG_NAME);

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
//                    System.out.println("contigSequence.length() = " + contigSequence.length());
//                    System.out.println("startPos = " + startPos);
//                    System.out.println("endPos = " + endPos);
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
                    tempGene.setSequence(contigSequence.substring(startPos, endPos));
                }

                //------------------------------------------------------------------------------------

            }
            //------------------------------------------------------------------------------------
            //------------------------------------------------------------------------------------



            //------------------------------------------------------------------------------------
            //------------------------------------------------------------------------------------

            annotation.setPredictedGenes(predictedGenes);
            annotation.setPredictedRnas(predictedRnas);
            return annotation;

        } catch (Exception ex) {
            Logger.getLogger(ImportGenBankFiles.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }

        return null;

    }
}
