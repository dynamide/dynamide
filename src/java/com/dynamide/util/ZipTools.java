package com.dynamide.util;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.zip.*;

/**
 * User: laramie
 * $LastChangedRevision:  $
 * $LastChangedDate:  $
 */
public class ZipTools {

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    public static void forceParentDirectories(String filename) throws Exception {
        File theFile = new File(filename);
        String parent = theFile.getParent();
        if (parent != null) {
            File p = new File(parent);
            if (p.exists()){
               //System.out.println("NOT Making directory, it exists: " + p.getCanonicalPath());
            } else {
                p.mkdirs();
                //System.out.println("Making directory: " + p.getCanonicalPath());
            }
        }
    }

    public static class UnzipResults {
        public final List<String> messages = new ArrayList<String>();
        public final List<String> extracted  = new ArrayList<String>();
        public final List<String> skipped  = new ArrayList<String>();
    }

    /**
     * It is HIGHLY recommended to use a baseOutputDir, such as "./", or
     * a local directory you know, such as "/tmp/foo", to prevent
     * files from being unzipped in your root directory.
     */
    public static final UnzipResults  unzip(String zipfileName, String baseOutputDir) {
        return unzip(zipfileName, baseOutputDir, false);
    }
    public static final UnzipResults unzip(String zipfileName, String baseOutputDir, boolean overwriteExistingFiles) {
        UnzipResults results = new UnzipResults();
        results.messages.add("init: ==============================================");
        results.messages.add("init-zipfile: "+zipfileName);
        results.messages.add("init-outputDir: "+baseOutputDir);
        results.messages.add("init-overwrite: "+overwriteExistingFiles);
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(zipfileName);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String entryName = entry.getName();
                String theName = baseOutputDir + '/' + entryName;
                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    File targetDir =  (new File(theName));
                    if (targetDir.exists()){
                        results.messages.add("mkdir-exists: "+theName);
                    } else {
                        results.messages.add("mkdir: "+theName);
                        targetDir.mkdirs();    //TODO: this name has not been vetted for character, conflicting paths, etc.
                    }
                    continue;
                }
                //(new File(theName)).mkdirs();
                File test = new File(theName) ;
                if (test.exists()){
                    if (overwriteExistingFiles){
                        results.messages.add("overwrite: "+theName);
                        results.extracted.add(theName);
                    } else {
                        results.messages.add("skip-existing: "+theName);
                        results.skipped.add(theName);
                        continue;
                    }
                } else {
                    results.messages.add("extract: "+theName);
                    results.extracted.add(theName);
                }
                forceParentDirectories(theName);
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(theName)));
            }
            zipFile.close();
            return results;
        } catch (Exception ioe) {
            System.err.println("Unhandled exception processing ("+zipfileName+"):"+ioe);
            results.messages.add("error: "+ioe);
            ioe.printStackTrace();

            System.err.println("exists: "+(new File(zipfileName)).exists());
            return results;
        }
    }

    public static void zipDiveDirectory(int stripLeadingPathChars, String directory, String zipFilename) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename));
        zipDir(stripLeadingPathChars, directory, zos);
        zos.close();
    }

    public static void zipDir(int stripLeadingPathChars, String dir2zip, ZipOutputStream zos) throws Exception {
        File fzipDir = new File(dir2zip);
        if (!fzipDir.exists()) {
            System.out.println("dir doesn't exist: " + dir2zip);
            return;
        }
        String[] dirList = fzipDir.list(); //get a listing of the directory content
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        //loop through dirList, and zip the files
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(fzipDir, dirList[i]);
            if (f.isDirectory()) {
                //if the File object is a directory, call this function again to add its content recursively
                zipDir(stripLeadingPathChars, f.getPath(), zos); //DIVE!
                continue;
            }
            //if we reached here, the File object f was not a directory
            String fpath = f.getPath();
            String nameInArchive = fpath.substring(stripLeadingPathChars, fpath.length());
            addToZip(zos, fpath, nameInArchive);
        }
    }

    public static void addToZip(ZipOutputStream zos, String filename, String nameInArchive) throws Exception {
        File file = new File(filename);
        if (!file.exists()) {
            System.err.println("File does not exist, skipping: " + filename);
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int bytesRead;
        byte[] buffer = new byte[1024];
        CRC32 crc = new CRC32();
        crc.reset();
        while ((bytesRead = bis.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        bis.close();
        // Reset to beginning of input stream
        bis = new BufferedInputStream(new FileInputStream(file));
        String nameInArchiveFixed = nameInArchive.replace("\\", "/");
        ZipEntry entry = new ZipEntry(nameInArchiveFixed);
        entry.setMethod(ZipEntry.STORED);
        entry.setCompressedSize(file.length());
        entry.setSize(file.length());
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        while ((bytesRead = bis.read(buffer)) != -1) {
            zos.write(buffer, 0, bytesRead);
        }
        bis.close();
    }

}

