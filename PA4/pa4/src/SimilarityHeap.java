import java.util.PriorityQueue;

public class SimilarityHeap {
	private PriorityQueue<Similarity> heap;
	
	public SimilarityHeap(){
		this.heap = new PriorityQueue<Similarity>(1095, new SimilarityComparator());
	}
	
	public void add(Similarity s){
		heap.offer(s);
	}
	
	public Similarity peek(){
		Similarity s = heap.peek();		
		if(s == null){
			return null;
		} else {
			return s;
		}
	}
	
	public void removeMax(){
		heap.poll();
	}
	
	public void removeAll(){
		heap.clear();
	}
	
	public void remove(Similarity s){
		heap.remove(s);
	}

}
