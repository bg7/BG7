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
package com.ohnosequences.bg7.gb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ohnosequences.util.Executable;
import com.ohnosequences.util.model.Feature;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class Control5ColumnsGenBankFilesQuality implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("This program expects three parameters: \n"
                    + "1. Annotation XML file\n"
                    + "2. 5 columns TBL file\n"
                    + "3. Prefix used in TBL file\n");
        } else {

            File xmlFile = new File(args[0]);
            File tblFile = new File(args[1]);
            String prefix = args[2];
            
            try{
                
                BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while((line = reader.readLine()) != null){
                    stBuilder.append(line);
                }
                reader.close();
                
                Annotation annotation = new Annotation(stBuilder.toString());
                
                PredictedGenes genes = annotation.getPredictedGenes();
                PredictedRnas rnas = annotation.getPredictedRnas();
                
                List<ContigXML> geneContigs = genes.getContigs();
                List<ContigXML> rnaContigs = rnas.getContigs();
                
                HashMap<String,HashMap<String,Feature>> tblMap = new HashMap<String, HashMap<String, Feature>>();
                
                HashMap<String,Feature> featuresMap = null;
                String currentContigId = null;
                
                reader = new BufferedReader(new FileReader(tblFile));
                line = reader.readLine();
                
                while(line != null){                    
                    if(line.startsWith(">Features ")){
                        
                        currentContigId = line.split(">Features")[1].trim();                        
                        
                        featuresMap = new HashMap<String, Feature>();                        
                        tblMap.put(currentContigId, featuresMap);
                        
                        line = reader.readLine();
                        
                        do{
                            
                            String[] columns = line.split("\t");
                            int begin = Integer.parseInt(columns[0]);
                            int end = Integer.parseInt(columns[1]);
                            String featureType = columns[2];
                            
                            line = reader.readLine();                            
                            String featureId = line.split("\t")[4].trim().split(prefix)[1];
                            Feature feature = new Feature(begin, end);
                            feature.setId(featureId);
                            feature.setType(featureType);
                            featuresMap.put(featureId, feature);
                            
                            line = reader.readLine();
                            
                        }while(line != null && !line.startsWith(">Features "));
                                                
                    }   
                }
                reader.close();
                
                //Now all features should be stored in the map
                //let's search for all correspondances between the two files/formats
                
                
                //-----------------------------CHECKING GENES-----------------------------
                System.out.println("Checking contig genes....");
                for (ContigXML contigXML : geneContigs) {
                    
                    System.out.println("currentContigId = " + contigXML.getId());
                    
                    HashMap<String,Feature> tempMap = tblMap.get(contigXML.getId());
                    if(tempMap == null){
                        System.out.println("Error! the contig: " + contigXML.getId() + " was not found in tbl file... :(");
                    }else{
                        List<Element> geneList = contigXML.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                        for (Element element : geneList) {
                            PredictedGene gene = new PredictedGene(element);
                            Feature geneFeature = tempMap.get(gene.getId());
                            
                            if(geneFeature == null){
                                System.out.println("Error! the gene with id: " + gene.getId() + " was not found in tbl file.... :(");
                            }else{
                                if(gene.getStartPosition() != geneFeature.getBegin()){
                                    System.out.println("Error! start position in gene with id: " + gene.getId() + " is different in tbl file.... :(");
                                }else if(gene.getEndPosition() != geneFeature.getEnd()){
                                    System.out.println("Error! end position in gene with id: " + gene.getId() + " is different in tbl file.... :(");
                                }
                                
                                if((gene.getStrand().equals(PredictedGene.POSITIVE_STRAND) && geneFeature.getBegin() > geneFeature.getEnd()) ||
                                        (gene.getStrand().equals(PredictedGene.NEGATIVE_STRAND) && geneFeature.getBegin() < geneFeature.getEnd())){
                                    System.out.println("Error! Strand found in xml for gene with id: " + gene.getId() + 
                                            " differs from position order found in tbl file.... :(");
                                }
                            }
                        }
                    }                    
                }
                //-----------------------------CHECKING RNAS-----------------------------
                System.out.println("Checking contig rnas....");
                for (ContigXML contigXML : rnaContigs) {
                    
                    
                    System.out.println("currentContigId = " + contigXML.getId());                   
                        
                    
                    List<Element> rnaList = contigXML.asJDomElement().getChildren(PredictedRna.TAG_NAME);                    
                    
                    HashMap<String,Feature> tempMap = tblMap.get(contigXML.getId());
                    
                    if(tempMap == null){
                        System.out.println("Error! the contig: " + contigXML.getId() + " was not found in tbl file... :(");
                        System.out.println("rnaList.size() = " + rnaList.size());
                    }else{                        
                        for (Element element : rnaList) {
                            PredictedRna rna = new PredictedRna(element);
                            Feature rnaFeature = tempMap.get(rna.getId());
                            
                            if(rnaFeature == null){
                                System.out.println("Error! the rna with id: " + rna.getId() + " was not found in tbl file.... :(");
                            }else{
                                if(rna.getStartPosition() != rnaFeature.getBegin()){
                                    System.out.println("Error! start position in rna with id: " + rna.getId() + " is different in tbl file.... :(");
                                }else if(rna.getEndPosition() != rnaFeature.getEnd()){
                                    System.out.println("Error! end position in rna with id: " + rna.getId() + " is different in tbl file.... :(");
                                }
                                
                                if((rna.getStrand().equals(PredictedRna.POSITIVE_STRAND) && rnaFeature.getBegin() > rnaFeature.getEnd()) ||
                                        (rna.getStrand().equals(PredictedRna.NEGATIVE_STRAND) && rnaFeature.getBegin() < rnaFeature.getEnd())){
                                    System.out.println("Error! Strand found in xml for rna with id: " + rna.getId() + 
                                            " differs from position order found in tbl file.... :(");
                                }
                            }
                        }
                    }
                    
                }
                
                
                System.out.println("All tests passed successfully ! :D");
                
                
                
                
            }catch(Exception e){
                e.printStackTrace();
            }
            
           
            
            
            
        }
    }
}
