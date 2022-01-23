import java.util.Comparator;

public class SimilarityComparator implements Comparator<Similarity>{
	@Override
	public int compare(Similarity s1, Similarity s2){
		if(s1==null || s2==null) throw new NullPointerException();
				
		if(s1.similarity > s2.similarity){
			return -1;
		}else if(s1.similarity < s2.similarity){
			return 1;
		}
		return 0;
	}
}
