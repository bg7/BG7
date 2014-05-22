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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ohnosequences.util.Executable;
import com.ohnosequences.xml.api.model.XMLElement;
import com.ohnosequences.xml.model.*;
import com.ohnosequences.xml.model.genome.feature.Intergenic;
import org.jdom2.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetIntergenicSequences implements Executable {

    public static String SEPARATOR = "|";
    public static String HEADER = ">";
    public static int FASTA_LINE_LENGTH = 60;

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
                    + "1. Final predicted genes XML filename \n"
                    + "2. Fasta file with the sequences of the contigs\n"
                    + "3. Output XML file including existing intergenic sequences\n"
                    + "4. Output fasta (multifasta) including existing intergenic sequences\n");
        } else {


            try {


                File genesFile, fastaFile, outFile, outFastaFile;
                genesFile = new File(args[0]);
                fastaFile = new File(args[1]);
                outFile = new File(args[2]);
                outFastaFile = new File(args[3]);
//                outTxtFile = new File(args[3]);


                //-------------first of all the sequences of the contigs are fetched and stored in memory----
                HashMap<String, String> contigsMap = new HashMap<String, String>();
                BufferedReader bufferedReader = new BufferedReader(new FileReader(fastaFile));
                String line = null;
                StringBuilder stringBuilder = new StringBuilder();
                String currentContigID = "";

                while ((line = bufferedReader.readLine()) != null) {
                    if (line.charAt(0) == '>') {
                        if (stringBuilder.length() > 0) {
                            contigsMap.put(currentContigID, stringBuilder.toString());
                            stringBuilder.delete(0, stringBuilder.length());
                        }
                        currentContigID = line.substring(1).trim().split(" ")[0].split("\t")[0];
                        //System.out.println("currentContigId = " + currentContigID);
                    } else {
                        stringBuilder.append(line);
                    }
                }

                if (stringBuilder.length() > 0) {
                    contigsMap.put(currentContigID, stringBuilder.toString());
                }
                //closing input contigs file
                bufferedReader.close();
                //--------------------------------------------------------------------------------------


                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                outBuff.write("<intergenic_regions>\n");
                BufferedWriter outFastaBuff = new BufferedWriter(new FileWriter(outFastaFile));

//                BufferedWriter outTxtBuff = new BufferedWriter(new FileWriter(outTxtFile));

                bufferedReader = new BufferedReader(new FileReader(genesFile));
                stringBuilder = new StringBuilder();
                line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                System.out.println("Parsing predicted genes xml file...");
                XMLElement xMLElement = new XMLElement(stringBuilder.toString());
                stringBuilder.delete(0, stringBuilder.length());
                System.out.println("done!");

                System.out.println("Extracting intergenic sequences...");
                List<Element> contigList = xMLElement.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                //adding to list the contigs which have no genes predicted at all
                for (String tempContigID : contigsMap.keySet()) {
                    
                    boolean found = false;

                    for (int i = 0; i < contigList.size(); i++) {
                        ContigXML tempContigXML = new ContigXML(contigList.get(i));
                        if (tempContigXML.getId().equals(tempContigID)) {
                            found = true;
                            break;
                        }
                    }
                    
                    if(!found){
                        ContigXML newContigXML = new ContigXML();
                        newContigXML.setId(tempContigID);
                        contigList.add(newContigXML.asJDomElement());
                    }
                }


                //----------CONTIGS LOOP---------------
                for (Element element : contigList) {
                    ContigXML currentContig = new ContigXML(element);
                    ContigXML contigResult = new ContigXML();
                    contigResult.setId(currentContig.getId());
                    int contigLength = contigsMap.get(currentContig.getId()).length();
                    List<Element> genes = currentContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);

                    //---------dismissed genes are removed from the list first----------------
                    for (Iterator<Element> it = genes.iterator(); it.hasNext();) {
                        Element element1 = it.next();
                        PredictedGene tempGene = new PredictedGene(element1);
                        if (tempGene.getStatus().equals(PredictedGene.STATUS_DISMISSED)) {
                            it.remove();
                        }

                    }
                    //-------------------------------------------------------------------------

                    double sumaBasesIntergenicas = 0;

                    PredictedGene lastGene = null;
                    PredictedGene currentGene = null;

                    if (genes.isEmpty()) {

                        //we add the whole contig 
                        Intergenic intergenic = new Intergenic();
                        intergenic.setStrand(Intergenic.POSITIVE_STRAND);
                        intergenic.setBegin(1);
                        intergenic.setEnd(contigLength);
                        intergenic.setSequence(contigsMap.get(currentContig.getId()));
                        contigResult.addChild(intergenic);
                        //It's also recorded in the multifasta output file
                        String header = HEADER + currentContig.getId() + SEPARATOR + intergenic.getBegin() + ".." + intergenic.getEnd() + "\n";
                        outFastaBuff.write(header + fastaFormat(intergenic.getSequence()));


                    } else {

                        //first intergenic going from the beginning of the contig to the first predicted gene                        
                        currentGene = new PredictedGene(genes.get(0));

                        int begin = currentGene.getStartPosition();
                        if (currentGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                            begin = currentGene.getEndPosition();
                        }

                        if (begin != 1) {

                            Intergenic intergenic = new Intergenic();
                            intergenic.setStrand(Intergenic.POSITIVE_STRAND);
                            intergenic.setBegin(1);
                            intergenic.setEnd(begin - 1);
                            intergenic.setSequence(contigsMap.get(currentContig.getId()).substring(0, begin - 1));
                            contigResult.addChild(intergenic);
                            //It's also recorded in the multifasta output file
                            String header = HEADER + currentContig.getId() + SEPARATOR + intergenic.getBegin() + ".." + intergenic.getEnd() + "\n";
                            outFastaBuff.write(header + fastaFormat(intergenic.getSequence()));
                        }


                        lastGene = new PredictedGene(genes.get(0));

                        //--------------RETRIEVING INTERGENICS LOOP -----------
                        for (int i = 1; i < genes.size(); i++) {
                            currentGene = new PredictedGene(genes.get(i));
                            int begin1, begin2, end1, end2;
                            begin1 = lastGene.getStartPosition();
                            end1 = lastGene.getEndPosition();
                            if (lastGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                                int swap = begin1;
                                begin1 = end1;
                                end1 = swap;
                            }

                            begin2 = currentGene.getStartPosition();
                            end2 = currentGene.getEndPosition();
                            if (currentGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                                int swap = begin2;
                                begin2 = end2;
                                end2 = swap;
                            }

                            //if condition is fulfilled it means there's an extragenic seq
                            if (end1 < (begin2 - 1)) {
                                Intergenic intergenic = new Intergenic();
                                intergenic.setBegin(end1 + 1);
                                intergenic.setEnd(begin2 - 1);
                                intergenic.setStrand(Intergenic.POSITIVE_STRAND);

                                intergenic.setSequence(contigsMap.get(currentContig.getId()).substring(
                                        intergenic.getBegin() - 1, intergenic.getEnd()));

                                contigResult.addChild(intergenic);

                                //It's also recorded in the multifasta output file
                                String header = HEADER + currentContig.getId() + SEPARATOR + intergenic.getBegin() + ".." + intergenic.getEnd() + "\n";
                                outFastaBuff.write(header + fastaFormat(intergenic.getSequence()));

                                sumaBasesIntergenicas += (intergenic.getEnd() - intergenic.getBegin() + 1);
                            }

                            lastGene = currentGene;
                        }

                        //now it's time to record the one between the last gene and the end of the contig                        
                        currentGene = new PredictedGene(genes.get(genes.size() - 1));
                        int end = currentGene.getEndPosition();
                        if (currentGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)) {
                            end = currentGene.getStartPosition();
                        }
                        if (end != contigLength) {
                            Intergenic intergenic = new Intergenic();
                            intergenic.setStrand(Intergenic.POSITIVE_STRAND);
                            intergenic.setBegin(end + 1);
                            intergenic.setEnd(contigLength);
                            intergenic.setSequence(contigsMap.get(currentContig.getId()).substring(end, contigLength));
                            contigResult.addChild(intergenic);
                            //It's also recorded in the multifasta output file
                            String header = HEADER + currentContig.getId() + SEPARATOR + intergenic.getBegin() + ".." + intergenic.getEnd() + "\n";
                            outFastaBuff.write(header + fastaFormat(intergenic.getSequence()));
                        }

                    }

                    contigResult.setLength(contigLength);
                    contigResult.setGapsPercentage(sumaBasesIntergenicas * 100.0 / contigLength);
                    outBuff.write(contigResult.toString() + "\n");


//                    outTxtBuff.write(currentContig.getId() + "\nLongitud secuencia contig: " + contigLength + "\n");
//                    outTxtBuff.write("Longitud suma de bases intergenicas: " + sumaBasesIntergenicas + "\n");
//                    outTxtBuff.write("Porcentaje de agujeros: " + (sumaBasesIntergenicas * 100 / contigLength) + "\n");
                }



                outBuff.write("</intergenic_regions>");
                outBuff.close();

//                outTxtBuff.close();

                outFastaBuff.close();

                System.out.println("Everything's done! :)");


            } catch (Exception e) {
                e.printStackTrace();
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
