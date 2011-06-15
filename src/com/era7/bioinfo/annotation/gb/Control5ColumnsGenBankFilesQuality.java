/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.annotation.gb;

import com.era7.lib.bioinfo.bioinfoutil.Executable;
import com.era7.lib.bioinfoxml.Annotation;
import com.era7.lib.bioinfoxml.ContigXML;
import com.era7.lib.bioinfoxml.PredictedGene;
import com.era7.lib.bioinfoxml.PredictedGenes;
import com.era7.lib.bioinfoxml.PredictedRna;
import com.era7.lib.bioinfoxml.PredictedRnas;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class Control5ColumnsGenBankFilesQuality implements Executable {

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
                    + "1. Annotation XML file\n"
                    + "2. 5 columns TBL file\n");
        } else {

            File xmlFile = new File(args[0]);
            File tblFile = new File(args[1]);
            
            try{
                
                BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
                String line = null;
                StringBuilder stBuilder = new StringBuilder();
                while((line = reader.readLine()) != null){
                    stBuilder.append(line);
                }
                reader.close();
                
                Annotation annotation = new Annotation(stBuilder.toString());
                
                PredictedGenes genes = annotation.getPredictedGenes();
                PredictedRnas rnas = annotation.getPredictedRnas();
                
                List<ContigXML> geneContigs = genes.getContigs();
                List<ContigXML> rnaContigs = rnas.getContigs();
                
                reader = new BufferedReader(new FileReader(tblFile));
                while((line = reader.readLine()) != null){
                    stBuilder.append(line);
                }
                reader.close();
                
                
                
                
                
            }catch(Exception e){
                e.printStackTrace();
            }
            
           
            
            
            
        }
    }
}
