package com.dynamide.util;


/*
*/

/** This class compiles but doesn't work.  You'd have to set some things up so that it can find the javac task,
 *   probably just missing tools.jar.
 */
public class AntRunnerJavac {
    static final String ALL_JAVA_FILES = "**/*.java" ;
/*
    public void go() {
        AntRunner run = new AntRunner();
        Task task = run.setupTask("javac");
        Javac javac = (Javac)task;
        Path fromHere = new Path(run.getProject());
        fromHere.setLocation ( new File(".") );
        File toHere = new File(".");
        // class path stuff
        Path antClassPath = new Path(run.getProject()) ;
        Path p = antClassPath.concatSystemClasspath() ;
        antClassPath.append( p ) ;
        javac.setClasspath( antClassPath ) ;
        javac.setDestdir( toHere ) ;
        javac.setSrcdir( fromHere ) ;
        //javac.setExcludes( ALL_JAVA_FILES ) ;
        javac.setIncludes( ALL_JAVA_FILES ) ;
        run.runTask();
   }

   public static void main( String[] args )
   {
      AntRunnerJavac code = new AntRunnerJavac() ;
      code.go() ;
    }
*/
}
