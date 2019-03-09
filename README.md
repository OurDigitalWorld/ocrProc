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

More to come...

art rhyno [ourdigitalworld/cdigs](https://github.com/artunit)
