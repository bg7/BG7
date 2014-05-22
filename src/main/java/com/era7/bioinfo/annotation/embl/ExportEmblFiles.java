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
package com.era7.bioinfo.annotation.embl;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfo.bioinfoutil.model.Feature;
import com.era7.lib.bioinfoxml.*;
import com.era7.lib.bioinfoxml.embl.EmblXML;
import com.era7.lib.era7xmlapi.model.XMLElementException;
import java.io.*;
import java.util.*;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class ExportEmblFiles implements Executable {

    public static final int DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES = 3;
    public static final int LINE_MAX_LENGTH = 80;
    
    public static int GENE_AND_RNA_COUNTER = 1;

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        main(args);
    }

    public static void main(String[] args) {

        if (args.length != 5) {
            System.out.println("This program expects 5 parameters: \n"
                    + "1. Gene annotation XML result filename \n"
                    + "2. Embl general info XML filename\n"
                    + "3. FNA file with both header and contig sequence\n"
                    + "4. Prefix string for output files\n"
                    + "5. Initial ID value for genes/rnas (integer)");
        } else {


            String annotationFileString = args[0];
            String emblXmlFileString = args[1];
            String fnaContigFileString = args[2];
            String outFileString = args[3];
            
            File annotationFile = new File(annotationFileString);
            File fnaContigFile = new File(fnaContigFileString);
            File emblXmlFile = new File(emblXmlFileString);

            File mainOutFile = new File(outFileString + "MainOutFile.embl");


            try {
                
                GENE_AND_RNA_COUNTER = Integer.parseInt(args[4]);

                BufferedWriter mainOutBuff = new BufferedWriter(new FileWriter(mainOutFile));

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

                //-----READING XML GENERAL INFO FILE------------
                reader = new BufferedReader(new FileReader(emblXmlFile));
                stBuilder = new StringBuilder();
                while ((tempSt = reader.readLine()) != null) {
                    stBuilder.append(tempSt);
                }
                //Closing file
                reader.close();

                EmblXML emblXML = new EmblXML(stBuilder.toString());
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

                //-----------CONTIGS LOOP-----------------------

                for (Element elem : contigList) {

                    ContigXML currentContig = new ContigXML(elem);

                    String mainSequence = contigsMap.get(currentContig.getId());
                    //removing the sequence from the map so that afterwards contigs
                    //with no annotations can be identified
                    contigsMap.remove(currentContig.getId());

                    exportContigToEmbl(currentContig, emblXML, outFileString, mainSequence, contigsRnaMap, mainOutBuff);

                }

                System.out.println("There are " + contigsMap.size() + " contigs with no annotations...");

                System.out.println("generating their embl files...");
                Set<String> keys = contigsMap.keySet();
                for (String tempKey : keys) {
                    System.out.println("generating file for contig: " + tempKey);
                    ContigXML currentContig = new ContigXML();
                    currentContig.setId(tempKey);
                    String mainSequence = contigsMap.get(currentContig.getId());
                    exportContigToEmbl(currentContig, emblXML, outFileString, mainSequence, contigsRnaMap, mainOutBuff);

                }

                //closing main out file
                mainOutBuff.close();

                System.out.println("Embl files succesfully created! :)");

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void exportContigToEmbl(ContigXML currentContig,
            EmblXML emblXml,
            String outFileString,
            String mainSequence,
            HashMap<String, ContigXML> contigsRnaMap,
            BufferedWriter mainOutFileBuff) throws IOException, XMLElementException {

        File outFile = new File(outFileString + currentContig.getId() + ".embl");


        StringBuilder fileStringBuilder = new StringBuilder();

        //-------------------------ID line-----------------------------------
        String idLineSt = "";
        idLineSt += "ID" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES);
        //idLineSt += currentContig.getId() + "; " + currentContig.getId() + "; ";
        idLineSt += emblXml.getId() + mainSequence.length() + " BP." + "\n";
        fileStringBuilder.append(idLineSt);

        fileStringBuilder.append("XX" + "\n");

        fileStringBuilder.append(("DE" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES)
                + emblXml.getDefinition() + " " + currentContig.getId() + "\n"));

        fileStringBuilder.append(("XX" + "\n"));

        fileStringBuilder.append(("AC" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES)
                + ";" + "\n"));

        fileStringBuilder.append("XX" + "\n");

        fileStringBuilder.append(("KW" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES)
                + "." + "\n"));

        fileStringBuilder.append(("XX" + "\n"));

        fileStringBuilder.append(("OS" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES)
                + emblXml.getOrganism() + "\n"));

        String[] lineageSplit = emblXml.getOrganismCompleteTaxonomyLineage().split(";");
        String tempLineageLine = "OC" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES);

        for (String lineageSt : lineageSplit) {
            if ((tempLineageLine.length() + lineageSt.length() + 1) < LINE_MAX_LENGTH) {
                tempLineageLine += lineageSt + ";";
            } else {
                fileStringBuilder.append((tempLineageLine + "\n"));
                if (lineageSt.charAt(0) == ' ') {
                    lineageSt = lineageSt.substring(1);
                }
                tempLineageLine = "OC" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES) + lineageSt + ";";
            }
        }
        if (tempLineageLine.length() > 0) {
            fileStringBuilder.append((tempLineageLine + "\n"));
        }

        fileStringBuilder.append(("XX" + "\n"));

        fileStringBuilder.append(("FH   Key             Location/Qualifiers" + "\n"));

        String sourceSt = "FT   source          ";
        sourceSt += "1.." + mainSequence.length() + "\n";
        fileStringBuilder.append(sourceSt);

        fileStringBuilder.append(("FT                   /organism=\"" + emblXml.getOrganism() + "\"" + "\n"));
        fileStringBuilder.append(("FT                   /mol_type=\"" + emblXml.getMolType() + "\"" + "\n"));
        fileStringBuilder.append(("FT                   /strain=\"" + emblXml.getStrain() + "\"" + "\n"));


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
            genesRnasMixedUpMap.put(gene.getId(), getGeneStringForEmbl(gene,emblXml.getLocusTagPrefix()));

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
                genesRnasMixedUpMap.put(rna.getId(), getRnaStringForEmbl(rna, emblXml.getLocusTagPrefix()));
            }
        }

        //Once genes & rnas are sorted, we just have to write them
        for (Feature f : featuresTreeSet) {
            fileStringBuilder.append(genesRnasMixedUpMap.get(f.getId()));
        }



        //--------------ORIGIN-----------------------------------------
        fileStringBuilder.append(("SQ   Sequence " + mainSequence.length() + " BP;" + "\n"));
        int maxDigits = 10;
        int positionCounter = 1;
        int maxBasesPerLine = 60;
        int currentBase = 0;
        int seqFragmentLength = 10;

