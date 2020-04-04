# npTranscript

npTranscript is a tool which was designed primarily for analysis of SARS-Cov-2 direct RNA sequence data.  It works via analysis of BAM file of direct RNA sequencing reads aligned to a reference genome.  It handles analysis of alignment CIGAR strings in order to cluster reads into different transcripts. It also enables analysis of 5'to 3' break points.  Example of the output of this tool can be seen at https://doi.org/10.26188/5e86c8229aebb 

npTranscript consists of two components - a java component, which utilises japsa ( https://github.com/mdcao/japsa) for analysis of CIGAR strings. 

The second part consists of R scripts which generate visualisation of outputs of the java component

###INSTALLATION AND USAGE

A precompiled jar is provided in the distribution. 

The script run_example.sh  provides an example of how you can run the java part of npTranscript.

The input required is 
 - 1 or more bam files (multiple bam files can be processed at the same time)
 - a reference genome (we provide a reference coronavirus genome in  data/SARS-Cov2/wuhan_coronavirus_australia.fasta.gz)
 - a coordinate file (we provde one in data/SARS-Cov2/Coordinates.csv)


The output of the java program are the following files:

0readToCluster.txt.gz    - master file of mapping of reads to clusters
0transcripts.txt.gz   - transcript cluster file
0breakpoints.txt.gz.0.gz   -  file with break point information
0clusters.h5   - coverage information for each transcript. 

These files can be further processed with the Rscripts provided

#RUNNING R SCRIPTS

Change into the resdir generated by the java program. 
run
```
Rscript npTranscript.R
```


### Authors and Contributors

npTranscript is currently maintained by Lachlan Coin (@lachlanjmc) and Dan Rawlinson. 

