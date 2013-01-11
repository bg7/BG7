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
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
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
            System.out.println("This program expects four parameters: \n"
                    + "1. XML filename with the initial predicted genes \n"
                    + "2. XML filename with the genes removed by the program 'RemoveDuplicatedGenes'\n"
                    + "3. XML filename with the resulting genes file after removing duplicates\n"
                    + "4. Output TXT filename (results of the basic quality control)\n");
        } else {


            try {

                File predictedGenesFile, removedGenesFile, withoutDuplicatesFile, outFile;
                
                predictedGenesFile = new File(args[0]);
                removedGenesFile = new File(args[1]);
                withoutDuplicatesFile = new File(args[2]);
                outFile = new File(args[3]);

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

                ArrayList<String> idsGenesPredichos = new ArrayList<String>();
                ArrayList<String> idsGenesEliminados = new ArrayList<String>();
                ArrayList<String> idsGenesResultantes = new ArrayList<String>();

                //------------PREDICTED GENES-------------------------

                BufferedReader bufferedReader = new BufferedReader(new FileReader(predictedGenesFile));
                StringBuilder stBuilder = new StringBuilder();
                String line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();
                
                System.out.println("parsing XML file with predicted genes...");
                XMLElement xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("done!");
                System.out.println("Extracting ids...");
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
                System.out.println("done!");
                outBuff.write("Number of genes predicted initially: " + idsGenesPredichos.size() + "\n");

                //------------REMOVED GENES-------------------------

                bufferedReader = new BufferedReader(new FileReader(removedGenesFile));
                stBuilder = new StringBuilder();
                line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();

                System.out.println("parsing removed genes XML file...");
                xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("done!");
                System.out.println("Extracting ids...");
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
                System.out.println("done!");
                outBuff.write("Number of genes removed by the program 'RemoveDuplicatedGenes': " + idsGenesEliminados.size() + "\n");

                //------------RESULTING GENES-------------------------

                bufferedReader = new BufferedReader(new FileReader(withoutDuplicatesFile));
                stBuilder = new StringBuilder();
                line = null;
                while((line = bufferedReader.readLine()) != null){
                    stBuilder.append(line);
                }
                bufferedReader.close();

                System.out.println("parsing the XML file of predicted genes without duplicates...");
                xMLElement = new XMLElement(stBuilder.toString());
                stBuilder.delete(0, stBuilder.length());
                System.out.println("done!");
                System.out.println("Extracting ids...");
                contigList = xMLElement.asJDomElement().getChildren(ContigXML.TAG_NAME);
                for (Element element : contigList) {
                    List<Element> genes = element.getChildren(PredictedGene.TAG_NAME);
                    for (Element element1 : genes) {
                        PredictedGene gene = new PredictedGene(element1);
                        idsGenesResultantes.add(gene.getId());
                    }
                }
                xMLElement = null;
                System.out.println("done!");
                outBuff.write("Resulting number of genes after executing program 'RemoveDuplicatedGenes': " + idsGenesResultantes.size() + "\n");
                int suma = (idsGenesEliminados.size() + idsGenesResultantes.size());
                outBuff.write("removed: " + idsGenesEliminados.size() + " + resulting: " + idsGenesResultantes.size() + " = " +
                 suma + "\n");
                if(idsGenesPredichos.size() == suma){
                    outBuff.write("The sum matchs up! :)\n");
                }else{
                    outBuff.write("The sum does NOT match up! :(\n");
                }
                
                outBuff.close();


            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
