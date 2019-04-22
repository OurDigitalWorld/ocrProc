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
#tesseract data file location
tessdata=/usr/share/tesseract-ocr/4.00/tessdata
```

A directory structure for identifying and managing OCR tasks. The directories that control the flow are:

*   **watchDir** - used for identfying files that be processed. ocrProc will flag anything with an image extension recognized by the 
host operating system's [JAI support](https://www.oracle.com/technetwork/java/iio-141084.html) as well as any PDF file.
*   **procDir** - candidatefiles are copied to this directory before OCR/text extraction is performed;
*   **destDir** - this is the directory that receives the result of the processing. Both _procDir_ and _destDir_ retain the 
directory structure used in the _watch_ directory, allowing nesting of sundirectories.
*   **rejectDir** - files that cannot be processed are placed here. For example, a PDF file with password access will not be
able to be processed automatically and will be placed here. This is the directory to use for tracking problematic files.

The other options are as follows:

*   **formats** - this can be set to a value of _text_ or _pdf_, as well as both, i.e., _text,pdf_. This refers to the **output** format. For example, the file _sample.pdf_ can be OCRed with the _text_ option to produce a _sample.txt_ in the destination directory. Similarly, the _pdf_ option will result in a _sample.pdf_ to be created in the destination directory, and the OCR in this case will be _embedded_ into each page of the _pdf_ file. Note that _ocrProc_ will not perform OCR on a PDF page if it already has embedded text. This could mean, for example, that one page in a 20 page PDF document has no text in the input file, but the output file has text on this page as a result of OCR processing.
*   **langs** - this is used to specify one or more codes as used by [Tesseract](https://github.com/tesseract-ocr/). The full list of
languages can be found in the [Tesseract wiki](https://github.com/tesseract-ocr/tesseract/wiki/Data-Files).
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
count=`ls -1 /leglib/*.lck 2>/dev/null | wc -l`
if [ $count == 0 ]
then
   export JAVA_HOME=/usr/lib/jvm/default-java
   cd /leglib && java -jar ocrProc/target/ocrProc-exe.jar
fi
```

In this case, the _leglib_ directory contains the _ocrProc_ distribution. The crontab entry might be set, for example, to run
every 15 minutes:

```
*/15 * * * * /leglib/ocrProc.sh
```

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
03-10@21:30:01 INFO      supported image formats: jpg,tiff,bmp,pcx,gif,wbmp,png,raw,jpeg,pnm,tif
03-10@21:30:01 INFO      moved: /leglib/watch/00001.pdf to: /leglib/process/00001.pdf
03-10@21:30:01 INFO      create ocr from sourceFile: /leglib/process/00001.pdf
03-10@21:30:01 INFO      target file: /leglib/output/00001.pdf
03-10@21:30:02 INFO      moved: /leglib/watch/00570.pdf to: /leglib/process/00570.pdf
03-10@21:30:02 INFO      create ocr from sourceFile: /leglib/process/00570.pdf
03-10@21:30:02 INFO      target file: /leglib/output/00570.pdf
03-10@21:30:02 INFO      moved: /leglib/watch/pdf-example-fonts.pdf to: /leglib/process/pdf-example-fonts.pdf
03-10@21:30:02 INFO      create ocr from sourceFile: /leglib/process/pdf-example-fonts.pdf
03-10@21:30:02 INFO      target file: /leglib/output/pdf-example-fonts.pdf
03-10@21:30:02 WARN      Using fallback font 'LiberationSans' for 'TimesNewRomanPSMT'
03-10@21:30:02 WARN      Using fallback font LiberationSans for Arial
03-10@21:30:02 INFO      moved: /leglib/watch/reports/water_quality1.png to: /leglib/process/reports/water_quality1.png
03-10@21:30:02 INFO      create ocr from sourceFile: /leglib/process/reports/water_quality1.png
03-10@21:30:02 INFO      target file: /leglib/output/leglib/process/reports/water_quality1.pdf
03-10@21:30:48 INFO      4 processed
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

art rhyno [ourdigitalworld/cdigs](https://github.com/artunit)
