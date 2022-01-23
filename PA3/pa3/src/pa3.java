import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import au.com.bytecode.opencsv.CSVWriter;

public class pa3 {
	private static ArrayList<ArrayList<String>> trainingList;
	private static ArrayList<ArrayList<String>> trainTokensList; // terms & docID & classID
	private static ArrayList<ArrayList<String>> mergedTrainTokensList; // terms & docID & classID
	private static ArrayList<ArrayList<String>> irtm_dict; // t_index & term & df
	private static ArrayList<ArrayList<String>> n11Lists; // term & class1's n11 &...& class13's n11
	private static ArrayList<String> dictionary;
	private static ArrayList<ArrayList<String>> condprobAllClass; 
	private static ArrayList<Integer> testingList;
	private static ArrayList<ArrayList<String>> testingResult;
	private static int irtm_size; // 1095
	private static int classNum; // number of classes
	private static int nc; // number of documents in each class
	private static int trainDocNum; // classNum*nc
	private static double prior; // nc/trainDocNum
	private static double logPrior; 

	public static void main(String[] args) throws Exception {
		irtm_size = 1095;
		classNum = 13;
		nc = 15;
		trainDocNum = classNum*nc;
		prior = nc/trainDocNum*1.0;
		logPrior = Math.log(prior);
		
		// Feature Selection
		createtrainingList(readTrainingFile());
		trainingDoc();
		n11Lists = new ArrayList<ArrayList<String>>();
		createN11Lists();
		
		// Dictionary
		dictionary = new ArrayList<String>();
		feature();
		System.out.println("Dictionary(" + dictionary.size() + ")" + dictionary);
				
		// Training
		condprobAllClass = new ArrayList<ArrayList<String>>();
		condprobAllClass();
//		System.out.println("condprobAllClass: " + condprobAllClass);
		
		// 找出 testing documents (docID)
		testingList = new ArrayList<Integer>();		
		for (int i = 1; i <= irtm_size; i++) {
			int count = 0;
			for (int j = 0; j < trainingList.size(); j++) {
				for (int k = 1; k < trainingList.get(j).size(); k++) {
					if (i == Integer.parseInt(trainingList.get(j).get(k))) {
						count++;
					}
				}
			}
			if (count == 0) {
				testingList.add(i);				
			}
		}
		System.out.println("Testing List: " + testingList);
		
		// Testing
		testingResult = new ArrayList<ArrayList<String>>();	
		ArrayList<String> firstElement = new ArrayList<String>();
		firstElement.add("Id");
		firstElement.add("Value");
		testingResult.add(firstElement);
		for (int i = 0; i < testingList.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			int docID = testingList.get(i);
			element.add(Integer.toString(docID));
			int result = testingResult(docID);
			element.add(Integer.toString(result));
			testingResult.add(element);
//			System.out.print(element.get(1));
		}						
		write(testingResult);		
	}
	
	/* Testing Phase */
	/**
	 * (2) 對文章做分類
	 * @param docID
	 * @return
	 * @throws Exception
	 */
	public static int testingResult(int docID) throws Exception {
		// 各個 class 的分數
		ArrayList<Double> score = new ArrayList<Double>(); // size = 13
		ArrayList<String> tokens = testingDoc(docID);
//		System.out.println("Tokens:" + tokens.size() + ", " + tokens);
		for (int i = 1; i <= classNum; i++) {
			double sum = 0; // 原本應該要是 logPrior 但因為大家都一樣就省略
			// 每個 token 找出 condprob 算分數
			for (int j = 0; j < tokens.size(); j++) {	
				for (int k = 0; k < condprobAllClass.size(); k++) {
					if (tokens.get(j).equals(condprobAllClass.get(k).get(0))){
						double condprob = Double.parseDouble(condprobAllClass.get(k).get(i));
						sum += condprob;
					}
				}
			}
			score.add(sum);
		}
//		System.out.println("Score:" + score);
		
		// 找出 13 個 score 中最大者
		double max = score.get(0);
		int classID = 1;
		for (int i = 1; i < score.size(); i++) {
			if (score.get(i) > max) {
				max = score.get(i);
				classID = i+1;
			}
		}
//		System.out.println("Result: " +  classID);
		return classID;
	}
	
