package npTranscript.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/**
 * @author Lachlan Coin
 *
 */

public class CigarHash2 extends ArrayList<Integer> {
	
	public CigarHash2 clone(boolean round){
		CigarHash2 obj =new CigarHash2();
		if(round) obj.addAllR(this);
		else obj.addAll(this);
		return obj;
	}
	
	public void addAllR(CigarHash2 obj) {
		for(int i=0; i<this.size(); i++){
			addR(obj.get(i));
		}
		
	}
	
	
	public static int round = 100;
	//static int[] breaks_in = new int[2];
	//static int[] breaks_out = new int[2];
//	public static boolean cluster_by_annotation = true;
	/* (non-Javadoc)
	 * @see npTranscript.cluster.CigHash#toString()
	 */
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<this.size(); i++){
			if(i>0) sb.append(";");
			sb.append(this.get(i));
		}
		return sb.toString();
	}
	
	
	
	private boolean addR(Integer i){ 
		Integer i1 = TranscriptUtils.round(i, round);
		return (super.add(i1));
	}



	

	
	
	/* (non-Javadoc)
	 * @see npTranscript.cluster.CigHash#hashCode()
	 */
	/*@Override
	public int hashCode(){
		return this.toString().hashCode();
	}*/
	/*@Override
	public boolean equals(Object obj){
		return this.hashCode()==obj.hashCode();
		//CigarHash2 ch = (CigarHash2)obj;
		//return this.toString().equals(ch.toString());

		/*if(ch.size()!=this.size()) return false;
		for(int i=0; i<size(); i++){
			if(!get(i).equals(ch.get(i))) return false;
		}
		return true;
	}*/
	/*private void roundBreaks(){
		for(int i =0; i<size(); i++){
   			set(i,  TranscriptUtils.round(get(i), round));
		}
   	}*/
	
	
	/*public void adjustBreaks(Annotation annot) {
		if(annot==null) return ;
		int st = get(0);
		if(get(0)<TranscriptUtils.startThresh) set(0,0);
		int end = get(size()-1);
		for(int i=1; i<size()-1; i+=2){
			breaks_in[0] = get(i);
			breaks_in[1] = get(i+1);
			annot.getBreaks(breaks_in, breaks_out, st, end);
			if(breaks_out[0] > get(i-1)){
				set(i, breaks_out[0]);
			}
			if( breaks_out[1] < get(i+2)){
				set(i+1,breaks_out[1]);
			}
		}
		if(annot.seqlen()-get(size()-1)<TranscriptUtils.endThresh) set(size()-1,annot.seqlen);
		this.roundBreaks();
		
	}*/

	
	}