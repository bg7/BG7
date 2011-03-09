/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation;

import com.era7.lib.bioinfoxml.Hsp;
import com.era7.lib.bioinfoxml.HspSet;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom.Element;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class HspSetTester {

    public static int DIF_SPAN = 30;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("This program expects one parameter: \n"
                    + "1. XML filename with the flat list of hspset \n");
        } else {
            BufferedReader reader = null;
            try {

                boolean errorsFound = false;
                boolean orientationErrorsFound = false;
                boolean hspQueryFromErrorsFound = false;
                boolean hspQueryToErrorsFound = false;
                boolean difSpanErrorsFound = false;

                String inFileString = args[0];
                File inFile;

                inFile = new File(inFileString);
                reader = new BufferedReader(new FileReader(inFile));
                String temp;
                StringBuilder stBuilder = new StringBuilder();

                while ((temp = reader.readLine()) != null) {
                    stBuilder.append(temp);
                }
                //closing input file
                reader.close();

                XMLElement hspSetsXML = new XMLElement(stBuilder.toString());
                System.out.println("There are " + hspSetsXML.getChildren().size()
                        + " HspSets to analize...");
                List<Element> list = hspSetsXML.getRoot().getChildren();

                ArrayList<HspSet> hspSets = new ArrayList<HspSet>();
                for (Element elem : list) {
                    HspSet hspSet = new HspSet(elem);
                    hspSets.add(hspSet);
                }


                //--------------------ORIENTATION TEST-------------------------
                //-----------------------------------------------------------------
                for (HspSet hspSet : hspSets) {

                    boolean orientation = hspSet.get(0).getOrientation();
                    for (Hsp hsp : hspSet.getHsps()) {
                        if (hsp.getOrientation() != orientation) {
                            orientationErrorsFound = true;
                            System.out.println("Orientation test failed for Hsp: " + hsp.getNum()
                                    + " from hit: " + hspSet.getHit().getUniprotID());
                        }
                    }

                }
                if (!orientationErrorsFound) {
                    System.out.println("Orientation test passed!");
                } else {
                    System.out.println("Orientation test failed... :(");
                }
                //-----------------------------------------------------------------
                //-----------------------------------------------------------------



                //--------------------HSP QUERY FROM TEST-------------------------
                //-----------------------------------------------------------------
                for (HspSet hspSet : hspSets) {
                    ArrayList<Hsp> hsps = hspSet.getHsps();
                    for (int i = 0; i < hsps.size(); i++) {
                        Hsp hsp = hsps.get(i);
                        int queryFrom = hsp.getQueryFrom();
                        for (int j = i + 1; j < hsps.size(); j++) {
                            Hsp hsp2 = hsps.get(j);
                            if (!(queryFrom < hsp2.getQueryFrom())) {
                                hspQueryFromErrorsFound = true;
                                System.out.println("HspFrom test failed for Hsp: " + hsp2.getNum()
                                        + "from hit: " + hspSet.getHit().getUniprotID());
                            }
                        }
                    }

                }
                if (!hspQueryFromErrorsFound) {
                    System.out.println("Hsp query from test passed!");
                } else {
                    System.out.println("Hsp query from test failed... :(");
                }
                //-----------------------------------------------------------------
                //-----------------------------------------------------------------



                //--------------------HSP QUERY TO TEST-------------------------
                //-----------------------------------------------------------------
                for (HspSet hspSet : hspSets) {
                    ArrayList<Hsp> hsps = hspSet.getHsps();
                    for (int i = 0; i < hsps.size(); i++) {
                        Hsp hsp = hsps.get(i);
                        int queryTo = hsp.getQueryTo();

                        for (int j = i - 1; j >= 0; j--) {
                            Hsp hsp2 = hsps.get(j);
                            if (!(queryTo > hsp2.getQueryTo())) {
                                hspQueryToErrorsFound = true;
                                System.out.println("HspTo test failed for Hsp: " + hsp2.getNum()
                                        + "from hit: " + hspSet.getHit().getUniprotID());
                            }
                        }
                    }

                }
                if (!hspQueryFromErrorsFound) {
                    System.out.println("Hsp query to test passed!");
                } else {
                    System.out.println("Hsp query to test failed... :(");
                }
                //-----------------------------------------------------------------
                //-----------------------------------------------------------------

                //--------------------DIF SPAN TEST-------------------------
                //-----------------------------------------------------------------
                for (HspSet hspSet : hspSets) {

                    boolean orientation = hspSet.getOrientation();                   

                    ArrayList<Hsp> hsps = hspSet.getHsps();
                    for (int i = 1; i < hsps.size(); i++) {

                        Hsp hsp = hsps.get(i);
                        HspSet subHspSet = new HspSet();
                        //building the sub-hspset
                        for(int j=0;j<i;j++){
                            subHspSet.addHsp(hsps.get(j));
                        }

                        //Now values spanHsps & spanQuery must be calculated
                        int spanHsps = 0;
                        if (!orientation) {
                            spanHsps = subHspSet.getHspHitFrom() - hsp.getHitTo();
                        } else {
                            spanHsps = hsp.getHitFrom() - subHspSet.getHspHitTo();
                        }
                        int spanQuery = (hsp.getQueryFrom() - subHspSet.getHspQueryTo()) * 3;
                        int difSpan = Math.abs((spanQuery - spanHsps));

                        if (difSpan > DIF_SPAN) {
//                            System.out.println("orientation = " + orientation);
//                            System.out.println("hit frame: " + hspSet.get(0).getHitFrame());
                            difSpanErrorsFound = true;
                            System.out.println("(" + difSpan + ") Dif span test failed for Hsp: " + hsp.getNum()
                                    + "from hit: " + hspSet.getHit().getUniprotID());
//                            System.out.println("hsp = " + hsp);
//                            System.out.println("subHspSet = " + subHspSet.getHsps());
                        }

                    }

                }
                if (!difSpanErrorsFound) {
                    System.out.println("Dif span test passed!");
                } else {
                    System.out.println("Dif span test failed... :(");
                }
                //-----------------------------------------------------------------
                //-----------------------------------------------------------------


                errorsFound = orientationErrorsFound || hspQueryFromErrorsFound
                        || hspQueryToErrorsFound || difSpanErrorsFound;
                if (errorsFound) {
                    System.out.println("Test was not passed successfully. Errors were found");
                } else {
                    System.out.println("Test was passed successfuly! :)");
                }



            } catch (Exception ex) {
                Logger.getLogger(HspSetTester.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
}
