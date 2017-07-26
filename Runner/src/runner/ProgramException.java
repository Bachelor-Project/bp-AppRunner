/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

/**
 *
 * @author dato
 */
public class ProgramException extends Exception {
    
    ExceptionType key;
    String message = "";
    
    public static enum ExceptionType {
        Timeout,
        OutOfMemory,
        SomeRuntimeExc,
        TestFailed,
        NoError;
    }
}
