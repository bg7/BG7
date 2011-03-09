/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.CodonUtil;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.Codon;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Frameshift;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.HspSet;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.era7xmlapi.model.XMLElementException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.symbol.SymbolList;
import org.jdom.Element;

/**
 * This is one of the most important programs/steps on the semi-automatic annotation
 * process. It carries out the gene prediction phase of the process
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class PredictGenes implements Executable {

    public static int DIF_SPAN = 30;

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        try {
            main(args);
        } catch (XMLElementException ex) {
            Logger.getLogger(PredictGenes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws XMLElementException {


        if (args.length != 5) {
            System.out.println("This program expects five parameters: \n"
                    + "1. BlastOutput XML filename \n"
                    + "2. Contigs FNA filename \n"
                    + "3. Output results XML filename \n"
                    + "4. Maximum gene length (integer)\n"
                    + "5. Flag (boolean) indicating if this genome corresponds to a virus (true/false)\n");
        } else {
            String blastOutputFileString = args[0];
            String fnaFileString = args[1];
            String outFileString = args[2];

            int MAXIMA_LONGITUD_GEN = 0;
            try {
                System.out.println("args[3] = " + args[3]);
                MAXIMA_LONGITUD_GEN = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                System.out.println("The parameter 'Maximum gene length' provided is not an integer");
                System.exit(-1);
            }
            if (MAXIMA_LONGITUD_GEN <= 0) {
                System.out.println("A number greater than 0 must be provided for the parameter 'Maximum gene length'");
                System.exit(-1);
            }

            boolean isVirus = false;
            String virusSt = args[4];
            if (virusSt.equals("true") || virusSt.equals("false")) {
                isVirus = Boolean.parseBoolean(virusSt);
            } else {
                System.out.println("The value provided for the virus flag parameter must be one of these two: \"true\" , \"false\"");
                System.exit(-1);
            }


            File blastOutputFile, fnaFile, outFile;

            blastOutputFile = new File(blastOutputFileString);
            fnaFile = new File(fnaFileString);

            try {

                HashMap<String, String> contigSequencesMap = new HashMap<String, String>();
                HashMap<String, String> contigsSequencesMapComplementaryInverted = new HashMap<String, String>();

                HashMap<String, ContigXML> contigsXMLMap = new HashMap<String, ContigXML>();

                HashMap<String, Integer> cuentaOrganismos = new HashMap<String, Integer>();


                //Leer datos archivo fna con contigs

                BufferedReader fnaReader = new BufferedReader(new FileReader(fnaFile));
                String tempFna;
                StringBuilder tempSecuenciaSt = new StringBuilder();
                String currentScaffoldId = "";

                System.out.println("Reading fna file...");

                while ((tempFna = fnaReader.readLine()) != null) {


                    if (tempFna.charAt(0) == '>') {

                        if (tempSecuenciaSt.length() > 0) {
                            contigSequencesMap.put(currentScaffoldId, tempSecuenciaSt.toString());
                            tempSecuenciaSt.delete(0, tempSecuenciaSt.length());
                        }

                        currentScaffoldId = tempFna.substring(1).split(" ")[0];
                    } else {
                        tempSecuenciaSt.append(tempFna);
                    }
                }

                if (tempSecuenciaSt.length() > 0) {
                    contigSequencesMap.put(currentScaffoldId, tempSecuenciaSt.toString());
                }

                //closing fna file reader
                fnaReader.close();

                System.out.println("Done!! :)");

                //Complementary inverted sequences from the contigs are now calculated
                //(they'll be useful later)

                System.out.println("Calculating complementary inverted sequences....");

                for (String key : contigSequencesMap.keySet()) {
                    SymbolList symL = DNATools.createDNA(contigSequencesMap.get(key));
                    symL = DNATools.reverseComplement(symL);
                    contigsSequencesMapComplementaryInverted.put(key, symL.seqString());
                }
                //-----------------------------------------------------

                System.out.println("Done!");

                //Reading data from xml Blastoutput
                BufferedReader reader = new BufferedReader(new FileReader(blastOutputFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                System.out.println("Parsing blastoutput XML file");

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //closing input reader
                reader.close();

                BlastOutput blast = new BlastOutput(stBuilder.toString());

                System.out.println("Done!");

                ArrayList<Iteration> iterations = blast.getBlastOutputIterations();
                System.out.println("Iterations size: " + iterations.size());

                //XMLElement where predicted genes will be added to
                PredictedGenes predictedGenes = new PredictedGenes();
                predictedGenes.setDifSpan(DIF_SPAN);
                predictedGenes.setGeneThreshold(MAXIMA_LONGITUD_GEN);
                // Array for hspsets containing hsps (groups with several
                // hsps joined together of groups with just one hsp) 
                ArrayList<HspSet> hspSets = new ArrayList<HspSet>();

                // Looping through every iteration
                for (Iteration iteration : iterations) {
                    ArrayList<Hit> hits = iteration.getIterationHits();

                    System.out.println("Iteration " + iteration.getUniprotIdFromQueryDef() + " has " + hits.size() + "hits");
                    //System.out.println("hits = " + hits);

                    //Retrieving organism from the iteration

                    String iterationOrganism = null;
                    if (hits.size() > 0) {
                        if (iteration.getQueryDef() != null) {
                            iterationOrganism = iteration.getQueryDef().split("\\|")[2].split("OS=")[1].split("GN=")[0].trim();
                        }
                    }

                    // Looping through all the hits in the iteracion
                    for (Hit hit : hits) {

                        System.out.println("hit = " + hit.getHitDef());

                        // Getting every hsp in this hit
                        ArrayList<Hsp> hsps = hit.getHitHsps();

                        //I get rid of elements which are not useful for us
                        for (int i = 0; i < hsps.size(); i++) {
                            Hsp hsp = hsps.get(i);
                            hsp.getRoot().removeChild("Hsp_qseq");
                            hsp.getRoot().removeChild("Hsp_hseq");
                            hsp.getRoot().removeChild("Hsp_midline");

                        }

                        HspSet hspSet = new HspSet();
                        hspSet.setOrganism(iterationOrganism);
                        //Saving the uniprot id linked to this hit
                        hit.setUniprotID(iteration.getUniprotIdFromQueryDef());
                        hspSet.setHit(hit);

                        System.out.println("Analyzing hsps hit, there are " + hsps.size());

                        while (hsps.size() > 0) {

                            System.out.println("Entering while... hsps.size()=" + hsps.size());

                            //Looping through hit hsps looking for the one with the least query from value
                            int indiceHspMenorQueryFrom = 0;
                            boolean orientacionHspMenorQueryFromEsNegativa = !hsps.get(0).getOrientation();
//                            System.out.println("hsps.get(0).getHitFrame() = " + hsps.get(0).getOrientation());
//                            System.out.println("orientacionHspMenorQueryFromEsNegativa = " + orientacionHspMenorQueryFromEsNegativa);
                            int hspQueryFrom = hsps.get(0).getQueryFrom();

                            for (int i = 0; i < hsps.size(); i++) {
                                Hsp tempHsp = hsps.get(i);

                                if (tempHsp.getQueryFrom() < hspQueryFrom) {
                                    hspQueryFrom = tempHsp.getQueryFrom();
                                    orientacionHspMenorQueryFromEsNegativa = (tempHsp.getHitFrame() < 0);
                                    indiceHspMenorQueryFrom = i;
                                }
                            }

                            if (hspSet.size() == 0) {
                                hspSet.addHsp(new Hsp((Element) hsps.get(indiceHspMenorQueryFrom).getRoot().clone()));

                                // It's removed from the array so that it does not belong to the set
                                // for searching hsps to be joined
                                hsps.remove(hsps.get(indiceHspMenorQueryFrom));
                            }


                            if (hsps.size() >= 1) {

                                //System.out.println("holaa!");

                                ArrayList<Hsp> hspsCopia = new ArrayList<Hsp>();
                                for (Hsp tempHsp : hsps) {
                                    hspsCopia.add(new Hsp((Element) tempHsp.getRoot().clone()));
                                }

                                // Now I must only select the hsps with the same orientation and see if
                                // there's any suitable candidate to be joined with
                                for (int i = 0; i < hspsCopia.size(); i++) {
                                    Hsp tempHsp = hspsCopia.get(i);
                                    System.out.println("tempHsp.getHitFrame() = " + tempHsp.getHitFrame());
                                    System.out.println("orientacionHspMenorQueryFromEsNegativa = " + orientacionHspMenorQueryFromEsNegativa);
                                    if (!((tempHsp.getHitFrame() < 0) == orientacionHspMenorQueryFromEsNegativa)) {
                                        //it's removed from the array cause if has a different orientation
                                        hspsCopia.remove(tempHsp);
                                    }
                                }

                                boolean seHaPegadoUnoNuevo = false;

                                while (hspsCopia.size() > 0) {

                                    //The ones left in the array are those wich have the same orientation
                                    //They must be filtered now so that only the ones with a 'query_to' value
                                    // bigger than the one from the last hsp in the hsp_set are left
                                    //System.out.println("hspsCopia = " + hspsCopia.get(0).getNum());
                                    for (int i = 0; i < hspsCopia.size(); i++) {
                                        Hsp tempHsp = hspsCopia.get(i);
                                        if (tempHsp.getQueryTo() <= hspSet.getHspQueryTo()) {
                                            //it's removed from the array cause its query_to value is smaller
                                            hspsCopia.remove(tempHsp);
                                        }
                                    }
                                    System.out.println("hspsCopia size after removing those with smaller query_to value = " + hspsCopia.size());

                                    //The one with the smallest query from value is selected among those which are left
                                    if (hspsCopia.size() > 0) {

                                        Hsp hspMenorQueryFrom = hspsCopia.get(0);
                                        for (int i = 1; i < hspsCopia.size(); i++) {
                                            Hsp tempppHsp = hspsCopia.get(i);
                                            if (tempppHsp.getQueryFrom() < hspMenorQueryFrom.getQueryFrom()) {
                                                hspMenorQueryFrom = tempppHsp;
                                            }
                                        }

                                        //Now spanHsps & spanQuery values must be calculated
                                        int spanHsps = 0;
                                        if (orientacionHspMenorQueryFromEsNegativa) {
                                            spanHsps = hspSet.getHspHitFrom() - hspMenorQueryFrom.getHitTo();
                                        } else {
                                            spanHsps = hspMenorQueryFrom.getHitFrom() - hspSet.getHspHitTo();
                                        }
                                        int spanQuery = (hspMenorQueryFrom.getQueryFrom() - hspSet.getHspQueryTo()) * 3;
                                        int difSpan = Math.abs((spanQuery - spanHsps));

                                        System.out.println("difSpan = " + difSpan);

                                        if (difSpan <= DIF_SPAN) {
                                            //Hsp is added to the hsp set
                                            Hsp hspToAdd = new Hsp((Element) hspMenorQueryFrom.getRoot().clone());
                                            System.out.println("Adding hsp: " + hspToAdd
                                                    + " from hit: " + hit.getHitDef()
                                                    + " difSpan = " + difSpan);
                                            System.out.println("hspSet = " + hspSet.getHsps());
                                            hspSet.addHsp(hspToAdd);
//                                            System.out.println("ESTE HSP SE HA JUNTADOOOOOOOOOOOOOOOOOOOOOOO!!!!!");
                                            seHaPegadoUnoNuevo = true;
                                            //Once a copy of the original has been added to the set, the original is removed
                                            for (int i = 0; i < hsps.size(); i++) {
                                                Hsp blau = hsps.get(i);
                                                if (blau.getNum().equals(hspMenorQueryFrom.getNum())) {
                                                    hsps.remove(blau);
                                                }
                                            }
                                            hspsCopia.remove(hspMenorQueryFrom);
                                        } else {
                                            //Hsp should not be added to the set, so it remains in the array for
                                            // the next loop
                                            hspsCopia.remove(hspMenorQueryFrom);
                                            seHaPegadoUnoNuevo = false;
                                        }

                                        //System.out.println("hspSet.size() = " + hspSet.size());
                                    }
                                }

                                if (!seHaPegadoUnoNuevo) {
                                    //We add what we found
                                    hspSets.add(hspSet);
                                    //Initializing the new hspset
                                    hspSet = new HspSet();
                                    //The uniprot id for the hit is stored
                                    hit.setUniprotID(iteration.getUniprotIdFromQueryDef());
                                    hspSet.setHit(hit);
                                }
                            }
                        }

                        // There are no hsps so I should go to the next hit
                        //Now it's time to add the hspSet found
                        hspSets.add(hspSet);

                    }
                }

                System.out.println("Iterations finished !!! :)");

                //Here hspSets are already retrieved, nos it's time to look for the genes!!
                int contadorIdsPredictedGenes = 1;

                for (HspSet hspSet : hspSets) {

                    PredictedGene predictedGene = new PredictedGene();
                    predictedGene.setId(String.valueOf(contadorIdsPredictedGenes));
                    contadorIdsPredictedGenes++;

                    //time to look for both begin and end codons
                    String currentContigId = hspSet.getHit().getHitDef().split(" ")[0];
                    String hitDnaSequence = contigSequencesMap.get(currentContigId);

                    //System.out.println("hsphitfrom original = " + hspSet.getHspHitFrom());

                    int hspHitFrom = hspSet.getHspHitFrom();
                    int hspHitTo = hspSet.getHspHitTo();

                    // Retrieving the sequence, if orientation is positive we just take the 'normal' one,
                    // otherwise we must take the complementary inverted seq.
                    if (!hspSet.getOrientation()) {
                        hitDnaSequence = contigsSequencesMapComplementaryInverted.get(currentContigId);

                        // If orientation is negative, the coordinates must be transformed since
                        // they are the other way round
                        hspHitFrom = Math.abs(hspHitFrom - hitDnaSequence.length());
                        hspHitTo = Math.abs(hspHitTo - hitDnaSequence.length());
                        //Besides that, the values must be swaped cause they are also changed when the
                        //orientation is negative
                        int tempSwap = hspHitFrom;
                        hspHitFrom = hspHitTo;
                        hspHitTo = tempSwap;

                        //I have to increment it in one, because the stop starts right after the last base
                        //and in this line the 'to' value is already the 'from' value due to the negative orientation
                        hspHitTo++;
                    } else {
                        hspHitFrom -= 1; // DECREASING IT IN ONE is important!!
                    }

                    //ArrayList with extra stop codons
                    ArrayList<Codon> extraStopCodonsArray = new ArrayList<Codon>();

                    //Now we look for the start codon depending on the orientation
                    int initCodonPosition = -1;
                    //int stopCodonPosition = -1;

                    int minInitCodonPosition = hspHitFrom;
                    int maxInitCodonPosition = minInitCodonPosition - MAXIMA_LONGITUD_GEN;
                    if (maxInitCodonPosition < 0) {
                        maxInitCodonPosition = 0;
                    }

                    int minRealStopCodonPosition = hspHitTo;
                    int maxRealStopCodonPosition = minRealStopCodonPosition + MAXIMA_LONGITUD_GEN;
                    if (maxRealStopCodonPosition > hitDnaSequence.length() - 3) {
                        maxRealStopCodonPosition = hitDnaSequence.length() - 3;
                    }

                    boolean initCodonFound = false;
                    boolean stopCodonFound = false;

                    //--------------INIT CODON SEARCH POSITIVE ORIENTATION----------------

                    for (int i = minInitCodonPosition; i >= maxInitCodonPosition && !stopCodonFound
                            && !initCodonFound; i -= 3) {
//                        System.out.println("init codon search i = " + i);
//                        System.out.println("hspHitFrom = " + hspHitFrom);
//                        System.out.println("hitDnaSequence.length() = " + hitDnaSequence.length());
//                        System.out.println("hspSet uniprot id = " + hspSet.getHit().getUniprotID());
//                        System.out.flush();

                        if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                            stopCodonFound = true;
                            //stopCodonPosition = i;
                        } else if (!isVirus) {
                            if (CodonUtil.isStartCodon(hitDnaSequence.substring(i))) {
                                initCodonFound = true;
                                initCodonPosition = i;
                            }
                        } else {
                            if (CodonUtil.isStartCodonVirus(hitDnaSequence.substring(i))) {
                                initCodonFound = true;
                                initCodonPosition = i;
                            }
                        }

                    }

                    if (!initCodonFound) {
                        initCodonPosition = minInitCodonPosition;
                    }


                    //If orientation is negative coordinates must be reconverted.
                    if (!hspSet.getOrientation()) {
                        initCodonPosition = Math.abs(initCodonPosition - hitDnaSequence.length());
                    } else {
                        initCodonPosition += 1; //notacion distinta ordenadores
                    }
                    predictedGene.setStartIsCanonical(initCodonFound);
                    predictedGene.setStartPosition(initCodonPosition);
//                    initCodon.setPosition(initCodonPosition);
//                    predictedGene.setStartCodon(initCodon);

                    //-----------------ESTE STOP NO SE GUARDA PORQUE ESTA FUERA DEL GEN !!!!
//                    if (stopCodonFound) {
//                        Codon stopCodon = new Codon();
//                        stopCodon.setType(Codon.STOP_CODON_TYPE);
//                        //Si es negativa hay que reconvertir las coord.
//                        if (!hspSet.getOrientation()) {
//                            stopCodonPosition = Math.abs(stopCodonPosition - hitDnaSequence.length());
//                        } else {
//                            stopCodonPosition += 1; //notacion distinta ordenadores
//                        }
//                        stopCodon.setPosition(stopCodonPosition);
//                        extraStopCodonsArray.add(stopCodon);
//                    }


                    boolean realStopCodonFound = false;
                    int realStopCodonPosition = -1;

                    //--------------REAL STOP CODON SEARCH POSITIVE ORIENTATION----------------

                    for (int i = minRealStopCodonPosition; i < maxRealStopCodonPosition
                            && !realStopCodonFound; i += 3) {
//                        System.out.println("i = " + i);
//                        System.out.println("hitDnaSequence.length() = " + hitDnaSequence.length());
//                        System.out.println("hspSet uniprot id = " + hspSet.getHit().getUniprotID());
//                        System.out.flush();
                        if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                            realStopCodonFound = true;
                            realStopCodonPosition = i;
                        }
                    }

                    if (!realStopCodonFound) {
                        realStopCodonPosition = minRealStopCodonPosition;
                    }


                    //We have to decrease in one the real stop codon position because in this case it
                    //should indicate the position of the last base of the codon previous to the stop codon,
                    // since this one does not codify amino acid
                    realStopCodonPosition -= 1;

                    //If orientation is negative coordinates must be reconverted
                    if (!hspSet.getOrientation()) {
                        realStopCodonPosition = Math.abs(realStopCodonPosition - hitDnaSequence.length());
                    } else {
                        realStopCodonPosition += 1; //Due to the differences between human-computar array notation
                    }

                    predictedGene.setEndIsCanonical(realStopCodonFound);
                    predictedGene.setEndPosition(realStopCodonPosition);

//                    realStopCodon.setPosition(realStopCodonPosition);
//                    predictedGene.setStopCodon(realStopCodon);


                    //---------------------EXTRA STOP CODONS SEARCH----------------------
                    if (hspSet.hspsAreFromTheSameReadingFrame()) {
                        //SAME READING FRAME

                        for (int i = hspHitFrom; i <= hspHitTo - 3; i += 3) {
                            if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                                Codon extraStopCodon = new Codon();
                                extraStopCodon.setType(Codon.STOP_CODON_TYPE);
                                int tempPosition = i;
                                //If orientation is negative coordinates must be reconverted
                                if (!hspSet.getOrientation()) {
                                    tempPosition = Math.abs(tempPosition - hitDnaSequence.length());
                                } else {
                                    tempPosition += 1; //Due to the differences between human-computar array notation
                                }
                                extraStopCodon.setPosition(tempPosition);
                                extraStopCodonsArray.add(extraStopCodon);
                            }
                        }

                    } else {
                        //DIFFERENT READING FRAME

                        for (Hsp lalala : hspSet.getHsps()) {

                            int tempHitFrom = lalala.getHitFrom();
                            int tempHitTo = lalala.getHitTo();
                            //Transforming coordinates

                            if (!hspSet.getOrientation()) {
                                //If orientation is negative coordinates must be transformed since they
                                //are the other way round
                                tempHitFrom = Math.abs(tempHitFrom - hitDnaSequence.length());
                                tempHitTo = Math.abs(tempHitTo - hitDnaSequence.length());
                                //Besides that, values must be swapped cause they are changed when orientation
                                //is negative
                                int tempSwap = tempHitFrom;
                                tempHitFrom = tempHitTo;
                                tempHitTo = tempSwap;
                            } else {
                                tempHitFrom -= 1; //DECREASING IT IN ONE is important!!
                                tempHitTo -= 1; //DECREASING IT IN ONE is important!!
                            }

                            for (int i = - 1; i < - 3; i += 3) {
                                if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                                    Codon extraStopCodon = new Codon();
                                    extraStopCodon.setType(Codon.STOP_CODON_TYPE);
                                    int tempPosition = i;
                                    //If orientation is negative coordinates must be reconverted
                                    if (!hspSet.getOrientation()) {
                                        tempPosition = Math.abs(tempPosition - hitDnaSequence.length());
                                    } else {
                                        tempPosition += 1; //Due to the differences between human-computar array notation
                                    }
                                    extraStopCodon.setPosition(tempPosition);
                                    extraStopCodonsArray.add(extraStopCodon);
                                }
                            }
                        }

                        //-----------------Frameshifts search--------------------
                        ArrayList<Hsp> tempArray = hspSet.getHsps();
                        for (int i = 0; i < tempArray.size() - 1; i++) {
                            Hsp hsp1 = tempArray.get(i);
                            Hsp hsp2 = tempArray.get(i + 1);
                            int hsp1Rframe = hsp1.getHitFrame();
                            int hsp2Rframe = hsp2.getHitFrame();
                            if (hsp1Rframe != hsp2Rframe) {
                                Frameshift frameshift = new Frameshift();
                                frameshift.setPosition(i + 1);
                                frameshift.setReadingFrameFrom(hsp1Rframe);
                                frameshift.setReadingFrameTo(hsp2Rframe);
                                predictedGene.addFrameShift(frameshift);
                            }
                        }
                    }


                    //Setting the DNA sequence
                    //predictedGene.setDnaSequence(hitDnaSequence);//.substring(hspSet.getHspHitFrom()-1,hspSet.getHspHitTo()));

                    //System.out.println("hitDnaSequence = " + hitDnaSequence);

                    //Adding extra stop codons
                    for (Codon stopCodon : extraStopCodonsArray) {
                        predictedGene.addExtraStopCodon(stopCodon);
                    }


                    //Adding the hspset to predicted gene element
                    predictedGene.setHspSet(hspSet);
                    //Setting the strand value for the predicted gene object
                    predictedGene.setStrand(hspSet.getOrientation());

                    //------------Setting blast hit start y blast hit end--------------------------
                    boolean orientationPredictedGene = predictedGene.getHspSet().getOrientation();
                    int blastHitStart = 0;
                    int blastHitEnd = 0;

                    HspSet setLalala = predictedGene.getHspSet();
                    Hsp hspPrimero = setLalala.get(0);
                    Hsp hspUltimo = setLalala.get(setLalala.size() - 1);
                    if (orientationPredictedGene) {
                        blastHitStart = hspPrimero.getHitFrom();
                        blastHitEnd = hspUltimo.getHitTo();
                    } else {
                        blastHitStart = hspPrimero.getHitTo();
                        blastHitEnd = hspUltimo.getHitFrom();
                    }

                    predictedGene.setBlastHitStart(blastHitStart);
                    predictedGene.setBlastHitEnd(blastHitEnd);
                    //---------------------------------------------------------------------------


                    //-----finally the organism value is added to the gene (it was extracted from the iteration)--------
                    predictedGene.setOrganism(hspSet.getOrganism());

                    if (!predictedGene.getOrganism().equals("")) {
                        Integer organismCount = cuentaOrganismos.get(predictedGene.getOrganism());
                        if (organismCount == null) {
                            cuentaOrganismos.put(predictedGene.getOrganism(), 0);
                            organismCount = 0;
                        }
                        cuentaOrganismos.put(predictedGene.getOrganism(), (organismCount + 1));
                    }


                    ContigXML currentContig = contigsXMLMap.get(currentContigId);
                    if (currentContig == null) {
                        currentContig = new ContigXML();
                        currentContig.setId(currentContigId);
                        contigsXMLMap.put(currentContigId, currentContig);
                    }

                    currentContig.addChild(predictedGene);
                }


                //Contigs are added to the Xml element 'predictedgenes'
                System.out.println("Adding contigs to predictedgenes element");
                Set<String> contigKeys = contigsXMLMap.keySet();
                for (String contigKey : contigKeys) {
                    ContigXML c = contigsXMLMap.get(contigKey);
                    predictedGenes.addChild(c);
                }

                //Counting which is the organism with more occurrences
                Set<String> keys = cuentaOrganismos.keySet();
                int maxCount = -1;
                String maxCountOrganism = "";
                for (String key : keys) {
                    int tempCount = cuentaOrganismos.get(key);
                    if (tempCount > maxCount) {
                        maxCount = tempCount;
                        maxCountOrganism = key;
                    }
                }
                //freeing some memory I don't need anymore
                keys.clear();
                cuentaOrganismos.clear();
                predictedGenes.setPreferentOrganism(maxCountOrganism);

                System.out.println("maxCountOrganism = " + maxCountOrganism);


                outFile = new File(outFileString);
                FileWriter fileWriter = new FileWriter(outFile);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                buffWriter.write(predictedGenes.toString());

                buffWriter.close();
                fileWriter.close();

                System.out.println("XML created with the name: " + outFileString);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }
}
