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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class GeneraArchivosFasta implements Executable {

    public static String SEPARATOR = "|";
    public static String HEADER = ">";
    public static int FASTA_LINE_LENGTH = 60;

    public static String DISMISSED_STRING = "Dismissed";
    public static String FASTA_EXTENSION = ".fasta";

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("El programa espera tres parametros: \n"
                    + "1. Nombre del archivo xml de entrada con los genes predichos \n"
                    + "2. Nombre del archivo fasta de salida con las secuencias de nucleotidos\n"
                    + "3. Nombre del archivo fasta de salida con las secuencias de aminoacidos\n");
        } else {
            BufferedReader reader = null;
            try {
                String inFileString = args[0];
                String nucleotidesFileString = args[1];
                String aminoacidsFileString = args[2];

                File inFile;
                File nucleotidesFile;
                File aminoacidsFile;
                File dismissedNucleotidesFile;
                File dismissedAminoacidsFile;

                inFile = new File(inFileString);
                nucleotidesFile = new File(nucleotidesFileString);
                dismissedNucleotidesFile = new File(nucleotidesFileString.split("\\.")[0] + DISMISSED_STRING + FASTA_EXTENSION);
                dismissedAminoacidsFile = new File(aminoacidsFileString.split("\\.")[0] + DISMISSED_STRING + FASTA_EXTENSION);
                aminoacidsFile = new File(aminoacidsFileString);

                reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Cerrar archivo de entrada blastouput
                reader.close();

                BufferedWriter aminoacidsBuff = new BufferedWriter(new FileWriter(aminoacidsFile));
                BufferedWriter dismissedAminoacidsBuff = new BufferedWriter(new FileWriter(dismissedAminoacidsFile));
                BufferedWriter nucleotidesBuff = new BufferedWriter(new FileWriter(nucleotidesFile));
                BufferedWriter dismissedNucleotidesBuff = new BufferedWriter(new FileWriter(dismissedNucleotidesFile));

                Annotation annotation = new Annotation(stBuilder.toString());
                List<Element> contigs = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                HashMap<String, TreeSet<PredictedGene>> contigsMap = new HashMap<String, TreeSet<PredictedGene>>();

                for (Element element : contigs) {
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    TreeSet<PredictedGene> geneSet = new TreeSet<PredictedGene>();
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());
                        geneSet.add(gene);
                    }
                    contigsMap.put(contig.getId(), geneSet);
                }


                Set<String> keys = contigsMap.keySet();
                for (String key : keys) {
                    TreeSet<PredictedGene> treeSet = contigsMap.get(key);
                    for (Iterator<PredictedGene> it = treeSet.iterator(); it.hasNext();) {
                        PredictedGene gene = it.next();
                        int beginPos = gene.getStartPosition();
                        int endPos = gene.getEndPosition();
                        if (gene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                            int swap = beginPos;
                            beginPos = endPos;
                            endPos = swap;
                        }
                        String header = HEADER + gene.getId() + SEPARATOR
                                + key + SEPARATOR
                                + beginPos + SEPARATOR
                                + endPos + SEPARATOR
                                + gene.getStrand();

                        BufferedWriter nBuff = nucleotidesBuff;
                        BufferedWriter aBuff = aminoacidsBuff;

                        if(gene.getStatus().equals(PredictedGene.STATUS_DISMISSED)){
                            header += SEPARATOR + PredictedGene.STATUS_DISMISSED;
                            nBuff = dismissedNucleotidesBuff;
                            aBuff = dismissedAminoacidsBuff;
                        }
                        header += "\n";                        
                        nBuff.write(header);
                        nBuff.write(fastaFormat(gene.getSequence()));

                        String proteinSeq = gene.getProteinSequence();

                        if (proteinSeq != null) {
                            aBuff.write(header);
                            aBuff.write(fastaFormat(proteinSeq));
                        }
                    }
                }

                aminoacidsBuff.close();
                dismissedAminoacidsBuff.close();
                nucleotidesBuff.close();
                dismissedNucleotidesBuff.close();

                System.out.println("Archivos creados con exito!! :)");


            } catch (Exception ex) {
                Logger.getLogger(GeneraArchivosFasta.class.getName()).log(Level.SEVERE, null, ex);
            }




        }
    }

    public static String fastaFormat(String seq) {
        if (seq.length() <= FASTA_LINE_LENGTH) {
            return seq + "\n";
        } else {
            String result = "";
            while (seq.length() > FASTA_LINE_LENGTH) {
                result += seq.substring(0, FASTA_LINE_LENGTH) + "\n";
                seq = seq.substring(FASTA_LINE_LENGTH);
            }
            if (seq.length() > 0) {
                result += seq + "\n";
            }

            return result;
        }
    }
}
