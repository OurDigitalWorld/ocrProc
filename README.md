ocrProc
=======

This project supports the use of a directory structure to batch process
files for OCR. The project is built with [Maven](https://maven.apache.org). The 
pieces can be pulled together with:

```
mvn assembly:assembly
```

The jar with all of the needed libraries should end up in the _target_
directory and everything is brought together in _ocrProc-exe.jar_. 
The command line options are:

    usage: ocrProc
     -d,--destination <arg>   destination directory
     -f,--formats <arg>       formats - text, pdf
     -h,--help                show help
     -l,--languages <arg>     languages, e.g eng, eng+fra
     -p,--process <arg>       process directory
     -r,--reject <arg>        rejects directory
     -t,--tessdata <arg>      tesseract data location
     -w,--watch <arg>         watch directory     

For example:

```
java -jar ocrProc-exe.jar -l eng+fra
```

However, all of the options can be specified in a _properties_ file, found in _src/config/ocrProc.properties_. For example:

```
#for windows paths, use forward slash (/) or double backward slash (\\)
watchDir=/leglib/watch
procDir=/leglib/process
destDir=/leglib/output
rejectDir=/leglib/reject
#choice of text, pdf, or both
formats=text,pdf
langs=eng+fra
#blank setting to detect gibberish
blanks=20
#tesseract data file location
tessdata=/usr/share/tesseract-ocr/4.00/tessdata
```

The directory structure for identifying and managing OCR tasks is given below. The directories that control the flow are:

*   **watchDir** - used for identfying files that be processed. ocrProc will flag anything with an image extension recognized by the 
host operating system's [JAI support](https://www.oracle.com/technetwork/java/iio-141084.html) as well as any PDF file.
*   **procDir** - files are copied to this directory as OCR/text extraction is performed;
*   **destDir** - this is the directory that receives the result of the processing. Both _procDir_ and _destDir_ retain the 
directory structure used in the _watch_ directory, allowing nesting of sundirectories.
*   **rejectDir** - all candidate files go through this directory, and remain 
if for some reason they cannot be processed. For example, a PDF file requiring 
password access will not be accessible. This is the directory to use for 
tracking problematic files.

The other options are as follows:

*   **formats** - this can be set to a value of _text_ or _pdf_, as well as both, i.e., _text,pdf_. This refers to the **output** format. For example, the file _sample.pdf_ can be OCRed with the _text_ option to produce a _sample.txt_ in the destination directory. Similarly, the _pdf_ option will result in a _sample.pdf_ to be created in the destination directory, and the OCR in this case will be _embedded_ into each page of the _pdf_ file. Note that _ocrProc_ will not perform OCR on a PDF page if it already has embedded text. This could mean, for example, that one page in a 20 page PDF document has no text in the input file, but the output file has text on this page as a result of OCR processing.
*   **langs** - this is used to specify one or more codes as used by [Tesseract](https://github.com/tesseract-ocr/). The full list of
languages can be found in the [Tesseract wiki](https://github.com/tesseract-ocr/tesseract/wiki/Data-Files).
*   **blanks** - threshold based on dividing text on a page by number of blanks, sometimes PDFs contain text that comes out as garbage because of mismatched encodings, etc.
*   **tessdata** - the location of the _Tesseract_ data directory. _ocrProc_ uses [Tess4J](http://tess4j.sourceforge.net/) to provide
access to the _Tesseract_ libraries, and this allows the appropriate directory to be explictedly set.

To install _Tesseract_, follow the 
[instructions for the desired platform](https://github.com/tesseract-ocr/tesseract#installing-tesseract). Note for _windows_ 
that the Universitätsbibliothek Mannheim maintains Tesseract installers for 32 and 64 bit _windows_ platforms on github at 
the [Tesseract at UB Mannheim](https://github.com/UB-Mannheim/tesseract/wiki) page.

A _lock_ file is created when _ocrProc_ is invoked. This is useful for running OCR on a recurring schedule and avoiding more than one
instance running at a time. In unix-like environments, the assumption is that a cron job would be used. For example, an _ocrProc.sh_
script could be added to crontab with the following syntax:

```
#if java is not running, clean up (may have crashed on a document)
if ! pgrep -u user -x "java" > /dev/null
then
   rm /tmp/temp*.pdf
   rm /tmp/multipage*.tif
   rm /leglib/*.lck
fi

#only run if there are no lck files
count=`ls -1 /leglib/*.lck 2>/dev/null | wc -l`
if [ $count == 0 ]
then
   rm /tmp/temp*.pdf
   rm /tmp/multipage*.tif
   export LC_ALL=C
   export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
   cd /leglib && java -jar ocrProc/target/ocrProc-exe.jar
   rm /tmp/temp*.pdf
   rm /tmp/multipage*.tif
fi
```

In this case, the _leglib_ directory contains the _ocrProc_ distribution. The crontab entry might be set, for example, to run
every 15 minutes:

```
*/15 * * * * /leglib/ocrProc.sh
```

If, for some reason, the program crashes, the script will remove the lock
file so that the process can start at the next file. The offending file
should be in the _reject_ folder. This is particularly important for PDF
files since odd objects seem more common with these. Also note the
_LC_ALL_ option, this seems to a common gotcha on linux systems when using
Tesseract.

ocrProc uses _log4j_ logging properties, set in the 
[resources directory](https://github.com/OurDigitalWorld/ocrProc/tree/master/src/main/resources). In this case, an _ocrProc.log_ 
file will be created in the _leglib_ directory, and give information about each invocation, for example:

```
03-10@22:45:02 INFO      -----------------------------------------
03-10@22:45:02 INFO      lock file for watch scan: 2019-45-10_10-45-02.lck
03-10@22:45:02 INFO      watch directory set to /leglib/watch
03-10@22:45:02 INFO      process directory set to /leglib/process
03-10@22:45:02 INFO      destination directory set to /leglib/output
03-10@22:45:02 INFO      rejection directory set to /leglib/reject
03-10@22:45:02 INFO      formats set to text,pdf
03-10@22:45:02 INFO      languages set to eng+fra
03-10@22:45:02 INFO      removing lock file: 2019-45-10_10-45-02.lck
```

The log will be more extensive if candidate files are found in the _watch_ direcory.

```
03-10@21:30:01 INFO      4 file(s) identified for processing
03-10@21:30:01 INFO      supported image formats: supported image formats: jpg,jpeg 2000,tiff,bmp,pcx,gif,wbmp,png,raw,jpeg,pnm,tif,jbig2,jpeg2000
03-10@21:30:01 INFO      moved: /leglib/watch/00001.pdf to: /leglib/process/00001.pdf
03-10@21:30:01 INFO      create ocr from sourceFile: /leglib/process/00001.pdf
03-10@21:30:01 INFO      Using fallback font 'LiberationSans' for 'Helvetica-Bold'
03-10@21:30:01 INFO      OpenType Layout tables used in font ABCDEE+Arial-BoldMT are not implemented in PDFBox and will be ignored
... and lots of font messages for large PDFs
03-10@21:30:48 INFO      removing lock file: 2019-30-10_09-30-01.lck
```

In _windows_, the workflow can be very similar by using a _bat_ file in combination with a _scheduled task_. The file can
contain a test for the _lock_ with the _not exist_ directive, for example:

```
if not exist *.lck (
    java -jar ocrProc-master/target/ocrProc-exe.jar 
) 
```

It is also possible to simply use _ocrProc_ from the command line and it is worthwhile testing it in this way to make sure it
is producing the desired results before creating a scheduled process.

ocrProc will expect [JAI](https://geoserver.geo-solutions.it/edu/en/install_run/jai_io_install.html) support in the operating 
system in order to handle _Jpeg2000_ images (common in PDF files).

art rhyno [ourdigitalworld/cdigs](https://github.com/artunit)