//                    System.out.println("currentContig.getId() = " + currentContig.getId());
//                    System.out.println("mainSequence.length() = " + mainSequence.length());
//                    System.out.println(contigsMap.get(currentContig.getId()).length());

        for (currentBase = 0; (currentBase + maxBasesPerLine) < mainSequence.length(); positionCounter += maxBasesPerLine) {

            String tempLine = getWhiteSpaces(5);
            for (int i = 1; i <= (maxBasesPerLine / seqFragmentLength); i++) {
                tempLine += " " + mainSequence.substring(currentBase, currentBase + seqFragmentLength);
                currentBase += seqFragmentLength;
            }
            String posSt = String.valueOf(positionCounter - 1 + maxBasesPerLine);
            tempLine += getWhiteSpaces(maxDigits - posSt.length()) + posSt;
            fileStringBuilder.append((tempLine + "\n"));
        }

        if (currentBase < mainSequence.length()) {

            String lastLine = getWhiteSpaces(5);
            while (currentBase < mainSequence.length()) {
                if ((currentBase + seqFragmentLength) < mainSequence.length()) {
                    lastLine += " " + mainSequence.substring(currentBase, currentBase + seqFragmentLength);
                } else {
                    lastLine += " " + mainSequence.substring(currentBase, mainSequence.length());
                }

                currentBase += seqFragmentLength;
            }
            String posSt = String.valueOf(mainSequence.length());
            lastLine += getWhiteSpaces(LINE_MAX_LENGTH - posSt.length() - lastLine.length() + 1) + posSt;

            fileStringBuilder.append((lastLine + "\n"));
        }

        //--------------------------------------------------------------


        //--- finally I have to add the string "//" in the last line--
        fileStringBuilder.append("//\n");

        BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
        outBuff.write(fileStringBuilder.toString());
        outBuff.close();

        mainOutFileBuff.write(fileStringBuilder.toString());
        mainOutFileBuff.flush();

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

        int lengthWithoutIndentation = LINE_MAX_LENGTH - numberOfWhiteSpacesForIndentation - 2;

        if (value.length() < (LINE_MAX_LENGTH - header.length())) {
            result += value;
            if (putQuotationMarksInTheEnd) {
                result += "\"";
            }
            result += "\n";
        } else if (value.length() == (LINE_MAX_LENGTH - header.length())) {
            result += value + "\n";
            if (putQuotationMarksInTheEnd) {
                result += "FT" + getWhiteSpaces(numberOfWhiteSpacesForIndentation) + "\"\n";
            }
        } else {
            result += value.substring(0, (LINE_MAX_LENGTH - header.length())) + "\n";
            value = value.substring((LINE_MAX_LENGTH - header.length()), value.length());

            while (value.length() > lengthWithoutIndentation) {
                result += "FT" + getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value.substring(0, lengthWithoutIndentation) + "\n";
                value = value.substring(lengthWithoutIndentation, value.length());
            }
            if (value.length() == lengthWithoutIndentation) {
                result += "FT" + getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value + "\n";
                if (putQuotationMarksInTheEnd) {
                    result += "FT" + getWhiteSpaces(numberOfWhiteSpacesForIndentation) + "\"\n";
                }
            } else {
                result += "FT" + getWhiteSpaces(numberOfWhiteSpacesForIndentation)
                        + value;
                if (putQuotationMarksInTheEnd) {
                    result += "\"";
                }
                result += "\n";
            }
        }
        return result;
    }

    private static String getGeneStringForEmbl(PredictedGene gene,
                                               String locusTagPrefix) throws XMLElementException {

        StringBuilder geneStBuilder = new StringBuilder();
        boolean negativeStrand = gene.getStrand().equals(PredictedGene.NEGATIVE_STRAND);

        String genePositionsString = "";
        String cdsPositionsString = "";


        if (negativeStrand) {

            genePositionsString += "complement(";
            if (!gene.getEndIsCanonical()) {
                genePositionsString += "<";
            }
            cdsPositionsString = genePositionsString;

            genePositionsString += gene.getEndPosition() + "..";

            if (gene.getEndPosition() > 4) {
                cdsPositionsString += (gene.getEndPosition() - 3) + "..";
            } else {
                cdsPositionsString = genePositionsString;
            }


            if (!gene.getStartIsCanonical()) {
                genePositionsString += ">";
                cdsPositionsString += ">";
            }
            genePositionsString += gene.getStartPosition() + ")";
            cdsPositionsString += gene.getStartPosition() + ")";

        } else {

            if (!gene.getStartIsCanonical()) {
                genePositionsString += "<";
            }
            genePositionsString += gene.getStartPosition() + "..";
            if (!gene.getEndIsCanonical()) {
                genePositionsString += ">";
            }
            cdsPositionsString = genePositionsString;

            genePositionsString += gene.getEndPosition();
            cdsPositionsString += gene.getEndPosition() + 3;

        }

        //gene part
//        String tempGeneStr = "FT   "
//                + "gene"
//                + getWhiteSpaces(12)
//                + genePositionsString + "\n";
//
//        geneStBuilder.append(tempGeneStr);
//        geneStBuilder.append(patatizaEnLineas("FT"
//                + getWhiteSpaces(19) + "/product=\"",
//                gene.getProteinNames(),
//                19,
//                true));
//        
//        geneStBuilder.append(patatizaEnLineas("FT"
//                + getWhiteSpaces(19) + "/locus_tag=\"",
//                getLocusTagNumberAsText(locusTagPrefix, GENE_AND_RNA_COUNTER),
//                19,
//                true));
//        
//        GENE_AND_RNA_COUNTER++;

        String tempCDSString = "FT" + getWhiteSpaces(DEFAULT_INDENTATION_NUMBER_OF_WHITESPACES)
                + "CDS"
                + getWhiteSpaces(13);

        tempCDSString += cdsPositionsString + "\n";
        geneStBuilder.append(tempCDSString);
        
        geneStBuilder.append(patatizaEnLineas("FT"
                + getWhiteSpaces(19) + "/locus_tag=\"",
                getLocusTagNumberAsText(locusTagPrefix, GENE_AND_RNA_COUNTER),
                19,
                true));
        
        GENE_AND_RNA_COUNTER++;

        geneStBuilder.append(patatizaEnLineas("FT" + 
                getWhiteSpaces(19) + "/product=\"",
                gene.getProteinNames(),
                19,
                true));
        
        if(!gene.getEndIsCanonical() || !gene.getStartIsCanonical() || (gene.getFrameshifts() != null)
                || (gene.getExtraStopCodons() != null)){
            
            geneStBuilder.append(("FT" + getWhiteSpaces(19) + "/pseudo" + "\n"));
        }

        if (gene.getProteinSequence() != null) {
            if (!gene.getProteinSequence().equals("")) {
                
                boolean includeSequence = gene.getEndIsCanonical() && gene.getStartIsCanonical()
                        && (gene.getExtraStopCodons() == null)
                        && (gene.getFrameshifts() == null);

                if (includeSequence) {
                    String protSeq = gene.getProteinSequence();

                    if (!protSeq.substring(0, 1).toUpperCase().equals("M")) {
                        protSeq = "M" + protSeq.substring(1);
                    }

                    geneStBuilder.append(patatizaEnLineas("FT"
                            + getWhiteSpaces(19) + "/translation=\"",
                            protSeq,
                            19,
                            true));
                }

            }
        }

        return geneStBuilder.toString();

    }

    private static String getRnaStringForEmbl(PredictedRna rna,
                                              String locusTagPrefix) {

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

        //rna part
//        String tempRnaStr = "FT   "
//                + "gene"
//                + getWhiteSpaces(12)
//                + positionsString + "\n";
//
//        rnaStBuilder.append(tempRnaStr);
//        rnaStBuilder.append(patatizaEnLineas("FT"
//                + getWhiteSpaces(19) + "/product=\"",
//                rna.getAnnotationUniprotId().split("\\|")[3],
//                19,
//                true));

        //System.out.println("rna.getId() = " + rna.getId());
        //System.out.println("rna.getAnnotationUniprotId() = " + rna.getAnnotationUniprotId());
        
        String rnaProduct = rna.getAnnotationUniprotId().split("\\|")[3];
        String rnaValue = "rna";
        
        if(rnaProduct.toLowerCase().contains("ribosomal")){
            rnaValue = "rRNA";
        }else if(rnaProduct.toLowerCase().contains("trna")){
            rnaValue = "tRNA";
        }
        
        String tempRNAString = "FT   "
                + rnaValue
                + getWhiteSpaces(13);

        tempRNAString += positionsString + "\n";
        rnaStBuilder.append(tempRNAString);
        
        rnaStBuilder.append(patatizaEnLineas("FT"
                + getWhiteSpaces(19) + "/locus_tag=\"",
                getLocusTagNumberAsText(locusTagPrefix, GENE_AND_RNA_COUNTER),
                19,
                true));
        
        GENE_AND_RNA_COUNTER++;

        rnaStBuilder.append(patatizaEnLineas("FT"
                + getWhiteSpaces(19) + "/product=\"",
                rnaProduct,
                19,
                true));
        

        return rnaStBuilder.toString();

    }
    
    
    private static String getLocusTagNumberAsText(String prefix, int number){
        String numberSt = String.valueOf(number);
        String result = prefix;
        
        for (int i = 0; i < (5 - numberSt.length()); i++) {
            result += "0";
        }
        
        return (result + numberSt);        
    }
}