	/**
	 * (1) 某篇文章所有有在 vocabulary 的 tokens
	 * @param docID
	 * @return
	 * @throws Exception
	 */
	public static ArrayList<String> testingDoc(int docID) throws Exception {
		ArrayList<String> testTokensList = new ArrayList<String>();
		ArrayList<String> list = new ArrayList<String>();		
    	// Read document
    	String pathname = String.format("src/input/%d.txt", docID);
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
    	stopwords += ", know, ye, yet, alex, john, lisa";
//    	stopwords += "janet, jame";
    	stopwords += ", hous, kill, offic";
//    	stopwords += ", nation, vladimir";
    	for(int i = 0; i < stemToken.length; i++) {
    		// 新增手動
    		stemToken[i] = stemToken[i].replace("knew", "know");
    		stemToken[i] = stemToken[i].replace("known", "know");
    		stemToken[i] = stemToken[i].replace("laboratori", "labor");
    		stemToken[i] = stemToken[i].replace("larger", "large");
    		stemToken[i] = stemToken[i].replace("largest", "large");
    		stemToken[i] = stemToken[i].replace("children", "child");
    		stemToken[i] = stemToken[i].replace("came", "come");
    		stemToken[i] = stemToken[i].replace("held", "hold");
    		stemToken[i] = stemToken[i].replace("prove", "proof");
    		stemToken[i] = stemToken[i].replace("smaller", "small");
    		stemToken[i] = stemToken[i].replace("taken", "take");
    		stemToken[i] = stemToken[i].replace("took", "take");
    		stemToken[i] = stemToken[i].replace("unknown", "unknow");
    		stemToken[i] = stemToken[i].replace("suspens", "suspend");

    		// stopwords
    		if(!stopwords.contains(stemToken[i])) {
    			list.add(stemToken[i]);
    		}
    	}
    	Collections.sort(list);
//    	System.out.println("Testing: " + list);
    	// 只取有在字典的 tokens
		for (int i = 0; i < list.size(); i++) {
			String term = list.get(i);
			for (int j = 0; j < dictionary.size(); j++) {
				if (dictionary.get(j).equals(term)) {
					testTokensList.add(term);
				} 
			}
		}		    	
    	// merge same term
//		mergedTrainTokensList = new ArrayList<ArrayList<String>>();
//		mergedTrainTokensList.add(trainTokensList.get(0));
//		for(int i = 1; i < trainTokensList.size(); i++) {
//			int pre = i-1;
//			if(!trainTokensList.get(i).equals(trainTokensList.get(pre))) {
//				mergedTrainTokensList.add(trainTokensList.get(i));
//			}
//		}   	
    	return testTokensList;
	}
	
	
	/* Training Phase */

	/**
	 * (4) 將各個class的condprob整合成一個二維list condprobAllClass
	 */
	public static void condprobAllClass() {
		// term & probability 1 & probability 2 & ...& probability 13
		for (int i = 1; i <= classNum; i++) {
			ArrayList<ArrayList<String>> list = condprob(i);
			for (int j = 0; j < list.size(); j++) {
				if (i == 1) {
					ArrayList<String> element = new ArrayList<String>();
					element.add(list.get(j).get(0));
					element.add(list.get(j).get(1));
					for (int k = 1; k <= classNum-1; k++) {
						element.add("0");
					}
					condprobAllClass.add(element);					
				} else {
					condprobAllClass.get(j).set(i, list.get(j).get(1));
				}				
			}			
		}
	}
	
