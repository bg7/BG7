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
 * @author ppareja
 */
public class EliminaGenesDuplicados implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 3 && args.length != 4) {
            System.out.println("El programa espera tres parametros: \n"
                    + "1. Nombre del archivo xml de entrada con los genes predichos \n"
                    + "2. Nombre del archivo xml de salida sin duplicados \n"
                    + "3. Nombre del archivo xml de salida con los genes eliminados.");
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


                //XMLElement que incluira los genes predichos sin duplicados
                PredictedGenes predictedGenesResult = new PredictedGenes();
                //XMLElement que incluira los genes que han sido eliminados
                PredictedGenes removedGenes = new PredictedGenes();


                //Leer datos archivo xml con predicted genes
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //Cerrar archivo de entrada blastouput
                reader.close();

                System.out.println("Creando predictedGenesXML....");
                PredictedGenes predictedGenesXML = new PredictedGenes(stBuilder.toString());
                System.out.println("Ya! :)");

                //copio los valores de difSpan y gene threshold del xml de entrada
                int difSpan = predictedGenesXML.getDifSpan();
                int threshold = predictedGenesXML.getGeneThreshold();
                predictedGenesResult.setDifSpan(difSpan);
                removedGenes.setDifSpan(difSpan);
                predictedGenesResult.setGeneThreshold(threshold);
                removedGenes.setGeneThreshold(threshold);


                int contadorEliminados = 0;

                //map con todos los genes mezclados identificados por su id
                HashMap<String, String> todosLosGenes = new HashMap<String, String>();
                List<Element> listContigs = predictedGenesXML.getRoot().getChildren(ContigXML.TAG_NAME);


                //Hay que ver si se ha especificado un organismo concreto o se usa el por defecto
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
                            System.out.println("Creando features y genes, quedan " + listGenes.size() + " para el contig: " + currentContigId);
                        }
                    }


                    ArrayList<String> idsGenesBorradosDelContig = new ArrayList<String>();
                    ArrayList<String> idsGenesSinBorrarDelContig = new ArrayList<String>();

                    System.out.println("Elminando duplicados del contig: " + currentContigId);
                    consoleBuff.write("Elminando duplicados del contig: " + currentContigId);

                    while (genesSet.size() > 0) {
                        //Cojo el primer elemento por posicion
                        Feature firstFeature = genesSet.pollFirst();
                        consoleBuff.write("firstFeature = " + firstFeature.getId() + "\n");
                        //Ahora recorro el resto y busco genes que esten incluidos dentro
                        boolean borrado = false;
                        for (Iterator<Feature> it = genesSet.iterator(); it.hasNext() && !borrado;) {
                            Feature feature = it.next();
                            if (feature.getEnd() <= firstFeature.getEnd()) {
                                consoleBuff.write("FirstFeature = " + firstFeature.getId() + "," + firstFeature.geteValue() + " feature = " + feature.getId() + "," + feature.geteValue() + "\n");
                                //Ahora hay que borrar el de mayor e, si tienen el mismo valor se mira el organismo
                                if (feature.geteValue() < firstFeature.geteValue()) {
                                    consoleBuff.write("Se borra firstFeature \n");
                                    idsGenesBorradosDelContig.add(firstFeature.getId());
                                    borrado = true;
                                } else if (firstFeature.geteValue() < feature.geteValue()) {
                                    consoleBuff.write("Se borra feature \n");
                                    idsGenesBorradosDelContig.add(feature.getId());
                                    it.remove();
                                } else {
                                    //como tienen la misma e hay que mirar el organismo

                                    consoleBuff.write("Hay dos con la misma e, el organismo que prima es: " + goodOrganism);
                                    consoleBuff.write(" firstFeature.getOrganism() = " + firstFeature.getOrganism());
                                    consoleBuff.write(" feature.getOrganism() = " + feature.getOrganism() + "\n");

                                    if (feature.getOrganism().equals(goodOrganism)) {
                                        idsGenesBorradosDelContig.add(firstFeature.getId());
                                        borrado = true;
                                        consoleBuff.write(" borro firstFeature " + "(" + firstFeature.getId() + ")\n");
                                    } else {
                                        idsGenesBorradosDelContig.add(feature.getId());
                                        it.remove();
                                        consoleBuff.write(" borro feature" + "(" + feature.getId() + ")\n");
                                    }
                                }
                            }

                        }
                        if (!borrado) {
                            idsGenesSinBorrarDelContig.add(firstFeature.getId());
                        }
                        System.out.println("Quedan " + genesSet.size() + " genes por analizar del contig: " + currentContigId);
                        consoleBuff.write("Quedan " + genesSet.size() + " genes por analizar del contig: " + currentContigId);


                    }

                    System.out.println("Se han detectado " + idsGenesBorradosDelContig.size() + " genes que hay que borrar...");
                    consoleBuff.write("Se han detectado " + idsGenesBorradosDelContig.size() + " genes que hay que borrar...");

                    contadorEliminados += idsGenesBorradosDelContig.size();

                    //ahora borro los genes para que no salgan al final
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


                    //ahora agrego al resultado final los genes que han quedado sin borrar
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

                System.out.println("Genes eliminados: " + contadorEliminados);
                System.out.println("XML created with the name: " + outFileNoDuplicadosString);
                System.out.println("XML created with the name: " + outFileGenesElminadosString);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
