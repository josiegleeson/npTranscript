/*****************************************************************************
 * Copyright (c)Lachlan Coin University of Melbourne, All rights reserved.         *
 *                                                                           *
 * Redistribution and use in source and binary forms, with or without        *
 * modification, are permitted provided that the following conditions        *
 * are met:                                                                  * 
 *                                                                           *
 * 1. Redistributions of source code must retain the above copyright notice, *
 *    this list of conditions and the following disclaimer.                  *
 * 2. Redistributions in binary form must reproduce the above copyright      *
 *    notice, this list of conditions and the following disclaimer in the    *
 *    documentation and/or other materials provided with the distribution.   *
 * 3. Neither the names of the institutions nor the names of the contributors*
 *    may be used to endorse or promote products derived from this software  *
 *    without specific prior written permission.                             *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS   *
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, *
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR    *
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR         *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,     *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,       *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR        *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING      *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS        *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.              *
 ****************************************************************************/

/*                                                    
 * 01/03/2020 - Lachlan Coin                                       
 ****************************************************************************/

package npTranscript.run;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.JapsaAnnotation;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import japsa.tools.seq.SequenceUtils;
import japsa.util.CommandLine;
import japsa.util.deploy.Deployable;
import npTranscript.cluster.Annotation;
import npTranscript.cluster.CigarCluster;
import npTranscript.cluster.CigarHash;
import npTranscript.cluster.CigarHash2;
import npTranscript.cluster.EmptyAnnotation;
import npTranscript.cluster.GFFAnnotation;
import npTranscript.cluster.IdentityProfile1;
import npTranscript.cluster.Outputs;
import npTranscript.cluster.TranscriptUtils;

/**
 * @author Lachlan Coin
 *
 */