	/**
	 * (3) 某個 class 中每個 term 的 condprob
	 * @param classID
	 * @return
	 */
	public static ArrayList<ArrayList<String>> condprob(int classID) {
		ArrayList<ArrayList<String>> tct = countTokensofTermV(classID); // term & 次數
		// term & probability
		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
		// 計算 tct 加總
		double sum = 0;
		for (int i = 0; i < tct.size(); i++) {
			sum += Integer.parseInt(tct.get(i).get(1));
		}
		// 算 probability
		for (int i = 0; i < tct.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			element.add(tct.get(i).get(0)); // term
			double prob = Integer.parseInt(tct.get(i).get(1))*1.0/sum;
			element.add(Double.toString(Math.round(Math.log(prob)*100.0)/100.0));
			list.add(element);
		}
		return list;
	}
	
	/**
	 * (2) 計算某個class中所有文章各個term的出現總次數，只留下有在 vocabulary 的 term（Add 1 smoothing）
	 * @param classID
	 * @return
	 */
	public static ArrayList<ArrayList<String>> countTokensofTermV(int classID) {
		// term & 次數
		ArrayList<ArrayList<String>> termV_f = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < dictionary.size(); i++) {
			String term = dictionary.get(i);
			ArrayList<String> element = new ArrayList<String>();
			element.add(term);
			element.add("1");
			ArrayList<ArrayList<String>> list = countTokensofTerm(classID);
			for (int j = 0; j < list.size(); j++) {
				if (list.get(j).get(0).equals(term)) {
					int count = Integer.parseInt(list.get(j).get(1))+1;
					element.set(1, Integer.toString(count));
				} 
			}
			termV_f.add(element);
		}		
		return termV_f;
	}
	
	/**
	 * (1) 計算某個class中所有文章各個term的出現總次數
	 * @param classID
	 * @return
	 */
	public static ArrayList<ArrayList<String>> countTokensofTerm(int classID) {
		// 從 trainTokensList 中取出 classID 相同者
		ArrayList<ArrayList<String>> list = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < trainTokensList.size(); i++) {
			if(trainTokensList.get(i).get(2).equals(Integer.toString(classID))) {
				ArrayList<String> element = new ArrayList<String>();
				element.add(trainTokensList.get(i).get(0));
				element.add(trainTokensList.get(i).get(2));
				list.add(element);
			}
		}	
