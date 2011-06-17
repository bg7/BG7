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
package com.era7.bioinfo.annotation.tests;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Hit;
import com.era7.lib.bioinfoxml.Iteration;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetOrganismFreqFromBlastIterations implements Executable {

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
                    + "1. BlastOutput XML filename \n"
                    + "2. Taxonomy results filename \n");
        } else {


            String blastOutputFileString = args[0];
            String outFileString = args[1];

            File blastOutputFile, outFile;

            blastOutputFile = new File(blastOutputFileString);
            outFile = new File(outFileString);

            try {



                HashMap<String, Integer> cuentaOrganismosWithHits = new HashMap<String, Integer>();
                HashMap<String, Integer> cuentaOrganismos = new HashMap<String, Integer>();
                HashMap<String, HashSet<String>> proteinsPerOrganismWitHits = new HashMap<String, HashSet<String>>();
                HashMap<String, HashSet<String>> proteinsPerOrganism = new HashMap<String, HashSet<String>>();

                //Reading data from xml Blastoutput
                BufferedReader reader = new BufferedReader(new FileReader(blastOutputFile));
                String line;


                System.out.println("Parsing blastoutput XML file");

                BufferedWriter outBuff = new BufferedWriter(new FileWriter(outFile));

                //---writing header----
                outBuff.write("organism\tnumber_of_iterations_with_hits\tnumber_of_iterations\tprots_per_organism_with_hits\tprots_per_organism\n");

                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("<Iteration>")) {

                        StringBuilder iterationStBuilder = new StringBuilder();

                        do {
                            iterationStBuilder.append(line);
                            line = reader.readLine();

                        } while (line != null && !line.trim().startsWith("</Iteration>"));

                        if (line != null) {
                            iterationStBuilder.append(line);

                            Iteration iteration = new Iteration(iterationStBuilder.toString());

                            ArrayList<Hit> hits = iteration.getIterationHits();

                            //System.out.println("Iteration " + iteration.getUniprotIdFromQueryDef() + " has " + hits.size() + "hits");
                            //System.out.println("hits = " + hits);

                            //Retrieving organism from the iteration

                            String iterationOrganismSt = null;
                            boolean withHits = hits.size() > 0;

                            if (iteration.getQueryDef() != null) {

                                String step1 = iteration.getQueryDef().split("\\|")[2];
                                String[] step2Array = step1.split("OS=");

                                if (step2Array.length < 2) {
                                    System.out.println("iteration.getQueryDef() = " + iteration.getQueryDef());
                                    System.out.println("step1 = " + step1);
                                    System.out.println("step2Array = " + step2Array);

                                    iterationOrganismSt = iteration.getQueryDef().split("\\|")[3].split("OS=")[1].split("GN=")[0].trim();

                                    System.out.println("iterationOrganismSt = " + iterationOrganismSt);

                                } else {
                                    iterationOrganismSt = iteration.getQueryDef().split("\\|")[2].split("OS=")[1].split("GN=")[0].trim();
                                }

                                //-----------Initializing maps------------------
                                Integer organismCountWithHits = cuentaOrganismosWithHits.get(iterationOrganismSt);
                                Integer organismCount = cuentaOrganismos.get(iterationOrganismSt);

                                if (organismCountWithHits == null) {
                                    cuentaOrganismosWithHits.put(iterationOrganismSt, 0);
                                    organismCountWithHits = 0;
                                    //--------initializing maps for the organism--------------
                                    proteinsPerOrganismWitHits.put(iterationOrganismSt, new HashSet<String>());
                                }

                                if (organismCount == null) {
                                    cuentaOrganismos.put(iterationOrganismSt, 0);
                                    organismCount = 0;
                                    //--------initializing maps for the organism--------------
                                    proteinsPerOrganism.put(iterationOrganismSt, new HashSet<String>());

                                }

                                if (withHits) {
                                    cuentaOrganismosWithHits.put(iterationOrganismSt, (organismCountWithHits + 1));
                                    proteinsPerOrganismWitHits.get(iterationOrganismSt).add(iteration.getUniprotIdFromQueryDef());

                                }
                                cuentaOrganismos.put(iterationOrganismSt, (organismCount + 1));
                                proteinsPerOrganism.get(iterationOrganismSt).add(iteration.getUniprotIdFromQueryDef());

                            }

                        }
                    }

                }
                //closing input reader
                reader.close();


                Set<String> keys = cuentaOrganismosWithHits.keySet();

                for (String key : keys) {
                    outBuff.write(key + "\t"
                            + cuentaOrganismosWithHits.get(key) + "\t"
                            + cuentaOrganismos.get(key) + "\t"
                            + proteinsPerOrganismWitHits.get(key).size() + "\t"
                            + proteinsPerOrganism.get(key).size() + "\n");
                }

                outBuff.close();
                System.out.println("Out file created successfully! :)");


            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    }
}
