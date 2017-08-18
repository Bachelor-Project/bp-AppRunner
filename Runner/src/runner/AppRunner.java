/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    
    public void run(String[] args) {
        // Command for docker:      -Xmx256m 2000 java ./codesData/users/dato/Money ./tasks/Money/tests 01.in,02.in
        // Command without docker:  -Xmx256m 2000 java /home/dato/Documents/project/codesData/users/Bob/java/Bob_9 /home/dato/Documents/project/tasks/Money/tests 01.in,02.in

        String memLimit = args[0];              // -Xmx1m
        long timeout = Long.parseLong(args[1]); // 2000 -> 2 seconds
        String languageCommand = args[2];       // java         

        String userClassFilePath = "";
        System.out.println("args[3]: " + args[3]);
        try {
            userClassFilePath = URLDecoder.decode(new String(args[3].getBytes(), "UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            String axali = new String(userClassFilePath.getBytes("iso-8859-1"), "UTF-8");
            System.out.println("axali: " + axali);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        System.out.println("userClassFilePath: " + userClassFilePath);
        
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
                    executeCommandLine(userClassFileDir, testFile,
                                        timeout, languageCommand, memLimit, userClassFile, "../../.." + testFile.substring(1));
                }
                catch (ProgramException ex) {
                   String testName = testFile.substring(testFile.lastIndexOf("/") + 1, testFile.lastIndexOf("."));
                   processException(ex, testName);
                }
            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int executeCommandLine(String path, String oldPath,
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
                    String testInputPath = oldPath; //commandLine[commandLine.length - 1];
                    String testOutputPath = testInputPath.substring(0, testInputPath.lastIndexOf(".")) + ".out";
                    String userOutputPath = path + File.separator + "result.out";
                    
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
                    if (errorContext.contains("java.lang.OutOfMemoryError") ||
                            errorContext.contains("MemoryError") ||
                            errorContext.contains("std::bad_alloc")) {
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
                printer.notifyResult(testName, ProgramException.ExceptionType.Timeout, "Timeout exception");
                break;
            case OutOfMemory:
                printer.notifyResult(testName, ProgramException.ExceptionType.OutOfMemory, "OutOfMemory exception");
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
        
        if (realOutput.size() != userOutput.size()){
            String msg = "Number of rows in output file is incorrect";
            printer.notifyResult(testName, ProgramException.ExceptionType.TestFailed, msg);
        }
        else {
            List<LineVerdict> verdicts = compareLists(realOutput, userOutput);
            long count = verdicts.stream().filter((LineVerdict v) -> !v.isSame).count();
            if (count == 0){
                printer.notifyResult(testName, ProgramException.ExceptionType.NoError, "Success");
            }
            else {
                printer.notifyResult(testName, ProgramException.ExceptionType.TestFailed, "Incorrect Output");
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
        File file = new File(filePath);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException ex) {
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

    /**
     * Python code runner method.
     * @param args 
     */
    public void runPython(String[] args) {
        // Params for docker container: "" 2000 python3 ./codesData/users/dato/Mem.py ./tasks/Money/tests 01.in,02.in
        // Params without docker:       "" 2000 python3 /home/dato/Documents/project/codesData/users/Bob/python/Bob_9.py /home/dato/Documents/project/tasks/Money/tests 01.in,02.in
        
        long timeout = Long.parseLong(args[1]);
        String languageCommand = args[2];
        String userClassFilePath = args[3];

        int lastSlashIndex = userClassFilePath.lastIndexOf("/");
        String userClassFile = userClassFilePath.substring(lastSlashIndex+1);
        String userClassFileDir = userClassFilePath.substring(0, lastSlashIndex);

        String taskTestsPath = args[4];
        String[] testsNames = args[5].split(",");
        
        for(int i = 0; i < testsNames.length; i++) {
            String testFile = taskTestsPath + File.separator + testsNames[i];
            try {
                executeCommandLine(userClassFileDir, testFile,
                                    timeout, languageCommand, userClassFile, "../../.." + testFile.substring(1));
            }
            catch (ProgramException ex) {
               String testName = testFile.substring(testFile.lastIndexOf("/") + 1, testFile.lastIndexOf("."));
               processException(ex, testName);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void runCpp(String[] args){
        // Params for docker container: 256 2000 g++ ./codesData/users/Bob/cpp/Money.out ./tasks/Money/tests 01.in,02.in
        // Params without docker:       256 2000 g++ /home/dato/Documents/project/codesData/users/Bob/cpp/a.out /home/dato/Documents/project/tasks/Money/tests 01.in,02.in
        
        System.out.println("------------- run cpp -------------");
        
        long timeout = Long.parseLong(args[1]);
        String languageCommand = args[2];
        String userClassFilePath = args[3];

        int lastSlashIndex = userClassFilePath.lastIndexOf("/");
        String userClassFile = userClassFilePath.substring(lastSlashIndex+1);
        String userClassFileDir = userClassFilePath.substring(0, lastSlashIndex);

        String taskTestsPath = args[4];
        String[] testsNames = args[5].split(",");
        
        for(int i = 0; i < testsNames.length; i++) {
            String testFile = taskTestsPath + File.separator + testsNames[i];
            try {
                executeCommandLine(userClassFileDir, testFile,
                                    timeout, "./"+userClassFile, testFile);
            }
            catch (ProgramException ex) {
               String testName = testFile.substring(testFile.lastIndexOf("/") + 1, testFile.lastIndexOf("."));
               processException(ex, testName);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(AppRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
