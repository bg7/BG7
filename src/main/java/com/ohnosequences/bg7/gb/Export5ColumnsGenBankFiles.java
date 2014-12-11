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

import java.io.*;
import java.util.*;

import com.ohnosequences.util.Executable;
import com.ohnosequences.util.model.Feature;
import com.ohnosequences.xml.model.*;
import org.jdom2.Element;

/**
 *
 * @author <a href="mailto:ppareja@era7.com">Pablo Pareja Tobes</a>
 */
public class Export5ColumnsGenBankFiles implements Executable {

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("This program expects 4 parameters: \n"
                    + "1. Gene annotation XML result filename \n"
                    + "2. GenomeProject ID\n"
                    + "3. Locus tag prefix\n"
                    + "4. Protein ID prefix\n");
        } else {

            String annotationFileSt = args[0];
            String genomeProjectIdSt = args[1];
            String locusTagPrefixSt = args[2];
            String proteinIdPrefix = args[3];


            File annotationFile = new File(annotationFileSt);

            try {

                //-----READING XML FILE WITH ANNOTATION DATA------------
                BufferedReader reader = new BufferedReader(new FileReader(annotationFile));
                String tempSt;
                StringBuilder stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Closing file
                reader.close();

                Annotation annotation = new Annotation(stBuilder.toString());
                //-------------------------------------------------------------

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(genomeProjectIdSt + "OutFile" + ".tbl"));


                List<Element> contigList = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                List<Element> contigListRna = annotation.asJDomElement().getChild(PredictedRnas.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                HashMap<String, ContigXML> contigsRnaMap = new HashMap<String, ContigXML>();
                HashMap<String, ContigXML> contigsGeneMap = new HashMap<String, ContigXML>();

                HashMap<String, PredictedGene> predictedGenesMap = new HashMap<String, PredictedGene>();
                HashMap<String, PredictedRna> predictedRnasMap = new HashMap<String, PredictedRna>();


                for (Element element : contigListRna) {

                    ContigXML rnaContig = new ContigXML(element);
                    contigsRnaMap.put(rnaContig.getId(), rnaContig);

                    //------------adding rnas to the map--------------
                    List<Element> rnas = rnaContig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
                    for (Element tempElem : rnas) {
                        PredictedRna rna = new PredictedRna(tempElem);
                        predictedRnasMap.put(rna.getId(), rna);
                    }
                }

                for (Element elem : contigList) {

                    ContigXML currentContig = new ContigXML(elem);
                    contigsGeneMap.put(currentContig.getId(), currentContig);

                    //------------adding genes to the map--------------
                    List<Element> genes = currentContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                    for (Element tempElem : genes) {
                        PredictedGene gene = new PredictedGene(tempElem);
                        predictedGenesMap.put(gene.getId(), gene);
                    }
                }

                Set<String> idsSet = new HashSet<String>();
                idsSet.addAll(contigsGeneMap.keySet());
                idsSet.addAll(contigsRnaMap.keySet());

                SortedSet<String> idsSorted = new TreeSet<String>();
                idsSorted.addAll(idsSet);

                for (String key : idsSorted) {

                    ContigXML currentGeneContig = contigsGeneMap.get(key);
                    ContigXML currentRnaContig = contigsRnaMap.get(key);

                    //----writing features line-----
                    outBuff.write(">Features " + key + "\n");

                    TreeSet<Feature> featuresTreeSet = new TreeSet<Feature>();

                    //----------------------GENES LOOP----------------------------
                    if (currentGeneContig != null) {
                        List<Element> genesList = currentGeneContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
                        for (Element element : genesList) {

                            PredictedGene gene = new PredictedGene(element);
                            Feature tempFeature = new Feature();
                            tempFeature.setType(Feature.ORF_FEATURE_TYPE);
                            tempFeature.setId(gene.getId());


                            if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                                tempFeature.setBegin(gene.getStartPosition());
                                tempFeature.setEnd(gene.getEndPosition());
                                tempFeature.setStrand('+');
                            } else {
                                tempFeature.setBegin(gene.getEndPosition());
                                tempFeature.setEnd(gene.getStartPosition());
                                tempFeature.setStrand('-');
                            }
                            featuresTreeSet.add(tempFeature);
                        }
                    }

                    //--------------------------------------------------------------

                    //Now rnas are added (if there are any) so that everything can be sort afterwards
                    if (currentRnaContig != null) {
                        List<Element> rnas = currentRnaContig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
                        for (Element tempElem : rnas) {
                            PredictedRna rna = new PredictedRna(tempElem);
                            Feature tempFeature = new Feature();
                            tempFeature.setType(Feature.RNA_FEATURE_TYPE);
                            tempFeature.setId(rna.getId());


                            if (rna.getStrand().equals(PredictedRna.POSITIVE_STRAND)) {
                                tempFeature.setBegin(rna.getStartPosition());
                                tempFeature.setEnd(rna.getEndPosition());
                                tempFeature.setStrand('+');
                            } else {
                                tempFeature.setBegin(rna.getEndPosition());
                                tempFeature.setEnd(rna.getStartPosition());
                                tempFeature.setStrand('-');
                            }
                            featuresTreeSet.add(tempFeature);
                        }
                    }


                    //---------here featuresTreeSet should have both contig genes and
                    //--- rnas already ordered by position-----------
                    for (Feature feature : featuresTreeSet) {

                        int begin = feature.getBegin();
                        int end = feature.getEnd();

                        if (feature.getStrand() == '-') {
                            begin = feature.getEnd();
                            end = feature.getBegin();
                        }

                        if (feature.getType().equals(Feature.ORF_FEATURE_TYPE)) {

                            PredictedGene gene = predictedGenesMap.get(feature.getId());

                            String beginSt = "" + begin;
                            String endSt = "" + end;

                            if (!gene.getEndIsCanonical()) {

                                endSt = ">" + endSt;

                            }
                            if (!gene.getStartIsCanonical()) {

                                beginSt = "<" + beginSt;

                            }

                            //--------gene------------
                            outBuff.write(beginSt + "\t" + endSt + "\t" + "gene" + "\n");
                            outBuff.write("\t\t\t" + "locus_tag" + "     " + locusTagPrefixSt + feature.getId() + "\n");

                            String pseudoReasonsSt = "";
                            boolean hasFrameshiftOrIntragenicStops = false;

                            if (gene.getFrameshifts() != null) {
                                hasFrameshiftOrIntragenicStops = true;
                                pseudoReasonsSt += " frameshift,";
                            }
                            if (gene.getExtraStopCodons() != null) {
                                hasFrameshiftOrIntragenicStops = true;
                                pseudoReasonsSt += " intragenic stops";
                            }

                            if (pseudoReasonsSt.endsWith(",")) {
                                //getting rid of last comma character
                                pseudoReasonsSt = pseudoReasonsSt.substring(0, pseudoReasonsSt.length() - 1);
                            }


                            if (hasFrameshiftOrIntragenicStops) {
                                outBuff.write("\t\t\t" + "gene_desc" + "\t" + gene.getProteinNames() + "\n");
                                outBuff.write("\t\t\t" + "note" + "\t" + pseudoReasonsSt + "\n");
                            } else {
                                //--------CDS------------
                                outBuff.write(beginSt + "\t" + endSt + "\t" + "CDS" + "\n");
                                outBuff.write("\t\t\t" + "product" + "\t" + gene.getProteinNames() + "\n");
                                if (gene.getEcNumbers() != null && !gene.getEcNumbers().isEmpty()) {
                                    outBuff.write("\t\t\t" + "EC_number" + "\t" + gene.getEcNumbers() + "\n");
                                }
                                outBuff.write("\t\t\t" + "protein_id" + "\t" + proteinIdPrefix + locusTagPrefixSt + feature.getId() + "\n");
                            }



                        } else if (feature.getType().equals(Feature.RNA_FEATURE_TYPE)) {

                            PredictedRna rna = predictedRnasMap.get(feature.getId());

                            //--------gene------------
                            outBuff.write(begin + "\t" + end + "\t" + "gene" + "\n");
                            outBuff.write("\t\t\t" + "locus_tag" + "     " + locusTagPrefixSt + feature.getId() + "\n");

                            //----figuring out kind of RNA---
                            String[] tempArray = rna.getAnnotationUniprotId().split("\\|");
                            String rnaKind = "xRNA";
                            String rnaProduct = "xRNA";

                            if (tempArray != null && tempArray.length > 3) {
                                String str = tempArray[3];
                                if (str.toLowerCase().indexOf("trna") >= 0) {
                                    rnaKind = "tRNA";
                                    rnaProduct = str;
                                } else if (str.toLowerCase().indexOf("ribosomal") >= 0) {
                                    rnaKind = "rRNA";
                                    rnaProduct = str;
                                }
                            }


                            //--------rna------------
                            outBuff.write(begin + "\t" + end + "\t" + rnaKind + "\n");
                            outBuff.write("\t\t\t" + "product" + "\t" + rnaProduct + "\n");

                        }

                    }

                }


                outBuff.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
