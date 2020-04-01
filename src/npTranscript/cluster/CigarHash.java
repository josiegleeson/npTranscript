package npTranscript.cluster;

import java.util.ArrayList;
/**
 * @author Lachlan Coin
 *
 */

public class CigarHash extends ArrayList<Integer>{
		public static int round = 100;
		@Override
		public String toString(){
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<this.size(); i++){
				if(i>0) sb.append(",");
				sb.append(this.get(i));
			}
			return sb.toString();
		}
		
		
		@Override
		public boolean add(Integer i){
			//if(this.contains(i)) throw new RuntimeException("error");
			return (super.add(i));
		}
		
	
		/*public int hashCode(){
			//int hash = this.stream() .reduce(0, Integer::sum);
			int hash = calcHash();
			return hash;
		}*/
		//int hash=0;
		
		/*@Override
		 public void clear() {
			 hash =0;
		 }*/
		@Override
		public int hashCode(){
			StringBuffer sb = new StringBuffer();
	   		for(int i =0; i<size(); i++){
	   			sb.append(get(i));
	   		}
	   		return sb.toString().hashCode();
		}
		private void roundBreaks(){
			for(int i =0; i<size(); i++){
	   			set(i,  TranscriptUtils.round(get(i), round));
			}
	   	}
		/*@Override
public boolean equals(Object o){
			CigarHash cc = (CigarHash) o;
		 return this.equals(cc.breaks);	
		}
	}*/
		static int[] breaks_in = new int[2];
		static int[] breaks_out = new int[2];

		
		
		public void adjustBreaks(Annotation annot) {
			//if(size()!=4) {
			//	return;
			//}
			int st = get(0);
			if(get(0)<100) set(0,0);
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
			if(annot.seqlen-get(size()-1)<100) set(size()-1,annot.seqlen);
			this.roundBreaks();
			
		}
	}