@Deployable(scriptName = "npTranscript.run", scriptDesc = "Analysis of coronavirus sequence data")
public class ViralTranscriptAnalysisCmd2 extends CommandLine {
	

//	private static final Logger LOG = LoggerFactory.getLogger(HTSErrorAnalysisCmd.class);

public static String getAnnotationsToInclude(String annotationType, boolean useExons){
	if(annotationType!=null) return annotationType;//.split(":");
	 if(!useExons) {
		 return  "gene:ncRNA:pseudogene:miRNA";	
	 }
	 else{
		 return "exon";
	 }
		
}
	public ViralTranscriptAnalysisCmd2() {
		super();
		Deployable annotation = getClass().getAnnotation(Deployable.class);
		setUsage(annotation.scriptName() + " [options]");
		setDesc(annotation.scriptDesc());
		addString("bamFile", null, "Name of bam file", false);
		addString("fastqFile", null, "Name of bam file", false);
		addString("reference", null, "Name of reference genome", true);
		addString("annotation", null, "ORF annotation file or GFF file", false);
		addBoolean("useExons", true, "wehether to use exons");
		addString("readList", "", "List of reads", false);
		
			addString("annotType", null, "Type of annotation (only included if annotation is GFF file", false);
		addString("chroms", "all", "Restrict to these chroms, colon delimited", false);
		addString("bedChr", null, "Use this for the chrom in bed chr, e.g. NC_045512v2, false");

		addString("resdir", "results"+System.currentTimeMillis(), "results directory");
		addString("GFF_features", "Name:description:ID:biotype:Parent", "GFF feature names");
		addBoolean("RNA", false, "If is direct RNA");
		addBoolean("recordDepthByPosition", false, "whether to store position specific depth (high memory");

		addInt("maxReads", Integer.MAX_VALUE, "ORF annotation file");
		addDouble("probInclude", 1.0, "probability of including each read");
		addInt("minClusterEntries",10,"threshold for consensus");
		addBoolean("tryComplementOnExtra", false, "look for negative strand matches on left over seqs");
		addBoolean("reAlignExtra", false, "whether to try realigning the extra sequence");
		addBoolean("combineOutput", false, "whether to combine output from different chroms");
		addString("pattern", null, "Pattern of read name, used for filtering");
		addString("span", "protein_coding", "Filtering span.  Use all not to filter.");
		addInt("qual", 0, "Minimum quality required");
		addInt("bin", 1, "Bin size for numerical hashing");
		addString("numExonsMSA", "none", "number of exons For  MSA");
		addInt("breakThresh", 1000, "Thresh for break points to match clusters.  If bigger than genome size then no break points");
		addBoolean("includeStart", true, "Whether to include start position in the cluster hash");

		addInt("coverageDepthThresh", 100, "Threshhold for writing base level depth information to h5 file");
		addString("isoformDepthThresh", "10", "Threshhold for printing out all isoforms");
		addDouble("msaDepthThresh", 10, "Threshhold for running MSA per subcluster");
		addDouble("qualThresh", 20, "Quality thresh for leftover seqs");
		addDouble("fail_thresh", 7, "Pass threshold");
		addString("doMSA", "false" , "Options: 5_3 or all or span=0 ");
		addString("msa_source", null , "indicates how to group msa, e.g 0,1,2;3,4,5 ");
		//addString("aligner", "kalign" , "Options: kalign3, poa, spoa, abpoa");
		addInt("startThresh", 100, "Threshold for having 5'");
		addInt("endThresh", 100, "Threshold for having 3'");
		addInt("maxThreads", 8, "max threads (only writing output uses threads at this stage)");
		addInt("extra_threshold", 200, "Threshold saving umatched 3'or 5'parts of reads");
		//addDouble("overlapThresh", 0.95, "Threshold for overlapping clusters");
	//	addBoolean("coexpression", false, "whether to calc coexperssion matrices (large memory for small bin size)");
		addBoolean("overwrite", false, "whether to overwrite existing results files");
		addBoolean("keepAlignment", false, "whether to keep alignment for MSA");
		addBoolean("attempt5rescue", true, "whether to attempt rescue of leader sequence if extra unmapped 5 read");
		addBoolean("attempt3rescue", true, "whether to attempt rescue of leader sequence if extra unmapped 5 read");
		addBoolean("writePolyA", false, "whether write reads with poly								qqq	A in middle");
		addBoolean("coronavirus", true, "whether to run in coronavirus mode (necessary to do breakpoint analysis, but takes more memory)");
		
		addString("mm2_path", "/sw/minimap2/current/minimap2",  "minimap2 path", false);
		addString("mm2Preset", "splice",  "preset for minimap2", false);
		//addString("mm2Preset", "map-ont",  "preset for minimap2", false);
		addString("mm2_memory", (Runtime.getRuntime().maxMemory()-1000000000)+"",  "minimap2 memory", false);
		addInt("mm2_threads", 4, "threads for mm2", false);
		addStdHelp();
	}

	
 static void printParams(File resDir, String[] args1) throws IOException{
		File params_file = new File(resDir, "params.txt");
		PrintWriter pw = new PrintWriter(new FileWriter(params_file));
		Date d = new Date();
		
		 // System.getProperties().list(System.out);
		String mem = "Xmx="+(int)Math.ceil((double)Runtime.getRuntime().maxMemory()/(1e6))+"m";
		pw.println("User directory\n"+System.getProperty("user.dir"));
		pw.println("Date\n"+d.toString());
		pw.println("OS="+System.getProperty("os.name")+"\nJVM="+System.getProperty("java.version"));
		pw.println(mem);
		pw.println("Run commands");
		for(int i=0; i<args1.length; i+=1){
			pw.print(args1[i]+" ");
		}
		pw.close();
		
	}
	public static int mm2_threads;
	public static String mm2_path, mm2_mem, mm2_index, mm2Preset, mm2_splicing;
 public static void run(CommandLine cmdLine, String[] bamFiles, String resDir,Map<String, JapsaAnnotation> anno, String chrs, boolean fastq, String reference) throws IOException{
		
		int qual = cmdLine.getIntVal("qual");
		int bin = cmdLine.getIntVal("bin");
		int breakThresh = cmdLine.getIntVal("breakThresh");
		String pattern = cmdLine.getStringVal("pattern");
		String annotFile = cmdLine.getStringVal("annotation");
		String[] readList = cmdLine.getStringVal("readList").split(":");
		//String genesToInclude = cmdLine.getStringVal("genesToInclude");
		String  annotationType = getAnnotationsToInclude(cmdLine.getStringVal("annotType"), cmdLine.getBooleanVal("useExons"));
		boolean overwrite  = cmdLine.getBooleanVal("overwrite");
		int startThresh = cmdLine.getIntVal("startThresh");
		int endThresh = cmdLine.getIntVal("endThresh");
		int maxReads = cmdLine.getIntVal("maxReads");
		Annotation.enforceStrand = cmdLine.getBooleanVal("RNA");
		Outputs.executor=  cmdLine.getIntVal("maxThreads")==1 ? Executors.newSingleThreadExecutor():  Executors.newFixedThreadPool(cmdLine.getIntVal("maxThreads"));
//		Outputs.executor=  ;
		TranscriptUtils.qual_thresh = cmdLine.getDoubleVal("qualThresh");
		ViralTranscriptAnalysisCmd2.combineOutput = cmdLine.getBooleanVal("combineOutput");
		String[] d_thresh = cmdLine.getStringVal("isoformDepthThresh").split(":");
		int[] isoformDepthThresh  = new int[d_thresh.length];
		for(int i=0; i<d_thresh.length; i++){
			isoformDepthThresh[i] = Integer.parseInt(d_thresh[i]);
		}
		int coverageDepthThresh = cmdLine.getIntVal("coverageDepthThresh");
		IdentityProfile1.msaDepthThresh =(int) Math.floor(cmdLine.getDoubleVal("msaDepthThresh"));
	TranscriptUtils.extra_threshold = cmdLine.getIntVal("extra_threshold");
		TranscriptUtils.tryComplementOnExtra = cmdLine.getBooleanVal("tryComplementOnExtra");
		TranscriptUtils.reAlignExtra = cmdLine.getBooleanVal("reAlignExtra");
		TranscriptUtils.attempt5rescue = cmdLine.getBooleanVal("attempt5rescue");
		TranscriptUtils.attempt3rescue = cmdLine.getBooleanVal("attempt3rescue");
		TranscriptUtils.bedChr = cmdLine.getStringVal("bedChr");
		fail_thresh = cmdLine.getDoubleVal("fail_thresh");
		Pattern patt = Pattern.compile(":");
		Outputs.numExonsMSA = cmdLine.getStringVal("numExonsMSA")=="none"  ?  Arrays.asList(new Integer[0]) : 
				patt.splitAsStream(cmdLine.getStringVal("numExonsMSA"))
		                            .map(Integer::valueOf)
		                            .collect(Collectors.toList());
		IdentityProfile1.includeStart = cmdLine.getBooleanVal("includeStart");
		GFFAnnotation.setGFFFeatureNames(cmdLine.getStringVal("GFF_features").split(":"));
		GFFAnnotation.span_only = cmdLine.getStringVal("span").equals("all") ?new ArrayList<String>() :   Arrays.asList(cmdLine.getStringVal("span").split(":"));
		boolean sorted = true;
		boolean coronavirus = cmdLine.getBooleanVal("coronavirus");
		String[] msaOpts = cmdLine.getStringVal("doMSA").split(":"); //e.g 5_3:sep or all:sep
		String msa_source = cmdLine.getStringVal("msa_source");
		double probInclude = cmdLine.getDoubleVal("probInclude");
	//	String[] bamFiles =bamFile.split(":"); 
		int len =  bamFiles.length;
		if(msa_source!=null){
			String[] str = msa_source.split(";");
			outer: for(int j=0; j<bamFiles.length;j++){
				for(int i=0; i<str.length;i++){
						String[] str1 = str[i].split(",");
						for(int k=0; k<str1.length; k++){
							if(bamFiles[j].indexOf(str1[k])>=0){
								Outputs.msa_sources.put(j, i);
								continue outer;
							}
						}
					}
			}
			System.err.println("msa_sources "+Outputs.msa_sources);
		}else{
			
			for(int i=0; i<len; i++){
				Outputs.msa_sources.put(i, 0);
			}
		}
		for(int i=0; i<msaOpts.length; i++){
			if(msaOpts[i].startsWith("span=")) msaOpts[i] = msaOpts[i].split("=")[1];
		}
		Outputs.doMSA =Arrays.asList((msaOpts[0].equals("all") ?"5_3,no5_3,5_no3,no5_no3": msaOpts[0]).split(",")) ;
		Outputs.minClusterEntries = cmdLine.getIntVal("minClusterEntries");
		if(msaOpts[0].equals("false")){
			Outputs.doMSA = null;
		}
		Outputs.keepAlignment = cmdLine.getBooleanVal("keepAlignment");
		Outputs.keepinputFasta = Outputs.keepAlignment ;
		//Outputs.MSA_at_cluster = false;
		boolean calcBreaks=true;// = cmdLine.getBooleanVal("calcBreaks");// whether to calculate data for the break point heatmap, true for SARS_COV2
		boolean filterBy5_3 = false;// should be true for SARS_COV2
		boolean annotByBreakPosition = false;  // should be true for SARS_COV2
	//	String msa = cmdLine.getStringVal("aligner");
		//if(msa==null) throw new RuntimeException("!!");
	//	ErrorCorrection.msa = msa;
		Outputs.writePolyA = cmdLine.getBooleanVal("writePolyA");
		CigarCluster.recordDepthByPosition = cmdLine.getBooleanVal("recordDepthByPosition");
		if(coronavirus){
		//	
		//	CigarCluster.recordDepthByPosition = true;
			System.err.println("running in coronavirus mode");
			calcBreaks  = true; 
			filterBy5_3 = true;
		//	Outputs.MSA_at_cluster = true;
			TranscriptUtils.checkAlign = true;
			TranscriptUtils.coronavirus = true;
			TranscriptUtils.extra_threshold1 =50;
			annotByBreakPosition = true;
			TranscriptUtils.writeAnnotP = true;
		//	CigarHash2.subclusterBasedOnStEnd = false;
			mm2_splicing = "-un";
		
		}else{
		//	TranscriptUtils.reAlignExtra = false;
		//	TranscriptUtils.findPolyA = false;
		
			TranscriptUtils.coronavirus = false;
			TranscriptUtils.extra_threshold1 = 1000000;
			//Outputs.writeUnSplicedFastq = false;
			TranscriptUtils.checkAlign = false;
			TranscriptUtils.writeAnnotP = false;
			System.err.println("running in host mode");
			//CigarHash2.subclusterBasedOnStEnd = false;
			calcBreaks = false;
			mm2_splicing = "-uf";
		}
			errorAnalysis(bamFiles, reference, annotFile,readList,annotationType, 
				resDir,pattern, qual, bin, breakThresh, startThresh, endThresh,maxReads,  sorted , 
				calcBreaks, filterBy5_3, annotByBreakPosition, anno, chrs, overwrite, isoformDepthThresh, coverageDepthThresh, probInclude, fastq);
	}
	public static void main(String[] args1) throws IOException, InterruptedException {
		long tme = System.currentTimeMillis();
		CommandLine cmdLine = new ViralTranscriptAnalysisCmd2();
		String[] args = cmdLine.stdParseLine(args1);
		String bamFile = cmdLine.getStringVal("bamFile");
		String fastqFile = cmdLine.getStringVal("fastqFile");
		
		
		
		mm2_threads = cmdLine.getIntVal("mm2_threads");
		mm2_mem = cmdLine.getStringVal("mm2_mem");
		mm2_path = cmdLine.getStringVal("mm2_path");
		mm2Preset = cmdLine.getStringVal("mm2Preset");
		String reference = cmdLine.getStringVal("reference");
		if(fastqFile!=null){
			try{
				//make a minimap index
			mm2_index = SequenceUtils.minimapIndex(new File(reference), mm2_path, mm2_mem, false);
			}catch(Exception exc){
				exc.printStackTrace();
			}
		}
		
		String resdir = cmdLine.getStringVal("resdir");
		String annot_file = cmdLine.getStringVal("annotation");
		String chroms= cmdLine.getStringVal("chroms");
		
		boolean gff = annot_file !=null && annot_file.contains(".gff");
		Map<String, JapsaAnnotation> anno  = null;
		//boolean SARS = !gff;
		if(gff){
			String  annotationType = getAnnotationsToInclude(cmdLine.getStringVal("annotType"), cmdLine.getBooleanVal("useExons"));

			System.err.println("reading annotation");
			 anno =  GFFAnnotation.readAnno(annot_file, annotationType);
			// GFFAnnotation.read
			System.err.println("done reading annotation");
		}
		File resDir = new File(resdir);
		if(!resDir.exists())resDir.mkdir();
		printParams(resDir, args1);
		String[] bamFiles_ = null;
		boolean fastq= true;
		String inputFile;
		if(bamFile!=null){
			bamFiles_=bamFile.split(":");
			fastq = false;
			inputFile = bamFile;
		}else{
			bamFiles_=fastqFile.split(":");
			fastq = true;
			inputFile = fastqFile;

		}
		final boolean fastq_ = fastq;
		if(inputFile.equals("all") || inputFile.equals(".")){
			bamFiles_ = (new File("./")).list(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return !fastq_ && name.endsWith(".bam") || (fastq_ && (name.endsWith(".fq.gz") || name.endsWith(".fastq.gz")  || name.endsWith(".fastq") || name.endsWith(".fq")));
				}
				
			});
		}
		run(cmdLine, bamFiles_, resdir, anno,  chroms, fastq, reference);
		Outputs.executor.shutdown();
		long time1 = System.currentTimeMillis();
		System.err.println((time1-tme)/(1000)+ " seconds");
		// paramEst(bamFile, reference, qual);
	}
