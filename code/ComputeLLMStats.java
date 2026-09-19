package com.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ComputeLLMStats {
    public static void main(String[] args) {
        BufferedReader reader = null;
        BufferedWriter writer = null;

        String inputfilePath = "LLMResFile.txt";
        String writeFile = "LLMSumm.csv";
        int datasetSize = 0;
        
        int count0 = 0;
        int count1 = 0;
        int sumCount0 = 0;
        int sumCount1 = 0;

        int countCorrect0 = 0;
        int countCorrect1 = 0;

        int sumCorrect0 = 0;
        int sumCorrect1 = 0;

        int nn = 0;
        int nr = 0;
        int rr = 0;
        int rn = 0;
        
        int nnAll = 0;
        int nrAll = 0; 
        int rrAll = 0;
        int rnAll = 0;
        int itrs = 1;

        double sumOverlap = 0;
        double sumKappa = 0;
        double sumPrecision = 0;
        double sumRecall = 0;
        double sumAccuracy = 0;

        try {
            reader = new BufferedReader(new FileReader(inputfilePath));
            writer = new BufferedWriter(new FileWriter(writeFile));

            writer.write("Query,NN,NR,RN,RR,Overlap,Kappa,accuracy,precision,recall\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
        String line;
        String lastQuery = "11";
        try {
            while ((line = reader.readLine()) != null) {
                
                String[] parts = line.split("\\s+");
                String query = parts[0];
                String relevance = parts[2];
                String response = parts[3];
                if(!query.equals(lastQuery)){
                    nnAll += nn;
                    nrAll += nr;
                    rnAll += rn;
                    rrAll += rr;

                    double overlap = rr/(double)(rn+nr+rr);
                    sumOverlap += overlap;
                    double p0 = rr+nn;
                    p0 = p0/(double)(rn+nr+rr+nn);
                    double peRel = (rr+rn)/(double)(rn+rr+nn+nr);
                    peRel = peRel*((rr+nr)/(double)(rn+rr+nn+nr));

                    double peNonRel = (nn+nr)/(double)(rn+rr+nn+nr);
                    peNonRel = peNonRel*((nn+rn)/(double)(rn+rr+nn+nr));
                    double pe = peRel + peNonRel;
                    double kappa = (p0-pe)/(1-pe);
                    sumKappa += kappa;

                    double precision = (double) countCorrect1 / (countCorrect1 + (count0 - countCorrect0));
                    double recall = (double) countCorrect1/count1;
                    sumPrecision+=precision;
                    sumRecall+=recall;

                    double accuracy = countCorrect0 + countCorrect1;
                    accuracy = accuracy/(double)(count0 + count1);
                    sumAccuracy+=accuracy;
                    System.out.println("Query: " + lastQuery + " NN =  " + nn + "  NR = " + nr + "  RN = " + rn + "  RR = " + rr + " Overlap = " + overlap + " Kappa = " + kappa + " accuracy = "+ accuracy+ " precision = "+ precision + " recall = "+ recall);
                    writer.write(lastQuery + "," + nn + "," + nr + "," + rn + "," + rr + "," + overlap + "," + kappa + ","+ accuracy+ ","+ precision + ","+ recall+"\n");
                    lastQuery = query;
                    nn=0;
                    nr=0;
                    rn=0;
                    rr=0;

                    countCorrect0 = 0;
                    countCorrect1 = 0;
                    count0 = 0;
                    count1 = 0;
                    itrs++;
                }   

                datasetSize++;
                if (relevance.equals("0")) {
                    sumCount0++;
                    count0++;
                    if(response.equals("0")) {
                        sumCorrect0++;
                        countCorrect0++;
                        nn++;
                    }else{
                        nr++;
                    }   
                }
                if (relevance.equals("1")||relevance.equals("2")) {
                    sumCount1++;
                    count1++;
                    if(response.equals("1")||response.equals("2"))   {
                        sumCorrect1++;
                        countCorrect1++;
                        rr++;
                    }else{
                        rn++;
                    }  
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double p0 = rr+nn;
        p0 = p0/(double)(rn+nr+rr+nn);
        double peRel = (rr+rn)/(double)(rn+rr+nn+nr);
        peRel = peRel*((rr+nr)/(double)(rn+rr+nn+nr));
        double peNonRel = (nn+nr)/(double)(rn+rr+nn+nr);
        peNonRel = peNonRel*((nn+rn)/(double)(rn+rr+nn+nr));
        double pe = peRel + peNonRel;
        double kappa = (p0-pe)/(1-pe);
        double overlap = rr/(double)(rn+nr+rr);    
        
        double precision = (double) countCorrect1 / (countCorrect1 + (count0 - countCorrect0));
        double recall = (double) countCorrect1/count1;

        sumOverlap += overlap;
        sumKappa += kappa;
        sumPrecision+=precision;
        sumRecall+=recall;

        double accuracy = countCorrect0 + countCorrect1;
        accuracy = accuracy/(double)(count0 + count1);
        sumAccuracy+=accuracy;
        System.out.println("Query: " + lastQuery + " NN =  " + nn + "  NR = " + nr + "  RN = " + rn + "  RR = " + rr + " Overlap = " + overlap + " Kappa = " + kappa + " accuracy = "+ accuracy+ " precision = "+ precision + " recall = "+ recall);
        try {
            writer.write(lastQuery + "," + nn + "," + nr + "," + rn + "," + rr + "," + overlap + "," + kappa + ","+ accuracy+ ","+ precision + ","+ recall+"\n");
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        nn+= nnAll;
        nr+= nrAll; 
        rn+= rnAll;
        rr+= rrAll;
        p0 = 0;
        peRel = 0;
        peNonRel = 0;
        pe = 0;
        kappa = 0;

        p0 = rr+nn;
        p0 = p0/(double)(rn+nr+rr+nn);
        peRel = (rr+rn)/(double)(rn+rr+nn+nr);
        peRel = peRel*((rr+nr)/(double)(rn+rr+nn+nr));
        peNonRel = (nn+nr)/(double)(rn+rr+nn+nr);
        peNonRel = peNonRel*((nn+rn)/(double)(rn+rr+nn+nr));
        pe = peRel + peNonRel;
        kappa = (p0-pe)/(1-pe);
        overlap = rr/(double)(rn+nr+rr);    

        System.out.println("All("+itrs+"), NN =  " + nn + "  NR = " + nr + "  RN = " + rn + "  RR = " + rr + " Overlap = " + overlap + " Kappa = " + kappa);

        System.out.println("\n\nAverage Overlap = " + sumOverlap/(double)itrs);
        System.out.println("Average Kappa = " + sumKappa/(double)itrs);
        System.out.println("Average Precision = " + sumPrecision/(double)itrs);
        System.out.println("Average Recall = " + sumRecall/(double)itrs);
        System.out.println("Average Accuracy = " + sumAccuracy/(double)itrs);

        System.out.println("\nDataset Size: " + datasetSize);
        System.out.println("Count of 0s: " + sumCount0);
        System.out.println("Count of 1s: " + sumCount1);
        System.out.println("Count of Correct 0s: " + sumCorrect0);
        System.out.println("Count of Correct 1s: " + sumCorrect1);

        // compute the accuracy for 0s and 1s
        double accuracy0 = (double) sumCorrect0 / sumCount0;
        double accuracy1 = (double) sumCorrect1 / sumCount1;
        System.out.println("Accuracy for 0s: " + accuracy0);
        System.out.println("Accuracy for 1s: " + accuracy1);    

        // compute the overall accuracy
        double overallAccuracy = (double) (sumCorrect0 + sumCorrect1) / datasetSize;
        System.out.println("Overall Accuracy: " + overallAccuracy);

        // compute the precision, recall, and F1 score for 1s
        double precision1 = (double) sumCorrect1 / (sumCorrect1 + (sumCount0 - sumCorrect0));
        double recall1 = (double) sumCorrect1 / sumCount1;       
        double f1Score1 = 2 * (precision1 * recall1) / (precision1 + recall1);
        System.out.println("Precision for 1s: " + precision1);  
        System.out.println("Recall for 1s: " + recall1);
        System.out.println("F1 Score for 1s: " + f1Score1);

        

    }

}
