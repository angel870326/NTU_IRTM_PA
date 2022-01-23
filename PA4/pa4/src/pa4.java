import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class pa4 {
	private static ArrayList<ArrayList<String>> tokensArrayList;
	private static ArrayList<ArrayList<String>> mergedTokensList;
	private static ArrayList<ArrayList<String>> irtm_dict;
	private static ArrayList<ArrayList<String>> index_idf;
	private static ArrayList<ArrayList<ArrayList<String>>> tfidfList; // doc_tfidf lists
	private static int docSize;
	private static ArrayList<SimilarityHeap> hac;
	private static ArrayList<ArrayList<Similarity>> similarityList;
	private static ArrayList<Integer> clusterLife;
	private static int liveCount;
	private static int k1;
	private static int k2;
	private static int k3;
	// docID in each cluster
	private static ArrayList<ArrayList<Integer>> kList; // size = 1095
	private static ArrayList<ArrayList<Integer>> k1List; // size = k1
	private static ArrayList<ArrayList<Integer>> k2List; // size = k2
	private static ArrayList<ArrayList<Integer>> k3List; // size = k3
	
	public static void main(String[] args) throws Exception {  
		docSize = 1095;
		k1 = 20;
		k2 = 13;
		k3 = 8;
		/* 1. Dictionary */
		// (1) terms & docID
		tokensArrayList = new ArrayList<ArrayList<String>>();
		for (int docID = 1; docID <= docSize; docID++) {
			doctTokens(docID);
		}
    	Collections.sort(tokensArrayList, new Comparator<ArrayList<String>>() {    
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                return o1.get(0).compareTo(o2.get(0));
            }
        });

    	// (2) merge same term from the same document
		mergedTokensList = new ArrayList<ArrayList<String>>();
		mergedTokensList.add(tokensArrayList.get(0));
		for(int i = 1; i < tokensArrayList.size(); i++) {
			int pre = i-1;
			if(!tokensArrayList.get(i).equals(tokensArrayList.get(pre))) {
				mergedTokensList.add(tokensArrayList.get(i));
			}
		}		
		
		// (3) t_index & term & df
		irtm_dict = new ArrayList<ArrayList<String>>();
		int t_index = 1;
		ArrayList<String> firstEle = new ArrayList<String>();
		firstEle.add(Integer.toString(t_index));
		firstEle.add(mergedTokensList.get(0).get(0));
		firstEle.add("1");
		irtm_dict.add(firstEle);
		for(int i = 1; i < mergedTokensList.size(); i++) {
			int pre = i-1;
			ArrayList<String> element = new ArrayList<String>();			
			if(!mergedTokensList.get(i).get(0).equals(mergedTokensList.get(pre).get(0))) {
				t_index++;
				element.add(Integer.toString(t_index));
				element.add(mergedTokensList.get(i).get(0));
				element.add("1");
				irtm_dict.add(element);
			} else {
				int count = Integer.parseInt(irtm_dict.get(irtm_dict.size()-1).get(2));
				count++;
				irtm_dict.get(irtm_dict.size()-1).set(2, Integer.toString(count));
			}
		}		
    	
		/* 2. Transfer each document into a tf-idf unit vector */
    	// (1) idf
		index_idf = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < irtm_dict.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();	
			element.add(irtm_dict.get(i).get(0));
			element.add(Double.toString(Math.log10(docSize/Integer.parseInt(irtm_dict.get(i).get(2)))));
			index_idf.add(element);
		}
		
		// (2) Build tf-idf Lists (size = 1095)
		tfidfList = new ArrayList<ArrayList<ArrayList<String>>>();
		for (int i = 1; i <= docSize; i++) {
			tfidfList.add(doc_tfidf(i));
		}
		System.out.println("Tf-idf lists done!");
		
		/* 3. Build priority queue for each document */		
		hac = new ArrayList<SimilarityHeap>();
		similarityList = new ArrayList<ArrayList<Similarity>>();
		clusterLife = new ArrayList<Integer>();
		liveCount = 0;  // 計算存活的 cluster 有幾個
		for (int n = 1; n <= docSize; n++) {
			SimilarityHeap heap = new SimilarityHeap(); // heap for doc_n
			ArrayList<Similarity> docSimList = new ArrayList<Similarity>(); // similarity list for doc_n
			ArrayList<ArrayList<String>> doc_n = tfidfList.get(n-1);
			for (int i = 1; i <= docSize; i++) {
				if(n != i) {
					ArrayList<ArrayList<String>> doc_i = tfidfList.get(i-1);
					double similarity = cosine(doc_n, doc_i);
					Similarity s = new Similarity(similarity, n, i);
					heap.add(s);	
					docSimList.add(s);
				}
			}
			hac.add(heap);
			similarityList.add(docSimList);
			clusterLife.add(1);
			liveCount++;
		}
		System.out.println("Priority queue lists done!");
		// check
