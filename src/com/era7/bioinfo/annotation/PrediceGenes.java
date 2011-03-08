/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfo.bioinfoutil.CodonUtil;
import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.BlastOutput;
import com.era7.lib.bioinfoxml.Codon;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.Frameshift;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.HspSet;
import com.era7.lib.bioinfoxml.Iteration;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.era7xmlapi.model.XMLElementException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.symbol.SymbolList;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class PrediceGenes implements Executable {

    public static int DIF_SPAN = 30;

    public void execute(ArrayList<String> array) {
        String[] args = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            args[i] = array.get(i);
        }
        try {
            main(args);
        } catch (XMLElementException ex) {
            Logger.getLogger(PrediceGenes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws XMLElementException {


        if (args.length != 5) {
            System.out.println("El programa espera cinco parametros: \n"
                    + "1. Nombre del archivo xml BlastOutput \n"
                    + "2. Nombre del archivo fna con los contigs \n"
                    + "3. Nombre del archivo xml de salida \n"
                    + "4. Maxima longitud de gen (numero entero)\n"
                    + "5. El genoma corresponde a un virus (true/false)\n");
        } else {
            String blastOutputFileString = args[0];
            String fnaFileString = args[1];
            String outFileString = args[2];

            int MAXIMA_LONGITUD_GEN = 0;
            try {
                System.out.println("args[3] = " + args[3]);
                MAXIMA_LONGITUD_GEN = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                System.out.println("El parametro 'Maxima lontigud de gen' especificado no es un numero entero");
                System.exit(-1);
            }
            if (MAXIMA_LONGITUD_GEN <= 0) {
                System.out.println("Debe introducir un numero entero mayor que cero para el parametro 'Maxima lontigud de gen'");
                System.exit(-1);
            }

            boolean isVirus = false;
            String virusSt = args[4];
            if (virusSt.equals("true") || virusSt.equals("false")) {
                isVirus = Boolean.parseBoolean(virusSt);
            } else {
                System.out.println("El valor introducido para el ultimo argumento debe ser \"true\" o  \"false\"");
                System.exit(-1);
            }


            File blastOutputFile, fnaFile, outFile;

            blastOutputFile = new File(blastOutputFileString);
            fnaFile = new File(fnaFileString);

            try {

                HashMap<String, String> contigSequencesMap = new HashMap<String, String>();
                HashMap<String, String> contigsSequencesMapComplementaryInverted = new HashMap<String, String>();

                HashMap<String, ContigXML> contigsXMLMap = new HashMap<String, ContigXML>();

                HashMap<String, Integer> cuentaOrganismos = new HashMap<String, Integer>();


                //Leer datos archivo fna con contigs

                BufferedReader fnaReader = new BufferedReader(new FileReader(fnaFile));
                String tempFna;
                StringBuilder tempSecuenciaSt = new StringBuilder();
                String currentScaffoldId = "";

                System.out.println("Leyendo archivo fna...");

                while ((tempFna = fnaReader.readLine()) != null) {


                    if (tempFna.charAt(0) == '>') {

                        if (tempSecuenciaSt.length() > 0) {
                            contigSequencesMap.put(currentScaffoldId, tempSecuenciaSt.toString());
                            tempSecuenciaSt.delete(0, tempSecuenciaSt.length());
                        }

                        currentScaffoldId = tempFna.substring(1).split(" ")[0];
                    } else {
                        tempSecuenciaSt.append(tempFna);
                    }
                }

                if (tempSecuenciaSt.length() > 0) {
                    contigSequencesMap.put(currentScaffoldId, tempSecuenciaSt.toString());
                }

                //Cerrar archivo de entrada blastouput
                fnaReader.close();

                System.out.println("Ya!! :)");

                //Ahora calculo las complementarias invertidas de todas las secuencias
                //de los contigs (me pueden hacer falta mas adelante)

                System.out.println("Calculando complementarias invertidas....");

                for (String key : contigSequencesMap.keySet()) {
                    SymbolList symL = DNATools.createDNA(contigSequencesMap.get(key));
                    symL = DNATools.reverseComplement(symL);
                    contigsSequencesMapComplementaryInverted.put(key, symL.seqString());
                }
                //-----------------------------------------------------

                System.out.println("Ya!");

                //Leer datos archivo xml Blastoutput
                BufferedReader reader = new BufferedReader(new FileReader(blastOutputFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                System.out.println("Construyendo el xml del blastoutput");

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //Cerrar archivo de entrada blastouput
                reader.close();

                BlastOutput blast = new BlastOutput(stBuilder.toString());

                System.out.println("YA!");

                ArrayList<Iteration> iterations = blast.getBlastOutputIterations();
                System.out.println("Iterations size: " + iterations.size());

                //XMLElement que incluira los genes predichos
                PredictedGenes predictedGenes = new PredictedGenes();
                predictedGenes.setDifSpan(DIF_SPAN);
                predictedGenes.setGeneThreshold(MAXIMA_LONGITUD_GEN);
                // Array donde voy a guardar los hspsets que contienen
                // hsps pegados o unicos
                ArrayList<HspSet> hspSets = new ArrayList<HspSet>();

                // Recorro todas las iteraciones
                for (Iteration iteration : iterations) {
                    ArrayList<Hit> hits = iteration.getIterationHits();

                    System.out.println("Iteration " + iteration.getUniprotIdFromQueryDef() + " has " + hits.size() + "hits");
                    //System.out.println("hits = " + hits);

                    //Saco el organismo de la iteration

                    String iterationOrganism = null;
                    if (hits.size() > 0) {
                        if (iteration.getQueryDef() != null) {
                            iterationOrganism = iteration.getQueryDef().split("\\|")[2].split("OS=")[1].split("GN=")[0].trim();
                        }
                    }

                    // Recorro todos los hits de la iteracion
                    for (Hit hit : hits) {

                        System.out.println("hit = " + hit.getHitDef());

                        // Obtengo todos los hsps del hit
                        ArrayList<Hsp> hsps = hit.getHitHsps();

                        //Limpio lo que no me sirve de los hsps para depurar
                        for (int i = 0; i < hsps.size(); i++) {
                            Hsp hsp = hsps.get(i);
                            hsp.getRoot().removeChild("Hsp_qseq");
                            hsp.getRoot().removeChild("Hsp_hseq");
                            hsp.getRoot().removeChild("Hsp_midline");

                        }

                        HspSet hspSet = new HspSet();
                        hspSet.setOrganism(iterationOrganism);
                        //Apuntamos el id de uniprot al que corresponde en el hit
                        hit.setUniprotID(iteration.getUniprotIdFromQueryDef());
                        hspSet.setHit(hit);

                        System.out.println("Analizando hsps hit, hay " + hsps.size());

                        while (hsps.size() > 0) {

                            System.out.println("Entrando en el while... hsps.size()=" + hsps.size());

                            //Recorro los hsps del hit buscando el de menor query from
                            int indiceHspMenorQueryFrom = 0;
                            boolean orientacionHspMenorQueryFromEsNegativa = !hsps.get(0).getOrientation();
//                            System.out.println("hsps.get(0).getHitFrame() = " + hsps.get(0).getOrientation());
//                            System.out.println("orientacionHspMenorQueryFromEsNegativa = " + orientacionHspMenorQueryFromEsNegativa);
                            int hspQueryFrom = hsps.get(0).getQueryFrom();

                            for (int i = 0; i < hsps.size(); i++) {
                                Hsp tempHsp = hsps.get(i);

                                if (tempHsp.getQueryFrom() < hspQueryFrom) {
                                    hspQueryFrom = tempHsp.getQueryFrom();
                                    orientacionHspMenorQueryFromEsNegativa = (tempHsp.getHitFrame() < 0);
                                    indiceHspMenorQueryFrom = i;
                                }
                            }

                            if (hspSet.size() == 0) {
                                hspSet.addHsp(new Hsp((Element) hsps.get(indiceHspMenorQueryFrom).getRoot().clone()));

                                //lo borro del array para que no forme parte del conjunto de busqueda
                                // de los hsps a pegar
                                hsps.remove(hsps.get(indiceHspMenorQueryFrom));
                            }


                            if (hsps.size() >= 1) {

                                //System.out.println("holaa!");

                                ArrayList<Hsp> hspsCopia = new ArrayList<Hsp>();
                                for (Hsp tempHsp : hsps) {
                                    hspsCopia.add(new Hsp((Element) tempHsp.getRoot().clone()));
                                }

                                //Ahora tengo que quedarme solo con los hsps que tengan la
                                // misma orientacion para ver si puedo pegarle alguno(s)
                                for (int i = 0; i < hspsCopia.size(); i++) {
                                    Hsp tempHsp = hspsCopia.get(i);
                                    System.out.println("tempHsp.getHitFrame() = " + tempHsp.getHitFrame());
                                    System.out.println("orientacionHspMenorQueryFromEsNegativa = " + orientacionHspMenorQueryFromEsNegativa);
                                    if (!((tempHsp.getHitFrame() < 0) == orientacionHspMenorQueryFromEsNegativa)) {
                                        //como no tiene la misma orientacion lo borro del array
                                        hspsCopia.remove(tempHsp);
                                    }
                                }

                                boolean seHaPegadoUnoNuevo = false;

                                while (hspsCopia.size() > 0) {

                                    //Los que quedan ahora en el array son los que tienen la misma orientacion
                                    //Ahora tengo que filtrar y quedarme solo con los que tengan un query_to mayor
                                    //que el ultimo hsp del hsp_set
                                    //System.out.println("hspsCopia = " + hspsCopia.get(0).getNum());
                                    for (int i = 0; i < hspsCopia.size(); i++) {
                                        Hsp tempHsp = hspsCopia.get(i);
                                        if (tempHsp.getQueryTo() <= hspSet.getHspQueryTo()) {
                                            //como el query to es menor lo borro del array
                                            hspsCopia.remove(tempHsp);
                                        }
                                    }
                                    System.out.println("hspsCopia size despues quitar menor query_to = " + hspsCopia.size());

                                    //Selecciono de los que quedan el que tenga menor query from
                                    if (hspsCopia.size() > 0) {

                                        Hsp hspMenorQueryFrom = hspsCopia.get(0);
                                        for (int i = 1; i < hspsCopia.size(); i++) {
                                            Hsp tempppHsp = hspsCopia.get(i);
                                            if (tempppHsp.getQueryFrom() < hspMenorQueryFrom.getQueryFrom()) {
                                                hspMenorQueryFrom = tempppHsp;
                                            }
                                        }

                                        //Ahora tengo que calcular los valores spanHsps y spanQuery
                                        int spanHsps = 0;
                                        if (orientacionHspMenorQueryFromEsNegativa) {
                                            spanHsps = hspSet.getHspHitFrom() - hspMenorQueryFrom.getHitTo();
                                        } else {
                                            spanHsps = hspMenorQueryFrom.getHitFrom() - hspSet.getHspHitTo();
                                        }
                                        int spanQuery = (hspMenorQueryFrom.getQueryFrom() - hspSet.getHspQueryTo()) * 3;
                                        int difSpan = Math.abs((spanQuery - spanHsps));

                                        System.out.println("difSpan = " + difSpan);

                                        if (difSpan <= DIF_SPAN) {
                                            //Hsp is added to the hsp set
                                            Hsp hspToAdd = new Hsp((Element) hspMenorQueryFrom.getRoot().clone());
                                            System.out.println("Adding hsp: " + hspToAdd
                                                    + " from hit: " + hit.getHitDef()
                                                    + " difSpan = " + difSpan);
                                            System.out.println("hspSet = " + hspSet.getHsps());
                                            hspSet.addHsp(hspToAdd);
//                                            System.out.println("ESTE HSP SE HA JUNTADOOOOOOOOOOOOOOOOOOOOOOO!!!!!");
                                            seHaPegadoUnoNuevo = true;
                                            //Once a copy of the original has been added to the set, the original is removed
                                            for (int i = 0; i < hsps.size(); i++) {
                                                Hsp blau = hsps.get(i);
                                                if (blau.getNum().equals(hspMenorQueryFrom.getNum())) {
                                                    hsps.remove(blau);
                                                }
                                            }
                                            hspsCopia.remove(hspMenorQueryFrom);
                                        } else {
                                            //Hsp should not be added to the set, so it remains in the array for
                                            // the next loop
                                            hspsCopia.remove(hspMenorQueryFrom);
                                            seHaPegadoUnoNuevo = false;
                                        }

                                        //System.out.println("hspSet.size() = " + hspSet.size());
                                    }
                                }

                                if (!seHaPegadoUnoNuevo) {
                                    //We add what we found
                                    hspSets.add(hspSet);
                                    //Initializing the new hspset
                                    hspSet = new HspSet();
                                    //Apuntamos el id de uniprot al que corresponde en el hit
                                    hit.setUniprotID(iteration.getUniprotIdFromQueryDef());
                                    hspSet.setHit(hit);
                                }
                            }
                        }

                        // no hay hsps asi q no hago nada y paso al siguiente hit
                        //Now it's time to add the hspSet found
                        hspSets.add(hspSet);

                    }
                }

                System.out.println("Acabe las iteraciones !!! :)");

                //Ahora ya tengo todos los hspSets asi que a buscar los genes!!
                int contadorIdsPredictedGenes = 1;

                for (HspSet hspSet : hspSets) {

                    PredictedGene predictedGene = new PredictedGene();
                    predictedGene.setId(String.valueOf(contadorIdsPredictedGenes));
                    contadorIdsPredictedGenes++;

                    //Now it's time to look for both begin and end codons
                    String currentContigId = hspSet.getHit().getHitDef().split(" ")[0];
                    String hitDnaSequence = contigSequencesMap.get(currentContigId);

                    //System.out.println("hsphitfrom original = " + hspSet.getHspHitFrom());

                    int hspHitFrom = hspSet.getHspHitFrom();
                    int hspHitTo = hspSet.getHspHitTo();

                    //Ahora se coge la secuencia, si la orientacion es
                    //positiva se coge la normal, si no, se coge la complementaria invertida.
                    if (!hspSet.getOrientation()) {
                        hitDnaSequence = contigsSequencesMapComplementaryInverted.get(currentContigId);

                        //Si la orientacion es negativa tengo que transformar las coordenadas
                        //ya que estan del reves
                        hspHitFrom = Math.abs(hspHitFrom - hitDnaSequence.length());
                        hspHitTo = Math.abs(hspHitTo - hitDnaSequence.length());
                        //Ademas ahora tengo que intercambiar los valores porque en las
                        //negativas vienen al reves
                        int tempSwap = hspHitFrom;
                        hspHitFrom = hspHitTo;
                        hspHitTo = tempSwap;

                        //Le sumo uno porque el stop empieza justo despues de la ultima base
                        //y en esta linea el to es ya el from por lo de ser negativo y todo eso
                        hspHitTo++;
                    } else {
                        hspHitFrom -= 1; //Es importante RESTARLE UNO!!
                        //hspHitTo -= 1; //Es importante RESTARLE UNO!!
                        //La resta del hitTo esta comentada porque se anula con la suma de una unidad
                        //que hay que hacer PORQUE EL GEN EMPIEZA JUSTO DESPUES DE LA ULTIMA BASE DEL GEN!!

                    }

                    //ArrayList with extra stop codons
                    ArrayList<Codon> extraStopCodonsArray = new ArrayList<Codon>();

                    //Ahora se busca el codon de inicio en funcion de la orientacion
                    int initCodonPosition = -1;
                    //int stopCodonPosition = -1;

                    int minInitCodonPosition = hspHitFrom;
                    int maxInitCodonPosition = minInitCodonPosition - MAXIMA_LONGITUD_GEN;
                    if (maxInitCodonPosition < 0) {
                        maxInitCodonPosition = 0;
                    }

                    int minRealStopCodonPosition = hspHitTo;
                    int maxRealStopCodonPosition = minRealStopCodonPosition + MAXIMA_LONGITUD_GEN;
                    if (maxRealStopCodonPosition > hitDnaSequence.length() - 3) {
                        maxRealStopCodonPosition = hitDnaSequence.length() - 3;
                    }

                    boolean initCodonFound = false;
                    boolean stopCodonFound = false;

                    //--------------INIT CODON SEARCH POSITIVE ORIENTATION----------------

                    for (int i = minInitCodonPosition; i >= maxInitCodonPosition && !stopCodonFound
                            && !initCodonFound; i -= 3) {
//                        System.out.println("init codon search i = " + i);
//                        System.out.println("hspHitFrom = " + hspHitFrom);
//                        System.out.println("hitDnaSequence.length() = " + hitDnaSequence.length());
//                        System.out.println("hspSet uniprot id = " + hspSet.getHit().getUniprotID());
//                        System.out.flush();

                        if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                            stopCodonFound = true;
                            //stopCodonPosition = i;
                        } else if (!isVirus) {
                            if (CodonUtil.isStartCodon(hitDnaSequence.substring(i))) {
                                initCodonFound = true;
                                initCodonPosition = i;
                            }
                        } else {
                            if (CodonUtil.isStartCodonVirus(hitDnaSequence.substring(i))) {
                                initCodonFound = true;
                                initCodonPosition = i;
                            }
                        }

                    }

                    if (!initCodonFound) {
                        initCodonPosition = minInitCodonPosition;
                    }


                    //Ahora se pone en el tag de start position de PredictedGene
//                    Codon initCodon = new Codon();
//                    initCodon.setType(Codon.START_CODON_TYPE);
//                    initCodon.setIsCanonical(initCodonFound);
                    //Si es negativa hay que reconvertir las coord.
                    if (!hspSet.getOrientation()) {
                        initCodonPosition = Math.abs(initCodonPosition - hitDnaSequence.length());
                    } else {
                        initCodonPosition += 1; //notacion distinta ordenadores
                    }
                    predictedGene.setStartIsCanonical(initCodonFound);
                    predictedGene.setStartPosition(initCodonPosition);
//                    initCodon.setPosition(initCodonPosition);
//                    predictedGene.setStartCodon(initCodon);

                    //-----------------ESTE STOP NO SE GUARDA PORQUE ESTA FUERA DEL GEN !!!!
//                    if (stopCodonFound) {
//                        Codon stopCodon = new Codon();
//                        stopCodon.setType(Codon.STOP_CODON_TYPE);
//                        //Si es negativa hay que reconvertir las coord.
//                        if (!hspSet.getOrientation()) {
//                            stopCodonPosition = Math.abs(stopCodonPosition - hitDnaSequence.length());
//                        } else {
//                            stopCodonPosition += 1; //notacion distinta ordenadores
//                        }
//                        stopCodon.setPosition(stopCodonPosition);
//                        extraStopCodonsArray.add(stopCodon);
//                    }


                    boolean realStopCodonFound = false;
                    int realStopCodonPosition = -1;

                    //--------------REAL STOP CODON SEARCH POSITIVE ORIENTATION----------------

                    for (int i = minRealStopCodonPosition; i < maxRealStopCodonPosition
                            && !realStopCodonFound; i += 3) {
//                        System.out.println("i = " + i);
//                        System.out.println("hitDnaSequence.length() = " + hitDnaSequence.length());
//                        System.out.println("hspSet uniprot id = " + hspSet.getHit().getUniprotID());
//                        System.out.flush();
                        if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                            realStopCodonFound = true;
                            realStopCodonPosition = i;
                        }
                    }

                    if (!realStopCodonFound) {
                        realStopCodonPosition = minRealStopCodonPosition;
                    }

                    //Ahora se pone en el tag de end position de PredictedGene
//                    Codon realStopCodon = new Codon();
//                    realStopCodon.setType(Codon.STOP_CODON_TYPE);
//                    realStopCodon.setIsCanonical(realStopCodonFound);


                    //Ahora tengo que restarle uno a la posicion del codon stop real porque
                    //lo que tiene que indicar la posicion en este caso es la ultima base
                    //del codon anterior al stop ya que este no codifica aminoacido.
                    realStopCodonPosition -= 1;

                    //Si es negativa hay que reconvertir las coord.
                    if (!hspSet.getOrientation()) {
                        realStopCodonPosition = Math.abs(realStopCodonPosition - hitDnaSequence.length());
                    } else {
                        realStopCodonPosition += 1; //Notacion distinta ordenadores
                    }

                    predictedGene.setEndIsCanonical(realStopCodonFound);
                    predictedGene.setEndPosition(realStopCodonPosition);

//                    realStopCodon.setPosition(realStopCodonPosition);
//                    predictedGene.setStopCodon(realStopCodon);


                    //---------------------EXTRA STOP CODONS SEARCH----------------------
                    if (hspSet.hspsAreFromTheSameReadingFrame()) {
                        //SAME READING FRAME

                        for (int i = hspHitFrom; i <= hspHitTo - 3; i += 3) {
                            if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                                Codon extraStopCodon = new Codon();
                                extraStopCodon.setType(Codon.STOP_CODON_TYPE);
                                int tempPosition = i;
                                //Si es negativa hay que reconvertir las coord.
                                if (!hspSet.getOrientation()) {
                                    tempPosition = Math.abs(tempPosition - hitDnaSequence.length());
                                } else {
                                    tempPosition += 1; //Notacion distinta ordenadores
                                }
                                extraStopCodon.setPosition(tempPosition);
                                extraStopCodonsArray.add(extraStopCodon);
                            }
                        }

                    } else {
                        //DIFFERENT READING FRAME

                        for (Hsp lalala : hspSet.getHsps()) {

                            int tempHitFrom = lalala.getHitFrom();
                            int tempHitTo = lalala.getHitTo();
                            //Transformacion coordenadas

                            if (!hspSet.getOrientation()) {
                                //Si la orientacion es negativa tengo que transformar las coordenadas
                                //ya que estan del reves
                                tempHitFrom = Math.abs(tempHitFrom - hitDnaSequence.length());
                                tempHitTo = Math.abs(tempHitTo - hitDnaSequence.length());
                                //Ademas ahora tengo que intercambiar los valores porque en las
                                //negativas vienen al reves
                                int tempSwap = tempHitFrom;
                                tempHitFrom = tempHitTo;
                                tempHitTo = tempSwap;
                            } else {
                                tempHitFrom -= 1; //Es importante RESTARLE UNO!!
                                tempHitTo -= 1; //Es importante RESTARLE UNO!!
                            }

                            for (int i = - 1; i < - 3; i += 3) {
                                if (CodonUtil.isStopCodon(hitDnaSequence.substring(i, i + 3))) {
                                    Codon extraStopCodon = new Codon();
                                    extraStopCodon.setType(Codon.STOP_CODON_TYPE);
                                    int tempPosition = i;
                                    //Si es negativa hay que reconvertir las coord.
                                    if (!hspSet.getOrientation()) {
                                        tempPosition = Math.abs(tempPosition - hitDnaSequence.length());
                                    } else {
                                        tempPosition += 1; //Notacion distinta ordenadores
                                    }
                                    extraStopCodon.setPosition(tempPosition);
                                    extraStopCodonsArray.add(extraStopCodon);
                                }
                            }
                        }

                        //-----------------Frameshifts search--------------------
                        ArrayList<Hsp> tempArray = hspSet.getHsps();
                        for (int i = 0; i < tempArray.size() - 1; i++) {
                            Hsp hsp1 = tempArray.get(i);
                            Hsp hsp2 = tempArray.get(i + 1);
                            int hsp1Rframe = hsp1.getHitFrame();
                            int hsp2Rframe = hsp2.getHitFrame();
                            if (hsp1Rframe != hsp2Rframe) {
                                Frameshift frameshift = new Frameshift();
                                frameshift.setPosition(i + 1);
                                frameshift.setReadingFrameFrom(hsp1Rframe);
                                frameshift.setReadingFrameTo(hsp2Rframe);
                                predictedGene.addFrameShift(frameshift);
                            }
                        }
                    }


                    //Setting the DNA sequence
                    //predictedGene.setDnaSequence(hitDnaSequence);//.substring(hspSet.getHspHitFrom()-1,hspSet.getHspHitTo()));

                    //System.out.println("hitDnaSequence = " + hitDnaSequence);

                    //Adding extra stop codons
                    for (Codon stopCodon : extraStopCodonsArray) {
                        predictedGene.addExtraStopCodon(stopCodon);
                    }


                    //Adding the hspset to predicted gene element
                    predictedGene.setHspSet(hspSet);
                    //Setting the strand value for the predicted gene object
                    predictedGene.setStrand(hspSet.getOrientation());

                    //------------Setting blast hit start y blast hit end--------------------------
                    boolean orientationPredictedGene = predictedGene.getHspSet().getOrientation();
                    int blastHitStart = 0;
                    int blastHitEnd = 0;

                    HspSet setLalala = predictedGene.getHspSet();
                    Hsp hspPrimero = setLalala.get(0);
                    Hsp hspUltimo = setLalala.get(setLalala.size() - 1);
                    if (orientationPredictedGene) {
                        blastHitStart = hspPrimero.getHitFrom();
                        blastHitEnd = hspUltimo.getHitTo();
                    } else {
                        blastHitStart = hspPrimero.getHitTo();
                        blastHitEnd = hspUltimo.getHitFrom();
                    }

                    predictedGene.setBlastHitStart(blastHitStart);
                    predictedGene.setBlastHitEnd(blastHitEnd);
                    //---------------------------------------------------------------------------


                    //-----por ultimo pongo el organismo extraido de la iteration--------
                    predictedGene.setOrganism(hspSet.getOrganism());

                    if (!predictedGene.getOrganism().equals("")) {
                        Integer organismCount = cuentaOrganismos.get(predictedGene.getOrganism());
                        if (organismCount == null) {
                            cuentaOrganismos.put(predictedGene.getOrganism(), 0);
                            organismCount = 0;
                        }
                        cuentaOrganismos.put(predictedGene.getOrganism(), (organismCount + 1));
                    }


                    ContigXML currentContig = contigsXMLMap.get(currentContigId);
                    if (currentContig == null) {
                        currentContig = new ContigXML();
                        currentContig.setId(currentContigId);
                        contigsXMLMap.put(currentContigId, currentContig);
                    }

                    currentContig.addChild(predictedGene);
                }


                //Ahora pegamos los contigs en el elemento predictedgenes
                System.out.println("Pegando contigs en predictedgenes");
                Set<String> contigKeys = contigsXMLMap.keySet();
                for (String contigKey : contigKeys) {
                    ContigXML c = contigsXMLMap.get(contigKey);
                    predictedGenes.addChild(c);
                }

                //Por ultimo cuento cual es el organismo que hay mas para ponerlo
                Set<String> keys = cuentaOrganismos.keySet();
                int maxCount = -1;
                String maxCountOrganism = "";
                for (String key : keys) {
                    int tempCount = cuentaOrganismos.get(key);
                    if (tempCount > maxCount) {
                        maxCount = tempCount;
                        maxCountOrganism = key;
                    }
                }
                //libero memoria de cosas que ya no me sirven
                keys.clear();
                cuentaOrganismos.clear();
                predictedGenes.setPreferentOrganism(maxCountOrganism);

                System.out.println("maxCountOrganism = " + maxCountOrganism);


                outFile = new File(outFileString);
                FileWriter fileWriter = new FileWriter(outFile);
                BufferedWriter buffWriter = new BufferedWriter(fileWriter);
                buffWriter.write(predictedGenes.toString());

                buffWriter.close();
                fileWriter.close();

                System.out.println("XML created with the name: " + outFileString);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }
}
