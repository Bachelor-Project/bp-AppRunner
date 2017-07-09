/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dato
 */
public class WriterCLass {
    
    public WriterCLass(){
        
        File f = new File("b.out");
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write("rame".getBytes());
            out.flush();
            out.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Runner.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