//		System.out.println("similarityList0: " + similarityList.toString());
		
		/* 4. Clustering result */
		kList = new ArrayList<ArrayList<Integer>>();
		for (int i = 1; i <= docSize; i++) {
			ArrayList<Integer> a = new ArrayList<Integer>();
			a.add(i);
			kList.add(a);
		}
		k1List = new ArrayList<ArrayList<Integer>>();
		k2List = new ArrayList<ArrayList<Integer>>();
		k3List = new ArrayList<ArrayList<Integer>>();
		System.out.println("Clustering result lists built!");

		/* 5. 做 N-1 次 merge */
		for (int i = 1; i < docSize-1 && liveCount > k3; i++) {
			// 從 1095 個 maxSim 中找出 maxSim
			SimilarityHeap heap = new SimilarityHeap();
			for (int j = 0; j < hac.size(); j++) {
				if (clusterLife.get(j) == 1) {
					heap.add(hac.get(j).peek());
				}
			}
			Similarity maxSim = heap.peek();
			// check
//			System.out.println("maxSim: " + maxSim.doc1 + "," + maxSim.doc2);
			
			// ID 大的 doc 併到 ID 小的 doc (cluster)
			int docS = Math.min(maxSim.doc1, maxSim.doc2);
			int docL = Math.max(maxSim.doc1, maxSim.doc2);
			clusterLife.set(docL-1, 0);
			liveCount--;
			ArrayList<Integer> docL_docs = kList.get(docL-1);
			for (int j = 0; j < docL_docs.size(); j++) {
				kList.get(docS-1).add(docL_docs.get(j));
			}
			Collections.sort(kList.get(docS-1));
			kList.get(docL-1).clear();
			// check kList
//			System.out.println("kList: " + kList);

			// 刪除 docL 的 priority queue
			hac.get(docL-1).removeAll();

			// 刪除 docS 及 docL 的 similarity
			for (int j = 0; j < hac.size(); j++) {
				if (clusterLife.get(j) == 1) {
					SimilarityHeap pq = new SimilarityHeap();
					pq = hac.get(j);
					ArrayList<Similarity> docSimList = new ArrayList<Similarity>();
					docSimList = similarityList.get(j);
					if (pq != null) {
						for (int l = 0; l < docSimList.size(); l++) {
							Similarity s = docSimList.get(l);
							if (s.doc2 == docS) {
								pq.remove(s);
							}
							if (s.doc2 == docL) {
								pq.remove(s);
							}
						}
					} else {
						System.out.println("There is error for deleting similarity.");
					}
				}
			}
			
			// 重算 docS 的 Complete Link Similarity
			SimilarityHeap newHeap = new SimilarityHeap(); // new heap for docS
			ArrayList<Integer> clusterS = new ArrayList<Integer>(); // clusterS 中所有 doc
			clusterS = kList.get(docS-1);			
			for (int j = 0; j < kList.size(); j++) {
				if (clusterLife.get(j) == 1 && j != docS-1) {
					ArrayList<Integer> clusterO = new ArrayList<Integer>(); // clusterO 中所有 doc
					clusterO = kList.get(j);
					double minSim = 0;
					for (int o = 0; o < clusterO.size(); o++) {
						int docID_o = clusterO.get(o);
						for (int m = 0; m < clusterS.size(); m++) {
							int docID_m = clusterS.get(m);
							int index = docID_o-2;
							if(docID_m > docID_o) {
								index = docID_o-1;
							}							
							double s = similarityList.get(docID_m-1).get(index).similarity;
							if (minSim == 0 || s < minSim) {
								minSim = s;
							}
						}
					}
					// update docS 的 heap 和 similarityList
					Similarity sim1 = new Similarity(minSim, docS, j+1);
					newHeap.add(sim1);
					int id1 = j-1;
					if(docS > j+1) {
						id1 = j;
					}
					similarityList.get(docS-1).set(id1, sim1);
					// update j 的 heap 和 similarityList
					Similarity sim2 = new Similarity(minSim, j+1, docS);
					hac.get(j).add(sim2);
					int id2 = docS-2;
					if(j+1 > docS) {
						id2 = docS-1;
					}
					similarityList.get(j).set(id2, sim2);
				}
			}
			hac.set(docS-1, newHeap);			
			
			// 刪除 docL 的 similarityList
//			for (int j = 0; j < similarityList.get(docL-1).size(); j++) {
//				similarityList.get(docL-1).clear();
//			}
			
			//  Build clustering result
			if (liveCount == k3) {
				System.out.println("There are " + k3 + " clusters!");
				for (int j = 0; j < kList.size(); j++) {
					ArrayList<Integer> list = kList.get(j);
					if (!list.isEmpty()) {
						k3List.add(list);
					}
				}
				writeClusterResult(k3List, k3);
				System.out.println("Write k3List done!");
				// check
				System.out.println(k3List.size() +"-"+k3List);

			} else if (liveCount == k2) {
				System.out.println("There are " + k2 + " clusters!");
				for (int j = 0; j < kList.size(); j++) {
					ArrayList<Integer> list = kList.get(j);
					if (!list.isEmpty()) {
						k2List.add(list);
					}
				}
				writeClusterResult(k2List, k2);
				System.out.println("Write k2List done!");
				// check
				System.out.println(k2List.size() +"-"+k2List);

			} else if (liveCount == k1) {
				System.out.println("There are " + k1 + " clusters!");
				for (int j = 0; j < kList.size(); j++) {
					ArrayList<Integer> list = kList.get(j);
					if (!list.isEmpty()) {
						k1List.add(list);
					}
				}
				writeClusterResult(k1List, k1);
				System.out.println("Write k1List done!");
				// check
				System.out.println(k1List.size() +"-"+k1List);
			}
		}
	}
	
	/* Method doctTokens(): Tokens from single document */
	public static void doctTokens(int docID) throws Exception {
    	// Read document
    	String pathname = String.format("src/data/%d.txt", docID);
    	File filename = new File(pathname);
    	InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
    	BufferedReader br = new BufferedReader(reader);
    	String line = br.readLine();
    	String document = line;
    	while (line != null) {
    		line = br.readLine();
    		if (line != null) {
        		document = document + line;
    		} else {
    	    	br.close();
    		}
    	}
   	
    	// tokenization and lowercasing
    	String[] token = document.replaceAll("[^a-zA-Z ]", "").toLowerCase().split("\\s+");
    	
    	// Porter's Algorithm
    	String[] stemToken = new String[token.length];
    	for(int i = 0; i < token.length; i++) {
        	Stemmer s = new Stemmer();
        	char charToken[] = new char[token[i].length()];
        	charToken = token[i].toCharArray();
        	s.add(charToken,token[i].length());
        	s.stem();
        	stemToken[i] = s.toString();
    	}
    	
    	// 自定義 stopword list
    	String stopwords = "i, me, my, myself, we, our, ours, ourselves, you, youre, youve, youll, youd, your, yours, yourself, yourselves, he, him, his, himself, she, shes, her, hers, herself, it, its, itself, they, them, their, theirs, themselves, what, which, who, whom, this, that, thatll, these, those, am, is, are, was, were, be, been, being, have, has, had, having, do, does, did, doing, a, an, the, and, but, if, or, because, as, until, while, of, at, by, for, with, about, against, between, into, through, during, before, after, above, below, to, from, up, down, in, out, on, off, over, under, again, further, then, once, here, there, when, where, why, how, all, any, both, each, few, more, most, other, some, such, no, nor, not, only, own, same, so, than, too, very, s, t, can, will, just, don, dont, should, shouldve, now, d, ll, m, o, re, ve, y, ain, aren, arent, couldn, couldnt, didn, didnt, doesn, doesnt, hadn, hadnt, hasn, hasnt, haven, havent, isn, isnt, ma, mightn, mightnt, mustn, mustnt, needn, neednt, shan, shant, shouldn, shouldnt, wasn, wasnt, weren, werent, won, wont, wouldn, wouldnt";
    	for(int i = 0; i < stemToken.length; i++) {
    		if(!stopwords.contains(stemToken[i])) {
    			ArrayList<String> element = new ArrayList<String>();
    			element.add(stemToken[i]);
    			element.add(Integer.toString(docID));
        		tokensArrayList.add(element);
    		}
    	}		
	}
	
	/* Method doc_tfidf(): calculate tf-idf for single document */
	public static ArrayList<ArrayList<String>> doc_tfidf(int docID) {		
		// term_frequency
		ArrayList<ArrayList<String>> term_f = new ArrayList<ArrayList<String>>();
		ArrayList<String> element1 = new ArrayList<String>();
		element1.add(tokensArrayList.get(0).get(0));
		element1.add("1");
		term_f.add(element1);
		for(int i = 1; i < tokensArrayList.size(); i++) {
			int pre = i-1;
			ArrayList<String> element = new ArrayList<String>();
			ArrayList<String> list_i = tokensArrayList.get(i);
			if(list_i.get(1).equals(Integer.toString(docID))) {
				if(!list_i.equals(tokensArrayList.get(pre))) {
					element.add(list_i.get(0));
					element.add("1");
					term_f.add(element);
				} else{
					int count = Integer.parseInt(term_f.get(term_f.size()-1).get(1));
					count++;
					term_f.get(term_f.size()-1).set(1, Integer.toString(count));
				}				
			}
		}		
		// term_tf
		ArrayList<ArrayList<String>> term_tf = new ArrayList<ArrayList<String>>();
		int total_f = 0;
		for(int i = 0; i < term_f.size(); i++) {
			total_f += Integer.parseInt(term_f.get(i).get(1));
		}
		for(int i = 0; i < term_f.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			ArrayList<String> list_i = term_f.get(i);
			element.add(list_i.get(0));
			element.add(Double.toString(Double.parseDouble(list_i.get(1))/total_f));
			term_tf.add(element);
		}		
		// index_tf_idf
		ArrayList<ArrayList<String>> index_tf_idf = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < term_tf.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			ArrayList<String> list_i = term_tf.get(i);
			Object term = list_i.get(0);
			int termIndex = -1;
			for(int j = 0; j < irtm_dict.size(); j++) {
				ArrayList<String> list_j = irtm_dict.get(j);
				if(list_j.get(1).equals(term)) {
					termIndex = Integer.parseInt(list_j.get(0)) - 1;
				}
			}			
			double idf = Double.parseDouble(index_idf.get(termIndex).get(1));
			element.add(irtm_dict.get(termIndex).get(0));
			element.add(Double.toString(Double.parseDouble(list_i.get(1))*idf));
			index_tf_idf.add(element);
		}		
		// vector length
		double sum = 0;
		for(int i = 0; i < index_tf_idf.size(); i++) {
			sum += Math.pow((Double.parseDouble(index_tf_idf.get(i).get(1))),2);			
		}
		double vector_length = Math.sqrt(sum);		
		// normalization		
		ArrayList<ArrayList<String>> index_tf_idf_n = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < index_tf_idf.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			element.add(index_tf_idf.get(i).get(0));
			element.add(Double.toString(Double.parseDouble(index_tf_idf.get(i).get(1))/vector_length));
			index_tf_idf_n.add(element);
		}		
		return index_tf_idf_n;		
	}	
	
	/* Method cosine(Docx, Docy) */
	public static double cosine(ArrayList<ArrayList<String>> x, ArrayList<ArrayList<String>> y) {	
		double similarity = 0;
		for(int i = 0; i < x.size(); i++) {
			ArrayList<String> xi = x.get(i);
			for(int j = 0; j < y.size(); j++) {
				ArrayList<String> yj = y.get(j);
				if(xi.get(0).equals(yj.get(0))) {
					double multi = Double.parseDouble(xi.get(1)) * Double.parseDouble(yj.get(1));
					similarity += multi;
				}
			}			
		}
		return similarity;
	}
	
	/* Write Clustering Result */
	public static void writeClusterResult (ArrayList<ArrayList<Integer>> list, int k) throws Exception {
    	File writepath = new File(String.format("src/output/%d.txt", k));
    	writepath.createNewFile();
    	BufferedWriter bw = new BufferedWriter(new FileWriter(writepath));
		String str = "";
    	for(int i = 0; i < list.size() && list.size() == k; i++) {
    		String s = "";
    		for(int j = 0; j < list.get(i).size(); j++) {
    			s = s + Integer.toString(list.get(i).get(j)) + "\n";
    		}
    		str = str + s + "\n";	
    	}
    	bw.write(str);
    	bw.flush();
    	bw.close();		
	}

}
