import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class pa2 {
	private static ArrayList<ArrayList<String>> tokensArrayList;
	private static ArrayList<ArrayList<String>> mergedTokensList;
	private static ArrayList<ArrayList<String>> irtm_dict;
	private static ArrayList<ArrayList<String>> index_idf;
	private static int docSize;
	
	public static void main(String[] args) throws Exception {  
		docSize = 1095;
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
    	// check output
//		System.out.println("Sorted List: " + tokensArrayList);

    	// (2) merge same term from the same document
		mergedTokensList = new ArrayList<ArrayList<String>>();
		mergedTokensList.add(tokensArrayList.get(0));
		for(int i = 1; i < tokensArrayList.size(); i++) {
			int pre = i-1;
			if(!tokensArrayList.get(i).equals(tokensArrayList.get(pre))) {
				mergedTokensList.add(tokensArrayList.get(i));
			}
		}
    	// check output
//		System.out.println("Merged List: " + mergedTokensList);
//    	File temptPath1 = new File("src/merge.txt");
//    	temptPath1.createNewFile();
//    	BufferedWriter tbw1 = new BufferedWriter(new FileWriter(temptPath1));
//		String temptStr1 = "";
//    	for(int i = 0; i < mergedTokensList.size(); i++) {
//    		temptStr1 = temptStr1 + mergedTokensList.get(i).toString() + "\n";
//    	}
//    	tbw1.write(temptStr1);
//    	tbw1.flush();
//    	tbw1.close();		
		
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
    	// check output
//		System.out.println("Dictionary: " + irtm_dict);

    	// (4) Write document
    	File writepath = new File("src/dictionary.txt");
    	writepath.createNewFile();
    	BufferedWriter bw = new BufferedWriter(new FileWriter(writepath));
		String dictStr = "";
    	for(int i = 0; i < irtm_dict.size(); i++) {
    		dictStr = dictStr + irtm_dict.get(i).toString() + "\n";
    	}
    	bw.write(dictStr);
    	bw.flush();
    	bw.close();		
		
    	
		/* 2. Transfer each document into a tf-idf unit vector */
    	// idf
		index_idf = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < irtm_dict.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();	
			element.add(irtm_dict.get(i).get(0));
			element.add(Double.toString(Math.log10(docSize/Integer.parseInt(irtm_dict.get(i).get(2)))));
			index_idf.add(element);
		}
    	// check output
//		if(index_idf.size() == irtm_dict.size()) {
//	    	File temptPath2 = new File("src/idf.txt");
//	    	temptPath2.createNewFile();
//	    	BufferedWriter tbw2 = new BufferedWriter(new FileWriter(temptPath2));
//			String temptStr2 = "";
//	    	for(int i = 0; i < index_idf.size(); i++) {
//	    		temptStr2 = temptStr2 + index_idf.get(i).toString() + "\n";
//	    	}
//	    	tbw2.write(temptStr2);
//	    	tbw2.flush();
//	    	tbw2.close();		
//		} else {
//			System.out.print("false");
//		}
		
		// tf-idf		
		for (int docID = 1; docID <= docSize; docID++) {
			write_doc_tfidf(docID, doc_tfidf(docID));
		}
		
		/* 3. doc1 and doc2 similarity */
		System.out.print(cosine(1,2));

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
    	// check document
//    	System.out.println("Document content" + document);
   	
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
        	// check output
//    		System.out.println("原本：" + token[i]);
//    		System.out.println("Stem：" + s.toString());
//    		System.out.println(stemToken[i]);
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

    	// check output
//		System.out.println("Raw List: " + tokensArrayList);
    			
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
			if(tokensArrayList.get(i).get(1).equals(Integer.toString(docID))) {
				if(!tokensArrayList.get(i).equals(tokensArrayList.get(pre))) {
					element.add(tokensArrayList.get(i).get(0));
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
			element.add(term_f.get(i).get(0));
			element.add(Double.toString(Double.parseDouble(term_f.get(i).get(1))/total_f));
			term_tf.add(element);
		}
		
		// index_tf_idf
		ArrayList<ArrayList<String>> index_tf_idf = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < term_tf.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			Object term = term_tf.get(i).get(0);
			int termIndex = -1;
			for(int j = 0; j < irtm_dict.size(); j++) {
				if(irtm_dict.get(j).get(1).equals(term)) {
					termIndex = Integer.parseInt(irtm_dict.get(j).get(0)) - 1;
				}
			}			
			double idf = Double.parseDouble(index_idf.get(termIndex).get(1));
			element.add(irtm_dict.get(termIndex).get(0));
			element.add(Double.toString(Double.parseDouble(term_tf.get(i).get(1))*idf));
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
	
	/* Method write_doc_tfidf(): write index & tf-idf to txt file for single document */
	public static void write_doc_tfidf(int docID, ArrayList<ArrayList<String>> n) throws Exception {
		// output
	    File outputPath = new File(String.format("src/output/doc%d.txt", docID));
	   	outputPath.createNewFile();
	   	BufferedWriter tbw_output = new BufferedWriter(new FileWriter(outputPath));
		String ouput_result = "";
	    for(int i = 0; i < n.size(); i++) {
	   		ouput_result = ouput_result + n.get(i).toString() + "\n";
	   	}
	   	tbw_output.write(ouput_result);
	   	tbw_output.flush();
    	tbw_output.close();		
	}
	
	
	/* Method cosine(Docx, Docy) */
	public static double cosine(int docx, int docy) {	
		ArrayList<ArrayList<String>> x = doc_tfidf(docx);
		ArrayList<ArrayList<String>> y = doc_tfidf(docy);
		double similarity = 0;
		for(int i = 0; i < x.size(); i++) {
			for(int j = 0; j < y.size(); j++) {
				if(x.get(i).get(0).equals(y.get(j).get(0))) {
					double multi = Double.parseDouble(x.get(i).get(1)) * Double.parseDouble(y.get(j).get(1));
					similarity += multi;
				}
			}			
		}
		return similarity;
	}



}
