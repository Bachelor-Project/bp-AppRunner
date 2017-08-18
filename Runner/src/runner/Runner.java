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
public class Runner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        AppRunner appRunner = new AppRunner(new MessagePrinter());
        
        String languageRunner = args[2];
        if (languageRunner.contains("python")){
            appRunner.runPython(args);
        }
        else if (languageRunner.contains("java")){
            appRunner.run(args);
        }
        else if (languageRunner.contains("g++")){
            appRunner.runCpp(args);
        }
    }

    private static void runProcessWithDocker() {
        RunProcess runer = new RunProcess();
        String runDockerImage = "sudo -S docker run --rm -v /home/dato/dockerImages:/test -w /test oracle-java";
        String runUserCode = "java -jar AppRunner -Xmx1m 2000 java Memory /home/dato/dockerImages/tests 01,02";

//        runer.run(new String[]{"/bin/bash","-c","echo albatrosi0289 | sudo -S ls"});
        runer.run(new String[]{"/bin/bash","-c","echo albatrosi0289 | " + runDockerImage + " " + runUserCode});

    }

    

    private static void makeFile() {
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
