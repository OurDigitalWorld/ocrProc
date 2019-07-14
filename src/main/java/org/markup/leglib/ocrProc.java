package org.markup.leglib;

/*
    ocrProc.java
 
    - art rhyno <https://github.com/artunit/>
    (c) Copyright GNU General Public License (GPL)
*/

//Tess4J
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.ITesseract.RenderedFormat;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.*;
import net.sourceforge.tess4j.Word;

//PdfBox
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

//Apache Commons
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//ImageIO
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;

//JCL
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class ocrProc {
    public static float DEFAULT_CONF = 50.0f;
    public static String PDF_EXT = "pdf";
    public static String TEXT_EXT = "txt";
    public static String TEXT_ENC = "utf8";

    public static CommandLine cl;
    public static Options options;

    public static ITesseract tesseract;
    public static Log logger;
    public static PdfBoxUtilities pdfU;
    public static List<RenderedFormat> list;

    public static String formats;
    public static String langs;
    public static String tessdata;

    public static boolean pdfFlag = false;
    public static boolean textFlag = false;

    //use to indicate gibberish in PDF text
    public static int blankLimit = 25;

    /*
       help() - invoke apache commons CLI help facility
    */
    public static void help() {
       HelpFormatter formatter = new HelpFormatter();
       formatter.printHelp("ocrProc", options);
    }//help

    /*
       getProcPath() - get path of program
    */
    public static String getProcPath() {
        String procPath = "";
        try {
            procPath = new File(ocrProc.class.getProtectionDomain().
                getCodeSource().getLocation().toURI()).getPath();
            procPath = getStem(procPath,File.separator);
        } catch (java.net.URISyntaxException ex) {
            logger.info("config path error: " + ex.toString() + " exiting");
            System.exit(0);
        }//try
        return procPath;
    }//getProcPath

    /*
       pdfOutFromOcr() - produce PDF with embedded OCR courtesy of Tesseract
    */
    public static boolean pdfOutFromOcr(File imageFile, String outFile) {
        try {
            tesseract.createDocuments(imageFile.getAbsolutePath(), outFile, list);
        } catch (TesseractException tex) {
            logger.info("tesseract error for: " + imageFile.toString() + 
                " - " + tex.toString());
            return false;
        }//try

        if ((new File(outFile + "." + PDF_EXT)).length() == 0) {
            logger.info("no ocr for: " + imageFile.toString());
            return false;
        }//if
        return true;
    }//pdfOcr

    /*
       getPdfTextIfAny() - try to extract text from PDF
    */
    public static String getPdfTextIfAny(String pdfFile) {
        String text = "";

        try {
            PDDocument document = PDDocument.load(new File(pdfFile));
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(document);
            int tlength = text.length();
            int tspace = tlength - (text.replaceAll(" ", "").length());
            if (tspace > 0 && (Math.round(tlength/tspace) > blankLimit))
                text = "";
            document.close();
        } catch (IOException ioe) {
            logger.info(pdfFile + " fails text test: " + ioe.toString());
            text = "";
        }//try

        return text;
    }//hasText

    /*
        getStem() - return stem of String
    */
    public static String getStem(String sBase, String sChar) 
    {
        int lastOcc = sBase.lastIndexOf(sChar);

        if (lastOcc == -1)
             return sBase;

        return sBase.substring(0,lastOcc);
    }//getStem

    /*
       listImgFormats() - convenience function for formats supported by ImageIO
    */
    public static String listImgFormats()
    {
        String iformats = "";
        IIORegistry registry = IIORegistry.getDefaultInstance();
        registry.registerServiceProvider(new com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi());
        registry.registerServiceProvider(new org.apache.pdfbox.jbig2.JBIG2ImageReaderSpi());

        String[] formatNames = ImageIO.getReaderFormatNames();
        for (int f = 0; f < formatNames.length; f++) {
            String format = formatNames[f].toLowerCase() + ",";
            if (!iformats.contains(format))
                iformats += format;
        }
        if (iformats.length() > 0)
            iformats = iformats.substring(0, iformats.length() - 1);

        return iformats;
    }//listImgFormats

    /*
       isInt() - see if string can be converted to Integer
    */
    public static boolean isInt(String in) {
        try {
            int x = Integer.parseInt(in);
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    /*
       getOcrWithConfVals() - building blocks for ALTO Glyph support (not used)
    */
    public static String getOcrWithConfVals(BufferedImage image)
        throws IOException, TesseractException
    {
        String finalText = "";

        try {
            int pageIteratorLevel = TessPageIteratorLevel.RIL_WORD;
            List<Word> result = tesseract.getWords(image, pageIteratorLevel);
            for (Word wordText : result) {

                System.out.println(wordText.getText() + " - " + wordText.getConfidence());
                String ocrtext = wordText.getText().replaceAll("[^\\d.]", "").trim();
                if (ocrtext.length() > 0 && isInt(ocrtext) && wordText.getConfidence() >= DEFAULT_CONF) {
                    System.out.println("ocr: " + ocrtext);
                    System.out.println("confidence: " + wordText.getConfidence());
                    System.out.println("x: " + wordText.getBoundingBox().x);
                    System.out.println("y: " + wordText.getBoundingBox().y);
                    System.out.println("w: " + wordText.getBoundingBox().width);
                    System.out.println("h: " + wordText.getBoundingBox().height);
                    finalText += ocrtext;
                }//if
            }//for
        } catch (Exception e) {
            finalText = "";
        }//try

        return finalText;
    }//getOcrWithConfVals

    /*
       textFromImgInput() - straightforward Tesseract call
    */
    public static String textFromImgInput(String imgFile) {
        String result = "";
        try {
            result = tesseract.doOCR(new File(imgFile));
        } catch (TesseractException tex) {
            logger.info("tesseract error for " + imgFile + " - " +
                tex.toString());
        }//try
        if (result == null) return "";
        return result; 
    }//textFromImgInput

    /*
       changeExt() - swap file extension values
    */
    public static String changeExt(String outFile, String ext, 
        String newExt, String sep) 
    {
        int extPos = outFile.toLowerCase().indexOf("." + ext);
        if (extPos != -1) return outFile.substring(0,extPos) + sep + newExt;
        return outFile;
    }//changeExt

    /*
       textFromPdfInput() - loop through PDF pages and OCR any without text
    */
    public static boolean textFromPdfInput(String sourceFile, String outFile, String ext) 
    {
        File pdfFile = new File(sourceFile);
        int pgno = 0;
        boolean ocrFlag = false;
        boolean textStart = false;
        String pdfText;

        try {
            pgno = pdfU.getPdfPageCount(pdfFile);
        } catch (Exception ex) {
            logger.info("pdf problem: " + ex.toString());
            pgno = 0;
        }//try
        if (pgno <= 0)
            return false;
        File[] fileList = new File[pgno];
        String txtFile = changeExt(outFile, ext, TEXT_EXT,".");
                    

        for(int pg=0; pg<pgno; pg++){
            try {
                File pdfPage = File.createTempFile("tempfile", "." + PDF_EXT);
                pdfPage.deleteOnExit();
                pdfU.splitPdf(pdfFile,pdfPage,pg+1,pg+1);
                pdfText = getPdfTextIfAny(pdfPage.toString());
                if (pdfFlag) fileList[pg] = pdfPage;
                if (pdfText.trim().length() > 0) {
                    //clear out text file in case of a reprocess
                    if (!textStart && textFlag) FileUtils.writeStringToFile(
                         new File(txtFile), "", TEXT_ENC, false);
                    if (textFlag) FileUtils.writeStringToFile(
                         new File(txtFile), pdfText, TEXT_ENC, true);
                    textStart = true;
                } else {
                     //probably biggest bottleneck but need an image for OCR
                     logger.info("create image file for: " + pdfPage);
                     File tiffFile = pdfU.convertPdf2Tiff(pdfPage);                
                     if (pdfFlag) {
                         File pdfPageOcr = File.
                             createTempFile("tempfile", "." + PDF_EXT);
                         pdfPageOcr.deleteOnExit();
                         //swap pdf page if OCR is possible
                         if (pdfOutFromOcr(tiffFile,changeExt(pdfPageOcr.toString(),PDF_EXT,"",""))) {
                             ocrFlag = true;
                             fileList[pg] = pdfPageOcr;  
                         }//if
                     }//if 
                     if (textFlag) {
                         pdfText = textFromImgInput(tiffFile.toString());
                         if (pdfText.trim().length() > 0) FileUtils.writeStringToFile(
                            new File(txtFile), pdfText, TEXT_ENC, true);
                     }//if               
                }//if
            } catch (IOException ioe) {
                logger.info("file problem for pg: " + pg + " of " + pdfFile);
                return false;
            }//try
        }//for
        //make sure this is a viable PDF
        if (pdfFlag && fileList.length > 0) { 
            //was there any OCR? copy to output if text is already there or OCR did not produce anything
            if (ocrFlag) {
                try {
                    pdfU.mergePdf(fileList, 
                        new File(outFile));
                } catch (Exception ex) {
                    logger.info("pdf problem: " + ex.toString());
                    return false;
                }//try
            } else {
                logger.info("copy PDF file, no OCR needed and/or produced");
                try {
                    FileUtils.copyFile(pdfFile,new File(outFile));
                } catch (Exception ex) {
                    logger.info("copy problem: " + ex.toString());
                    return false;
                }//try
            }//if
        }//if
        return true;
    }//textFromPdfInput

    /*
       createOcrOutput() - attempt to produce OCR for supplied files
    */
    public static boolean createOcrOutput(String sourceFile, String outFile, String ext) {
        logger.info("create ocr from sourceFile: " + sourceFile);
        logger.info("target file: " + outFile);
        if (ext.contains(PDF_EXT)) 
            return textFromPdfInput(sourceFile, outFile, ext);
        else {
            if (pdfFlag) {
                //remove extension for Tesseract pdf output
                String pdfFile = changeExt(outFile, ext, "", "");
                if (pdfOutFromOcr(new File(sourceFile), pdfFile)) 
                {
                    logger.info("pdf created for: " + sourceFile);
                } else {
                    logger.info("unable to create pdf for: " + sourceFile);
                    return false;
                }//if
            }//if
            if (textFlag) {   
                String txtFile = changeExt(outFile, ext, TEXT_EXT, ".");
                String imgText = textFromImgInput(sourceFile);
                if (imgText.length() > 0) {
                    try {
                        FileUtils.writeStringToFile(
                            new File(txtFile), imgText, TEXT_ENC, true);
                    } catch (IOException ioe) {
                        logger.info("unable to create: " + txtFile);
                        return false;
                    }//try
                }//if
            }//if
        }//if
        return true;

    }//createOcrOutput

    /*
       countFiles() - check file extensions for OCR candidates
    */
    public static int countFiles(String dirPath, int start) {
        int count = start;
        File f = new File(dirPath);
        File[] files = f.listFiles();
        String iformats = listImgFormats();

        if (files != null)
            for (int i = 0; i < files.length; i++) {
                File file = files[i];

                if (file.isDirectory())
                    count = countFiles(file.getAbsolutePath(),count);   
                else {
                    String ext = FilenameUtils.getExtension(file.toString());
                    ext = ext.toLowerCase();
                    if (iformats.contains(ext) || ext.contains(PDF_EXT)) 
                        count++;
                }//if                 
            }//for
        return count;
    }//countFiles

    /*
       dealWithFiles() - figure out whether files need to be OCRed or moved
    */
    public static void dealWithFiles(String wDir, File file,String fullPath, 
        String outPath, String rejectsPath, String fileName, String ext) 
    {
        File curDir = new File(file.getParent());   

        File procFile = new File(fullPath + fileName);
        File ocrFile = new File(outPath + fileName);
        File rejectsFile = new File(rejectsPath + fileName);
        //File dirFile = new File(procFile.getParent());
        File dirFile = new File(rejectsFile.getParent());
        File outFile = new File(ocrFile.getParent());

        if (!dirFile.exists()) {
             dirFile.mkdirs();
             logger.info(dirFile.toString() + ": created");
        }//if

        if (!outFile.exists()) {
             outFile.mkdirs();
             logger.info(outFile.toString() + ": created");
        }//if

        //if (!procFile.exists() && file.renameTo(procFile)) {
        if (!rejectsFile.exists() && file.renameTo(rejectsFile)) {
             if (curDir.list().length == 0 && !wDir.contains(curDir.toString())) {
                 curDir.delete();
             }//if
             logger.info("moved: " + file.toString() +
                 " to: " + rejectsFile.toString());
             if (!createOcrOutput(rejectsFile.toString(),
                 ocrFile.toString(), ext)) 
             {
                 logger.info("ocr failed for: " + 
                     rejectsFile.toString());
             } else {
                 File procDir = new File(procFile.getParent());
                 if (!procDir.exists()) {
                     procDir.mkdirs();
                     logger.info(procDir.toString() + ": created");
                 }
                 rejectsFile.renameTo(procFile);
                 logger.info("moved: " + rejectsFile.toString() +
                     " to: " + procFile.toString());
             }//if
        }//if
    }//dealWithFiles

    /*
       procFiles() - loop through directories looking for candidate files
    */
    public static int procFiles(String wDir, File[] files,String base,String out, 
        String rejects, int fileCnt) 
    {
        int curCnt = fileCnt;
        String fullPath = "";
        String outPath = "";
        String rejectsPath = "";
        String iformats = listImgFormats();

        if (base.length() > 0) fullPath = base + File.separator;
        if (out.length() > 0) outPath = out + File.separator;
        if (rejects.length() > 0) rejectsPath = rejects + File.separator;

        for (File file : files) {
            if (file.isDirectory()) {
               curCnt = procFiles(wDir,file.listFiles(),fullPath + file.getName(), 
                   outPath + file.getName(), rejectsPath + file.getName(), curCnt);
               file.delete();
            } else {
               curCnt++;
               String fileName = file.getName();
               String ext = FilenameUtils.getExtension(fileName).toLowerCase();
               if (iformats.contains(ext) || ext.contains(PDF_EXT))
                   dealWithFiles(wDir,file,fullPath,outPath,rejectsPath,fileName, ext);
            }//if
        }//for
        return curCnt;
    }//procFiles

    /*
       ocrProcess() - set flags for OCR output formats
    */
    public static int ocrProcess(File fWatchDir,File fProcDir,File 
        fDestDir, File fRejectDir) 
    {
        File[] files = fWatchDir.listFiles();
        if (formats.contains(PDF_EXT)) pdfFlag = true;
        if (formats.contains("text")) textFlag = true;
        return procFiles(fWatchDir.toString(),files,fProcDir.toString(),
            fDestDir.toString(), fRejectDir.toString(), 0);
    }//procFiles

    /*
       checkOption() - convenience function for options
    */
    public static String checkOption(String option, String defOption) {
        String testOption;
        if (cl.hasOption(option)) {
            testOption = cl.getOptionValue(option);
            if (testOption != null && testOption.length() > 0)
                return testOption;
        }//if
        return defOption;
    }//checkOption

    /*
       main() - set up the options and pull together the switches
    */
    public static void main( String[] args ) throws IOException,
        ParseException, TesseractException
    {
        options = new Options();
        options.addOption("d", "destination", true, "destination directory");
        options.addOption("b", "blanks", true, "blank count");
        options.addOption("f", "formats", true, "formats - text, pdf");
        options.addOption("h", "help", false, "show help");
        options.addOption("l", "languages", true, "languages, e.g eng, eng+fra");  
        options.addOption("p", "process", true, "process directory");
        options.addOption("r", "reject", true, "rejects directory");
        options.addOption("t", "tessdata", true, "tesseract data location"); 
        options.addOption("w", "watch", true, "watch directory");

        BasicParser parser = new BasicParser();
        cl = parser.parse(options, args);

        if (cl.hasOption('h')) {
            help();
            System.exit(0);
        }//if

        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        String strLckFile = dateFormat.format(date) + ".lck";

        File lckFile = new File(strLckFile);
        lckFile.createNewFile(); //creates empty lock file

        //start logging
        logger = LogFactory.getLog(ocrProc.class);
        logger.info("-----------------------------------------");
        logger.info("lock file for watch scan: " + strLckFile);

	Properties prop = new Properties();
        InputStream input = new FileInputStream(getProcPath() + 
            "/config/ocrProc.properties");
        prop.load(input);

        String blanks = prop.getProperty("blanks");
        if (cl.hasOption("b"))
           blanks = checkOption("b",blanks);

        if (isInt(blanks))
            blankLimit = Integer.parseInt(blanks);

        String sWatchDir = prop.getProperty("watchDir");
        String sProcDir = prop.getProperty("procDir");
        String sDestDir = prop.getProperty("destDir");
        String sRejectDir = prop.getProperty("rejectDir");

        if (cl.hasOption("w"))
            sWatchDir = checkOption("w",sWatchDir);
        logger.info("watch directory set to " + sWatchDir);

        if (cl.hasOption("p"))
           sProcDir = checkOption("p",sProcDir);
        logger.info("process directory set to " + sProcDir);

        if (cl.hasOption("d"))
           sDestDir = checkOption("d",sDestDir);
        logger.info("destination directory set to " + sDestDir);

        if (cl.hasOption("r"))
           sRejectDir = checkOption("r",sRejectDir);
        logger.info("rejection directory set to " + sRejectDir);

        formats = prop.getProperty("formats");
        if (cl.hasOption("f"))
           formats = checkOption("f",formats);
        formats = formats.toLowerCase();
        if (formats.contains(PDF_EXT)) {
            list = new ArrayList<RenderedFormat>();
            list.add(RenderedFormat.PDF);
        }//if
        logger.info("formats set to " + formats);

        langs = prop.getProperty("langs");
        if (cl.hasOption("l"))
           langs = checkOption("l",langs);
        langs = langs.toLowerCase();
        logger.info("languages set to " + langs);

        tessdata = prop.getProperty("tessdata");
        if (cl.hasOption("t"))
           tessdata = checkOption("t",tessdata);

        File fWatchDir = new File(sWatchDir);
        File fProcDir = new File(sProcDir);
        File fDestDir = new File(sDestDir);
        File fRejectDir = new File(sRejectDir);

        //see if there is anything to process
        int fileNo = countFiles(sWatchDir,0);

        if ((fWatchDir.exists() && fWatchDir.isDirectory()) &&
            (fProcDir.exists() && fProcDir.isDirectory()) &&
            (fDestDir.exists() && fDestDir.isDirectory()) &&
            (fRejectDir.exists() && fRejectDir.isDirectory()) &&
            fileNo > 0)
        {
            tesseract = new Tesseract();
            tesseract.setDatapath(tessdata);
            tesseract.setLanguage(langs);
            pdfU = new PdfBoxUtilities();
            logger.info(fileNo + " file(s) identified for processing");
            logger.info("supported image formats: " + listImgFormats());
            logger.info(ocrProcess(fWatchDir,fProcDir,fDestDir,fRejectDir) +
                " processed");
        }//if

        //remove lock file
        logger.info("removing lock file: " + lckFile.toString());
        lckFile.delete();

    }//main
}//ocrProc
