package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;


public class GenerateQrelsGPT {
    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Usage: no LLM model was provided");
            return;
        }
        String llm = args[0];
        OpenAiChatModel openAImodel = null;
        ChatLanguageModel ollamaModel = null;
        String apiKey = "";

        if(llm.equalsIgnoreCase("GPT")){
            openAImodel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-5.6-luna")
                .build();
        }else if (llm.equalsIgnoreCase("Deepseek")){
            openAImodel = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
                .modelName("deepseek-v4-flash")
                .build();
        } else if (llm.equalsIgnoreCase("Gemini")){
            openAImodel = OpenAiChatModel.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .apiKey(apiKey)
                .modelName("google/gemini-3.7-flash")
                .build();
        } else if (llm.equalsIgnoreCase("Llama")){
            ollamaModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2")                
                .build();
        }else{            
            System.out.println("Usage: no LLM model was provided");
            return;
        }

        String rel2015 = "C:\\Users\\malsu\\Documents\\Code\\lucenesearch\\resources\\qrels-treceval-2015.txt";   
        BufferedReader reader = null;
        HashMap<String, HashMap<String, String>> relMap = new HashMap<>();
        HashSet<String> docs = new HashSet<>();
        try {
            reader = new BufferedReader(new FileReader(rel2015));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String queryId = parts[0];
                    String docId = parts[2];
                    String relevance = parts[3];
                    relMap.computeIfAbsent(queryId, k -> new HashMap<>()).put(docId, relevance);
                    docs.add(docId+".nxml");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
                   
        BufferedWriter writer = null;
 
        try{

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File("topics2015B.xml"));
 
            document.getDocumentElement().normalize();

            NodeList list = document.getElementsByTagName("topic");

            String[] qnos = new String[20];
            String[] queries = new String[20];
            String[] diagnoses = new String[20];
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    
                    qnos[i] = element.getAttribute("number");
                    
                    queries[i] = element.getElementsByTagName("description").item(0).getTextContent();
                    diagnoses[i] = element.getElementsByTagName("diagnosis").item(0).getTextContent();   
                }
            }

            writer = new BufferedWriter(new FileWriter("output.txt", true));    //21 luna    
            for(int i = 0; i<qnos.length; i++){ 

            
                String queryNo = qnos[i];
                String queryText = queries[i];
                String diagnosis = diagnoses[i];
            
                HashMap<String,String> docNotoPathHashMap = getDocNotoPathHashMap(docs);
                System.out.println("Document Path Map size:" + relMap.get(queryNo).keySet().size());
                for(String docId : relMap.get(queryNo).keySet()) {
                    String relevance = relMap.get(queryNo).get(docId);
                    String docPath = docNotoPathHashMap.get(docId+".nxml");
                    String docText = extractTextFromNXML(docPath);
                    String prompt = "Consider the following clinical case:\n clinical case:\n";
                    prompt+= queryText +"\n" + "differential diagnosis for this clinical case includes: "+ diagnosis+"\n";
                    prompt+= "Now, evaluate the relevance of the following article as evidence for supporting or establishing the given diagnosis for this specific clinical case.\n";
                    prompt+= "Use the following relevance scale:\n";
                    prompt+= "0, Not relevant: The article does not provide useful evidence for diagnosing the clinical case or is unrelated to the diagnosis.\n";
                    prompt+= "1, Potentially Relevant: The article provides evidence, information, findings, diagnostic criteria, differential diagnosis information, or clinical knowledge that is potentially relevant to the given diagnosis and could support the diagnosis, testing, or treatments .\n";
                    prompt+= "2, Definitely Relevant: The article provides direct, specific, and strong evidence for the given diagnosis in the context of the clinical case. It closely matches the patient's condition, diagnostic features, or the clinical reasoning required to establish the diagnosis, testing, or treatments.\n";
                    prompt+= "Judge relevance with respect to the clinical case and the given diagnosis, not merely whether the article mentions the diagnosis.\n";
                    prompt+= "Please provide your response as a single number (0, 1, or 2) without any additional explanation or text.\n";
                    prompt+= "Article:\n" + docText + "\n";

                    String response = "";
                    if(llm.equalsIgnoreCase("Llama")){
                        response = ollamaModel.generate(prompt);  
                    }else{
                        response = OpenAiChatModel.chat(prompt);           
                    }
                
                    System.out.println(response+"R"+relevance);
                               
                    writer.write(queryNo + " " + docId + " " + relevance + " " + response+"\n"); 
                    writer.flush();
            
                }
        
            }
        } catch(IOException | ParserConfigurationException | SAXException e){}
    }
    public static String extractTextFromNXML(String filePath) {
        String textContent = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                 if (line.contains("<article-title>")) {
                        textContent = line.substring(line.lastIndexOf("<article-title>") + 15, line.lastIndexOf("</article-title>"))+"\n\n";
                    }
                     if (line.contains("<abstract>")) {
                        String abstractText = line.substring(line.lastIndexOf("<abstract>") + 10, line.lastIndexOf("</abstract>"))+"\n";
                        textContent += abstractText;
                        if(abstractText.trim().length() == 0) {
                            textContent += line.substring(line.indexOf("<body>") + 6, line.indexOf("</body>")).substring(0, 2000)+"\n";
                        }
                    }
                    // now remove all the tags from the articleTitle, articleAbstract and articleBody and put space in place of the tags
            
                    textContent = textContent.replaceAll("<[^>]*>", " ");              

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return textContent;
    }
    public static HashMap<String,String> getDocNotoPathHashMap(HashSet<String> docs) {
        HashMap<String,String> docPathMap = new HashMap<>();
 
        String paths[] = new String[4];
        paths[0] = "C:\\Users\\malsu\\Documents\\Col\\d1";       
        paths[1] = "C:\\Users\\malsu\\Documents\\Col\\d2";         
        paths[2] = "C:\\Users\\malsu\\Documents\\Col\\d3";         
        paths[3] = "C:\\Users\\malsu\\Documents\\Col\\d4"; 
        
        for (String path : paths) {
            File dirs = new File(path);
            for (File directory : dirs.listFiles()) {
                for(File file : directory.listFiles()) {
                    //System.out.println(file.getName());
                    if (docs.contains(file.getName())) {
                        String filePath = file.getAbsolutePath();
                        docPathMap.put(file.getName(), filePath);
                        //System.out.println("file "+file.getName()+" found at path: " + filePath);
                    }
                }
            }
        }
        
        return docPathMap;
    }

}
