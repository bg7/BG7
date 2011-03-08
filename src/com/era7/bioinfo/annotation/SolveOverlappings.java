/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Entry;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.Pair;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.HspSet;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SymbolList;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class SolveOverlappings implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 5) {
            System.out.println("El programa espera cinco parametros: \n"
                    + "1. Nombre del archivo xml de entrada con los genes predichos \n"
                    + "2. Nombre del archivo xml de salida con los solapamientos resueltos\n"
                    + "3. Umbral de numero de bases para marcar como solapamiento (numero entero)\n"
                    + "4. Nombre del archivo xml de blast con la salida de rnas \n"
                    + "5. Nombre del archivo fna con las secuencias de los contigs\n");
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

                //Leer datos archivo xml con predicted genes
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //Cerrar archivo de entrada blastouput
                reader.close();

                PredictedGenes predictedGenesXML = new PredictedGenes(stBuilder.toString());

                int contadorSolapamientos = 0;

                List<Element> contigs = predictedGenesXML.getRoot().getChildren(ContigXML.TAG_NAME);
                //System.out.println(Contig.TAG_NAME);

                for (Element elemContig : contigs) {

                    ContigXML contig = new ContigXML(elemContig);
                    consoleBuff.write("Analizando solapamientos en el contig " + contig.getId() + " quedan " + (contigs.size()));
                    ArrayList<PredictedGene> arrayGenes = new ArrayList<PredictedGene>();

                    System.out.println("Construyendo array de genes...");
                    List<Element> listGenes = contig.getRoot().getChildren(PredictedGene.TAG_NAME);
                    for (Element elemGen : listGenes) {
                        PredictedGene tempPGene = new PredictedGene(elemGen);
                        arrayGenes.add(tempPGene);
                    }
                    System.out.println("Array construido! :)");

                    ArrayList<Pair<Entry<String, Double>, Entry<String, Double>>> arraySolapamientosMayoresUmbral = new ArrayList<Pair<Entry<String, Double>, Entry<String, Double>>>();

                    for (int j = 0; j < arrayGenes.size() - 1; j++) {

                        PredictedGene tempGene1 = arrayGenes.get(j);
                        int initGene1 = tempGene1.getStartPosition();
                        int stopGene1 = tempGene1.getEndPosition();

                        //------Si la orientacion es negativa son al reves----
                        if (!tempGene1.getHspSet().getOrientation()) {
                            int tempSwap = initGene1;
                            initGene1 = stopGene1;
                            stopGene1 = tempSwap;
                        }

                        for (int k = j + 1; k < arrayGenes.size(); k++) {

                            PredictedGene tempGene2 = arrayGenes.get(k);
                            int initGene2 = tempGene2.getStartPosition();
                            int stopGene2 = tempGene2.getEndPosition();

                            //------Si la orientacion es negativa son al reves----
                            if (!tempGene2.getHspSet().getOrientation()) {
                                int tempSwap = initGene2;
                                initGene2 = stopGene2;
                                stopGene2 = tempSwap;
                            }

                            if ((stopGene1 >= initGene2) && (initGene1 <= initGene2)) {
                                //System.out.println("stopGene1 = " + stopGene1);
                                //System.out.println("initGene2 = " + initGene2);
                                consoleBuff.write("[" + tempGene1.getId() + "] solapa con ["
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

                    //---aqui ya tengo los solapamientos existentes en el array separados por mayores y
                    //menores que el umbral de bases. Ahora viene todo el circo de agrupar y ordenar
                    //parejas para ver cuales se anulan antes y despues.
                    TreeSet<GeneEValuePair> treeSet = new TreeSet<GeneEValuePair>();
                    for (Pair<Entry<String, Double>, Entry<String, Double>> pair : arraySolapamientosMayoresUmbral) {
                        GeneEValuePair gene1 = new GeneEValuePair(pair.getValue1().getKey(), pair.getValue1().getValue());
                        treeSet.add(gene1);
                        //System.out.println("gene1.id = " + gene1.id);
                        GeneEValuePair gene2 = new GeneEValuePair(pair.getValue2().getKey(), pair.getValue2().getValue());
                        //System.out.println("gene2.id = " + gene2.id);
                        treeSet.add(gene2);
                    }

                    consoleBuff.write("treeSet.size() = " + treeSet.size());

                    //map con los genes anulados por lo de la e, la primera String es el id del gen
                    //descartado y la segunda String el id del gen que lo descarta.
                    HashMap<String, String> dismissedGenes = new HashMap<String, String>();

//                    System.out.println("Treeset:");
//                    for (Iterator<GeneEValuePair> it = treeSet.iterator(); it.hasNext();) {
//                        GeneEValuePair geneEValuePair = it.next();
//                        System.out.println(geneEValuePair.id + "," + geneEValuePair.eValue);
//                    }

                    while (treeSet.size() > 0) {
                        GeneEValuePair tempGenePair = treeSet.pollFirst();
                        consoleBuff.write("tempGenePair = " + tempGenePair.id + ", e= " + tempGenePair.eValue + "\n");

                        //aqui voy a guardar los ids de los genes que anula el tempGenePair actual
                        ArrayList<String> anuladosArray = new ArrayList<String>();

                        for (Pair<Entry<String, Double>, Entry<String, Double>> pair : arraySolapamientosMayoresUmbral) {

                            Entry<String, Double> value1 = pair.getValue1();
                            Entry<String, Double> value2 = pair.getValue2();

                            if (value1.getKey().equals(tempGenePair.id)) {
                                consoleBuff.write("value1 = " + value1 + "\n");
                                consoleBuff.write("value2 = " + value2);

                                //Estoy en una pareja correspondiente al current geneEvaluePair en value1
                                if (value2.getValue() >= tempGenePair.eValue) {
                                    dismissedGenes.put(value2.getKey(), tempGenePair.id);
                                    anuladosArray.add(value2.getKey());
                                    consoleBuff.write("se borra value2: " + value2.getKey() + "\n");
                                } else {
                                    dismissedGenes.put(tempGenePair.id, value2.getKey());
                                    anuladosArray.add(tempGenePair.id);
                                }
                            } else if (value2.getKey().equals(tempGenePair.id)) {

                                consoleBuff.write("value1 = " + value1 + "\n");
                                consoleBuff.write("value2 = " + value2 + "\n");

                                //Estoy en una pareja correspondiente al current geneEvaluePair en value2
                                if (value1.getValue() >= tempGenePair.eValue) {
                                    consoleBuff.write("se borra value1: " + value1.getKey() + "\n");
                                    dismissedGenes.put(value1.getKey(), tempGenePair.id);
                                    anuladosArray.add(value1.getKey());
                                } else {
                                    dismissedGenes.put(tempGenePair.id, value1.getKey());
                                    anuladosArray.add(tempGenePair.id);
                                }

                            }

                        }


                        //ahora borro de la lista los que han sido anulados por el actual
                        for (Iterator<GeneEValuePair> it = treeSet.iterator(); it.hasNext();) {
                            GeneEValuePair tempPair = it.next();
                            for (String string : anuladosArray) {
                                if (tempPair.id.equals(string)) {
                                    consoleBuff.write("borrando " + tempPair.id + "\n");
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
                            if(tempGene.getStatus() == null){
                                tempGene.setStatus(PredictedGene.STATUS_SELECTED);
                            }else if (!tempGene.getStatus().equals(PredictedGene.STATUS_SELECTED_MINOR_THRESHOLD)) {
                                tempGene.setStatus(PredictedGene.STATUS_SELECTED);
                            }
                        }

                        //System.out.println("status2: " + tempGene.getStatus());



                        //tempGene.detach();
                        //contig.addPredictedGene(tempGene);
                    }


                }




                //Una vez que todos los contigs han sido modificados segun los genes se
                //han seleccionado o descartado, los detacheo del xml original y los pongo en el resultado
                Element element = predictedGenesXML.getRoot().getChild(ContigXML.TAG_NAME);
                while (element != null) {
                    element.detach();
                    resultadoGenes.getRoot().addContent(element);
                    element = predictedGenesXML.getRoot().getChild(ContigXML.TAG_NAME);
                }

                //-----------OBTENCION DE LOS RNAS----------------
                System.out.println("Obteniendo los RNAs !");

                reader = new BufferedReader(new FileReader(rnaFile));
                stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //Cerrar archivo de entrada blastouput
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
                                HspSet hspSet = new HspSet();
                                hspSet.addHsp(hsp);
                                PredictedRna rna = new PredictedRna();
                                rna.setHitDef(iteration.getQueryDef());
                                //se pone en el mismo sitio que el identificador de uniprot
                                //el chorizo del hitdef donde viene el id del elem.genoma y mas cosas
                                rna.setAnnotationUniprotId(hit.getHitDef());
                                rna.setStartPosition(hspSet.getHspQueryFrom());
                                rna.setEndPosition(hspSet.getHspQueryTo());
                                rna.setEvalue(hspSet.getEvalue());
                                rna.setStrand(hspSet.getOrientation());
                                rna.setId("r" + contadorRnas);
                                rnas.add(rna);

                                contadorRnas++;
                            }

                        }

                        System.out.println("rnas.size() = " + rnas.size());

                        //Aqui ya tengo los rnas (de un mismo contig) ordenados por posicion de inicio en el treeset
                        //ahora tengo que quitar los solapamientos que haya
                        PredictedRna[] rnasArray = rnas.toArray(new PredictedRna[rnas.size()]);
                        TreeSet<Integer> indicesABorrar = new TreeSet<Integer>();
                        for (int i = 0; i < rnasArray.length; i++) {
                            PredictedRna predictedRna = rnasArray[i];
                            boolean puedenSolapar = true;
                            for (int j = i + 1; j < rnasArray.length && puedenSolapar; j++) {
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
                                        consoleBuff.write("hay que borrar: \n" + predictedRna + "\n por culpa de: \n" + predictedRna2 + "\n");
                                    } else {
                                        indicesABorrar.add(j);
                                        consoleBuff.write("hay que borrar: \n" + predictedRna2 + "\n por culpa de: \n" + predictedRna + "\n");
                                    }
                                }
                            }
                        }
                        //Ahora tengo que aniadir al contig los rnas que no solapan
                        for (int i = 0; i < rnasArray.length; i++) {
                            PredictedRna predictedRna = rnasArray[i];
                            if (!indicesABorrar.contains(i)) {
                                contig.addPredictedRna(predictedRna);
                            }
                        }

                        //El contig esta completo con sus rnas asi que solo falta attachearlo al array
                        contigsRnas.add(contig);

                    }
                }

                //--------------------------------------------


                //Aniadir los rnas por contig al resultado
                for (ContigXML contig : contigsRnas) {
                    resultadoRnas.addChild(contig);
                }


                //----------OBTENCION DE LAS SECUENCIAS DE LOS GENES Y RNAS---------
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
                //Cerrar archivo de entrada con los contigs
                fnaReader.close();

                //Ahora ya tengo en memoria las secuencias de todos los contigs
                //Asi que vamos a rellenar las secuencias de todos los genes

                //------------------------SEQUENCES PREDICTED GENES---------------------------
                //---------------------------------------------------------------------------

                List<XMLElement> genesContigList = resultadoGenes.getChildrenWith(ContigXML.TAG_NAME);
                for (XMLElement xMLElement : genesContigList) {
                    ContigXML contig = new ContigXML(xMLElement.asJDomElement());
                    String contigSequence = contigsMap.get(contig.getId());
                    List<XMLElement> geneList = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement1 : geneList) {
                        PredictedGene gene = new PredictedGene(xMLElement1.asJDomElement());

                        //Si la orientacion es positiva se coge tal cual
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
                                //Ahora traduzco la secuencia a proteina
                                SymbolList symL = DNATools.createDNA(tempSeq);
                                symL = DNATools.toRNA(symL);
                                symL = RNATools.translate(symL);
                                gene.setProteinSequence(symL.seqString());
                            }


                        } else {
                            //Si la orientacion es negativa hay que hacer el circo de la complementaria
                            //invertida y todo eso
                            SymbolList symL = DNATools.createDNA(tempSeq);
                            symL = DNATools.reverseComplement(symL);
                            gene.setSequence(symL.seqString().toUpperCase());

                            if (sePuedeTraducirAProteina) {
                                //Ahora traduzco la secuencia a proteina
                                symL = DNATools.toRNA(symL);
                                symL = RNATools.translate(symL);
                                gene.setProteinSequence(symL.seqString());
                            }

                        }

                        //Ahora saco el id de uniprot del hit y lo pongo en el primer nivel
                        gene.setAnnotationUniprotId(gene.getHspSet().getHit().getUniprotID());
                        //Cambio el hitdef a su sitio
                        gene.setHitDef(gene.getHspSet().getHit().getHitDef());
                        //Cambio tambien el evalue a su sitio
                        gene.setEvalue(gene.getHspSet().getEvalue());
                        //Ahora aprovecho y borro los hspset que no sirven ya para nada
                        gene.asJDomElement().removeChild(HspSet.TAG_NAME);
                    }
                }

                //------------------------SEQUENCES PREDICTED RNAS---------------------------
                //---------------------------------------------------------------------------
                List<XMLElement> rnaContigList = resultadoRnas.getChildrenWith(ContigXML.TAG_NAME);
                for (XMLElement xMLElement : rnaContigList) {
                    ContigXML contig = new ContigXML(xMLElement.asJDomElement());
                    System.out.println("Obteniendo secuencia del contig: " + contig.getId());
                    String contigSequence = contigsMap.get(contig.getId());
                    List<XMLElement> rnaList = contig.getChildrenWith(PredictedRna.TAG_NAME);
                    for (XMLElement xMLElement1 : rnaList) {
                        PredictedRna rna = new PredictedRna(xMLElement1.asJDomElement());

                        //Si la orientacion es positiva se coge tal cual
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
                            //Si la orientacion es negativa hay que hacer el circo de la complementaria
                            //invertida y todo eso
                            SymbolList symL = DNATools.createDNA(tempSeq);
                            symL = DNATools.reverseComplement(symL);
                            rna.setSequence(symL.seqString());
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

                System.out.println("Fichero xml de salida creado con el nombre: " + outFileString);
                //System.out.println("Numero de solapamientos detectados: " + contadorSolapamientos);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }
}
