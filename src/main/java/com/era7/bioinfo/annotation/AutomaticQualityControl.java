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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class AutomaticQualityControl implements Executable {

    public static final int FACTOR = 100;
    public static final String FINISHED_JOB_STATUS = "FINISHED";
    public static final String BLAST_URL = "http://www.ebi.ac.uk/Tools/services/rest/wublast/run/";
    public static final String CHECK_JOB_STATUS_URL = "http://www.ebi.ac.uk/Tools/services/rest/wublast/status/";
    public static final String JOB_RESULT_URL = "http://www.ebi.ac.uk/Tools/services/rest/wublast/result/";

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
                    + "1. Gene annotation XML filename \n"
                    + "2. Reference protein set (.fasta)\n"
                    + "3. Output TXT filename\n"
                    + "4. Initial Blast XML results filename (the one used at the very beginning of the semiautomatic annotation process)\n");
        } else {

            

            BufferedWriter outBuff = null;

            try {

                File inFile = new File(args[0]);
                File fastaFile = new File(args[1]);
                File outFile = new File(args[2]);
                File blastFile = new File(args[3]);

                
                
                
                //Primero cargo todos los datos del archivo xml del blast
                BufferedReader buffReader = new BufferedReader(new FileReader(blastFile));
                StringBuilder stBuilder = new StringBuilder();
                String line = null;                               
                
                while ((line = buffReader.readLine()) != null) {                    
                    stBuilder.append(line);
                }                
                
                buffReader.close();
                System.out.println("Creating blastoutput...");
                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());
                System.out.println("BlastOutput created! :)");
                stBuilder.delete(0, stBuilder.length());

                HashMap<String, String> blastProteinsMap = new HashMap<String, String>();
                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();
                for (Iteration iteration : iterations) {
                    blastProteinsMap.put(iteration.getQueryDef().split("\\|")[1].trim(), iteration.toString());
                }
                //freeing some memory
                blastOutput = null;
                //------------------------------------------------------------------------
                
                
                //Initializing writer for output file
                outBuff = new BufferedWriter(new FileWriter(outFile));

                //reading gene annotation xml file.....
                buffReader = new BufferedReader(new FileReader(inFile));
                stBuilder = new StringBuilder();
                line = null;
                while ((line = buffReader.readLine()) != null) {
                    stBuilder.append(line);
                }
                buffReader.close();

                XMLElement genesXML = new XMLElement(stBuilder.toString());
                //freeing some memory I don't need anymore
                stBuilder.delete(0, stBuilder.length());

                //reading file with the reference proteins set
                ArrayList<String> proteinsReferenceSet = new ArrayList<String>();
                buffReader = new BufferedReader(new FileReader(fastaFile));
                while ((line = buffReader.readLine()) != null) {
                    if (line.charAt(0) == '>') {
                        proteinsReferenceSet.add(line.split("\\|")[1]);
                    }
                }
                buffReader.close();

                Element pGenes = genesXML.asJDomElement().getChild(PredictedGenes.TAG_NAME);

                List<Element> contigs = pGenes.getChildren(ContigXML.TAG_NAME);

                System.out.println("There are " + contigs.size() + " contigs to be checked... ");

                outBuff.write("There are " + contigs.size() + " contigs to be checked... \n");
                outBuff.write("Proteins reference set: \n");
                for (String st : proteinsReferenceSet) {
                    outBuff.write(st + ",");
                }
                outBuff.write("\n");

                
                for (Element elem : contigs) {
                    ContigXML contig = new ContigXML(elem);

                    //escribo el id del contig en el que estoy
                    outBuff.write("Checking contig: " + contig.getId() + "\n");
                    outBuff.flush();

                    List<XMLElement> geneList = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    System.out.println("geneList.size() = " + geneList.size());

                    int numeroDeGenesParaAnalizar = geneList.size() / FACTOR;
                    if (numeroDeGenesParaAnalizar == 0) {
                        numeroDeGenesParaAnalizar++;
                    }

                    ArrayList<Integer> indicesUtilizados = new ArrayList<Integer>();

                    outBuff.write("\nThe contig has " + geneList.size() + " predicted genes, let's analyze: " + numeroDeGenesParaAnalizar + "\n");

                    for (int j = 0; j < numeroDeGenesParaAnalizar; j++) {
                        int geneIndex;

                        boolean geneIsDismissed = false;
                        do {
                            geneIsDismissed = false;
                            geneIndex = (int) Math.round(Math.floor(Math.random() * geneList.size()));
                            PredictedGene tempGene = new PredictedGene(geneList.get(geneIndex).asJDomElement());
                            if(tempGene.getStatus().equals(PredictedGene.STATUS_DISMISSED)){
                                geneIsDismissed = true;
                            }
                        } while (indicesUtilizados.contains(new Integer(geneIndex)) && geneIsDismissed);
                        
                        indicesUtilizados.add(geneIndex);
                        System.out.println("geneIndex = " + geneIndex);

                        //Ahora hay que sacar el gen correspondiente al indice y hacer el control de calidad
                        PredictedGene gene = new PredictedGene(geneList.get(geneIndex).asJDomElement());

                        outBuff.write("\nAnalyzing gene with id: " + gene.getId() + " , annotation uniprot id: " + gene.getAnnotationUniprotId() + "\n");
                        outBuff.write("eValue: " + gene.getEvalue() + "\n");


                        //--------------PETICION POST HTTP BLAST----------------------
                        PostMethod post = new PostMethod(BLAST_URL);
                        post.addParameter("program", "blastx");
                        post.addParameter("sequence", gene.getSequence());
                        post.addParameter("database", "uniprotkb");
                        post.addParameter("email", "ppareja@era7.com");
                        post.addParameter("exp", "1e-10");
                        post.addParameter("stype", "dna");

                        // execute the POST
                        HttpClient client = new HttpClient();
                        int status = client.executeMethod(post);
                        System.out.println("status post = " + status);
                        InputStream inStream = post.getResponseBodyAsStream();

                        String fileName = "jobid.txt";
                        FileOutputStream outStream = new FileOutputStream(new File(fileName));
                        byte[] buffer = new byte[1024];
                        int len;

                        while ((len = inStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, len);
                        }
                        outStream.close();

                        //Once the file is created I just have to read one line in order to extract the job id
                        buffReader = new BufferedReader(new FileReader(new File(fileName)));
                        String jobId = buffReader.readLine();
                        buffReader.close();

                        System.out.println("jobId = " + jobId);


                        //--------------HTTP CHECK JOB STATUS REQUEST----------------------
                        GetMethod get = new GetMethod(CHECK_JOB_STATUS_URL + jobId);
                        String jobStatus = "";
                        do {

                            try {
                                Thread.sleep(1000);//sleep for 1000 ms                                
                            } catch (InterruptedException ie) {
                                //If this thread was intrrupted by nother thread
                            }

                            status = client.executeMethod(get);
                            //System.out.println("status get = " + status);

                            inStream = get.getResponseBodyAsStream();

                            fileName = "jobStatus.txt";
                            outStream = new FileOutputStream(new File(fileName));

                            while ((len = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, len);
                            }
                            outStream.close();

                            //Once the file is created I just have to read one line in order to extract the job id
                            buffReader = new BufferedReader(new FileReader(new File(fileName)));
                            jobStatus = buffReader.readLine();
                            //System.out.println("jobStatus = " + jobStatus);
                            buffReader.close();

                        } while (!jobStatus.equals(FINISHED_JOB_STATUS));

                        //Once I'm here the blast should've already finished

                        //--------------JOB RESULTS HTTP REQUEST----------------------
                        get = new GetMethod(JOB_RESULT_URL + jobId + "/out");

                        status = client.executeMethod(get);
                        System.out.println("status get = " + status);

                        inStream = get.getResponseBodyAsStream();

                        fileName = "jobResults.txt";
                        outStream = new FileOutputStream(new File(fileName));

                        while ((len = inStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, len);
                        }
                        outStream.close();


                        //--------parsing the blast results file-----

                        TreeSet<GeneEValuePair> featuresBlast = new TreeSet<GeneEValuePair>();

                        buffReader = new BufferedReader(new FileReader(new File(fileName)));
                        while ((line = buffReader.readLine()) != null) {
                            if (line.length() > 3) {
                                String prefix = line.substring(0, 3);
                                if (prefix.equals("TR:") || prefix.equals("SP:")) {
                                    String[] columns = line.split(" ");
                                    String id = columns[1];
                                    //System.out.println("id = " + id);

                                    String e = "";

                                    String[] arraySt = line.split("\\.\\.\\.");
                                    if (arraySt.length > 1) {
                                        arraySt = arraySt[1].trim().split(" ");
                                        int contador = 0;
                                        for (int k = 0; k < arraySt.length && contador <= 2; k++) {
                                            String string = arraySt[k];
                                            if (!string.equals("")) {
                                                contador++;
                                                if (contador == 2) {
                                                    e = string;
                                                }
                                            }

                                        }
                                    } else {
                                        //Number before e-
                                        String[] arr = arraySt[0].split("e-")[0].split(" ");
                                        String numeroAntesE = arr[arr.length - 1];
                                        String numeroDespuesE = arraySt[0].split("e-")[1].split(" ")[0];
                                        e = numeroAntesE + "e-" + numeroDespuesE;
                                    }

                                    double eValue = Double.parseDouble(e);
                                    //System.out.println("eValue = " + eValue);
                                    GeneEValuePair g = new GeneEValuePair(id, eValue);
                                    featuresBlast.add(g);
                                }
                            }
                        }

                        GeneEValuePair currentGeneEValuePair = new GeneEValuePair(gene.getAnnotationUniprotId(), gene.getEvalue());

                        System.out.println("currentGeneEValuePair.id = " + currentGeneEValuePair.id);
                        System.out.println("currentGeneEValuePair.eValue = " + currentGeneEValuePair.eValue);
                        boolean blastContainsGene = false;
                        for (GeneEValuePair geneEValuePair : featuresBlast) {
                            if (geneEValuePair.id.equals(currentGeneEValuePair.id)) {
                                blastContainsGene = true;
                                //le pongo la e que tiene en el wu-blast para poder comparar
                                currentGeneEValuePair.eValue = geneEValuePair.eValue;
                                break;
                            }
                        }


                        if (blastContainsGene) {
                            outBuff.write("The protein was found in the WU-BLAST result.. \n");
                            //Una vez que se que esta en el blast tengo que ver que sea la mejor
                            GeneEValuePair first = featuresBlast.first();
                            outBuff.write("Protein with best eValue according to the WU-BLAST result: " + first.id + " , " + first.eValue + "\n");
                            if (first.id.equals(currentGeneEValuePair.id)) {
                                outBuff.write("Proteins with best eValue match up \n");
                            } else {
                                if (first.eValue == currentGeneEValuePair.eValue) {
                                    outBuff.write("The one with best eValue is not the same protein but has the same eValue \n");
                                } else if (first.eValue > currentGeneEValuePair.eValue) {
                                    outBuff.write("The one with best eValue is not the same protein but has a worse eValue :) \n");
                                } else {
                                    outBuff.write("The best protein from BLAST has an eValue smaller than ours, checking if it's part of the reference set...\n");
                                    //System.exit(-1);
                                    if (proteinsReferenceSet.contains(first.id)) {
                                        //The protein is in the reference set and that shouldn't happen
                                        outBuff.write("The protein was found on the reference set, checking if it belongs to the same contig...\n");
                                        String iterationSt = blastProteinsMap.get(gene.getAnnotationUniprotId());
                                        if (iterationSt != null) {
                                            outBuff.write("The protein was found in the BLAST used at the beginning of the annotation process.\n");
                                            Iteration iteration = new Iteration(iterationSt);
                                            ArrayList<Hit> hits = iteration.getIterationHits();
                                            boolean contigFound = false;
                                            Hit errorHit = null;
                                            for (Hit hit : hits) {
                                                if (hit.getHitDef().indexOf(contig.getId()) >= 0) {
                                                    contigFound = true;
                                                    errorHit = hit;
                                                    break;
                                                }
                                            }
                                            if (contigFound) {
                                                outBuff.write("ERROR: A hit from the same contig was find in the Blast file: \n" + errorHit.toString() + "\n");
                                            } else {
                                                outBuff.write("There is no hit with the same contig! :)\n");
                                            }
                                        }else{
                                            outBuff.write("The protein is NOT in the BLAST used at the beginning of the annotation process.\n");
                                        }

                                    } else {
                                        //The protein was not found on the reference set so everything's ok
                                        outBuff.write("The protein was not found on the reference, everything's ok :)\n");
                                    }
                                }
                            }

                        } else {
                            outBuff.write("The protein was NOT found on the WU-BLAST !! :( \n");

                            //System.exit(-1);
                        }


                    }

                }
                


            } catch (Exception ex) {
                ex.printStackTrace();
            }finally{
                try {
                    //closing outputfile
                    outBuff.close();
                } catch (IOException ex) {
                    Logger.getLogger(AutomaticQualityControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }


        }
    }
}
