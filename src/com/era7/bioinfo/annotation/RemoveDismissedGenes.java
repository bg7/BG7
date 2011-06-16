/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
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
 * @author Pablo Pareja Tobes <ppareja@era7.com>
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