//		System.out.println("ClassID相同者：" + list);
		// terms 在 class 中出現次數（term & 次數）
		ArrayList<ArrayList<String>> term_f = new ArrayList<ArrayList<String>>();
		ArrayList<String> element1 = new ArrayList<String>();
		element1.add(list.get(0).get(0));
		element1.add("1");
		term_f.add(element1);
		for(int i = 1; i < list.size(); i++) {
			int pre = i-1;
			ArrayList<String> element = new ArrayList<String>();
			if(!list.get(i).equals(list.get(pre))) {
				element.add(list.get(i).get(0));
				element.add("1");
				term_f.add(element);
			} else{
				int count = Integer.parseInt(term_f.get(term_f.size()-1).get(1));
				count++;
				term_f.get(term_f.size()-1).set(1, Integer.toString(count));
			}							
		}
		return term_f;
	}
	
	
	/* Feature Selection */
	
	/**
	 * (4) 建立字典
	 */
	public static void feature() {
		// 待刪
		ArrayList<String> same = new ArrayList<String>();
		
		// term
		ArrayList<String> dictionary520 = new ArrayList<String>();
		for (int i = 1; i <= classNum; i++) {
			for (int j = 0; j < LLR(i).size(); j++) {
				dictionary520.add(LLR(i).get(j));				
			}
		}
		System.out.println(dictionary520);
		Collections.sort(dictionary520);
    	dictionary.add(dictionary520.get(0));
		for(int i = 1; i < dictionary520.size(); i++) {
			int pre = i-1;
			if(!dictionary520.get(i).equals(dictionary520.get(pre))) {
				dictionary.add(dictionary520.get(i));
			} 
			// 待刪
			else {
				same.add(dictionary520.get(i));
			}
		}
		// 待刪
		System.out.println("Same: " + same);

	}
	
	/**
	 * (3) LLR for each class (term, LLR)
	 * @param classID
	 * @return
	 */
	public static ArrayList<String> LLR(int classID) {
		ArrayList<ArrayList<String>> term_LLR = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < n11Lists.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			ArrayList<String> n11List = n11Lists.get(i);
			element.add(n11List.get(0));
			double n11 = Integer.parseInt(n11List.get(classID))+1.0;
			double df = Integer.parseInt(irtm_dict.get(i).get(2))+2.0;
			double pt = df/(trainDocNum + 4);
			double n10 = (15.0 + 2.0) - n11;
			double n01 = df - n11;
			double n00 = (trainDocNum + 4) - (15+2) - n01;
			double p1 = (n11/(n11+n10));
			double p2 = (n01/(n01+n00));
			double LLR = -2*((n11+n01)*Math.log(pt) + (n10+n00)*Math.log((1-pt)) - n11*Math.log(p1) - n10*Math.log(1-p1) - n01*Math.log(p2) -n00*Math.log(1-p2)*1.0);
			LLR =  Math.round(LLR*100.0)/100.0;
			element.add(Double.toString(LLR));
			term_LLR.add(element);	
		}
		// 按照 LLR 大小排序
    	Collections.sort(term_LLR, new Comparator<ArrayList<String>>() {    
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                return -o1.get(1).compareTo(o2.get(1));
            }
        });
    	// top 40 term
		ArrayList<String> top40LLR = new ArrayList<String>();
