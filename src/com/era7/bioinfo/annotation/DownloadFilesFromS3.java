/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
