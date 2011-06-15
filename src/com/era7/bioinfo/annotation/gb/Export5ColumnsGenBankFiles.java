/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.model.Feature;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.jdom.Element;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
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

        if (args.length != 3) {
            System.out.println("This program expects 3 parameters: \n"
                    + "1. Gene annotation XML result filename \n"
                    + "2. GenomeProject ID\n"
                    + "3. Locus tag prefix\n");
        } else {

            String annotationFileSt = args[0];
            String genomeProjectIdSt = args[1];
            String locusTagPrefixSt = args[2];


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
                for (Element element : contigListRna) {
                    ContigXML rnaContig = new ContigXML(element);
                    contigsRnaMap.put(rnaContig.getId(), rnaContig);
                }

                //-----------CONTIGS LOOP-----------------------

                //---ordering contigs----
                HashMap<String, ContigXML> contigXMLMap = new HashMap<String, ContigXML>();

                for (Element elem : contigList) {

                    ContigXML currentContig = new ContigXML(elem);

                    contigXMLMap.put(currentContig.getId(), currentContig);
                }

                Set<String> idsSet = contigXMLMap.keySet();
                SortedSet<String> idsSorted = new TreeSet<String>();
                idsSorted.addAll(idsSet);

                for (String key : idsSorted) {

                    ContigXML currentContig = contigXMLMap.get(key);

                    //----writing features line-----
                    outBuff.write(">Features " + currentContig.getId() + "\n");

                    TreeSet<Feature> featuresTreeSet = new TreeSet<Feature>();

                    //----------------------GENES LOOP----------------------------
                    List<Element> genesList = currentContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
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
                    //--------------------------------------------------------------

                    //Now rnas are added (if there are any) so that everything can be sort afterwards
                    ContigXML contig = contigsRnaMap.get(currentContig.getId());
                    if (contig != null) {
                        List<Element> rnas = contig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
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

                            //--------gene------------
                            outBuff.write(begin + "\t" + end + "\t" + "gene" + "\n");
                            outBuff.write("\t\t\t" + "locus_tag" + "\t" + locusTagPrefixSt + feature.getId() + "\n");

                            //--------CDS------------
                            outBuff.write(begin + "\t" + end + "\t" + "CDS" + "\n");
                            outBuff.write("\t\t\t" + "protein_id" + "\t" + locusTagPrefixSt + feature.getId() + "\n");

                        } else if (feature.getType().equals(Feature.RNA_FEATURE_TYPE)) {

                            //--------rna------------
                            outBuff.write(begin + "\t" + end + "\t" + "rna" + "\n");
                            outBuff.write("\t\t\t" + "locus_tag" + "\t" + locusTagPrefixSt + feature.getId() + "\n");

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
