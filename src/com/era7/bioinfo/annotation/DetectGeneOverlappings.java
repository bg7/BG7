/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Overlap;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class DetectGeneOverlappings implements Executable{

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for(int i=0;i<array.size();i++){
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 2) {
            System.out.println("El programa espera dos parametros: \n"
                    + "1. Nombre del archivo xml de entrada con los genes predichos \n"
                    + "2. Nombre del archivo xml con la salida de los solapamientos");
        } else {
            String inFileString = args[0];
            String outFileString = args[1];

            try {

                File inFile,outFile;
                inFile = new File(inFileString);
                outFile = new File(outFileString);

                

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
                XMLElement overlaps = new XMLElement(new Element("overlaps"));

                int contadorSolapamientos = 0;

                //Ahora tengo que agrupar los predicted genes por scaffolds
                //para eso hago un mapa de scaffold id a array de predicted genes
                HashMap<String, ArrayList<PredictedGene>> map = new HashMap<String, ArrayList<PredictedGene>>();

                //Rellenando los arrays de los diferentes scaffolds/contigs
                List<Element> list = predictedGenesXML.getRoot().getChildren(PredictedGene.TAG_NAME);

                for (int i=0;i<list.size();i++) {
                    Element elem = list.get(i);
                    PredictedGene tempGene = new PredictedGene(elem);
                    //System.out.println("tempGene.getId() = " + tempGene.getId());
                    String scaffoldId = tempGene.getHspSet().getHit().getScaffoldIDFromHitDef();
                    
                    ArrayList<PredictedGene> arrayGenes = map.get(scaffoldId);

                    if (arrayGenes == null) {
                        //System.out.println("arrayGenes = " + arrayGenes);
                        arrayGenes = new ArrayList<PredictedGene>();
                        arrayGenes.add(tempGene);
                        map.put(scaffoldId, arrayGenes);
                    } else {
                        arrayGenes.add(tempGene);
                    }
                }

                //System.exit(-1);

                for (String key : map.keySet()) {

                    int contadorScaffold = 0;
                    System.out.println("Detectando solapamientos en scaffold: " + key);

                    ArrayList<PredictedGene> array = map.get(key);

                    for (int i = 0; i < array.size() - 1; i++) {

                        PredictedGene tempGene1 = array.get(i);
                        int initGene1 = tempGene1.getStartPosition();
                        int stopGene1 = tempGene1.getEndPosition();
                        //------Si la orientacion es negativa son al reves----
                        if (!tempGene1.getHspSet().getOrientation()) {
                            int tempSwap = initGene1;
                            initGene1 = stopGene1;
                            stopGene1 = tempSwap;
                        }


                        for (int j = i + 1; j < array.size(); j++) {                            

                            PredictedGene tempGene2 = array.get(j);
                            int initGene2 = tempGene2.getStartPosition();
                            int stopGene2 = tempGene2.getEndPosition();
                            //------Si la orientacion es negativa son al reves----
                            if (!tempGene2.getHspSet().getOrientation()) {
                                int tempSwap = initGene2;
                                initGene2 = stopGene2;
                                stopGene2 = tempSwap;
                            }                            

                            if ( (stopGene1 >= initGene2) && (initGene1 <= initGene2)) {
                                System.out.println("stopGene1 = " + stopGene1);
                                System.out.println("initGene2 = " + initGene2);
                                System.out.println("[" + tempGene1.getId() + "] solapa con ["
                                        + tempGene2.getId() + "]");

                                Overlap overlap = new Overlap();
                                PredictedGene gene1 = new PredictedGene();
                                gene1.setId(tempGene1.getId());
                                PredictedGene gene2 = new PredictedGene();
                                gene2.setId(tempGene2.getId());
                                overlap.addChild(gene1);
                                overlap.addChild(gene2);
                                overlaps.addChild(overlap);

                                contadorScaffold++;
                                contadorSolapamientos++;
                            }

                            
                        }
                    }

                    System.out.println("contadorScaffold = " + contadorScaffold);

                }

                FileWriter fileWriter = new FileWriter(outFile);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                buffWriter.write(overlaps.toString());
                buffWriter.close();
                fileWriter.close();

                System.out.println("Fichero xml de salida creado con el nombre: " + outFileString);
                System.out.println("Numero de solapamientos detectados: " + contadorSolapamientos);

            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
