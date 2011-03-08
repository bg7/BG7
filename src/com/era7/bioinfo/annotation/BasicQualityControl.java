/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class BasicQualityControl implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {


        if (args.length != 4) {
            System.out.println("El programa espera cuatro parametros: \n"
                    + "1. Nombre del archivo xml con los genes iniciales predichos \n"
                    + "2. Nombre del archivo xml con los genes eliminados por el programa 'EliminaGenesDuplicados'\n"
                    + "3. Nombre del archivo xml con los genes resultantes tras eliminar los duplicados\n"
                    + "4. Nombre del archivo txt con el resultado del control de calidad basico\n");
        } else {


            try {

                File genesPredichosFile, eliminadosFile, sinDuplicadosFile, outFile;
                
                genesPredichosFile = new File(args[0]);
                eliminadosFile = new File(args[1]);
                sinDuplicadosFile = new File(args[2]);
                outFile = new File(args[3]);

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

                ArrayList<String> idsGenesPredichos = new ArrayList<String>();
                ArrayList<String> idsGenesEliminados = new ArrayList<String>();
                ArrayList<String> idsGenesResultantes = new ArrayList<String>();

                //------------GENES PREDICHOS-------------------------

                BufferedReader bufferedReader = new BufferedReader(new FileReader(genesPredichosFile));
                StringBuilder stBuilder = new StringBuilder();
                String line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();
                
                System.out.println("Construyendo el xml de genes predichos...");
                XMLElement xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("ya!");
                System.out.println("Extrayendo ids...");
                List<Element> contigList = xMLElement.asJDomElement().getChildren(ContigXML.TAG_NAME);
                for (Element element : contigList) {
                    List<Element> genes = element.getChildren(PredictedGene.TAG_NAME);
                    for (Element element1 : genes) {
                        PredictedGene gene = new PredictedGene(element1);
                        idsGenesPredichos.add(gene.getId());
                    }
                }
                xMLElement = null;
                contigList = null;
                System.out.println("ya!");
                outBuff.write("Numero de genes predichos inicialmente: " + idsGenesPredichos.size() + "\n");

                //------------GENES ELIMINADOS-------------------------

                bufferedReader = new BufferedReader(new FileReader(eliminadosFile));
                stBuilder = new StringBuilder();
                line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();

                System.out.println("Construyendo el xml de genes eliminados...");
                xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("ya!");
                System.out.println("Extrayendo ids...");
                contigList = xMLElement.asJDomElement().getChildren(ContigXML.TAG_NAME);
                //System.out.println("contigList.size() = " + contigList.size());
                for (Element element : contigList) {
                    List<Element> genes = element.getChildren(PredictedGene.TAG_NAME);
                    //System.out.println("genes.size() = " + genes.size());
                    for (Element element1 : genes) {
                        PredictedGene gene = new PredictedGene(element1);
                        idsGenesEliminados.add(gene.getId());
                    }
                }
                xMLElement = null;
                System.out.println("ya!");
                outBuff.write("Numero de genes eliminados por 'EliminaGenesDuplicados': " + idsGenesEliminados.size() + "\n");

                //------------GENES RESULTANTES-------------------------

                bufferedReader = new BufferedReader(new FileReader(sinDuplicadosFile));
                stBuilder = new StringBuilder();
                line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();

                System.out.println("Construyendo el xml de genes resultantes sin duplicados...");
                xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("ya!");
                System.out.println("Extrayendo ids...");
                contigList = xMLElement.asJDomElement().getChildren(ContigXML.TAG_NAME);
                for (Element element : contigList) {
                    List<Element> genes = element.getChildren(PredictedGene.TAG_NAME);
                    for (Element element1 : genes) {
                        PredictedGene gene = new PredictedGene(element1);
                        idsGenesResultantes.add(gene.getId());
                    }
                }
                xMLElement = null;
                System.out.println("ya!");
                outBuff.write("Numero de genes resultantes tras ejecutar 'EliminaGenesDuplicados': " + idsGenesResultantes.size() + "\n");
                int suma = (idsGenesEliminados.size() + idsGenesResultantes.size());
                outBuff.write("eliminados: " + idsGenesEliminados.size() + " + resultantes: " + idsGenesResultantes.size() + " = " +
                 suma + "\n");
                if(idsGenesPredichos.size() == suma){
                    outBuff.write("La suma coincide! :)\n");
                }else{
                    outBuff.write("La suma NO coincide! :(\n");
                }


                outBuff.close();


            } catch (Exception e) {
                e.printStackTrace();
            }





        }
    }
}
