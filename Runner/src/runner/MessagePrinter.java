/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

import java.util.List;
import runner.ProgramException.ExceptionType;

/**
 *
 * @author dato
 */
public class MessagePrinter {
    
    public void printError(String message){
        System.err.println(message);
    }
    
    public void printInfo(String message){
        System.out.println(message);
    }

    public void printLists(List<String> realOutput, List<String> userOutput) {
        if (realOutput.size() > userOutput.size()){
            printListsEntries(realOutput, userOutput);
        }
        else {
            printListsEntries(userOutput, realOutput);
        }
    }
    
    private void printListsEntries(List<String> bigList, List<String> smallList){
        for (int i = 0; i < smallList.size(); i++) {
            String bigListElem = bigList.get(i);
            String smallListElem = smallList.get(i);
            printInfo(bigListElem + "\t\t\t" + smallListElem);
        }
        for (int i = smallList.size(); i < bigList.size(); i++) {
            printInfo(bigList.get(i));
        }
    }
    
    public void notifyResult(String testName, ExceptionType exType, String message){
        System.out.println("Test: " + testName);
        System.out.println("Error: " + exType.toString());
        System.out.println("Message: " + message);
        System.out.println(""); // print newLine
    }
}
