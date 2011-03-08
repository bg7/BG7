/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.uniprot.UniprotProteinRetreiver;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
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
public class FillDataFromUniprot implements Executable {

    

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("El programa espera dos parametros: \n"
                    + "1. Nombre del archivo xml con los genes \n"
                    + "2. Nombre del arhicvo xml con los datos de uniprot de los genes rellenos\n");
        } else {

            String inFileString = args[0];
            String outFileString = args[1];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);

            try {

                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Cerrar archivo de entrada
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());
                List<Element> contigs = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                int contadorContigs = 0;

                for (Element element : contigs) {
                    System.out.println("Hay = " + contigs.size() + " contigs que rellenar...");
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());

                        //System.out.println("gene.getAnnotationUniprotId() = " + gene.getAnnotationUniprotId());

                        gene = UniprotProteinRetreiver.getUniprotDataFor(gene,false);

                        //System.out.println("gene = " + gene);

                        System.out.println("gen = " + gene.getAnnotationUniprotId() + " relleno!");
                    }

                    contadorContigs++;
                    System.out.println("Llevo " + contadorContigs + " contigs rellenos");


                }

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                outBuff.write(annotation.toString());
                outBuff.close();

                System.out.println("Acabeeee!!! :D");


            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }

        }
    }
}
