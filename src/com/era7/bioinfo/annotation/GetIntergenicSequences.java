/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.genome.feature.Intergenic;
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
import org.jdom.Element;

/**
 *
 * @author ppareja
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
            System.out.println("El programa espera tres parametros: \n"
                    + "1. Nombre del archivo xml con los genes finales predichos \n"
                    + "2. Nombre del archivo .fasta con las secuencias de los contigs\n"
                    + "3. Nombre del archivo xml con las secuencias intergenicas existentes\n"
                    + "4. Nombre del archivo .fasta (multifasta) con las secuencias intergenicas existentes\n");
        } else {


            try {


                File genesFile, fastaFile, outFile, outFastaFile;
                genesFile = new File(args[0]);
                fastaFile = new File(args[1]);
                outFile = new File(args[2]);
                outFastaFile = new File(args[3]);
//                outTxtFile = new File(args[3]);


                //-------------primero saco las secuencias de los contigs para tenerlas en memoria----
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
                        System.out.println("currentContigId = " + currentContigID);
                    } else {
                        stringBuilder.append(line);
                    }
                }

                if (stringBuilder.length() > 0) {
                    contigsMap.put(currentContigID, stringBuilder.toString());
                }
                //Cerrar archivo de entrada con los contigs
                bufferedReader.close();
                //--------------------------------------------------------------------------------------


                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));
                outBuff.write("<intergenic_regions>\n");
                BufferedWriter outFastaBuff = new BufferedWriter(new FileWriter(outFastaFile));

//                BufferedWriter outTxtBuff = new BufferedWriter(new FileWriter(outTxtFile));

                bufferedReader = new BufferedReader(new FileReader(genesFile));
                stringBuilder = new StringBuilder();
                line = null;
                while((line = bufferedReader.readLine()) != null){
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                System.out.println("Construyendo el xml de genes predichos...");
                XMLElement xMLElement = new XMLElement(stringBuilder.toString());
                stringBuilder.delete(0, stringBuilder.length());
                System.out.println("ya!");
                System.out.println("Extrayendo intergenicas...");
                List<Element> contigList = xMLElement.asJDomElement().getChild(PredictedGenes.TAG_NAME).getChildren(ContigXML.TAG_NAME);

                //----------CICLO DE CONTIGS---------------
                for (Element element : contigList) {
                    ContigXML currentContig = new ContigXML(element);
                    ContigXML contigResult = new ContigXML();
                    contigResult.setId(currentContig.getId());
                    List<Element> genes = currentContig.asJDomElement().getChildren(PredictedGene.TAG_NAME);

                    //---------primero borro los que sean dismissed de la lista----------------
                    for (Iterator<Element> it = genes.iterator(); it.hasNext();) {
                        Element element1 = it.next();
                        PredictedGene tempGene = new PredictedGene(element1);
                        if(tempGene.getStatus().equals(PredictedGene.STATUS_DISMISSED)){
                            it.remove();
                        }

                    }
                    //-------------------------------------------------------------------------

                    double sumaBasesIntergenicas = 0;
                    
                    PredictedGene lastGene = null;
                    PredictedGene currentGene = null;

                    lastGene = new PredictedGene(genes.get(0));

                    //--------------CICLO DE GENES PARA SACER INTERGENICAS-------------
                    for (int i = 1; i < genes.size(); i++) {
                        currentGene = new PredictedGene(genes.get(i));
                        int begin1,begin2,end1,end2;
                        begin1 = lastGene.getStartPosition();
                        end1 = lastGene.getEndPosition();
                        if(lastGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)){
                            int swap = begin1;
                            begin1 = end1;
                            end1 = swap;
                        }

                        begin2 = currentGene.getStartPosition();
                        end2 = currentGene.getEndPosition();
                        if(currentGene.getStrand().equals(PredictedGene.NEGATIVE_STRAND)){
                            int swap = begin2;
                            begin2 = end2;
                            end2 = swap;
                        }

                        //Si se cumple esta condicion es que hay una extragenica
                        if(end1 < (begin2 - 1)){
                            Intergenic intergenic = new Intergenic();
                            intergenic.setBegin(end1 +1);
                            intergenic.setEnd(begin2-1);
                            intergenic.setStrand(Intergenic.POSITIVE_STRAND);

                            intergenic.setSequence(contigsMap.get(currentContig.getId()).substring(
                                    intergenic.getBegin()-1,intergenic.getEnd()));

                            contigResult.addChild(intergenic);

                            //Tambien la guardamos en el multifasta de salida
                            String header = HEADER + currentContig.getId() + SEPARATOR + intergenic.getBegin() + ".." + intergenic.getEnd() + "\n";
                            outFastaBuff.write(header + fastaFormat(intergenic.getSequence()));

                            sumaBasesIntergenicas += (intergenic.getEnd() - intergenic.getBegin() + 1);
                        }

                        lastGene = currentGene;

                    }



                    int contigLength = contigsMap.get(currentContig.getId()).length();
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

                System.out.println("Ya esta todo! :)");


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
