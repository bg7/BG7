/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.genbank.GBCommon;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.model.Feature;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import com.era7.lib.bioinfoxml.gb.GenBankXML;
import com.era7.lib.era7xmlapi.model.XMLElementException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
public class ExportGenBankFiles implements Executable {

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
                    + "2. Contig external general info XML filename\n"
                    + "3. FNA file with both header and contig sequence\n"
                    + "4. Prefix string for output files\n");
        } else {


            String annotationFileString = args[0];
            String genBankXmlFileString = args[1];
            String fnaContigFileString = args[2];
            String outFileString = args[3];

            File annotationFile = new File(annotationFileString);
            File fnaContigFile = new File(fnaContigFileString);
            File genBankXmlFile = new File(genBankXmlFileString);

            File mainOutFile = new File(args[3] + GBCommon.GEN_BANK_FILE_EXTENSION);

            File allContigsFile = new File(args[3] + "_all" + GBCommon.GEN_BANK_FILE_EXTENSION);

            try {


                //---Writer for file containing all gbks together----
                BufferedWriter allContigsOutBuff = new BufferedWriter(new FileWriter(allContigsFile));

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

                //-----READING GENBANK XML------------
                reader = new BufferedReader(new FileReader(genBankXmlFile));
                stBuilder.delete(0, stBuilder.length());
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Closing file
                reader.close();

                GenBankXML genBankXml = new GenBankXML(stBuilder.toString());
                //-------------------------------------------------------------

                //-------------PARSING CONTIGS & THEIR SEQUENCES------------------
                HashMap<String, String> contigsMap = new HashMap<String, String>();
                reader = new BufferedReader(new FileReader(fnaContigFile));
                stBuilder.delete(0, stBuilder.length());
                String currentContigId = "";

                while ((tempSt = reader.readLine()) != null) {
                    if (tempSt.charAt(0) == '>') {
                        if (stBuilder.length() > 0) {
                            contigsMap.put(currentContigId, stBuilder.toString());
                            stBuilder.delete(0, stBuilder.length());
                        }
                        currentContigId = tempSt.substring(1).trim().split(" ")[0].split("\t")[0];
                        System.out.println("currentContigId = " + currentContigId);
                    } else {
                        stBuilder.append(tempSt);
                    }
                }
                if (stBuilder.length() > 0) {
                    contigsMap.put(currentContigId, stBuilder.toString());
                }
                reader.close();
                //-------------------------------------------------------------

                List<Element> contigList = annotation.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                List<Element> contigListRna = annotation.asJDomElement().getChild(PredictedRnas.TAG_NAME).getChildren(ContigXML.TAG_NAME);
                HashMap<String, ContigXML> contigsRnaMap = new HashMap<String, ContigXML>();
                for (Element element : contigListRna) {
                    ContigXML rnaContig = new ContigXML(element);
                    contigsRnaMap.put(rnaContig.getId(), rnaContig);
                }

                //-----------WRITING GENERAL FILE------------------
                BufferedWriter mainOutBuff = new BufferedWriter(new FileWriter(mainOutFile));

                String linearSt = GBCommon.LINEAR_STR + getWhiteSpaces(2);
                
                if(!genBankXml.getLinear()){   
                    linearSt = GBCommon.CIRCULAR_STR;
                }
                
                
                //------------------------------------------------------
                //------------------------locus line------------------
                mainOutBuff.write(GBCommon.LOCUS_STR
                        + getWhiteSpaceIndentationForString(GBCommon.LOCUS_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                        + genBankXml.getLocusName() + GBCommon.LOCUS_LINE_SEPARATOR
                        + contigsMap.size() + " " + GBCommon.CONTIGS_SIZE_STR + GBCommon.LOCUS_LINE_SEPARATOR
                        + GBCommon.DNA_STR + GBCommon.LOCUS_LINE_SEPARATOR
                        + linearSt + GBCommon.LOCUS_LINE_SEPARATOR
                        + genBankXml.getGenBankDivision() + GBCommon.LOCUS_LINE_SEPARATOR
                        + genBankXml.getModificationDate()
                        + "\n");
                //------------------------------------------------------
                //------------------------------------------------------

                mainOutBuff.write(GBCommon.DEFINITION_STR
                        + getWhiteSpaceIndentationForString(GBCommon.DEFINITION_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                        + genBankXml.getDefinition() + "\n");
                mainOutBuff.write(GBCommon.ACCESSION_STR
                        + getWhiteSpaceIndentationForString(GBCommon.ACCESSION_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                        + genBankXml.getLocusName() + "\n");
                mainOutBuff.write(GBCommon.VERSION_STR
                        + getWhiteSpaceIndentationForString(GBCommon.VERSION_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                        + genBankXml.getLocusName() + ".1" + "\n");
                mainOutBuff.write(GBCommon.KEYWORDS_STR
                        + getWhiteSpaceIndentationForString(GBCommon.KEYWORDS_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                        + genBankXml.getKeywords() + "\n");
                mainOutBuff.write(
                        patatizaEnLineas(GBCommon.COMMENT_STR
                        + getWhiteSpaceIndentationForString(GBCommon.COMMENT_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION),
                        genBankXml.getComment(),
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION,
                        false));

                mainOutBuff.write(GBCommon.FEATURES_STR
                        + getWhiteSpaceIndentationForString(GBCommon.FEATURES_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                        + "Location/Qualifiers" + "\n");

                mainOutBuff.write(GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                        + GBCommon.SOURCE_FEATURES_STR
                        + getWhiteSpaceIndentationForString(GBCommon.SOURCE_FEATURES_STR + GBCommon.FIRST_LEVEL_INDENTATION_FEATURES,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                        + "1.." + contigsMap.size() + "\n");
                mainOutBuff.write(getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                        + "/organism=\"" + genBankXml.getOrganism() + "\"\n");


                Set<String> keySet = contigsMap.keySet();
                List<String> keySetList = new ArrayList<String>(keySet);
                Collections.sort(keySetList);
                String contigsIdsList = "";
                for (String string : keySetList) {
                    contigsIdsList += string + ", ";
                }
                //I have to get rid of the last comma
                contigsIdsList = contigsIdsList.substring(0, contigsIdsList.length() - 1);

                System.out.println(contigsIdsList);

                mainOutBuff.write(
                        patatizaEnLineas(GBCommon.WGS_STR
                        + getWhiteSpaceIndentationForString(GBCommon.WGS_STR,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION),
                        contigsIdsList,
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION,
                        false));

                mainOutBuff.close();

                //-------------------------------------------------------------



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

                    ContigXML contig = contigXMLMap.get(key);

                    String mainSequence = contigsMap.get(contig.getId());
                    //removing the sequence from the map so that afterwards contigs
                    //with no annotations can be identified
                    contigsMap.remove(contig.getId());

                    exportContigToGenBank(contig, genBankXml, outFileString, mainSequence, contigsRnaMap, allContigsOutBuff);
                }

                System.out.println("There are " + contigsMap.size() + " contigs with no annotations...");

                System.out.println("generating their gbk files...");
                Set<String> keys = contigsMap.keySet();
                for (String tempKey : keys) {
                    System.out.println("generating file for contig: " + tempKey);
                    ContigXML currentContig = new ContigXML();
                    currentContig.setId(tempKey);
                    String mainSequence = contigsMap.get(currentContig.getId());
                    exportContigToGenBank(currentContig, genBankXml, outFileString, mainSequence, contigsRnaMap, allContigsOutBuff);

                }

                //---closing all contigs out buff----
                allContigsOutBuff.close();

                System.out.println("Gbk files succesfully created! :)");

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void exportContigToGenBank(ContigXML currentContig,
            GenBankXML genBankXml,
            String outFileString,
            String mainSequence,
            HashMap<String, ContigXML> contigsRnaMap,
            BufferedWriter allContigsOutBuff) throws IOException, XMLElementException {

        File outFile = new File(outFileString + currentContig.getId() + GBCommon.GEN_BANK_FILE_EXTENSION);
        BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

        StringBuilder outStringBuilder = new StringBuilder();

        //-------------------------locus line-----------------------------------
        //in this case the format is a bit more restrictive so we have to write
        //words in specific positions paying also attention to their separators
        String locusLineSt = "";
        locusLineSt += GBCommon.LOCUS_STR + getWhiteSpaces(6);
        if (genBankXml.getLocusName().length() > 16) {
            locusLineSt += genBankXml.getLocusName().substring(0, 16);
        } else {
            locusLineSt += genBankXml.getLocusName() + getWhiteSpaces(16 - genBankXml.getLocusName().length());
        }
        String seqLength = String.valueOf(mainSequence.length());
        locusLineSt += getWhiteSpaces(1) + getWhiteSpaces(11 - seqLength.length()) + seqLength;

        locusLineSt += getWhiteSpaces(1) + GBCommon.BASE_PAIRS_STR + getWhiteSpaces(1);
        if (genBankXml.getStrandedType().length() == 0) {
            locusLineSt += getWhiteSpaces(3);
        } else {
            locusLineSt += genBankXml.getStrandedType();
        }
        locusLineSt += genBankXml.getDnaType() + getWhiteSpaces(1);


        if (genBankXml.getLinear()) {
            locusLineSt += GBCommon.LINEAR_STR + getWhiteSpaces(2);
        } else {
            locusLineSt += GBCommon.CIRCULAR_STR;
        }
        locusLineSt += getWhiteSpaces(1) + genBankXml.getGenBankDivision() + getWhiteSpaces(1);
        locusLineSt += genBankXml.getModificationDate() + "\n";
        outStringBuilder.append(locusLineSt);
//                    outBuff.write(GBCommon.LOCUS_STR
//                            + getWhiteSpaceIndentationForString(GBCommon.LOCUS_STR,
//                            GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
//                            + genBankXml.getLocusName() + GBCommon.LOCUS_LINE_SEPARATOR
//                            + genBankXml.getSequenceLength() + " "
//                            + GBCommon.BASE_PAIRS_STR + GBCommon.LOCUS_LINE_SEPARATOR
//                            + GBCommon.DNA_STR + GBCommon.LOCUS_LINE_SEPARATOR
//                            + GBCommon.LINEAR_STR + GBCommon.LOCUS_LINE_SEPARATOR
//                            + genBankXml.getGenBankDivision() + GBCommon.LOCUS_LINE_SEPARATOR
//                            + genBankXml.getModificationDate()
//                            + "\n");
        outStringBuilder.append((GBCommon.DEFINITION_STR
                + getWhiteSpaceIndentationForString(GBCommon.DEFINITION_STR,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                + genBankXml.getDefinition()
                + ". " + currentContig.getId() + "\n"));
        outStringBuilder.append((GBCommon.ACCESSION_STR
                + getWhiteSpaceIndentationForString(GBCommon.ACCESSION_STR,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                + currentContig.getId() + "\n"));

        outStringBuilder.append((GBCommon.VERSION_STR
                + getWhiteSpaceIndentationForString(GBCommon.VERSION_STR,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                + currentContig.getId() + ".1" + "\n"));

        outStringBuilder.append((GBCommon.KEYWORDS_STR
                + getWhiteSpaceIndentationForString(GBCommon.KEYWORDS_STR,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                + genBankXml.getKeywords() + "\n"));

        outStringBuilder.append((GBCommon.SOURCE_STR
                + getWhiteSpaceIndentationForString(GBCommon.SOURCE_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION)
                + genBankXml.getOrganism() + "\n"));

        outStringBuilder.append(
                patatizaEnLineas(GBCommon.FIRST_LEVEL_INDENTATION
                + GBCommon.ORGANISM_STR
                + getWhiteSpaceIndentationForString(GBCommon.FIRST_LEVEL_INDENTATION
                + GBCommon.ORGANISM_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION),
                genBankXml.getOrganismCompleteTaxonomyLineage() + "\n",
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION,
                false));

        outStringBuilder.append((GBCommon.FEATURES_STR
                + getWhiteSpaceIndentationForString(GBCommon.FEATURES_STR,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                + "Location/Qualifiers" + "\n"));

        outStringBuilder.append((GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.SOURCE_FEATURES_STR
                + getWhiteSpaceIndentationForString(GBCommon.SOURCE_FEATURES_STR + GBCommon.FIRST_LEVEL_INDENTATION_FEATURES,
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                + "1.." + mainSequence.length() + "\n"));

        outStringBuilder.append((getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                + "/organism=\"" + genBankXml.getOrganism() + "\"\n"));



        //------Hashmap with key = gene/rna id and the value =
        //---- respective String exactly as is must be written to the result file---------------------------
        HashMap<String, String> genesRnasMixedUpMap = new HashMap<String, String>();

        TreeSet<Feature> featuresTreeSet = new TreeSet<Feature>();

        //----------------------GENES LOOP----------------------------
        List<Element> genesList = currentContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);
        for (Element element : genesList) {

            PredictedGene gene = new PredictedGene(element);
            Feature tempFeature = new Feature();
            tempFeature.setId(gene.getId());
            if (gene.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                tempFeature.setBegin(gene.getStartPosition());
                tempFeature.setEnd(gene.getEndPosition());
            } else {
                tempFeature.setBegin(gene.getEndPosition());
                tempFeature.setEnd(gene.getStartPosition());
            }
            featuresTreeSet.add(tempFeature);
            genesRnasMixedUpMap.put(gene.getId(), getGeneStringForGenBank(gene));

        }
        //--------------------------------------------------------------

        //Now rnas are added (if there are any) so that everything can be sort afterwards
        ContigXML contig = contigsRnaMap.get(currentContig.getId());
        if (contig != null) {
            List<Element> rnas = contig.asJDomElement().getChildren(PredictedRna.TAG_NAME);
            for (Element tempElem : rnas) {
                PredictedRna rna = new PredictedRna(tempElem);
                Feature tempFeature = new Feature();
                tempFeature.setId(rna.getId());
                if (rna.getStrand().equals(PredictedGene.POSITIVE_STRAND)) {
                    tempFeature.setBegin(rna.getStartPosition());
                    tempFeature.setEnd(rna.getEndPosition());
                } else {
                    tempFeature.setBegin(rna.getEndPosition());
                    tempFeature.setEnd(rna.getStartPosition());
                }
                featuresTreeSet.add(tempFeature);
                genesRnasMixedUpMap.put(rna.getId(), getRnaStringForGenBank(rna));
            }
        }

        //Once genes & rnas are sorted, we just have to write them
        for (Feature f : featuresTreeSet) {
            outStringBuilder.append(genesRnasMixedUpMap.get(f.getId()));
        }



        //--------------ORIGIN-----------------------------------------
        outStringBuilder.append((GBCommon.ORIGIN_STR + "\n"));
        int maxDigits = 9;
        int positionCounter = 1;
        int maxBasesPerLine = 60;
        int currentBase = 0;
        int seqFragmentLength = 10;

//                    System.out.println("currentContig.getId() = " + currentContig.getId());
//                    System.out.println("mainSequence.length() = " + mainSequence.length());
//                    System.out.println(contigsMap.get(currentContig.getId()).length());

        for (currentBase = 0; (currentBase + maxBasesPerLine) < mainSequence.length(); positionCounter += maxBasesPerLine) {

            String posSt = String.valueOf(positionCounter);
            String tempLine = getWhiteSpaces(maxDigits - posSt.length()) + posSt;
            for (int i = 1; i <= (maxBasesPerLine / seqFragmentLength); i++) {
                tempLine += " " + mainSequence.substring(currentBase, currentBase + seqFragmentLength);
                currentBase += seqFragmentLength;
            }
            outStringBuilder.append((tempLine + "\n"));
        }

        if (currentBase < mainSequence.length()) {
            String posSt = String.valueOf(positionCounter);
            String lastLine = getWhiteSpaces(maxDigits - posSt.length()) + posSt;
            while (currentBase < mainSequence.length()) {
                if ((currentBase + seqFragmentLength) < mainSequence.length()) {
                    lastLine += " " + mainSequence.substring(currentBase, currentBase + seqFragmentLength);
                } else {
                    lastLine += " " + mainSequence.substring(currentBase, mainSequence.length());
                }

                currentBase += seqFragmentLength;
            }
            outStringBuilder.append((lastLine + "\n"));
        }

        //--------------------------------------------------------------


        //--- finally I have to add the string "//" in the last line--
        outStringBuilder.append("//\n");

        outBuff.write(outStringBuilder.toString());
        outBuff.close();

        allContigsOutBuff.write(outStringBuilder.toString());

    }

    private static String getWhiteSpaceIndentationForString(String value,
            int numberOfWhiteSpaces) {
        int number = numberOfWhiteSpaces - value.length();
        if (number <= 0) {
            return "";
        } else {
            return getWhiteSpaces(number);
        }
    }

    private static String getWhiteSpaces(int number) {
        String result = "";
        for (int i = 0; i < number; i++) {
            result += " ";
        }
        return result;
    }

    private static String patatizaEnLineas(String header,
            String value,
            int numberOfWhiteSpacesForIndentation,
            boolean putQuotationMarksInTheEnd) {

        //value = value.toUpperCase();
        String result = "";

        result += header;

        int lengthWithoutIndentation = GBCommon.LINE_MAX_LENGTH - numberOfWhiteSpacesForIndentation;

        if (value.length() < (GBCommon.LINE_MAX_LENGTH - header.length())) {
            result += value;
            if (putQuotationMarksInTheEnd) {
                result += "\"";
            }
            result += "\n";
        } else if (value.length() == (GBCommon.LINE_MAX_LENGTH - header.length())) {
            result += value + "\n";
            if (putQuotationMarksInTheEnd) {
                result += getWhiteSpaces(numberOfWhiteSpacesForIndentation) + "\"\n";
            }
        } else {
            result += value.substring(0, (GBCommon.LINE_MAX_LENGTH - header.length())) + "\n";
            value = value.substring((GBCommon.LINE_MAX_LENGTH - header.length()), value.length());

            while (value.length() > lengthWithoutIndentation) {
                result += getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value.substring(0, lengthWithoutIndentation) + "\n";
                value = value.substring(lengthWithoutIndentation, value.length());
            }
            if (value.length() == lengthWithoutIndentation) {
                result += getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value + "\n";
                if (putQuotationMarksInTheEnd) {
                    result += getWhiteSpaces(numberOfWhiteSpacesForIndentation) + "\"\n";
                }
            } else {
                result += getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value;
                if (putQuotationMarksInTheEnd) {
                    result += "\"";
                }
                result += "\n";
            }
        }
        return result;
    }

    private static String getGeneStringForGenBank(PredictedGene gene) {

        StringBuilder geneStBuilder = new StringBuilder();
        boolean negativeStrand = gene.getStrand().equals(PredictedGene.NEGATIVE_STRAND);

        String positionsString = "";


        if (negativeStrand) {

            positionsString += "complement(";
            if (!gene.getEndIsCanonical()) {
                positionsString += "<";
            }
            positionsString += gene.getEndPosition() + "..";

            if (!gene.getStartIsCanonical()) {
                positionsString += ">";
            }
            positionsString += gene.getStartPosition() + ")";

        } else {

            if (!gene.getStartIsCanonical()) {
                positionsString += "<";
            }
            positionsString += gene.getStartPosition() + "..";
            if (!gene.getEndIsCanonical()) {
                positionsString += ">";
            }

            positionsString += gene.getEndPosition();

        }

        //gene part
        String tempGeneStr = GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.GENE_STR
                + getWhiteSpaceIndentationForString(GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.GENE_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                + positionsString + "\n";

        geneStBuilder.append(tempGeneStr);
        geneStBuilder.append(patatizaEnLineas(
                getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/product=\"",
                gene.getProteinNames(),
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
                true));

        String tempCDSString = GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.CDS_STR
                + getWhiteSpaceIndentationForString(GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.CDS_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES);

        tempCDSString += positionsString + "\n";
        geneStBuilder.append(tempCDSString);

        geneStBuilder.append(patatizaEnLineas(
                getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/product=\"",
                gene.getProteinNames(),
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
                true));

        if (gene.getProteinSequence() != null) {
            if (!gene.getProteinSequence().equals("")) {
                geneStBuilder.append(patatizaEnLineas(
                        getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/translation=\"",
                        gene.getProteinSequence(),
                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
                        true));
            }
        }

        return geneStBuilder.toString();

    }

    private static String getRnaStringForGenBank(PredictedRna rna) {

        StringBuilder rnaStBuilder = new StringBuilder();
        boolean negativeStrand = rna.getStrand().equals(PredictedRna.NEGATIVE_STRAND);

        String positionsString = "";


        if (negativeStrand) {

            positionsString += "complement(";
//            if (!rna.getEndIsCanonical()) {
//                positionsString += "<";
//            }
            positionsString += "<" + rna.getEndPosition() + ".." + ">" + rna.getStartPosition();
//            if (!rna.getStartIsCanonical()) {
//                positionsString += ">";
//            }
            positionsString += ")";

        } else {

//            if (!rna.getStartIsCanonical()) {
//                positionsString += "<";
//            }
            positionsString += "<" + rna.getStartPosition() + ".." + ">" + rna.getEndPosition();
//            if (!rna.getEndIsCanonical()) {
//                positionsString += ">";
//            }

        }

        //gene part
        String tempRnaStr = GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.GENE_STR
                + getWhiteSpaceIndentationForString(GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.GENE_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES)
                + positionsString + "\n";

        rnaStBuilder.append(tempRnaStr);
        rnaStBuilder.append(patatizaEnLineas(
                getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/product=\"",
                rna.getAnnotationUniprotId().split("\\|")[3],
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
                true));

        String tempRNAString = GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.RNA_STR
                + getWhiteSpaceIndentationForString(GBCommon.FIRST_LEVEL_INDENTATION_FEATURES
                + GBCommon.RNA_STR, GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES);

        tempRNAString += positionsString + "\n";
        rnaStBuilder.append(tempRNAString);

        rnaStBuilder.append(patatizaEnLineas(
                getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/product=\"",
                rna.getAnnotationUniprotId().split("\\|")[3],
                GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
                true));

//        if (rna.getProteinSequence() != null) {
//            if (!gene.getProteinSequence().equals("")) {
//                geneStBuilder.append(patatizaEnLineas(
//                        getWhiteSpaces(GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES) + "/translation=\"",
//                        gene.getProteinSequence(),
//                        GBCommon.NUMBER_OF_WHITE_SPACES_FOR_INDENTATION_FEATURES,
//                        true));
//            }
//        }

        return rnaStBuilder.toString();

    }
}
