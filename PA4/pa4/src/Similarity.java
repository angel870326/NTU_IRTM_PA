
public class Similarity {
	public double similarity;
	public int doc1;
	public int doc2;
	
	public Similarity(double similarity, int doc1, int doc2){
		this.similarity = similarity;
		this.doc1 = doc1;
		this.doc2 = doc2;
	}
	
	@Override
	public String toString(){
		return "["+similarity+","+doc1+","+doc2+"]";
	}

}
