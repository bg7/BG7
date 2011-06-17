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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.era7.bioinfo.bioinfoaws.s3.S3FileDownloader;
import java.io.File;

/**
 * 
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class DownloadFilesFromS3 {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("This program expects three parameters: \n"
                    + "1. Name of the file to be downloaded \n"
                    + "2. Name of the bucket the file belongs to \n"
                    + "3. Name of the folder where the file should be downloaded");
        } else {

            try {
                AWSCredentials credentials = new PropertiesCredentials(new File("AwsCredentials.properties"));
                AmazonS3 s3 = new AmazonS3Client(credentials);

                S3FileDownloader.downloadFileFromS3(args[0],
                                            args[1],
                                            s3,
                                            new File(args[2]));
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
}
