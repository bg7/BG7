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
import com.era7.lib.bioinfoxml.*;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.*;
import java.util.*;
import org.jdom.Element;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GenerateGffFile implements Executable {

    public static String SEPARATOR = "\t";
    public static String GENE = "gene";
    public static String CDS = "CDS";
    public static String START_CODON = "start_codon";
    public static String STOP_CODON = "stop_codon";
    public static String RNA = "rna";
    public static String LOCUS_TAG = "locus_tag=";
    public static String PRODUCT = "product=";
    public static String CHORIZO_GEN = "Similarity-based Predicted Gene";
    public static String CHORIZO_RNA = "BLASTN";
    public static String CHORIZO_INFERENCE = "inference=similar to AA sequence:UniprotKB/Swissprot:";
    public static String GFF_HEADER = "##gff-version 3";
    public static String DATE_HEADER = "##date ";
    public static String TYPE_HEADER = "##Type genomic DNA";

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("This program expects two parameters: \n"
                    + "1. Input predicted genes XML filename \n"
                    + "2. Output GFF filename\n");
        } else {

            String inFileString = args[0];
            String outFileString = args[1];

            File inFile = new File(inFileString);
            File outFile = new File(outFileString);

            try {

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));


                //writing header first
                outBuff.write(GFF_HEADER + "\n");
                Date currentDate = new Date();
                outBuff.write(DATE_HEADER + currentDate.toString() + "\n");
                outBuff.write(TYPE_HEADER + "\n");


                BufferedReader reader = new BufferedReader(new FileReader(inFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //closing input file reader
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());


                HashMap<String, TreeSet<GffLine>> linesPerContig = new HashMap<String, TreeSet<GffLine>>();

                //-----------POTATIZING GENES----------------
                List<Element> contigsGenes = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                for (Element element : contigsGenes) {
                    ContigXML contig = new ContigXML(element);
                    TreeSet<GffLine> lines = new TreeSet<GffLine>();
                    linesPerContig.put(contig.getId(), lines);
                    List<XMLElement> genes = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    for (XMLElement xMLElement : genes) {
                        PredictedGene gene = new PredictedGene(xMLElement.asJDomElement());

                        String geneLine = contig.getId() + SEPARATOR + CHORIZO_GEN + SEPARATOR + GENE + SEPARATOR;
                        int beginPos = gene.getStartPosition();
                        int endPos = gene.getEndPosition();
                        int initPos = beginPos;
                        if (beginPos < endPos) {
                            geneLine += beginPos + SEPARATOR + endPos + SEPARATOR;
                        } else {
                            geneLine += endPos + SEPARATOR + beginPos + SEPARATOR;
                            initPos = endPos;
                        }
                        geneLine += gene.getEvalue() + SEPARATOR + gene.getStrand() + SEPARATOR + "." + SEPARATOR + LOCUS_TAG + gene.getId() + ";\n";
                        lines.add(new GffLine(initPos, geneLine));
                        //outBuff.write(geneLine);

                        String cdsLine = contig.getId() + SEPARATOR + CHORIZO_GEN + SEPARATOR + CDS + SEPARATOR;
                        if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                            cdsLine += gene.getStartPosition() + SEPARATOR + (gene.getEndPosition() - 3) + SEPARATOR;
                        } else {
                            cdsLine += (gene.getEndPosition() - 3) + SEPARATOR + gene.getStartPosition() + SEPARATOR;
                        }
                        cdsLine += gene.getEvalue() + SEPARATOR + gene.getStrand() + SEPARATOR + "0" + SEPARATOR;
                        cdsLine += LOCUS_TAG + gene.getId() + ";" + PRODUCT + gene.getProteinNames() + ";" + CHORIZO_INFERENCE + gene.getAccession() + "\n";
                        //outBuff.write(cdsLine);
                        lines.add(new GffLine(initPos, cdsLine));

                        String startCodonLine = contig.getId() + SEPARATOR + CHORIZO_GEN + SEPARATOR + START_CODON + SEPARATOR;
                        if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                            startCodonLine += gene.getStartPosition() + SEPARATOR + (gene.getStartPosition() + 2) + SEPARATOR;
                        } else {
                            startCodonLine += (gene.getStartPosition() - 2) + SEPARATOR + gene.getStartPosition() + SEPARATOR;
                        }
                        startCodonLine += gene.getEvalue() + SEPARATOR + gene.getStrand() + SEPARATOR + "0" + SEPARATOR + LOCUS_TAG + gene.getId() + ";";
                        startCodonLine += PRODUCT + gene.getProteinNames() + ";" + CHORIZO_INFERENCE + gene.getAccession() + "\n";
                        //outBuff.write(startCodonLine);
                        lines.add(new GffLine(initPos, startCodonLine));

                        String stopCodonLine = contig.getId() + SEPARATOR + CHORIZO_GEN + SEPARATOR + STOP_CODON + SEPARATOR;
                        if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                            stopCodonLine += (gene.getEndPosition() + 1) + SEPARATOR + (gene.getEndPosition() + 3) + SEPARATOR;
                        } else {
                            stopCodonLine += (gene.getEndPosition() - 3) + SEPARATOR + (gene.getEndPosition() - 1) + SEPARATOR;
                        }
                        stopCodonLine += gene.getEvalue() + SEPARATOR + gene.getStrand() + SEPARATOR + "0" + SEPARATOR + LOCUS_TAG + gene.getId() + ";";
                        stopCodonLine += PRODUCT + gene.getProteinNames() + ";" + CHORIZO_INFERENCE + gene.getAccession() + "\n";
                        //outBuff.write(stopCodonLine);
                        lines.add(new GffLine(initPos, stopCodonLine));

                    }
                }


                //-----------POTATIZING RNAS-----------------
                List<Element> contigsRnas = annotation.asJDomElement().getChild(PredictedRnas.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                for (Element element : contigsRnas) {
                    ContigXML contig = new ContigXML(element);
                    List<XMLElement> rnas = contig.getChildrenWith(PredictedRna.TAG_NAME);

                    TreeSet<GffLine> lines = linesPerContig.get(contig.getId());
                    if (lines == null) {
                        lines = new TreeSet<GffLine>();
                        linesPerContig.put(contig.getId(), lines);
                    }

                    for (XMLElement xMLElement : rnas) {
                        PredictedRna rna = new PredictedRna(xMLElement.asJDomElement());

                        String rnaLine = contig.getId() + SEPARATOR + CHORIZO_RNA + SEPARATOR + RNA + SEPARATOR;
                        int beginPos = rna.getStartPosition();
                        int endPos = rna.getEndPosition();
                        int initPos = beginPos;
                        if (beginPos < endPos) {
                            rnaLine += beginPos + SEPARATOR + endPos + SEPARATOR;
                        } else {
                            rnaLine += endPos + SEPARATOR + beginPos + SEPARATOR;
                            initPos = endPos;
                        }
                        rnaLine += rna.getEvalue() + SEPARATOR + rna.getStrand() + SEPARATOR + "." + SEPARATOR + LOCUS_TAG + rna.getId() + ";";
                        String columns[] = rna.getAnnotationUniprotId().split("\\|");
                        String rnaProduct = columns[3];
                        String refSeqId = columns[1];
                        String positions = columns[2].substring(1);
                        //ref|NC_007413|:3894075-3895562|16S ribosomal RNA| [locus_tag=Ava_R0035]
                        rnaLine += PRODUCT + rnaProduct + "," + "rna:RefSeq:" + refSeqId + " " + positions + "\n";
                        //outBuff.write(rnaLine);
                        lines.add(new GffLine(initPos, rnaLine));
                    }
                }



                Set<String> keys = linesPerContig.keySet();
                for (String key : keys) {
                    TreeSet<GffLine> lines = linesPerContig.get(key);
                    GffLine line = lines.pollFirst();
                    while (line != null) {
                        outBuff.write(line.getLine());
                        line = lines.pollFirst();
                    }
                }




                outBuff.close();
                System.out.println("Done!!! :D");


            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
