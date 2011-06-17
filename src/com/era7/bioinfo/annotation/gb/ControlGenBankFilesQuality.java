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
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.genbank.GBCommon;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import com.era7.lib.era7xmlapi.model.XMLElementException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class ControlGenBankFilesQuality implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("This program expects two parameters: \n"
                    + "1. File/folder name with contigs in XML format \n"
                    + "2. File/folder name with contigs in GBK format\n");
        } else {

            File xmlFile = new File(args[0]);
            File gbFile = new File(args[1]);

            if (gbFile.isDirectory()) {
                if (xmlFile.isDirectory()) {
                    File[] files = gbFile.listFiles();
                    for (File subFile : files) {
                        if (subFile.isFile() && subFile.getName().endsWith(GBCommon.GEN_BANK_FILE_EXTENSION)) {
                            boolean found = false;
                            File subXmlFile = null;
                            for (File tempSubXmlFile : xmlFile.listFiles()) {
                                if (tempSubXmlFile.getName().endsWith(".xml") && tempSubXmlFile.getName().split("\\.")[0].equals(subFile.getName().split("\\.")[0])) {
                                    found = true;
                                    subXmlFile = tempSubXmlFile;
                                    break;
                                }
                            }
                            if (found) {
                                boolean result = controlaCalidadFiles(subFile, subXmlFile);
                                if (result) {
                                    System.out.println("Quality control passed successfully!! :)");
                                } else {
                                    System.out.println("Errors found in quality control :(");
                                }
                            } else {
                                System.out.println("There's no corresponding XML file for : " + subFile.getName());
                            }
                        }
                    }
                } else {
                    System.out.println("Error: A folder and a file were entered as parameters.");
                }
            } else {

                if (xmlFile.isFile()) {
                    if (gbFile.getName().endsWith(GBCommon.GEN_BANK_FILE_EXTENSION)
                            && xmlFile.getName().endsWith(".xml")) {
                        boolean result = controlaCalidadFiles(gbFile, xmlFile);
                        if (result) {
                            System.out.println("Quality control passed successfully!! :)");
                        } else {
                            System.out.println("Errors found in quality control :(");
                        }
                    } else {
                        System.out.println("The file(s) provided are not GenBank files");
                    }
                } else {
                    System.out.println("Error: A folder and a file were entered as parameters.");
                }
            }
        }
    }

    private static boolean controlaCalidadFiles(File gbFile, File xmlFile) {

        System.out.println("xmlFile.getName() = " + xmlFile.getName());
        System.out.println("gbFile.getName() = " + gbFile.getName());

        BufferedReader reader = null;
        boolean noError = true;

        try {


            Annotation gbAnnotation = ImportGenBankFiles.importGenBankFile(gbFile);
            reader = new BufferedReader(new FileReader(xmlFile));
            StringBuilder stBuilder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                stBuilder.append(line);
            }
            reader.close();
            Annotation xmlAnnotation = new Annotation(stBuilder.toString());

            PredictedGenes gbPredictedGenes = new PredictedGenes(gbAnnotation.asJDomElement().getChild(PredictedGenes.TAG_NAME));
            PredictedRnas gbPredictedRnas = new PredictedRnas(gbAnnotation.asJDomElement().getChild(PredictedRnas.TAG_NAME));

            PredictedGenes xmlPredictedGenes = new PredictedGenes(xmlAnnotation.asJDomElement().getChild(PredictedGenes.TAG_NAME));
            PredictedRnas xmlPredictedRnas = new PredictedRnas(xmlAnnotation.asJDomElement().getChild(PredictedRnas.TAG_NAME));

            
            //-----------GENES TEST--------------
            System.out.println("Checking contig genes....");
            List<Element> xmlContigs = xmlPredictedGenes.asJDomElement().getChildren(ContigXML.TAG_NAME);
            List<Element> gbContigs = gbPredictedGenes.asJDomElement().getChildren(ContigXML.TAG_NAME);
            noError = noError && checkGenesContigs(xmlContigs, gbContigs);

            //-----------RNAS TEST-----------------
            System.out.println("Checking contig rnas....");
            List<Element> xmlContigsRnas = xmlPredictedRnas.asJDomElement().getChildren(ContigXML.TAG_NAME);
            List<Element> gbContigsRnas = gbPredictedRnas.asJDomElement().getChildren(ContigXML.TAG_NAME);
            noError = noError && checkRnasContigs(xmlContigsRnas, gbContigsRnas);



        } catch (Exception ex) {
            Logger.getLogger(ControlGenBankFilesQuality.class.getName()).log(Level.SEVERE, null, ex);
            noError = false;
        }

        return noError;
    }


    private static boolean checkRnasContigs(List<Element> xmlContigs, List<Element> gbContigs) throws XMLElementException{
        boolean noError = true;
        for (Element element : xmlContigs) {
            ContigXML xmlContig = new ContigXML(element);
            ContigXML gbContig = null;
            for (Element element1 : gbContigs) {
                ContigXML tempContig = new ContigXML(element1);
                if (xmlContig.getId().equals(tempContig.getId())) {
                    gbContig = tempContig;
                    break;
                }
            }
            if (gbContig == null) {
                System.out.println("The contig " + xmlContig.getId() + " was not found in the corresponding GenBank file!!! :(");
                noError = false;
            } else {
                List<Element> xmlRnas = xmlContig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
                List<Element> gbRnas = gbContig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
                for (Element tempXmlRna : xmlRnas) {
                    PredictedRna goodRna = new PredictedRna(tempXmlRna);
                    PredictedRna testRna = null;
                    for (Element tempGbRna : gbRnas) {
                        PredictedRna tempTestRna = new PredictedRna(tempGbRna);
                        if (tempTestRna.getStartPosition() == goodRna.getStartPosition()
                                && tempTestRna.getEndPosition() == goodRna.getEndPosition()) {
                            testRna = tempTestRna;
                            break;
                        }
                    }
                    if (testRna == null) {
                        noError = false;
                        System.out.println("The rna " + goodRna.getId() + " was not found in the corresponding GenBank file!!! :(");
                    } else {
                        noError = noError && checkRna(goodRna, testRna);
                    }
                }
            }
        }
        return noError;
    }

    private static boolean checkGenesContigs(List<Element> xmlContigs, List<Element> gbContigs) throws XMLElementException {
        boolean noError = true;
        for (Element element : xmlContigs) {
            ContigXML xmlContig = new ContigXML(element);
            ContigXML gbContig = null;
            for (Element element1 : gbContigs) {
                ContigXML tempContig = new ContigXML(element1);
                if (xmlContig.getId().equals(tempContig.getId())) {
                    gbContig = tempContig;
                    break;
                }
            }
            if (gbContig == null) {
                System.out.println("The contig " + xmlContig.getId() + " was not found in the corresponding GenBank file!!! :(");
                noError = false;
            } else {
                List<Element> xmlGenes = xmlContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                List<Element> gbGenes = gbContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                for (Element tempXmlGene : xmlGenes) {
                    PredictedGene goodGene = new PredictedGene(tempXmlGene);
                    PredictedGene testGene = null;
                    for (Element tempGbGene : gbGenes) {
                        PredictedGene tempTestGene = new PredictedGene(tempGbGene);
                        if (tempTestGene.getStartPosition() == goodGene.getStartPosition()
                                && tempTestGene.getEndPosition() == goodGene.getEndPosition()) {
                            testGene = tempTestGene;
                            break;
                        }
                    }
                    if (testGene == null) {
                        noError = false;
                        System.out.println("The gene " + goodGene.getId() + " was not found in the corresponding GenBank file!!! :(");
                    } else {
                        noError = noError && checkGene(goodGene, testGene);
                    }
                }
            }
        }
        return noError;
    }

    private static boolean checkGene(PredictedGene goodGene, PredictedGene testGene) {
        boolean result = true;

        if (goodGene.getStartPosition() != testGene.getStartPosition()) {
            System.out.println("Different startPosition!!");
            result = false;
        } else if (goodGene.getEndPosition() != testGene.getEndPosition()) {
            System.out.println("Different endPosition!!");
            result = false;
        } else if (goodGene.getStartIsCanonical() != testGene.getStartIsCanonical()) {
            System.out.println("Different startIsCanonical!!");
            result = false;
        } else if (goodGene.getEndIsCanonical() != testGene.getEndIsCanonical()) {
            System.out.println("Different endIsCanonical!!");
            result = false;
        } else if (!goodGene.getStrand().equals(testGene.getStrand())) {
            System.out.println("Different strand!!");
            result = false;
        } else if (!goodGene.getSequence().equals(testGene.getSequence())) {
            System.out.println("Different sequence!!");
            result = false;
        }

        if (goodGene.getProteinSequence() != null) {
            if (!goodGene.getProteinSequence().equals(testGene.getProteinSequence())) {
                System.out.println("Different protein sequence!!");
                result = false;
            }
        } else {
            if (testGene.getProteinSequence() != null) {
                System.out.println("Different protein sequence!!");
                result = false;
            }
        }

        if (goodGene.getProteinNames() != null) {
            if (!goodGene.getProteinNames().equals(testGene.getProteinNames())) {
                System.out.println("Different protein names!!");
                result = false;
            }
        } else {
            if (testGene.getProteinNames() != null) {
                System.out.println("Different protein names!!");
                result = false;
            }
        }


        if (result == false) {
            System.out.println("goodGene = " + goodGene);
            System.out.println("testGene = " + testGene);
        }

        return result;
    }

    private static boolean checkRna(PredictedRna goodRna, PredictedRna testRna) {
        boolean result = true;

        if (goodRna.getStartPosition() != testRna.getStartPosition()) {
            System.out.println("Different startPosition!!");
            result = false;
        } else if (goodRna.getEndPosition() != testRna.getEndPosition()) {
            System.out.println("Different endPosition!!");
            result = false;
        } else if (!goodRna.getStrand().equals(testRna.getStrand())) {
            System.out.println("Different strand!!");
            result = false;
        } else if (!goodRna.getSequence().equals(testRna.getSequence())) {
            System.out.println("Different sequence!!");
            result = false;
        } //else if (!goodRna.getAnnotationUniprotId().equals(testRna.getAnnotationUniprotId())) {
//            System.out.println("Different annotation uniprot id!!");
//            result = false;
//        }

        if (result == false) {
            System.out.println("goodRna = " + goodRna);
            System.out.println("testRna = " + testRna);
        }

        return result;
    }
}
