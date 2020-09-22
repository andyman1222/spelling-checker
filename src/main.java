import javax.management.BadStringOperationException;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.UnsupportedCharsetException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Character.isLetter;

public class main implements NativeKeyListener {

    static wordContainer container = new wordContainer(System.getProperty("user.dir") + "\\src\\words.txt");

    private static String c = "";
    private static String allWords = "";
    private static char in = '\0';
    private static boolean suggest = false;


    public static void main(String[] args) throws IOException{
        try {

            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);

            logger.setUseParentHandlers(false);
            GlobalScreen.registerNativeHook();
        }
        catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());

            System.exit(1);
        }

        GlobalScreen.addNativeKeyListener(new main());

        if(args.length > 0 && args[0].indexOf("r") > -1) {
            suggest = true;
            System.out.println("Enter in characters to get word recommendations. Sentences work. A-Z or a-z only (no other characters).");
            while (true) ;
        }
        else
            while(true){
                System.out.println("Enter in words to spell check. Sentences work. A-Z or a-z only (no other characters).");
                Scanner in = new Scanner(System.in);
                String i = in.nextLine().toLowerCase().replaceAll("[^A-Za-z]+", " ");
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

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        ArrayList<Integer> letters = new ArrayList<>();
        int[] l = {NativeKeyEvent.VC_A, NativeKeyEvent.VC_B, NativeKeyEvent.VC_C, NativeKeyEvent.VC_D, NativeKeyEvent.VC_E, NativeKeyEvent.VC_F, NativeKeyEvent.VC_G, NativeKeyEvent.VC_H, NativeKeyEvent.VC_I, NativeKeyEvent.VC_J, NativeKeyEvent.VC_K, NativeKeyEvent.VC_L, NativeKeyEvent.VC_M, NativeKeyEvent.VC_N, NativeKeyEvent.VC_O, NativeKeyEvent.VC_P, NativeKeyEvent.VC_Q, NativeKeyEvent.VC_R, NativeKeyEvent.VC_S, NativeKeyEvent.VC_T, NativeKeyEvent.VC_U, NativeKeyEvent.VC_V, NativeKeyEvent.VC_W, NativeKeyEvent.VC_X, NativeKeyEvent.VC_Y, NativeKeyEvent.VC_Z};

        for(int g : l){
            letters.add(g);
        }
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
                GlobalScreen.unregisterNativeHook();
                System.exit(0);
            } catch (NativeHookException ex) {
                ex.printStackTrace();
            }
        }
        //in = e.getKeyText(e.getKeyCode()).toLowerCase().charAt(0);
        if(suggest) {
            //System.out.println("KeyPress " + e.getKeyText(e.getKeyCode()));
            if(!e.isActionKey() && letters.indexOf(e.getKeyCode()) > -1) {
                c += e.getKeyText(e.getKeyCode()).toLowerCase().charAt(0);
                allWords += e.getKeyText(e.getKeyCode()).toLowerCase().charAt(0);
                String[] s = container.getSuggestions(c.toLowerCase().replaceAll("[^A-Za-z]+", " "));
                if(s != null && s.length > 0)
                {
                    System.out.print("\nSuggestions: ");
                    for(int j = 0; j < s.length && j < 5; j++)
                        System.out.print(" " + s[j] + " ");
                } else System.out.print("\nNo suggestions found!!!");
                System.out.print("\n\n"+c);
            }
            else if (e.getKeyCode() == NativeKeyEvent.VC_ENTER || e.getKeyCode() == NativeKeyEvent.VC_SPACE || e.getKeyCode() == NativeKeyEvent.VC_TAB) {
                if (e.getKeyCode() == NativeKeyEvent.VC_SPACE){
                    c += " ";
                    allWords += " ";
                }
                String[] words = c.toLowerCase().replaceAll("[^A-Za-z]+", " ").split(" ");
                for (String w : words) {
                    try {
                        container.wordSearch(w);
                    } catch (BadStringOperationException b) {
                        b.printStackTrace();
                    }
                }
                if (e.getKeyCode() == NativeKeyEvent.VC_ENTER) {
                    System.out.println(allWords + "\nEnter in characters to get word recommendations. Sentences work. A-Z or a-z only (no other characters).");
                }
                c = "";

            } else if(!e.isActionKey()){
                allWords += e.getKeyText(e.getKeyCode()).toLowerCase().charAt(0);
                String[] words = c.toLowerCase().replaceAll("[^A-Za-z]+", " ").split(" ");
                for(String w : words) {
                    try {
                        container.wordSearch(w);
                    } catch (BadStringOperationException b) {
                        b.printStackTrace();
                    }
                }
                c = "";
            }
            in = '\0';
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {

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

    public String[] getSuggestions(String s) {
        Node n = root;
        Node prevNode = null;
        int depth = 0;
        try {

            for (char c : s.toCharArray()) {
                depth++;
                prevNode = n;
                n = n.getNode(c);
                if (n == null) {
                    //System.out.println("Unknown word " + s);
                    //System.out.println("Did you mean: " + findSuggestion(new Node(prevNode, prevNode.depth+1), s, depth) + "?");
                    break;
                }
            }
            if (prevNode == null || n == null || n.getChar() == '\0') return null;
            /*if (n != null && !n.isEnd()) {
                System.out.println("Unknown word " + s);
                System.out.println("Did you mean: " + findSuggestion(n, s, depth) + "?");
            }
             */
        } catch (Exception e){
            e.printStackTrace();
        }


        //for the remaining chars in s:
        //1. search in all next nodes
        //2. save number of instances where a char exists and is in remaining part of string
        //3. go to next node. repeat until remaining chars are taken care of. Then select first char with the highest instance count to end of word.
        HashMap<Node, Integer> priority = new HashMap<Node, Integer>();

        boolean reverseCheck = true;

        Node tempParent = n.getParentNode();

        //System.out.println(n);
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
        /*while(reverseCheck){
            for(int i = 0; i < 26; i++){
                if(n != null || n.getNodeI(i) != null){
                    reverseCheck = false;
                    break;
                }
            }
            n = n.getParentNode();
            //priorityI--;
        }*/
        //given a parent node, return all child nodes that are ends, along with their weights
        priority.putAll(getChildEnds(n, priorityI, n.depth, 0, s));
        if(priority.isEmpty()) return null;

        String[] r = new String[priority.size()];
        ArrayList<Map.Entry<Node, Integer>> L = new ArrayList<>(priority.entrySet());
        L.sort(Map.Entry.comparingByValue());
        //Map.Entry<Node, Integer>[] ns = (Map.Entry<Node, Integer>[])L.toArray();
        for(int i = L.size()-1; i >= 0; i--){
            r[L.size()-1-i] = L.get(i).getKey().toString();
        }
        return r;
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
        int wordCount = 0;

        public Node(char c, Node parentNode, int depth, boolean isEnd){
            this.c = c;
            this.parentNode = parentNode;
            this.depth = depth;
            this.isEnd = isEnd;
            Node temp = parentNode;
            while(temp != null && temp.getChar() != '\0'){
                temp.wordCount++;
                temp = temp.parentNode;
            }
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