public static boolean combineOutput = false;
	public static double fail_thresh = 7.0;
	/**
	 * Error analysis of a bam file. Assume it has been sorted
	 */
	static void errorAnalysis(String[] bamFiles_, String refFile, String annot_file, String[] readList,    String annotationType, String resdir, String pattern, int qual, int round, 
			int break_thresh, int startThresh, int endThresh, int max_reads,  boolean sorted,
			boolean calcBreaks , boolean filterBy5_3, boolean annotByBreakPosition,Map<String, JapsaAnnotation> anno, String chrToInclude, boolean overwrite,
			int[] writeIsoformDepthThresh, int writeCoverageDepthThresh, double probInclude, boolean fastq) throws IOException {
		boolean cluster_reads = true;
		CigarHash2.round = round;
		
		IdentityProfile1.annotByBreakPosition = annotByBreakPosition;
		CigarHash.cluster_by_annotation =true;// cluster_by_annotation;
		TranscriptUtils.startThresh = startThresh;
		TranscriptUtils.endThresh = endThresh;
		IdentityProfile1.writeIsoformDepthThresh =writeIsoformDepthThresh;
		IdentityProfile1.writeCoverageDepthThresh =writeCoverageDepthThresh;
		File annotSummary = new File(resdir, "annotation.csv.gz");
		if(annotSummary.exists()) annotSummary.delete();
		PrintWriter annotation_pw = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(annotSummary, false))));
	
		Map<String, Integer> reads= new HashMap<String, Integer>();
		if(readList.length>0 && readList[0].length()>0){
		 if( readList[0].indexOf("readToCluster.txt.gz")>=0){
			 Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
			 for(int i=0; i<readList.length; i++){
			 BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(readList[i])))));
				String st = br.readLine();
				List<String> head = Arrays.asList(st.split("\\t"));
				int readind = head.indexOf("readID");
				int orfind = head.indexOf("ORFs");
				int chromind = head.indexOf("chrom");
				while((st = br.readLine())!=null){
					String[] str  = st.split("\\t");
					String readId = str[readind];
					String orfID = str[chromind]+";"+str[orfind];
					Collection<String> l= map.get(orfID) ;
					if(l==null) {
						map.put(orfID, new HashSet<String>());
						l= map.get(orfID) ;
					}
					l.add(readId);
				}
				br.close();
			 }
			readList = map.keySet().toArray(new String[0]);
			for(int i=0; i<readList.length; i++){
				Iterator<String> it =  map.get(readList[i]).iterator();
				while(it.hasNext()){
					reads.put(it.next(),i);
				}
			}
		 }else{
		// reads= new Collection[readList.length];
		 for(int i=0; i<readList.length; i++){
		//	reads[i] = new HashSet<String>();
			BufferedReader br = new BufferedReader(new FileReader(new File(readList[i])));
			String st;
			while((st = br.readLine())!=null){
				String st_ = st.split("\\s+")[0];
				System.err.println(st_);
				reads.put(st, i);
			}
			br.close();
		 }
		 }
		}
		if(reads.size()==0) reads=null;  //make it null to turn off this feature
		TranscriptUtils.break_thresh = break_thresh;
		int len = bamFiles_.length;
		// len = 1;
		String[] in_nmes  = new String[len];
		ArrayList<Sequence> genomes = SequenceReader.readAll(refFile, Alphabet.DNA());

		// get the first chrom
		File resDir =  new File(resdir);
		if(!resDir.exists()) resDir.mkdir();
		else if(!resDir.isDirectory()) throw new RuntimeException(resDir+"should be a directory");
		///ArrayList<IdentityProfile1> profiles = new ArrayList<IdentityProfile1>();
		
		boolean gff = anno!=null;
		for (int ii = 0; ii < len; ii++) {
			String bamFile = bamFiles_[ii];
			File bam = new File( bamFile);
			in_nmes[ii] = bam.getName().split("\\.")[0];
		}
			
		
	//	genes_all_pw.close();
		IdentityProfile1 profile = null;
		
		final Iterator<SAMRecord>[] samIters = new Iterator[len];
		SamReader[] samReaders = new SamReader[len];
		for (int ii = 0; ii < len; ii++) {
			String bamFile = bamFiles_[ii];
			File bam = new File( bamFile);
			SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
			if(bam.getName().endsWith(".bam")){
				samReaders[ii] = SamReaderFactory.makeDefault().open(bam);
				samIters[ii] = samReaders[ii].iterator();
			}else{
				//(File inFile, File mm2Index, String mm2_path, 
			//	int mm2_threads, String mm2Preset, String mm2_mem)
				samIters[ii] = SequenceUtils.getSAMIteratorFromFastq(bam, mm2_index, mm2_path, mm2_threads,  mm2Preset, mm2_mem, mm2_splicing);
			}
		}
		Map<Integer, int[]> chrom_indices_to_include = new HashMap<Integer, int[]>();
		Map<String, int[]> chromsToInclude = new HashMap<String, int[]>();
		if(chrToInclude!=null && !chrToInclude.equals("all")){
			String[] chri = chrToInclude.split(":");
			for(int i=0; i<chri.length; i++){
				String[] str = chri[i].split(",");
				int[] vals ;
				if(str.length==1) vals = new int[] {0,Integer.MAX_VALUE};
				else{
					vals = new int[] {Integer.parseInt(str[1]), Integer.parseInt(str[2])};
				}
				chrom_indices_to_include.put(Integer.parseInt(str[0]), vals);
			}
		}
		if(chrom_indices_to_include.size()==0) chrom_indices_to_include=null;
		
		Iterator<SAMRecord> samIter= SequenceUtils.getCombined(samIters);
		System.err.println("chrom indices to include");
		System.err.println(chrom_indices_to_include);
		for(int key=0; key<genomes.size();key++){
			if(chrom_indices_to_include==null || chrom_indices_to_include.containsKey(key))
				chromsToInclude.put(genomes.get(key).getName(),chrom_indices_to_include==null ? new int[]{0,Integer.MAX_VALUE} :chrom_indices_to_include.get(key));
		}
		System.err.println(chromsToInclude.size());
			int currentIndex = 0;
			
			Sequence chr = genomes.get(currentIndex);
			Outputs outp = null;
			if(combineOutput)	outp = new Outputs(resDir,  in_nmes, overwrite, 0, chr.getName(), true, CigarCluster.recordDepthByPosition); 
			int primelen = 500;//chr.length();
			Sequence chr5prime = TranscriptUtils.coronavirus ? chr.subSequence(0	, Math.min( primelen, chr.length())) : null;
			Sequence chr3prime = TranscriptUtils.coronavirus ? chr.subSequence(Math.max(0, chr.length()-primelen), chr.length()) : null;

			Set<String> doneChr = new HashSet<String>();
			
			long totReadBase = 0, totRefBase = 0;
			int numReads = 0;

			int numNotAligned = 0;
			int prev_src_index =-1;
			int numSecondary =0;
			outer: for (; samIter.hasNext() ; ) {
				SAMRecord sam= null;
				try{
					sam = samIter.next();
				}catch(Exception exc){
					exc.printStackTrace();
				}
				if(chrom_indices_to_include!=null && !chrom_indices_to_include.containsKey(sam.getReferenceIndex())){
					continue;
				}
				int poolID = 0;
				if(reads!=null){ 
					Integer readInd = reads.get(sam.getReadName());
					if(readInd==null) continue;
					else poolID = readInd;
				}
				byte[] b = sam.getBaseQualities();
				double sump = 0;
				for(int i=0; i<b.length; i++){
					sump+= Math.pow(10, -b[i]/10);
				}
				sump = sump/b.length;
				double q1 = -10*Math.log10(sump);
				
				if(q1 < fail_thresh) {
					continue;
				}
			//	System.err.println(q1);
				
				if(sam==null) break outer;
				int source_index = (Integer) sam.getAttribute(SequenceUtils.src_tag);
				//if(source_index==null) source_index = 0;
				
				if (pattern != null && (!sam.getReadName().contains(pattern)))
					continue;
				Sequence readSeq = new Sequence(Alphabet.DNA(), sam.getReadString(), sam.getReadName());
				//String poolID = readSeq.get
				if (readSeq.length() <= 1) {
					//LOG.warn(sam.getReadName() +" ignored");
					
					// TODO: This might be secondary alignment, need to do something about it
					continue;
				}

				numReads++;
				if(sam.isSecondaryOrSupplementary()) {
					numSecondary++;
					continue;
				}
				if (sam.getReadUnmappedFlag()) {
					numNotAligned++;
					continue;
				}

				int flag = sam.getFlags();

				if (sam.getMappingQuality() < qual) {
					numNotAligned++;
					continue;
				}
				if(probInclude<1.0 && Math.random()>probInclude){
					//randomly exclude
					continue;
				}
				
				// int refPos = sam.getAlignmentStart() - 1;//convert to 0-based index
				int refIndex = sam.getReferenceIndex();
				
				// if move to another chrom, get that chrom
				if (refIndex != currentIndex) {
					if( profile!=null && currentIndex>=0){
						profile.printBreakPoints();
						profile.getConsensus();
						if(!combineOutput){
							outp.close();
							outp = null;
						}
						profile= null;
						
						doneChr.add(chr.getName());
						System.err.println("finished "+chr.getName());
						if(chromsToInclude != null ){
							chromsToInclude.remove(chr.getName());
							if(chromsToInclude.size()==0) {
								System.err.println("finished sample "+in_nmes[source_index]);
								break outer;
							}
						}
					}
					currentIndex = refIndex;
					String prev_chrom = chr==null ? "null": chr.getName();
					if(currentIndex>=genomes.size() || currentIndex <0){
						System.err.println("for some reason the refIndex greater than genome size, so finishing here "+currentIndex+" "+genomes.size());
						break outer;
					}
					chr = genomes.get(currentIndex);
					chr5prime = TranscriptUtils.coronavirus ? chr.subSequence(0	, Math.min( primelen, chr.length())) : null;
					chr3prime = TranscriptUtils.coronavirus ? chr.subSequence(Math.max(0, chr.length()-primelen), chr.length()) : null;
					System.err.println("switch chrom "+prev_chrom+"  to "+chr.getName());
				}
				if(source_index!=prev_src_index){
					System.err.println("switch "+prev_src_index+"  to "+source_index+ " chr:"+chr.getName());
					 prev_src_index = source_index;
				}
				
				
					if(profile==null){
							if(doneChr.contains(chr.getName())){
								try{
									throw new RuntimeException("not sorted contains "+ chr.getName());
								}catch(Exception exc){
									exc.printStackTrace();
								}
							//	System.err.println("warning done"+chr.getName());
							}
							int seqlen = chr.length();
						//	if(!anno.containsKey(chr))
							Annotation annot = null;
						
							
							if(gff){
								JapsaAnnotation annot1 = anno.get(chr.getName());
								if(annot1==null) annot1 = anno.get("chr"+chr.getName());
								if(annot1==null) annot1 = anno.get(chr.getName().replaceAll("chr", ""));
								if(annot1==null){
									try{
										annot = new EmptyAnnotation(chr.getName(), chr.getDesc(), seqlen, annotation_pw);
									System.err.println("no annotation for  "+ chr.getName());
									}catch(Exception exc){
										exc.printStackTrace();
									}
								}else{
									
									annot = new GFFAnnotation(annot1, seqlen, annotation_pw, len);
									
								}
							}else{
								annot = annot_file == null ? new EmptyAnnotation(chr.getName(), chr.getDesc(), seqlen, annotation_pw) : 
									new Annotation(new File(annot_file), currentIndex+"", seqlen, len);
							}
						//	pw.close();
						if(!combineOutput)	outp = new Outputs(resDir,  in_nmes, overwrite, currentIndex, chr.getName(), true, CigarCluster.recordDepthByPosition); 
							boolean calcBreaks1 = calcBreaks && break_thresh < seqlen;
							profile = new IdentityProfile1(chr, outp,  in_nmes, startThresh, endThresh, annot, calcBreaks1, chr.getName(), currentIndex);
					}
				
					try{
						String pool = readList==null || poolID<0 ? "" : (readList[poolID]+"|");
						TranscriptUtils.identity1(chr, chr5prime,chr3prime, readSeq, sam, profile, source_index, cluster_reads, chr.length(), pool);
					}catch(NumberFormatException exc){
						System.err.println(readSeq.getName());
						exc.printStackTrace();
					}
			}//samIter.hasNext()
			for(int ii=0; ii<samReaders.length; ii++){
			if(samReaders[ii] !=null) samReaders[ii].close();
			}
			
		
		
			if(profile!=null){
				profile.printBreakPoints();
				profile.getConsensus();
				
			}
		
		if(outp!=null) outp.close();
	
		if(annotation_pw!=null) annotation_pw.close();
		
	
	}
	
}