import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class pa1 {
    public static void main(String[] args) throws Exception {

    	// Read document
    	String pathname = "src/document/28.txt";
    	File filename = new File(pathname);
    	InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
    	BufferedReader br = new BufferedReader(reader);
    	String document = br.readLine();
    	br.close();
    	
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
    	
    	// 自定義 stop word list
    	String stopwords = "and,the,to,are,of,in,on,with,that,for";
    	String result = "";
    	for(int i = 0; i < stemToken.length; i++) {
    		if(!stopwords.contains(stemToken[i])) {
    			result = result + stemToken[i] + "\n";
    		}
    	}
    	// check output
		System.out.println(result);
    	
    	// Write document
    	File writepath = new File("src/document/result.txt");
    	writepath.createNewFile();
    	BufferedWriter bw = new BufferedWriter(new FileWriter(writepath));
    	bw.write(result);
    	bw.flush();
    	bw.close();
    }
}