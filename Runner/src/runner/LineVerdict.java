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
public class LineVerdict {
    
    public boolean isSame;
    public String realLine;
    public String userLine;
    
    public LineVerdict(boolean isSame, String realLine, String userLine){
        this.isSame = isSame;
        this.realLine = realLine;
        this.userLine = userLine;
    }

    @Override
    public String toString() {
        return "LineVerdict{" + "verdict=" + isSame + ", realLine=" + realLine + ", userLine=" + userLine + '}';
    }
          
}
