/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Frameshift;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
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
public class PatatizaGenes implements Executable{

    public static String SEPARATOR = "\t";
    public static String GENE = "gene";
    public static String RNA = "rna";

    public static String HEADER = "Contig_id" + SEPARATOR + "Gen_id" + SEPARATOR + "Gene/Rna" + SEPARATOR +
                            "start_is_canonical" + SEPARATOR + "start_position" + SEPARATOR +
                            "end_is_canonical" + SEPARATOR + "end_position" + SEPARATOR +
                            "Hit_def" + SEPARATOR + "Similar_to" + SEPARATOR +
                            "Protein names" + SEPARATOR + "Organism" + SEPARATOR +
                            "Comment (FUNCTION)" + SEPARATOR +
                            "EC numbers" + SEPARATOR + "InterPro" + SEPARATOR +
                            "Gene Ontology" + SEPARATOR + "Pathway" + SEPARATOR +
                            "Protein family" + SEPARATOR + "Keywords" + SEPARATOR +
                            "Length" + SEPARATOR + "Subcellular locations" + SEPARATOR +
                            "PubMed ID" + SEPARATOR + "Strand" + SEPARATOR +
                            "Intragenic_stops" + SEPARATOR + "Frameshifts" + SEPARATOR +
                            "Gene status" + SEPARATOR + "gene_dismissed_by" + SEPARATOR +
                            "Evalue" + SEPARATOR + 
                            "Nucleotide sequence" + SEPARATOR + "Aminoacid sequence" + "\n";

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
                    + "1. Nombre del archivo xml con los genes \n"
                    + "2. Nombre del arhicvo txt donde va todo patatizado\n");
        } else {

            String inFileString = args[0];
            String outFileString = args[1];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);

            try {

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));


                //Escribo la cabecera en primer lugar
                outBuff.write(HEADER);

                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Cerrar archivo de entrada
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());


                //-----------PATATIZACION DE LOS GENES-----------------
                List<Element> contigsGenes = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                for (Element element : contigsGenes) {
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());

                        if(!gene.getPubmedId().equals("")){
                            System.out.println("gene.getPubmedId() = " + gene.getPubmedId());
                        }

                        outBuff.write(contig.getId() + SEPARATOR + gene.getId() + SEPARATOR + GENE + SEPARATOR);
                        outBuff.write(gene.getStartIsCanonical() + SEPARATOR + gene.getStartPosition() + SEPARATOR);
                        outBuff.write(gene.getEndIsCanonical() + SEPARATOR + gene.getEndPosition() + SEPARATOR);
                        outBuff.write(gene.getHitDef() + SEPARATOR + gene.getAccession() + SEPARATOR);
                        outBuff.write(gene.getProteinNames() + SEPARATOR + gene.getOrganism() + SEPARATOR);
                        outBuff.write(gene.getCommentFunction() + SEPARATOR + gene.getEcNumbers() + SEPARATOR);
                        //outBuff.write(gene.getEcNumbers() + SEPARATOR);
                        outBuff.write(gene.getInterpro() + SEPARATOR + gene.getGeneOntology() + SEPARATOR);
                        outBuff.write(gene.getPathway() + SEPARATOR + gene.getProteinFamily() + SEPARATOR);
                        outBuff.write(gene.getKeywords() + SEPARATOR + gene.getLength() + SEPARATOR + gene.getSubcellularLocations() + SEPARATOR);
                        outBuff.write(gene.getPubmedId() + SEPARATOR + gene.getStrand() + SEPARATOR);
                        Element extraStops = gene.getExtraStopCodons();

                        if(extraStops != null){
                            outBuff.write(new XMLElement(extraStops).toString() + SEPARATOR);
                        }else{
                            outBuff.write(SEPARATOR);
                        }
                        
                        ArrayList<Frameshift> frameshifts = gene.getFrameshifts();
                        if(frameshifts != null){
                            String f = "";
                            for (Frameshift frameshift : frameshifts) {
                                f += frameshift.toString();
                            }
                            outBuff.write(f + SEPARATOR);
                        }else{
                            outBuff.write(SEPARATOR);
                        }
                        outBuff.write(gene.getStatus() + SEPARATOR + gene.getGeneDismissedBy() + SEPARATOR);
                        //System.out.println("gene = " + gene);
                        outBuff.write(gene.getEvalue() + SEPARATOR);
                        outBuff.write(gene.getSequence() + SEPARATOR + gene.getProteinSequence() + SEPARATOR + "\n");
                    }
                }


                //-----------PATATIZACION DE LOS RNAS-----------------
                List<Element> contigsRnas = annotation.asJDomElement().getChild(PredictedRnas.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                for (Element element : contigsRnas) {
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> rnas = contig.getChildrenWith(PredictedRna.TAG_NAME);
                    for (XMLElement xMLElement : rnas) {
                        PredictedRna rna = new PredictedRna(xMLElement.asJDomElement());
                        outBuff.write(contig.getId() + SEPARATOR + rna.getId() + SEPARATOR + RNA + SEPARATOR);
                        outBuff.write(rna.getStartIsCanonical() + SEPARATOR + rna.getStartPosition() + SEPARATOR);
                        outBuff.write(rna.getEndIsCanonical() + SEPARATOR + rna.getEndPosition() + SEPARATOR);
                        outBuff.write(rna.getHitDef() + SEPARATOR + rna.getAnnotationUniprotId() + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        //outBuff.write(SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR + rna.getStrand() + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(SEPARATOR + SEPARATOR);
                        outBuff.write(rna.getEvalue() + SEPARATOR);
                        outBuff.write(rna.getSequence().toUpperCase() + SEPARATOR + SEPARATOR + "\n");
                    }
                }


                outBuff.close();
                System.out.println("Acabeeee!!! :D");


            }catch(Exception e){
                e.printStackTrace();
            }
            
        }
    }
}
