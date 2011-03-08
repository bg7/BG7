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
 * @author ppareja
 */
public class DescargaArchivoDeS3 {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("El programa espera tres parametros: \n"
                    + "1. Nombre del archivo a descargar \n"
                    + "2. Nombre del bucket donde se encuentra el archivo \n"
                    + "3. Directorio de descarga");
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
