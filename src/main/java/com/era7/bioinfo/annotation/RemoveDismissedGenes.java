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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.ohnosequences.util.Executable;
import com.ohnosequences.xml.api.model.XMLElement;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class RemoveDismissedGenes implements Executable{
    
    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for(int i=0;i<array.size();i++){
            args[i] = array.get(i);
        }
        main(args);
    }
    
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("This program expects two parameters: \n"
                    + "1. Input annotation XML file \n"
                    + "2. Output filename for annotation XML file without dismissed genes\n");
        } else {
            
            String inFileString = args[0];
            String outFileString = args[1];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);
            
            try{
                
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                
                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //closing input file reader
                reader.close();

                System.out.println("reading annotation file...");
                
                Annotation annotation = new Annotation(stBuilder.toString());
                
                System.out.println("removing dismissed genes....");
                
                List<Element> contigsGenes = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                for (Element element : contigsGenes) {
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());
                        if(gene.getStatus().equals(PredictedGene.STATUS_DISMISSED)){
                            contig.asJDomElement().removeContent(gene.asJDomElement());
                        }
                    }       
                }
                 
                System.out.println("writing output file...");
                
                outBuff.write(annotation.toString());
                                
                outBuff.close();
                
                System.out.println("file created successfully! :)");
                
            }catch(Exception e){
                e.printStackTrace();
            }
            
            
        }
    }
    
}
