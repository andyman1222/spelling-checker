import javax.management.BadStringOperationException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.UnsupportedCharsetException;
import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

public class main {

    static wordContainer container = new wordContainer("C:\\Users\\herberac\\IdeaProjects\\spellchecker\\src\\words.txt");

    public static void main(String[] args){
        while(true){
            System.out.println("Enter in words to spell check. Sentences work. A-Z or a-z only (no other characters).");
            Scanner in = new Scanner(System.in);
            String i = in.nextLine().toLowerCase().replaceAll("[^A-Za-z]+", " ");;
            String[] words = i.split(" ");
            for(String w : words) {
                try {
                    container.wordSearch(w);
                } catch (BadStringOperationException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class wordContainer {

    BufferedReader s;
    Node root = new Node('\0', null, 0, false);

    public wordContainer(String wordList){
        try {
            s = new BufferedReader(new FileReader(wordList));
        } catch(FileNotFoundException e) {
            System.out.println("File read error!");
            e.printStackTrace();
        }
        String a = "";
        do{
            try {
                a = s.readLine();
                Node temp = root;
                if(a != null)
                    for(int i = 0; i < a.toCharArray().length; i++){
                        if(a.toCharArray()[i] != '\n'){
                            char c = a.toCharArray()[i];
                            temp = temp.addNode(c, (i == a.toCharArray().length-1)?true:false);
                        }
                    }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while(a != null);
    }

    public void wordSearch(String s) throws BadStringOperationException {
        try {
            Node temp = root;
            Node prevNode = null;
            int depth = 0;
            for (char c : s.toCharArray()) {
                depth++;
                prevNode = temp;
                temp = temp.getNode(c);
                if (temp == null) {
                    System.out.println("Unknown word " + s);
                    System.out.println("Did you mean: " + findSuggestion(new Node(prevNode, prevNode.depth+1), s, depth) + "?");
                    break;
                }
            }
            if (prevNode == null) return;
            if (temp != null && !temp.isEnd()) {
                System.out.println("Unknown word " + s);
                System.out.println("Did you mean: " + findSuggestion(temp, s, depth) + "?");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String findSuggestion(Node n, String s, int depth) throws Exception {
        //for the remaining chars in s:
        //1. search in all next nodes
        //2. save number of instances where a char exists and is in remaining part of string
        //3. go to next node. repeat until remaining chars are taken care of. Then select first char with the highest instance count to end of word.
        HashMap<Node, Integer> priority = new HashMap<Node, Integer>();

        boolean reverseCheck = true;

        Node temp = n;
        Node tempParent = n.getParentNode();
        System.out.println(n);
        //check if entered word is made up of any legit words. If so, get closest word from the end to beginning, starting from char a to z.
        int priorityI = 0;
        while(reverseCheck && tempParent.getChar()!='\0'){
            for(int i = 0; i < 26; i++){
                Node temp2 = tempParent.getNodeI(i);
                if(temp2 != null && temp2.isEnd()){
                    priority.put(temp2, priorityI);
                    reverseCheck = false;
                    break;
                }
            }
            tempParent = tempParent.getParentNode();
            priorityI++;
        }

        //check for last known char combination with legit words, then search thru its tree, finding words that match the incorrect word the best in terms of char count.
        priorityI = 0;
        reverseCheck = true;
        while(reverseCheck){
            for(int i = 0; i < 26; i++){
                if(temp != null || temp.getNodeI(i) != null){
                    reverseCheck = false;
                    break;
                }
            }
            temp = temp.getParentNode();
            //priorityI--;
        }
        //given a parent node, return all child nodes that are ends, along with their weights
        priority.putAll(getChildEnds(n, priorityI, n.depth, 0, s));
        if(priority.isEmpty()) throw new Exception("No matching words found!");
        Node bestNode = null;
        for(Node tmp : priority.keySet()) {
            //System.out.println("" + tmp.toString() + " weight: " + priority.get(tmp));
            if (bestNode == null) bestNode = tmp;
            if (priority.get(tmp) > priority.get(bestNode)) bestNode = tmp;
            else if(priority.get(tmp) == priority.get(bestNode)){
                int base = 0, tmpL = 0, bestL = 0;
                for(int i = 0; i < s.length(); i++) base += i;
                for(int i = 0; i < tmp.depth; i++) tmpL += s.indexOf(tmp.toString().charAt(i), i);
                for(int i = 0; i < bestNode.depth; i++) bestL += s.indexOf(bestNode.toString().charAt(i), i);
                if(Math.abs(base-tmpL)<Math.abs(base-bestL)) bestNode = tmp;
                if (Math.abs(s.length() - tmp.depth) < (s.length() - bestNode.depth))
                    bestNode = tmp;
            }
        }
        return bestNode.toString();
    }

    private HashMap<Node, Integer> getChildEnds(Node n, int depth, int initPos, int badCharCount, String s){
        HashMap<Node, Integer> temp = new HashMap<>();
        for(int i = 0; i < 26; i++){
            badCharCount = (s.indexOf(n.getChar()) == -1?-depth:0) + badCharCount;
            int depthAdd = (s.indexOf(n.getChar()) == -1?0:Math.abs((n.depth-s.indexOf(n.getChar()))));
            //System.out.println("" + n.toString() + " depthAdd to " + s + ": " + depthAdd);
            int weight = ((depth <= s.length()-initPos)?depth:(s.length()-initPos)-depth) + badCharCount - depthAdd;
            if(s.indexOf(n.toString())>-1) weight+=n.toString().length()+s.length();
            if(n.getNodeI(i) != null && n.getNodeI(i).isEnd()) temp.put(n.getNodeI(i), weight);
            else if(n.getNodeI(i) != null){
                //s = s.replace("" + n.getNodeI(i).getChar(), "");
                HashMap<Node, Integer> temp2 = getChildEnds(n.getNodeI(i), depth+1, initPos, badCharCount, s.replace("" + n.getNodeI(i), ""));
                if(!temp2.isEmpty()) temp.putAll(temp2);
            }
        }
        return temp;
    }

    private class Node{
        private Node[] nodes = new Node[26];
        private Node parentNode;
        private char c = '\0';
        private boolean isEnd = false;
        private int depth = 0;

        public Node(char c, Node parentNode, int depth, boolean isEnd){
            this.c = c;
            this.parentNode = parentNode;
            this.depth = depth;
            this.isEnd = isEnd;
        }

        public Node(Node parentNode, int depth){
            this.parentNode = parentNode;
            this.depth = depth;
        }

        public Node addNode(char node, boolean isEnd){
            //if(node <= 'Z' && node >= 'A') node = (char)(node + ('a'-'A'));
            if(node < 'a' || node > 'z') throw new UnsupportedCharsetException("cannot use char " + node);
            if(nodes[(int)(node-'a')] == null) nodes[(int)(node-'a')] = new Node(node, this, depth+1, isEnd);
            return nodes[(int)(node-'a')];
        }

        public Node getNode(char node){
            return nodes[(int)(node-'a')];
        }

        public Node getNodeI(int i){
            return nodes[i];
        }

        public Node getParentNode(){return parentNode;}

        public boolean isEnd(){ return isEnd;}

        public char getChar(){return c;}

        public String toString(){
            if(c != '\0')
            return parentNode.toString() + c;
            else return "";
        }
    }
}