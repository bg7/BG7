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

import com.era7.bioinfo.bio4jmodel.nodes.*;
import com.era7.bioinfo.bio4jmodel.nodes.citation.ArticleNode;
import com.era7.bioinfo.bio4jmodel.relationships.comment.DomainCommentRel;
import com.era7.bioinfo.bio4jmodel.relationships.comment.FunctionCommentRel;
import com.era7.bioinfo.bio4jmodel.relationships.comment.PathwayCommentRel;
import com.era7.bioinfo.bio4jmodel.relationships.comment.SimilarityCommentRel;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.NodeRetriever;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class FillDataFromBio4j implements Executable {

    

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
                    + "1. Bio4j DB root folder \n"
                    + "2. Name of the XML file with predicted genes \n"
                    + "3. Output XML filename with uniprot data incorporated\n");
        } else {

            String bio4jFolder = args[0];
            String inFileString = args[1];
            String outFileString = args[2];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);

            Bio4jManager bio4jManager = null;
            
            try {
                
                bio4jManager = new Bio4jManager(bio4jFolder);
                
                NodeRetriever nodeRetriever = new NodeRetriever(bio4jManager);

                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //closing input file
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());
                List<Element> contigs = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                int contadorContigs = 0;
                

                for (Element element : contigs) {
                    System.out.println("There are = " + contigs.size() + " contigs to be completed with uniprot data...");
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());

                        ProteinNode proteinNode = nodeRetriever.getProteinNodeByAccession(gene.getAnnotationUniprotId());

                        completePredicteedGeneData(proteinNode, gene);
                        
                        System.out.println("gene = " + gene.getAnnotationUniprotId() + " completed!");
                    }

                    contadorContigs++;
                    System.out.println(contadorContigs + " contigs already completed");


                }

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                outBuff.write(annotation.toString());
                outBuff.close();

                System.out.println("Done!!! :D");


            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }finally{
                bio4jManager.shutDown();
            }

        }
    }
    
    private static void completePredicteedGeneData(ProteinNode protein, PredictedGene gene){
        
        System.out.println("retrieving data from: " + gene.getAnnotationUniprotId());
        
        gene.setAccession(protein.getAccession());
        gene.setLength(protein.getLength());
        gene.setOrganism(protein.getOrganism().getScientificName());
        gene.setSequence(protein.getSequence());
        //------------protein names---------------
        String proteinNamesSt = "";
        ArrayList<String> names = new ArrayList<String>();        
        if(!protein.getName().isEmpty()){
            names.add(protein.getName());
        }
        if(!protein.getFullName().isEmpty()){
            names.add(protein.getFullName());
        }
        if(!protein.getShortName().isEmpty()){
            names.add(protein.getShortName());
        }
        for (String nameSt : names) {
            proteinNamesSt += nameSt + ",";
        }
        if(proteinNamesSt.length() > 0){
            proteinNamesSt = proteinNamesSt.substring(0, proteinNamesSt.length() - 1);
        }
        gene.setProteinNames(proteinNamesSt);
        //--------------------------------------
        
        
        //--------protein family----------
        List<SimilarityCommentRel> similarityList = protein.getSimilarityComment();
        String familySt = "";
        for (SimilarityCommentRel similarityCommentRel : similarityList) {
            String textSt = similarityCommentRel.getText();            
            if(textSt.toLowerCase().indexOf("belongs to") >= 0){
                familySt += textSt + ",";
            }
        }
        if(!familySt.isEmpty()){
            familySt = familySt.substring(0,familySt.length() - 1);
        }
        gene.setProteinFamily(familySt);
        
        //-----------------------------
        
        //-----subcellular locations------
        String subcellLocsSt = "";
        for (SubcellularLocationNode subcellNode : protein.getSubcellularLocations()) {
            subcellLocsSt += subcellNode.getName() + ", ";
        }
        if(subcellLocsSt.length() > 0){
            subcellLocsSt = subcellLocsSt.substring(0,subcellLocsSt.length() - 2);
        }               
        gene.setSubcellularLocations(subcellLocsSt);
        
        //------go annotations------------
        String goAnnotationsIdsSt = "";
        String goAnnotationsSt = "";
        for (GoTermNode goTerm : protein.getGOAnnotations()) {
            goAnnotationsSt += goTerm.getName() + ", ";
            goAnnotationsIdsSt += goTerm.getId() + ", ";
        }
        if(goAnnotationsSt.length() > 0){
            goAnnotationsSt = goAnnotationsSt.substring(0,goAnnotationsSt.length() - 2);
        }   
        if(goAnnotationsIdsSt.length() > 0){
            goAnnotationsIdsSt = goAnnotationsIdsSt.substring(0,goAnnotationsIdsSt.length() - 2);
        }
        gene.setGeneOntology(goAnnotationsSt);
        gene.setGeneOntologyId(goAnnotationsIdsSt);
        
        //------keywords------------
        String keywordsSt = "";
        for (KeywordNode keyword : protein.getKeywords()) {
            keywordsSt += keyword.getId() + ", ";
        }
        if(keywordsSt.length() > 0){
            keywordsSt = keywordsSt.substring(0,keywordsSt.length() - 2);
        }               
        gene.setKeywords(keywordsSt);
        
        //------interpro------------
        String interproSt = "";
        for (InterproNode interpro : protein.getInterpro()) {
            interproSt += interpro.getId() + ", ";
        }
        if(interproSt.length() > 0){
            interproSt = interproSt.substring(0,interproSt.length() - 2);
        }               
        gene.setInterpro(interproSt);
        
        
        //----gene-names-------
        String geneNamesSt = "";
        for (String st : protein.getGeneNames()) {
            geneNamesSt += st + ", ";
        }
        if(geneNamesSt.length() > 0){
            geneNamesSt = geneNamesSt.substring(0,geneNamesSt.length() - 2);
        }               
        gene.setGeneNames(geneNamesSt);
        
        //-------comment domains-------
        String domainsSt = "";
        for (DomainCommentRel domainRel : protein.getDomainComment()) {
            domainsSt += domainRel.getText() + ", ";
        }
        if(domainsSt.length() > 0){
            domainsSt = domainsSt.substring(0,domainsSt.length() - 2);
        }
        gene.setDomains(domainsSt);
        
        //-------comment pathway-------
        String pathwaySt = "";
        for (PathwayCommentRel domainRel : protein.getPathwayComment()) {
            pathwaySt += domainRel.getText() + ", ";
        }
        if(pathwaySt.length() > 0){
            pathwaySt = pathwaySt.substring(0,pathwaySt.length() - 2);
        }
        gene.setPathway(pathwaySt);
        
        //-------comment function-------
        String functionSt = "";
        for (FunctionCommentRel functionRel : protein.getFunctionComment()) {
            functionSt += functionRel.getText() + ", ";
        }
        if(functionSt.length() > 0){
            functionSt = functionSt.substring(0,functionSt.length() - 2);
        }
        gene.setCommentFunction(functionSt);
        
        //-----citations-------
        String pubmedIdSt = "";
        List<ArticleNode> citations = protein.getArticleCitations();
        for (ArticleNode articleNode : citations) {
            pubmedIdSt += articleNode.getPubmedId() + ", ";
        }
        if(pubmedIdSt.length() > 0){
            pubmedIdSt = pubmedIdSt.substring(0,pubmedIdSt.length() - 2);
        }
        gene.setPubmedId(pubmedIdSt);
        
    }
}
