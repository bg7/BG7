/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class ControlDeCalidadAutomatico implements Executable {

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
            System.out.println("El programa espera cuatro parametros: \n"
                    + "1. Nombre del archivo xml con los genes \n"
                    + "2. Conjunto de proteinas de referencia (.fasta)\n"
                    + "3. Nombre del archivo txt de salida\n"
                    + "4. Nombre del archivo xml resultado del BLAST inicial para el proceso de anotacion\n");
        } else {


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
                System.out.println("Creando blastoutput...");
                BlastOutput blastOutput = new BlastOutput(stBuilder.toString());
                System.out.println("BlastOutput creado! :)");
                stBuilder.delete(0, stBuilder.length());

                HashMap<String, String> blastProteinsMap = new HashMap<String, String>();
                ArrayList<Iteration> iterations = blastOutput.getBlastOutputIterations();
                for (Iteration iteration : iterations) {
                    blastProteinsMap.put(iteration.getQueryDef().split("\\|")[1].trim(), iteration.toString());
                }
                //libero memoria
                blastOutput = null;
                //------------------------------------------------------------------------

                //Preparo el writer para el archivo de salida
                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

                //Primero leo el archivo con el xml de los genes.....
                buffReader = new BufferedReader(new FileReader(inFile));
                stBuilder = new StringBuilder();
                line = null;
                while ((line = buffReader.readLine()) != null) {
                    stBuilder.append(line);
                }
                buffReader.close();

                XMLElement genesXML = new XMLElement(stBuilder.toString());
                //libero memoria que ya no necesito
                stBuilder.delete(0, stBuilder.length());

                //Ahora leo el archivo con las proteinas de referencia
                ArrayList<String> proteinasConjuntoReferencia = new ArrayList<String>();
                buffReader = new BufferedReader(new FileReader(fastaFile));
                while ((line = buffReader.readLine()) != null) {
                    if (line.charAt(0) == '>') {
                        proteinasConjuntoReferencia.add(line.split("\\|")[1]);
                    }
                }
                buffReader.close();

                Element pGenes = genesXML.asJDomElement().getChild(PredictedGenes.TAG_NAME);

                List<Element> contigs = pGenes.getChildren(ContigXML.TAG_NAME);

                System.out.println("Hay " + contigs.size() + " contigs que controlar... ");

                outBuff.write("Hay " + contigs.size() + " contigs que controlar... \n");
                outBuff.write("Conjunto de proteinas de referencia: \n");
                for (String st : proteinasConjuntoReferencia) {
                    outBuff.write(st + ",");
                }
                outBuff.write("\n");

                for (Element elem : contigs) {
                    ContigXML contig = new ContigXML(elem);

                    //escribo el id del contig en el que estoy
                    outBuff.write("Controlando calidad del contig: " + contig.getId() + "\n");
                    outBuff.flush();

                    List<XMLElement> geneList = contig.getChildrenWith(PredictedGene.TAG_NAME);
                    System.out.println("geneList.size() = " + geneList.size());

                    int numeroDeGenesParaAnalizar = geneList.size() / FACTOR;
                    if (numeroDeGenesParaAnalizar == 0) {
                        numeroDeGenesParaAnalizar++;
                    }

                    ArrayList<Integer> indicesUtilizados = new ArrayList<Integer>();

                    outBuff.write("\nEl contig tiene " + geneList.size() + " genes predichos, vamos a analizar: " + numeroDeGenesParaAnalizar + "\n");

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

                        outBuff.write("\nAnalizando gen con id: " + gene.getId() + " , annotation uniprot id: " + gene.getAnnotationUniprotId() + "\n");
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

                        //Una vez creado el archivo leo su unica linea para extraer el identificador del job
                        buffReader = new BufferedReader(new FileReader(new File(fileName)));
                        String jobId = buffReader.readLine();
                        buffReader.close();

                        System.out.println("jobId = " + jobId);


                        //--------------PETICION HTTP CHECK JOB STATUS----------------------
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

                            //Una vez creado el archivo leo su unica linea para extraer el identificador del job
                            buffReader = new BufferedReader(new FileReader(new File(fileName)));
                            jobStatus = buffReader.readLine();
                            //System.out.println("jobStatus = " + jobStatus);
                            buffReader.close();

                        } while (!jobStatus.equals(FINISHED_JOB_STATUS));

                        //Cuando llegue aqui el blast deberia haber acabado ya

                        //--------------PETICION HTTP RESULTADOS DEL JOB----------------------
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


                        //--------Ahora recorro el archivo con los resultados de blast-----

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
                                        //Numero antes de e-
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
                            outBuff.write("La proteina esta en el WU-BLAST hecho por internet.. \n");
                            //Una vez que se que esta en el blast tengo que ver que sea la mejor
                            GeneEValuePair first = featuresBlast.first();
                            outBuff.write("Proteina con mejor valor de e segun el WU-BLAST hecho por internet: " + first.id + " , " + first.eValue + "\n");
                            if (first.id.equals(currentGeneEValuePair.id)) {
                                outBuff.write("La de mejor e coincide \n");
                            } else {
                                if (first.eValue == currentGeneEValuePair.eValue) {
                                    outBuff.write("La de mejor e no es la misma pero tiene la misma e \n");
                                } else if (first.eValue > currentGeneEValuePair.eValue) {
                                    outBuff.write("La de mejor e no es la misma pero tiene una e peor :) \n");
                                } else {
                                    outBuff.write("La mejor proteina del BLAST tiene una e menor que la nuestra, comprobando si se encuentra en el conjunto de referencia...\n");
                                    //System.exit(-1);
                                    if (proteinasConjuntoReferencia.contains(first.id)) {
                                        //La proteina se encuentra en el conjunto de referencia, y eso no deberia pasar
                                        outBuff.write("La proteina se encuentra en el conjunto de referencia, viendo si es del mismo contig...\n");
                                        String iterationSt = blastProteinsMap.get(gene.getAnnotationUniprotId());
                                        if (iterationSt != null) {
                                            outBuff.write("La proteina se encuentra en el BLAST inicial del proceso de anotacion.\n");
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
                                                outBuff.write("ERROR: Se ha encontrado un hit del mismo contig en el blast: \n" + errorHit.toString() + "\n");
                                            } else {
                                                outBuff.write("No existe ningun hit con el mismo contig! :)\n");
                                            }
                                        }else{
                                            outBuff.write("La proteina NO esta en el BLAST inicial del proceso de anotacion.\n");
                                        }

                                    } else {
                                        //La proteina no esta en el conjunto de referencia, asi que todo esta normal
                                        outBuff.write("La proteina no esta en el conjunto de referencia asi que todo esta bien :)\n");
                                    }
                                }
                            }

                        } else {
                            outBuff.write("La proteina NO esta en el WU-BLAST hecho por internet!! :( \n");

                            //System.exit(-1);
                        }



                    }

                }




                //cierro el archivo de salida
                outBuff.close();


            } catch (Exception ex) {
                ex.printStackTrace();
            }


        }
    }
}
