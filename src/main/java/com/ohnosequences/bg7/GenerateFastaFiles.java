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

import com.ohnosequences.util.Executable;
import com.ohnosequences.xml.api.model.XMLElement;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class GenerateFastaFiles implements Executable {

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
            System.out.println("This program expects three parameters: \n"
                    + "1. Predicted genes XML input filename \n"
                    + "2. Output fasta file including the nucleotide sequences\n"
                    + "3. Output fasta file including the amino acid sequences\n");
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
                //closing input file reader
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

                System.out.println("Files created successfully!! :)");


            } catch (Exception ex) {
                Logger.getLogger(GenerateFastaFiles.class.getName()).log(Level.SEVERE, null, ex);
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