//		for (int i = 0; i < 42; i++) {// 取前40個
//			top40LLR.add(term_LLR.get(i).get(0));
//		}
		for (int i = 0; i < 42; i++) {// 取前40個
			ArrayList<String> element = term_LLR.get(i);
			if (Double.parseDouble(element.get(1)) > 5) {
				top40LLR.add(element.get(0));
			}
		}
		return top40LLR;
	}
	
	/**
	 * (2) 把各個class的n11併成 n11Lists (term & class1's n11 &...& class13's n11)
	 */
	public static void createN11Lists() {
		for (int i = 1; i <= classNum; i++) {
			ArrayList<ArrayList<String>> list = n11(Integer.toString(i));
			for (int j = 0; j < list.size(); j++) {
				if (i == 1) {
					ArrayList<String> element = new ArrayList<String>();
					element.add(list.get(j).get(0));
					element.add(list.get(j).get(1));
					for (int k = 1; k <= classNum-1; k++) {
						element.add("0");
					}
					n11Lists.add(element);					
				} else {
					n11Lists.get(j).set(i, list.get(j).get(1));
				}				
			}			
		}
	}	
	
	/**
	 * (1) n11 for each class (term & n11)
	 * @param classID
	 * @return
	 */
	public static ArrayList<ArrayList<String>> n11(String classID) {
		// terms & docID & classID for each class
		ArrayList<ArrayList<String>> tokensList = new ArrayList<ArrayList<String>>(); 
		for (int i = 0; i < mergedTrainTokensList.size(); i++) {
			if (mergedTrainTokensList.get(i).get(2).equals(classID)) {
				tokensList.add(mergedTrainTokensList.get(i));
			}
		}
		// terms & df for each class (n11)
		ArrayList<ArrayList<String>> df = new ArrayList<ArrayList<String>>();
		ArrayList<String> firstEle = new ArrayList<String>();
		firstEle.add(tokensList.get(0).get(0));
		firstEle.add("1");
		df.add(firstEle);
		for(int i = 1; i < tokensList.size(); i++) {
			int pre = i-1;
			ArrayList<String> element = new ArrayList<String>();			
			if(!tokensList.get(i).get(0).equals(tokensList.get(pre).get(0))) {
				element.add(tokensList.get(i).get(0));
				element.add("1");
				df.add(element);
			} else {
				int count = Integer.parseInt(df.get(df.size()-1).get(1));
				count++;
				df.get(df.size()-1).set(1, Integer.toString(count));
			}
		}
		// term & n11
		ArrayList<ArrayList<String>> n11 = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < irtm_dict.size(); i++) {
				ArrayList<String> element = new ArrayList<String>();
				element.add(irtm_dict.get(i).get(1));
				element.add("0");
				n11.add(element);
		}
		for (int i = 0; i < n11.size(); i++) {
			for (int j = 0; j < df.size(); j++) {
				if (n11.get(i).get(0).equals(df.get(j).get(0))) {
					n11.get(i).set(1, df.get(j).get(1));
				}
			}
		}		
		return n11;
	}
	

	/* 前置工作 */
	
	/**
	 * Create trainTokensList
	 */
	public static void trainingDoc() throws Exception {
		// terms & docID & classID
		trainTokensList = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < trainingList.size(); i++) {
			for (int j = 1; j < trainingList.get(i).size(); j++) {
				String docID = trainingList.get(i).get(j);
				doctTokens(Integer.parseInt(docID), trainingList.get(i).get(0), trainTokensList);				
			}
		}
    	Collections.sort(trainTokensList, new Comparator<ArrayList<String>>() {    
            @Override
            public int compare(ArrayList<String> o1, ArrayList<String> o2) {
                return o1.get(0).compareTo(o2.get(0));
            }
        });
    	// merge same term from the same document
		mergedTrainTokensList = new ArrayList<ArrayList<String>>();
		mergedTrainTokensList.add(trainTokensList.get(0));
		for(int i = 1; i < trainTokensList.size(); i++) {
			int pre = i-1;
			if(!trainTokensList.get(i).equals(trainTokensList.get(pre))) {
				mergedTrainTokensList.add(trainTokensList.get(i));
			}
		}
		// t_index & term & df
		irtm_dict = new ArrayList<ArrayList<String>>();
		int t_index = 1;
		ArrayList<String> firstEle = new ArrayList<String>();
		firstEle.add(Integer.toString(t_index));
		firstEle.add(mergedTrainTokensList.get(0).get(0));
		firstEle.add("1");
		irtm_dict.add(firstEle);
		for(int i = 1; i < mergedTrainTokensList.size(); i++) {
			int pre = i-1;
			ArrayList<String> element = new ArrayList<String>();			
			if(!mergedTrainTokensList.get(i).get(0).equals(mergedTrainTokensList.get(pre).get(0))) {
				t_index++;
				element.add(Integer.toString(t_index));
				element.add(mergedTrainTokensList.get(i).get(0));
				element.add("1");
				irtm_dict.add(element);
			} else {
				int count = Integer.parseInt(irtm_dict.get(irtm_dict.size()-1).get(2));
				count++;
				irtm_dict.get(irtm_dict.size()-1).set(2, Integer.toString(count));
			}
		}
	}
	
	
	/**
	 * Method doctTokens(): Tokens from single document
	 * @param docID
	 * @param classID
	 * @param tokenList
	 * @throws Exception
	 */
	public static void doctTokens(int docID, String classID, ArrayList<ArrayList<String>> tokenList) throws Exception {
    	// Read document
    	String pathname = String.format("src/input/%d.txt", docID);
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
    	stopwords += ", know, ye, yet, alex, john, lisa";
//    	stopwords += "janet, jame";
    	stopwords += ", hous, kill, offic";
//    	stopwords += ", nation, vladimir";

    	for(int i = 0; i < stemToken.length; i++) {
    		// 新增手動
    		stemToken[i] = stemToken[i].replace("knew", "know");
    		stemToken[i] = stemToken[i].replace("known", "know");
    		stemToken[i] = stemToken[i].replace("laboratori", "labor");
    		stemToken[i] = stemToken[i].replace("larger", "large");
    		stemToken[i] = stemToken[i].replace("largest", "large");
    		stemToken[i] = stemToken[i].replace("children", "child");
    		stemToken[i] = stemToken[i].replace("came", "come");
    		stemToken[i] = stemToken[i].replace("held", "hold");
    		stemToken[i] = stemToken[i].replace("prove", "proof");
    		stemToken[i] = stemToken[i].replace("smaller", "small");
    		stemToken[i] = stemToken[i].replace("taken", "take");
    		stemToken[i] = stemToken[i].replace("took", "take");
    		stemToken[i] = stemToken[i].replace("unknown", "unknow");
    		stemToken[i] = stemToken[i].replace("suspens", "suspend");


    		// stopwords
    		if(!stopwords.contains(stemToken[i])) {
    			ArrayList<String> element = new ArrayList<String>();
    			element.add(stemToken[i]);
    			element.add(Integer.toString(docID));
    			element.add(classID);
    			tokenList.add(element);
    		}
    	}     		
	}
		
	/**
	 * Read src/input/training.txt
	 * @return an ArrayList
	 * @throws Exception
	 */
	public static ArrayList<String> readTrainingFile() throws Exception {	
		ArrayList<String> a = new ArrayList<String>();
		String pathname = "src/input/training.txt";
    	File filename = new File(pathname);
    	InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
    	BufferedReader br = new BufferedReader(reader);
    	String line = br.readLine();
    	a.add(line);
    	while (line != null) {
    		line = br.readLine();
    		if (line != null) {
        		a.add(line);
    		} else {
    	    	br.close();
    		}
    	}
    	return a;		
	}	
	
	/**
	 * Create trainingList from training.txt
	 * @param a
	 */
	public static void createtrainingList(ArrayList<String> a) {	
		char split = ' ';
		trainingList = new ArrayList<ArrayList<String>>();
		for(int i = 0; i < a.size(); i++) {
			ArrayList<String> element = new ArrayList<String>();
			/* 找出 splitter 的 index */
			ArrayList<Integer> splitIndex = new ArrayList<Integer>();
			String str = a.get(i);
			for(int j = 0; j < str.length(); j++) {
				if(str.charAt(j) == split) {
					splitIndex.add(j);
				}
			}		
			/* 找出每個詞的開頭 index */
			ArrayList<Integer> starIndex = new ArrayList<Integer>();
			starIndex.add(0);
			for(int j = 0; j < splitIndex.size()-1; j++) {
				int star = splitIndex.get(j) + 1;
				starIndex.add(star);
			}
			/* 找出每個詞的結尾 index */
			ArrayList<Integer> endIndex = new ArrayList<Integer>();
			for(int j = 0; j < splitIndex.size(); j++) {
				int end = splitIndex.get(j) - 1;
				endIndex.add(end);
			}
			/* create trainingList element */
			for (int j = 0; j < starIndex.size(); j++) {
				int star = starIndex.get(j);
				int end = endIndex.get(j);
				String id = "";
				for (int k = star; k <= end; k++) {
					id += str.charAt(k) ;
				}
				element.add(id);
			}
			trainingList.add(element);			
		}
		/* check trainingList */
		System.out.println("trainingList: ");		
		for (int i = 0; i < trainingList.size(); i++) {
			System.out.println(trainingList.get(i));			
		}
	}
	
	/**
	 * Write to csv
	 * @param n
	 * @throws Exception
	 */
	public static void write(ArrayList<ArrayList<String>> result) throws Exception {
		CSVWriter writer = new CSVWriter(new FileWriter("src/output/sub.csv"), ','); 
		for (int i = 0; i < result.size(); i++) {
			ArrayList<String> list = result.get(i);
			String[] line = {list.get(0),list.get(1)};
			writer.writeNext(line);
		}
		writer.close();
	}
}
