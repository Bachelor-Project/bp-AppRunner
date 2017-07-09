/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dato
 */
public class RunProcess {
    
    
    public RunProcess(){
        
    }
    
    public void run(String[] command){
        try {
            Process p = Runtime.getRuntime().exec(command);
            
            int exitValue = p.waitFor();
            System.out.println("exitValue: " + exitValue);

            printErrorStreamData(p);
            printInputStreamData(p);
            
        } catch (InterruptedException | IOException ex) {
            Logger.getLogger(RunProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void printErrorStreamData(Process p){
        try {
            InputStream errorStream = p.getErrorStream();
            int errorBytes = errorStream.available();
            System.out.println("errorBytes: " + errorBytes);
            
            printInputStream(errorStream, "ErrorStream");
        } catch (IOException ex) {
            Logger.getLogger(RunProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void printInputStreamData(Process p){
        try {
            InputStream inputStream = p.getInputStream();
            int inputBytes = inputStream.available();
            System.out.println("inputBytes: " + inputBytes);
            
            printInputStream(inputStream, "InputStream");
        } catch (IOException ex) {
            Logger.getLogger(RunProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void printInputStream(InputStream in, String streamType){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = reader.readLine()) != null){
                System.out.println(streamType + " line: " + line);
            }
        } catch (IOException ex) {
            Logger.getLogger(RunProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
