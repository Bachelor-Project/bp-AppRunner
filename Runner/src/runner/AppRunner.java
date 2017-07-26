/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dato
 */
public class AppRunner {
    
    private final MessagePrinter printer;

    public AppRunner(MessagePrinter printer) {
        this.printer = printer;
    }
    
    public void run2(String path, String... args){
//        /home/dato/Documents/project/users/Bob "java -Xmx1m user /home/dato/dockerImages/tests 01,02"
        
        String[] tests = args[args.length - 1].split(",");
        for (String test : tests) {
            String tp = args[args.length - 2] + test;
        
            System.out.println(args[0] + " " + args[1] + "  " + args[2] +" " + tp);
            try {
                executeCommandLine(path, 2000, args[0], args[1], args[2], tp);
            } catch (IOException | InterruptedException | ProgramException ex) {
                Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void run(String[] args) {
        // sudo docker run -it --rm -v /home/dato/dockerImages:/test -w /test oracle-java
        // java AppRunner -Xmx1m 2000 java user1 /home/dato/dockerImages/tests 01,02
//-Xmx1m 2000 java /home/dato/Documents/project/users/Bob/user /home/dato/dockerImages/tests 01.in,02.in
// -Xmx1m 2000 java Memory ./codesData/users/Bob/ /home/dato/dockerImages/tests 01,02"

            String memLimit = args[0];              // -Xmx1m
            long timeout = Long.parseLong(args[1]); // 2000 -> 2 seconds

            String languageCommand = args[2];       // java         
            String userClassFilePath = args[3];     // user1 (must be class file (user1.class) without file extension)
            int lastSlashIndex = userClassFilePath.lastIndexOf("/");
            String userClassFile = userClassFilePath.substring(lastSlashIndex+1);
            String userClassFileDir = userClassFilePath.substring(0, lastSlashIndex);
//            String outputFilesDirPath = args[4];    // docker-istvis sadacaa result.out, anu damauntebuli direqtoriidan rogor miagnos da ara realuri diskis path-idan.
            String taskTestsPath = args[4];         // /home/dato/dockerImages/tests

            String[] testsNames = args[5].split(","); 	// 01.in,02.in
            
        try {
            for (int i = 0; i < testsNames.length; i++) {
                String testFile = taskTestsPath + File.separator + testsNames[i];
                try {
                    executeCommandLine(userClassFileDir, 
                                        timeout, languageCommand, memLimit, userClassFile, testFile);
                }
                catch (ProgramException ex) {
                   processException(ex, testsNames[i]);
                }
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("AppRunner -> Done.");
    }

    public int executeCommandLine(String path,
                                    final long timeout, final String... commandLine)
                                        throws IOException, InterruptedException, ProgramException {
        ProgramException ex = new ProgramException();
        
        ProcessBuilder procBuilder = new ProcessBuilder(commandLine);
        procBuilder.directory(new File(path));
        Process process = procBuilder.start();
        
        /* Set up process I/O. */
        Worker worker = new Worker(process);
        worker.start();
        try {
            worker.join(timeout);
            if (worker.exit == null) {
                ex.key = ProgramException.ExceptionType.Timeout;
                throw ex;
            } else {
                int exitVal = process.exitValue();
                if (exitVal == 0) {
                    String testInputPath = commandLine[commandLine.length - 1];
                    String testOutputPath = testInputPath.substring(0, testInputPath.lastIndexOf(".")) + ".out";
                    String userOutputPath = path + File.separator + "result.out";
                    
                    System.out.println("userOutputPath: " + userOutputPath);
                    
                    checkOutputFile(testOutputPath, userOutputPath);
                    File userOutputFile = new File(userOutputPath);
                    if (userOutputFile.exists()){
                        userOutputFile.delete();
                    }
                } else { // become exception.
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    String errorContext="";
                    String strmLine;
                    while((strmLine = reader.readLine()) != null){
                        errorContext += strmLine;
                    }
                    if (errorContext.contains("java.lang.OutOfMemoryError")) {
                        ex.key = ProgramException.ExceptionType.OutOfMemory;
                    } else {
                        ex.key = ProgramException.ExceptionType.SomeRuntimeExc;
                        ex.message = errorContext;
                    }
                    throw ex;
                }
                return worker.exit;
            }
        } catch (InterruptedException e) {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            process.destroy();
        }
    }

    private void processException(ProgramException ex, String testName) {
        switch (ex.key) {
            case Timeout:
                printer.notifyResult(testName, ProgramException.ExceptionType.Timeout, "");
                break;
            case OutOfMemory:
                printer.notifyResult(testName, ProgramException.ExceptionType.OutOfMemory, "");
                break;
            case SomeRuntimeExc:
                printer.notifyResult(testName, ProgramException.ExceptionType.SomeRuntimeExc, ex.message);
                break;
            default:
                break;
        }
    }

    private void checkOutputFile(String testOutputFilePath, String userOutputFilePath) 
                                        throws ProgramException, IOException {

        String testName = testOutputFilePath.substring(testOutputFilePath.lastIndexOf("/") + 1, testOutputFilePath.lastIndexOf("."));
        
        List<String> realOutput = readOutput(testOutputFilePath);
        List<String> userOutput = readOutput(userOutputFilePath);
        
        System.out.println("realOutput.size: " + realOutput.size());
        System.out.println("userOutput.size: " + userOutput.size());
        
        if (realOutput.size() != userOutput.size()){
            String msg = "Number of rows in output file is incorrect";
            printer.notifyResult(testName, ProgramException.ExceptionType.TestFailed, msg);
        }
        else {
            List<LineVerdict> verdicts = compareLists(realOutput, userOutput);
            long count = verdicts.stream().filter((LineVerdict v) -> !v.isSame).count();
            if (count == 0){
                printer.notifyResult(testName, ProgramException.ExceptionType.NoError, "Success");
//                printer.printInfo("Pass test: " + testName);
            }
            else {
                printer.notifyResult(testName, ProgramException.ExceptionType.TestFailed, "Incorrect Output");
//                printer.printInfo("Fail test: " + testName);
//                printer.printLists(realOutput, userOutput);
            }
        }
    }
    
    
    private List<LineVerdict> compareLists(List<String> realOutput, List<String> userOutput) {
        List<LineVerdict> result = new ArrayList<>();
        for (int i = 0; i < realOutput.size(); i++) {
            String realEntry = realOutput.get(i);
            String userEntry = userOutput.get(i);
            LineVerdict verdict = new LineVerdict(realEntry.equals(userEntry), realEntry, userEntry);
            result.add(verdict);
        }
        return result;
    }
    
    private List<String> readOutput(String filePath) throws ProgramException, IOException {
        System.out.println("readOutput! filePath: " + filePath);
        
        File file = new File(filePath);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
            System.out.println("roca error: " + filePath);
            
            ProgramException e = new ProgramException();
            e.key = ProgramException.ExceptionType.SomeRuntimeExc;
            e.message = "Output file name must be - result.out";
            throw e;
        }
        ArrayList<String> lines = new ArrayList<>();
        String line;
        while((line = reader.readLine()) != null){
            lines.add(line.trim());
        }
        reader.close();
        return lines;
    }


    private static class Worker extends Thread {

        private final Process process;
        private Integer exit;

        private Worker(Process process) {
            this.process = process;
        }

        public void run() {
            try {
                exit = process.waitFor();
            } catch (InterruptedException ignore) {
                return;
            }
        }
    }

}
