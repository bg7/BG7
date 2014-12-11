package com.ohnosequences.bg7.tests;

import com.ohnosequences.bg7.gb.ImportGenBankFiles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

/**
 * Created by ppareja on 5/27/2014.
 */
public class ImportGenBankFilesTest  {

    @BeforeClass
    public static void testSetup() {

        System.out.println(new File(".").getAbsolutePath());
    }

    @AfterClass
    public static void testCleanup() {

    }

    @Test
    public void testImportGenBankFile(){
        // ImportGenBankFiles.importGenBankFile(new File("C:/Users/ppareja/Desktop/bg7_tests/NC_000913.gbk"));
    }
}
