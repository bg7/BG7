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
package com.ohnosequences.bg7;

import java.io.*;
import java.util.*;

import com.ohnosequences.util.Entry;
import com.ohnosequences.util.Executable;
import com.ohnosequences.util.Pair;
import com.ohnosequences.util.seq.SeqUtil;
import com.ohnosequences.xml.api.model.XMLElement;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class SolveOverlappings implements Executable {

    @Override
    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 6) {
            System.out.println("This program expects six parameters: \n"
                    + "1. Predicted genes XML input file \n"
                    + "2. Output XML file with solved overlappings\n"
                    + "3. Number of bases overlapping threshold (integer)\n"
                    + "4. Input blast XML file with the rnas \n"
                    + "5. Input FNA file with the sequences of the contigs\n"
                    + "6. Genetic code file");
        } else {
            String inFileString = args[0];
            String outFileString = args[1];
            int umbralSolapamientoMenor = Integer.parseInt(args[2]);
            String rnaFileString = args[3];
            String fnaFileString = args[4];

            try {

                File inFile, outFile, rnaFile, fnaFile;
                inFile = new File(inFileString);
                rnaFile = new File(rnaFileString);
                outFile = new File(outFileString);
                fnaFile = new File(fnaFileString);

                //temporal
                File consoleFile = new File("consoleSolapamientos.txt");
                BufferedWriter consoleBuff = new BufferedWriter(new FileWriter(consoleFile));

                Annotation resultadoTotal = new Annotation();

                PredictedGenes resultadoGenes = new PredictedGenes();
                PredictedRnas resultadoRnas = new PredictedRnas();

                File geneticCodeFile = new File(args[5]);

                //reading predicted genes xml file
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //closing input file reader
                reader.close();

                PredictedGenes predictedGenesXML = new PredictedGenes(stBuilder.toString());

                int contadorSolapamientos = 0;

                List<Element> contigs = predictedGenesXML.getRoot().getChildren(ContigXML.TAG_NAME);
                //System.out.println(Contig.TAG_NAME);

                for (Element elemContig : contigs) {

                    ContigXML contig = new ContigXML(elemContig);
                    consoleBuff.write("Analyzing overlappings in contig " + contig.getId() + " there are " + (contigs.size() + " contigs left"));
                    ArrayList<PredictedGene> arrayGenes = new ArrayList<PredictedGene>();

                    System.out.println("Building genes array up ...");
                    List<Element> listGenes = contig.getRoot().getChildren(PredictedGene.TAG_NAME);
                    for (Element elemGen : listGenes) {
                        PredictedGene tempPGene = new PredictedGene(elemGen);
                        arrayGenes.add(tempPGene);
                    }
                    System.out.println("Done! :)");

                    ArrayList<Pair<Entry<String, Double>, Entry<String, Double>>> arraySolapamientosMayoresUmbral = new ArrayList<Pair<Entry<String, Double>, Entry<String, Double>>>();

                    for (int j = 0; j < arrayGenes.size() - 1; j++) {

                        PredictedGene tempGene1 = arrayGenes.get(j);
                        int initGene1 = tempGene1.getStartPosition();
                        int stopGene1 = tempGene1.getEndPosition();

                        //------If orientation is negative they're inverted----
                        if (!tempGene1.getHspSet().getOrientation()) {
                            int tempSwap = initGene1;
                            initGene1 = stopGene1;
                            stopGene1 = tempSwap;
                        }

                        for (int k = j + 1; k < arrayGenes.size(); k++) {

                            PredictedGene tempGene2 = arrayGenes.get(k);
                            int initGene2 = tempGene2.getStartPosition();
                            int stopGene2 = tempGene2.getEndPosition();

                            //------If orientation is negative they're inverted----
                            if (!tempGene2.getHspSet().getOrientation()) {
                                int tempSwap = initGene2;
                                initGene2 = stopGene2;
                                stopGene2 = tempSwap;
                            }

                            if ((stopGene1 >= initGene2) && (initGene1 <= initGene2)) {
                                //System.out.println("stopGene1 = " + stopGene1);
                                //System.out.println("initGene2 = " + initGene2);
                                consoleBuff.write("[" + tempGene1.getId() + "] overlaps with ["
                                        + tempGene2.getId() + "]" + "\n");

                                int difference = stopGene1 - initGene2;


                                if ((difference - 1) < umbralSolapamientoMenor) {
                                    tempGene1.setStatus(PredictedGene.STATUS_SELECTED_MINOR_THRESHOLD);
                                    tempGene2.setStatus(PredictedGene.STATUS_SELECTED_MINOR_THRESHOLD);
                                } else {

                                    Entry<String, Double> entry1 = new Entry<String, Double>(tempGene1.getId(), tempGene1.getHspSet().getEvalue());
                                    Entry<String, Double> entry2 = new Entry<String, Double>(tempGene2.getId(), tempGene2.getHspSet().getEvalue());
                                    Pair<Entry<String, Double>, Entry<String, Double>> pair = new Pair<Entry<String, Double>, Entry<String, Double>>(entry1, entry2);
                                    consoleBuff.write("pair = " + entry1.getKey() + "," + entry2.getKey() + "\n");

                                    arraySolapamientosMayoresUmbral.add(pair);
                                }

                                contadorSolapamientos++;
                            }

                        }
                    }

                    //here I already have in the array the existing overlappings divided in two groups,
                    // greater or smaller than the bases threshold. Now it's time for all the magic
                    //regarding grouping and sorting pairs in order to see which ones are dismissed and when.
                    TreeSet<Pair<String, Double>> treeSet = new TreeSet<>();
                    for (Pair<Entry<String, Double>, Entry<String, Double>> pair : arraySolapamientosMayoresUmbral) {
                        Pair<String, Double> gene1 = new Pair<String, Double>(pair.getValue1().getKey(), pair.getValue1().getValue());
                        treeSet.add(gene1);
                        //System.out.println("gene1.id = " + gene1.id);
                        Pair<String, Double> gene2 = new Pair<String, Double>(pair.getValue2().getKey(), pair.getValue2().getValue());
                        //System.out.println("gene2.id = " + gene2.id);
                        treeSet.add(gene2);
                    }

                    consoleBuff.write("treeSet.size() = " + treeSet.size());

                    //map with dismisse genes due to their e value,
                    //the first String is the gene id, the latter is the id of the gene which dismisses the first.
                    HashMap<String, String> dismissedGenes = new HashMap<String, String>();

//                    System.out.println("Treeset:");
//                    for (Iterator<GeneEValuePair> it = treeSet.iterator(); it.hasNext();) {
//                        GeneEValuePair geneEValuePair = it.next();
//                        System.out.println(geneEValuePair.id + "," + geneEValuePair.eValue);
//                    }

                    while (treeSet.size() > 0) {
                        Pair<String, Double> tempGenePair = treeSet.pollFirst();
                        consoleBuff.write("tempGenePair = " + tempGenePair.getValue1() + ", e= " + tempGenePair.getValue2() + "\n");

                        //array for storing the ids of the genes dismissed by the current tempGenePair 
                        ArrayList<String> anuladosArray = new ArrayList<String>();

                        for (Pair<Entry<String, Double>, Entry<String, Double>> pair : arraySolapamientosMayoresUmbral) {

                            Entry<String, Double> value1 = pair.getValue1();
                            Entry<String, Double> value2 = pair.getValue2();

                            if (value1.getKey().equals(tempGenePair.getValue1())) {
                                consoleBuff.write("value1 = " + value1 + "\n");
                                consoleBuff.write("value2 = " + value2);

                                //I'm in a pair corresponding to the current geneEvaluePair in value1
                                if (value2.getValue() >= tempGenePair.getValue2()) {
                                    dismissedGenes.put(value2.getKey(), tempGenePair.getValue1());
                                    anuladosArray.add(value2.getKey());
                                    consoleBuff.write("value2 must be removed: " + value2.getKey() + "\n");
                                } else {
                                    dismissedGenes.put(tempGenePair.getValue1(), value2.getKey());
                                    anuladosArray.add(tempGenePair.getValue1());
                                }
                            } else if (value2.getKey().equals(tempGenePair.getValue1())) {

                                consoleBuff.write("value1 = " + value1 + "\n");
                                consoleBuff.write("value2 = " + value2 + "\n");

                                //I'm in a pair corresponding to the current geneEvaluePair in value1
                                if (value1.getValue() >= tempGenePair.getValue2()) {
                                    consoleBuff.write("value1 must be removed: " + value1.getKey() + "\n");
                                    dismissedGenes.put(value1.getKey(), tempGenePair.getValue1());
                                    anuladosArray.add(value1.getKey());
                                } else {
                                    dismissedGenes.put(tempGenePair.getValue1(), value1.getKey());
                                    anuladosArray.add(tempGenePair.getValue1());
                                }

                            }

                        }


                        //now genes which were dismissed by the current one are removed from the list
                        for (Iterator<Pair<String, Double>> it = treeSet.iterator(); it.hasNext();) {
                            Pair<String, Double> tempPair = it.next();
                            for (String string : anuladosArray) {
                                if (tempPair.getValue1().equals(string)) {
                                    consoleBuff.write("removing " + tempPair.getValue1() + "\n");
                                    it.remove();
                                }
                            }

                        }


                    }


                    //----------------------------------------------------------------               

                    for (PredictedGene tempGene : arrayGenes) {


                        //System.out.println("id: " + tempGene.getId());
                        //System.out.println("status1: " + tempGene.getStatus());

                        String genQueDescarta = dismissedGenes.get(tempGene.getId());
                        if (genQueDescarta != null) {
                            tempGene.setStatus(PredictedGene.STATUS_DISMISSED);
                            tempGene.setGeneDismissedBy(genQueDescarta);
                        } else {
                            if (tempGene.getStatus() == null) {
                                tempGene.setStatus(PredictedGene.STATUS_SELECTED);
                            } else if (!tempGene.getStatus().equals(PredictedGene.STATUS_SELECTED_MINOR_THRESHOLD)) {
                                tempGene.setStatus(PredictedGene.STATUS_SELECTED);
                            }
                        }

                        //System.out.println("status2: " + tempGene.getStatus());

                        //tempGene.detach();
                        //contig.addPredictedGene(tempGene);
                    }

                }

                //Once every contig has been modified according to genes dismissed or not,
                //they are dettached from the original XML and attached to the result
                Element element = predictedGenesXML.getRoot().getChild(ContigXML.TAG_NAME);
                while (element != null) {
                    element.detach();
                    resultadoGenes.getRoot().addContent(element);
                    element = predictedGenesXML.getRoot().getChild(ContigXML.TAG_NAME);
                }

                //-----------RNAS RETRIEVAL----------------
                System.out.println("Retrieving RNAs !");

                reader = new BufferedReader(new FileReader(rnaFile));
                stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //closing input reader file
                reader.close();

                int contadorRnas = 0;

                ArrayList<ContigXML> contigsRnas = new ArrayList<ContigXML>();

                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());
                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();

                for (Iteration iteration : iterations) {
                    ArrayList<Hit> hits = iteration.getIterationHits();
                    if (!hits.isEmpty()) {

                        ContigXML contig = new ContigXML();
                        consoleBuff.write("iteration.getQueryDef() = " + iteration.getQueryDef() + "\n");
                        String contigId = iteration.getQueryDef().split(" ")[0].trim();
                        contig.setId(contigId);

                        TreeSet<PredictedRna> rnas = new TreeSet<PredictedRna>();

                        consoleBuff.write("hits.size() = " + hits.size() + "\n");

                        for (Hit hit : hits) {
                            ArrayList<Hsp> hsps = hit.getHitHsps();

                            for (Hsp hsp : hsps) {

                                int queryFrom = hsp.getQueryFrom();
                                int queryTo = hsp.getQueryTo();
                                boolean hspOrientation = hsp.getHitFrame() > 0;
                                //checking positions are in the order indicated by the hit-frame value
                                //if not, we have to swap them
                                if (hspOrientation) {
                                    if (queryFrom > queryTo) {
                                        int swap = queryFrom;
                                        queryFrom = queryTo;
                                        queryTo = swap;
                                    }
                                } else {
                                    if (queryTo > queryFrom) {
                                        int swap = queryFrom;
                                        queryFrom = queryTo;
                                        queryTo = swap;
                                    }
                                }


                                PredictedRna rna = new PredictedRna();
                                rna.setHitDef(iteration.getQueryDef());
                                // the hitdef 'chorizo' where the id of the genome element (besides more things) can be found
                                //is stored in the same tag as the one for the uniprot id
                                rna.setAnnotationUniprotId(hit.getHitDef());
                                rna.setStartPosition(queryFrom);
                                rna.setEndPosition(queryTo);
                                rna.setEvalue(hsp.getEvalueDoubleFormat());
                                rna.setStrand(hspOrientation);
                                rna.setId("r" + contadorRnas);
                                rnas.add(rna);

                                contadorRnas++;
                            }

                        }

                        System.out.println("rnas.size() = " + rnas.size());

                        //Here I should already have the rnas (from the same contig) ordered by position in the treeset
                        //now it's time to get rid of any kind of overlapping
                        PredictedRna[] rnasArray = rnas.toArray(new PredictedRna[rnas.size()]);
                        TreeSet<Integer> indicesABorrar = new TreeSet<Integer>();
                        for (int i = 0; i < rnasArray.length; i++) {
                            PredictedRna predictedRna = rnasArray[i];
                            boolean puedenSolapar = true;
                            if (!indicesABorrar.contains(i)) {
                                for (int j = i + 1; j < rnasArray.length && puedenSolapar; j++) {
                                    if (!indicesABorrar.contains(j)) {
                                        PredictedRna predictedRna2 = rnasArray[j];
                                        int end, begin2;
                                        end = predictedRna.getEndPosition();
                                        if (predictedRna.getStrand().equals(PredictedRna.NEGATIVE_STRAND)) {
                                            end = predictedRna.getStartPosition();
                                        }
                                        begin2 = predictedRna2.getStartPosition();
                                        if (predictedRna2.getStrand().equals(PredictedRna.NEGATIVE_STRAND)) {
                                            begin2 = predictedRna2.getEndPosition();
                                        }
                                        if (end < begin2) {
                                            puedenSolapar = false;
                                        } else {
                                            if (predictedRna.getEvalue() > predictedRna2.getEvalue()) {
                                                indicesABorrar.add(i);
                                                consoleBuff.write("this rna must be removed: \n" + predictedRna + "\n due to: \n" + predictedRna2 + "\n");
                                            } else {
                                                indicesABorrar.add(j);
                                                consoleBuff.write("this rna must be removed: \n" + predictedRna2 + "\n due to: \n" + predictedRna + "\n");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        //Now I have to add the non-overlapping rnas to the contig
                        for (int i = 0; i < rnasArray.length; i++) {
                            PredictedRna predictedRna = rnasArray[i];
                            if (!indicesABorrar.contains(i)) {
                                contig.addPredictedRna(predictedRna);
                            }
                        }

                        //This contig is already complete with its rnas so only adding it to the array is left
                        contigsRnas.add(contig);

                    }
                }

                //--------------------------------------------


                //Adding contig rnas to the result
                for (ContigXML contig : contigsRnas) {
                    resultadoRnas.addChild(contig);
                }


                //----------------RETRIEVING GENES & RNAS SEQUENCES------------------
                //------------------------------------------------------------------

                HashMap<String, String> contigsMap = new HashMap<String, String>();
                BufferedReader fnaReader = new BufferedReader(new FileReader(fnaFile));
                String tempFna;
                StringBuilder tempSecuenciaSt = new StringBuilder();
                String currentContigId = "";

                while ((tempFna = fnaReader.readLine()) != null) {
                    if (tempFna.charAt(0) == '>') {
                        if (tempSecuenciaSt.length() > 0) {
                            contigsMap.put(currentContigId, tempSecuenciaSt.toString());
                            tempSecuenciaSt.delete(0, tempSecuenciaSt.length());
                        }
                        currentContigId = tempFna.substring(1).trim().split(" ")[0].split("\t")[0];
                        System.out.println("currentContigId = " + currentContigId);
                    } else {
                        tempSecuenciaSt.append(tempFna);
                    }
                }

                if (tempSecuenciaSt.length() > 0) {
                    contigsMap.put(currentContigId, tempSecuenciaSt.toString());
                }
                //closing input fna reader
                fnaReader.close();

                //Now that all the sequences are in memory only adding them to the genes/rnas is left

                //------------------------SEQUENCES PREDICTED GENES---------------------------
                //---------------------------------------------------------------------------

                List<XMLElement> genesContigList = resultadoGenes.getChildrenWith(ContigXML.TAG_NAME);
                for (XMLElement xMLElement : genesContigList) {
                    ContigXML contig = new ContigXML(xMLElement.asJDomElement());
                    String contigSequence = contigsMap.get(contig.getId());
                    List<XMLElement> geneList = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement1 : geneList) {
                        PredictedGene gene = new PredictedGene(xMLElement1.asJDomElement());

                        //If orientation is positive we can take the positions directly
                        int beginPosition = gene.getStartPosition();
                        int endPosition = gene.getEndPosition();

                        if (gene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                            int tempSwap = beginPosition;
                            beginPosition = endPosition;
                            endPosition = tempSwap;
                        }

                        String tempSeq = contigSequence.substring(beginPosition - 1, endPosition);

                        boolean sePuedeTraducirAProteina = true;
                        sePuedeTraducirAProteina = gene.getEndIsCanonical() && gene.getStartIsCanonical()
                                && (gene.getFrameshifts() == null) && (gene.getExtraStopCodons() == null);


                        if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                            gene.setSequence(tempSeq);

                            if (sePuedeTraducirAProteina) {
                                //Translating sequence to protein
//                                SymbolList symL = DNATools.createDNA(tempSeq);
//                                symL = DNATools.toRNA(symL);
//                                symL = RNATools.translate(symL);
//                                gene.setProteinSequence(symL.seqString());
                                String seqTranslation = SeqUtil.translateDNAtoProtein(tempSeq, geneticCodeFile);
                                gene.setProteinSequence(seqTranslation);
                            }


                        } else {
                            //If orientation is negative we have to do all the magic regarding
                            //complementary inverted and so on
                            String complementaryInvertedSeq = SeqUtil.getComplementaryInverted(tempSeq);
                            gene.setSequence(complementaryInvertedSeq);

                            if (sePuedeTraducirAProteina) {
//                                //Translating sequence to protein
//                                symL = DNATools.toRNA(symL);
//                                symL = RNATools.translate(symL);
//                                gene.setProteinSequence(symL.seqString());
                                String seqTranslation = SeqUtil.translateDNAtoProtein(complementaryInvertedSeq, geneticCodeFile);
                                gene.setProteinSequence(seqTranslation);
                            }

                        }

                        //Retrieving the uniprot id from the hit and storing it at predicted gene level
                        gene.setAnnotationUniprotId(gene.getHspSet().getHit().getUniprotID());
                        //Changing hitdef to its correct location
                        gene.setHitDef(gene.getHspSet().getHit().getHitDef());
                        //Changing the evalue to its correct location
                        gene.setEvalue(gene.getHspSet().getEvalue());
                        //Getting rid of hspset which are no longer necessary for anything
                        gene.asJDomElement().removeChild(HspSet.TAG_NAME);
                    }
                }

                //------------------------PREDICTED RNAS SEQUENCES---------------------------
                //---------------------------------------------------------------------------
                List<XMLElement> rnaContigList = resultadoRnas.getChildrenWith(ContigXML.TAG_NAME);
                for (XMLElement xMLElement : rnaContigList) {
                    ContigXML contig = new ContigXML(xMLElement.asJDomElement());
                    System.out.println("Retrieving sequence from contig: " + contig.getId());
                    String contigSequence = contigsMap.get(contig.getId());
                    List<XMLElement> rnaList = contig.getChildrenWith(PredictedRna.TAG_NAME);
                    for (XMLElement xMLElement1 : rnaList) {
                        PredictedRna rna = new PredictedRna(xMLElement1.asJDomElement());

                        //If orientation is positive positions can be taken directly
                        int beginPosition = rna.getStartPosition();
                        int endPosition = rna.getEndPosition();

                        if (rna.getStrand().equals(PredictedRna.NEGATIVE_STRAND)) {
                            int tempSwap = beginPosition;
                            beginPosition = endPosition;
                            endPosition = tempSwap;
                        }

                        //System.out.println("rna = " + rna);
                        //System.out.println("contigSequence.length() = " + contigSequence.length());

                        String tempSeq = contigSequence.substring(beginPosition - 1, endPosition);

                        if (rna.getStrand().equals(PredictedRna.POSITIVE_STRAND)) {
                            rna.setSequence(tempSeq);
                        } else {
                            //All the magic regarding complemenary inverted seq if orientation is negative
                            rna.setSequence(SeqUtil.getComplementaryInverted(tempSeq));
                        }
                    }
                }

                //------------------------------------------------------------------
                //------------------------------------------------------------------

                resultadoTotal.setPredictedGenes(resultadoGenes);
                resultadoTotal.setPredictedRnas(resultadoRnas);

                //temporal
                consoleBuff.close();

                FileWriter fileWriter = new FileWriter(outFile);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                buffWriter.write(resultadoTotal.toString());
                buffWriter.close();
                fileWriter.close();

                System.out.println("XML output file successfully created with name: " + outFileString);
                //System.out.println("Numero de solapamientos detectados: " + contadorSolapamientos);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }
}
