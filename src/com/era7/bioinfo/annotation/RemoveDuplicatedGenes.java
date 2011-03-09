/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.model.Feature;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
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
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class RemoveDuplicatedGenes implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 3 && args.length != 4) {
            System.out.println("This program expects three parameters: \n"
                    + "1. Predicted genes XML input file\n"
                    + "2. Output XML file without duplicates \n"
                    + "3. Output XML file including removed genes.");
        } else {
            String inFileString = args[0];
            String outFileNoDuplicadosString = args[1];
            String outFileGenesElminadosString = args[2];

            String preferentOrganism = null;

            if (args.length == 4) {
                preferentOrganism = args[3];
            }


            try {

                File inFile, outFileNoDuplicados, outFileGenesEliminados;

                inFile = new File(inFileString);

                //temporal
                File consoleFile = new File("console.txt");
                BufferedWriter consoleBuff = new BufferedWriter(new FileWriter(consoleFile));


                //XMLElement holding predicted genes without duplicates
                PredictedGenes predictedGenesResult = new PredictedGenes();
                //XMLElement including removed predicted genes
                PredictedGenes removedGenes = new PredictedGenes();


                //Reading data from input xml file
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //closing input file reader
                reader.close();

                System.out.println("Creating predictedGenesXML....");
                PredictedGenes predictedGenesXML = new PredictedGenes(stBuilder.toString());
                System.out.println("Done! :)");

                //Copying difSpan & gene threshold values from the input xml file
                int difSpan = predictedGenesXML.getDifSpan();
                int threshold = predictedGenesXML.getGeneThreshold();
                predictedGenesResult.setDifSpan(difSpan);
                removedGenes.setDifSpan(difSpan);
                predictedGenesResult.setGeneThreshold(threshold);
                removedGenes.setGeneThreshold(threshold);


                int contadorEliminados = 0;

                //Map with every predicted gene mixed up (gene id --> gene)
                HashMap<String, String> todosLosGenes = new HashMap<String, String>();
                List<Element> listContigs = predictedGenesXML.getRoot().getChildren(ContigXML.TAG_NAME);


                //Checking if a preferent organism was specified or the default one
                // should be used instead
                String goodOrganism = predictedGenesXML.getPreferentOrganism();
                if (preferentOrganism != null) {
                    goodOrganism = preferentOrganism;
                }

                predictedGenesResult.setPreferentOrganism(goodOrganism);


                for (int index = 0; index < listContigs.size(); index++) {
                    Element elem = listContigs.get(index);

                    ContigXML currentContig = new ContigXML(elem);
                    String currentContigId = currentContig.getId();

                    List<Element> listGenes = currentContig.getRoot().getChildren(PredictedGene.TAG_NAME);
                    System.out.println("currentContigId = " + currentContigId);
                    System.out.println("listGenes.size() = " + listGenes.size());

                    TreeSet<Feature> genesSet = new TreeSet<Feature>();

                    for (int indexGene = 0; indexGene < listGenes.size(); indexGene++) {

                        Element geneElem = listGenes.get(indexGene);
                        PredictedGene tempGene = new PredictedGene(geneElem);

                        todosLosGenes.put(tempGene.getId(), tempGene.toString());

                        Feature f = new Feature();
                        if (tempGene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                            f.setBegin(tempGene.getStartPosition());
                            f.setEnd(tempGene.getEndPosition());
                        } else {
                            f.setBegin(tempGene.getEndPosition());
                            f.setEnd(tempGene.getStartPosition());
                        }
                        f.seteValue(tempGene.getHspSet().getEvalue());
                        f.setId(tempGene.getId());
                        f.setOrganism(tempGene.getOrganism());
                        genesSet.add(f);

                        if (listGenes.size() % 1000 == 0) {
                            System.out.println("Creating features and genes, " + listGenes.size() + " are left for the contig: " + currentContigId);
                        }
                    }


                    ArrayList<String> idsGenesBorradosDelContig = new ArrayList<String>();
                    ArrayList<String> idsGenesSinBorrarDelContig = new ArrayList<String>();

                    System.out.println("Removing duplicated genes from the contig: " + currentContigId);
                    consoleBuff.write("Removing duplicated genes from the contig: " + currentContigId);

                    while (genesSet.size() > 0) {
                        //The first element is selected by position
                        Feature firstFeature = genesSet.pollFirst();
                        consoleBuff.write("firstFeature = " + firstFeature.getId() + "\n");
                        //Now I loop through the rest looking for genes that might be included in this one
                        boolean borrado = false;
                        for (Iterator<Feature> it = genesSet.iterator(); it.hasNext() && !borrado;) {
                            Feature feature = it.next();
                            if (feature.getEnd() <= firstFeature.getEnd()) {
                                consoleBuff.write("FirstFeature = " + firstFeature.getId() + "," + firstFeature.geteValue() + " feature = " + feature.getId() + "," + feature.geteValue() + "\n");
                                //The one with a bigger eValue must be removed, if they have the same value
                                //it depends on their organism
                                if (feature.geteValue() < firstFeature.geteValue()) {
                                    consoleBuff.write("Removing firstFeature \n");
                                    idsGenesBorradosDelContig.add(firstFeature.getId());
                                    borrado = true;
                                } else if (firstFeature.geteValue() < feature.geteValue()) {
                                    consoleBuff.write("Removing feature \n");
                                    idsGenesBorradosDelContig.add(feature.getId());
                                    it.remove();
                                } else {
                                    //checking their organism (they have the same eValue)

                                    consoleBuff.write("There are two with the same e, the organism that predominates is: " + goodOrganism);
                                    consoleBuff.write(" firstFeature.getOrganism() = " + firstFeature.getOrganism());
                                    consoleBuff.write(" feature.getOrganism() = " + feature.getOrganism() + "\n");

                                    if (feature.getOrganism().equals(goodOrganism)) {
                                        idsGenesBorradosDelContig.add(firstFeature.getId());
                                        borrado = true;
                                        consoleBuff.write(" removing firstFeature " + "(" + firstFeature.getId() + ")\n");
                                    } else {
                                        idsGenesBorradosDelContig.add(feature.getId());
                                        it.remove();
                                        consoleBuff.write(" removing feature" + "(" + feature.getId() + ")\n");
                                    }
                                }
                            }

                        }
                        if (!borrado) {
                            idsGenesSinBorrarDelContig.add(firstFeature.getId());
                        }
                        System.out.println("There are " + genesSet.size() + " genes left to be analyzed from the contig: " + currentContigId);
                        consoleBuff.write("There are " + genesSet.size() + " genes left to be analyzed from the contig: " + currentContigId);


                    }

                    System.out.println("There were " + idsGenesBorradosDelContig.size() + " genes detected that must be removed...");
                    consoleBuff.write("There were " + idsGenesBorradosDelContig.size() + " genes detected that must be removed...");

                    contadorEliminados += idsGenesBorradosDelContig.size();

                    //removing genes
                    if (idsGenesBorradosDelContig.size() > 0) {
                        ContigXML contig = new ContigXML();
                        contig.setId(currentContigId);
                        for (int indexBorrar = 0; indexBorrar < idsGenesBorradosDelContig.size(); indexBorrar++) {
                            String string = idsGenesBorradosDelContig.get(indexBorrar);
                            consoleBuff.write("borrando " + string + "\n");
                            //consoleBuff.flush();
                            //System.out.println("string = " + string);
                            PredictedGene pg = new PredictedGene(todosLosGenes.get(string));
                            pg.detach();
                            contig.addChild(pg);
                            todosLosGenes.remove(string);

                        }
                        removedGenes.addChild(contig);
                    }


                    //resulting genes are added to the result
                    if (idsGenesSinBorrarDelContig.size() > 0) {
                        ContigXML contig = new ContigXML();
                        contig.setId(currentContigId);

                        for (int indexAgregar = 0; indexAgregar < idsGenesSinBorrarDelContig.size(); indexAgregar++) {
                            String string = idsGenesSinBorrarDelContig.get(indexAgregar);
                            consoleBuff.write("adding to final result: " + string + "\n");
                            //consoleBuff.flush();
                            System.out.println("string = " + string);
                            PredictedGene pg = new PredictedGene(todosLosGenes.get(string));
                            pg.detach();
                            contig.addChild(pg);
                            todosLosGenes.remove(string);
                        }
                        predictedGenesResult.addChild(contig);
                    }
                }

                //temporal
                consoleBuff.close();

                outFileNoDuplicados = new File(outFileNoDuplicadosString);
                FileWriter fileWriter = new FileWriter(outFileNoDuplicados);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                buffWriter.write(predictedGenesResult.toString());

                buffWriter.close();
                fileWriter.close();

                outFileGenesEliminados = new File(outFileGenesElminadosString);
                FileWriter fileWriterElminados = new FileWriter(outFileGenesEliminados);
                BufferedWriter buffWriterEliminados = new BufferedWriter(fileWriterElminados);
                buffWriterEliminados.write(removedGenes.toString());

                buffWriterEliminados.close();
                fileWriterElminados.close();

                System.out.println("Number of genes which were removed: " + contadorEliminados);
                System.out.println("XML output file created with the name: " + outFileNoDuplicadosString);
                System.out.println("XML output file created with the name: " + outFileGenesElminadosString);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
