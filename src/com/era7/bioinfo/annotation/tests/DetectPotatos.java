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

import com.era7.lib.era7xmlapi.model.XMLElement;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class DetectPotatos {

    public static void main(String[] args) throws Exception {



        if (args.length != 2) {
            System.out.println("The parameteres for this program are:" + "\n"
                    + "1.- Name of the file" + "\n"
                    + "2.- Number of bars expected");
        } else {


            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            String line = null;

            int counter = 0;
            int numberOfBars = Integer.parseInt(args[1]);

            while ((line = reader.readLine()) != null) {
                counter++;
                if (line.trim().startsWith("<Iteration_query-def>")) {
                    
                    char[] array = line.trim().toCharArray();
                    int barsCounter = 0;
                    for (char c : array) {
                        if (c == '|') {
                            barsCounter++;
                        }
                    }

                    if (barsCounter != numberOfBars) {
                        System.out.println("Line: " + counter);
                    }
                    
                }

            }


        }
    }
}
