package com.dynamide.util;

/*

*/
/**  Run any ant task from Java.
 *   Any java class can call this utility class and add specific Task initialization.
 *   The steps are:
 *   <ul>
 *   <li>Create AntRunner
 *   <li>Call setupTask(taskname)
 *   <li>Perform any Task subclass-specific initialization that makes your Task do it's job
 *   <li>Tell the runner to run the task.
 *   </ul>
 *   Example:
 *   <pre>
        AntRunner run = new AntRunner() ;
        Task task = run.setupTask("cvs") ;
        Cvs cvs = (Cvs)task;
        cvs.setCommand("update");
        cvs.setNoexec(true);
        run.runTask() ;
 *   </pre>
 */
public class AntRunner {
/*    static final String PROJECT_NAME   = "TheProject" ;
    static final String COMPILER_NAME  = "TheCompiler" ;
    static final String TARGET_NAME    = "TheTarget" ;

    private Project m_antProject = null;
    public Project getProject(){return m_antProject;}

    private File m_projectBaseDir = null;
    public File getProjectBaseDir(){return m_projectBaseDir;}
    public void setProjectBaseDir(File  new_value){m_projectBaseDir = new_value;}

    private Target m_antTarget = null;
    public Target getAntTarget(){return m_antTarget;}

    private Task m_task = null;
    public Task getTask(){return m_task;}

    private int m_messageOutputLevel = Project.MSG_INFO;
    public int getMessageOutputLevel(){return m_messageOutputLevel;}
    public void setMessageOutputLevel(int new_value){m_messageOutputLevel = new_value;}

    public Task setupTask(String taskName) {
        m_antProject   = new Project() ;
        m_antTarget    = new Target() ;
        // name them
        m_antProject.setName(PROJECT_NAME);
        DefaultLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(m_messageOutputLevel);
        m_antProject.addBuildListener(logger);
        if (m_projectBaseDir != null){
            m_antProject.setBaseDir(m_projectBaseDir);
        }
        m_antProject.init();
        m_antTarget.setName(TARGET_NAME) ;
        m_task = m_antProject.createTask(taskName);
        m_task.setOwningTarget(m_antTarget);
        m_antTarget.addTask(m_task);
        m_antProject.setDefaultTarget(TARGET_NAME);
        m_task.init();
        // link them together
        m_antTarget.setProject(m_antProject);
        m_antProject.addTarget(TARGET_NAME, m_antTarget);
        return m_task;
    }
  */
    /** You may call this after performing configuration on you specific Task.  setupTask
     *  creates a Task object, of the type you passed in, but doesn't set any of the properties
     *  that are specific to that task.  The properties mostly map directly to the attributes
     *  and child nodes of the  XML task.
     */
/*    public void runTask(){
        m_antProject.executeTarget(TARGET_NAME) ;
    }

    public static void main( String[] args ){
        AntRunner code = new AntRunner() ;
        Task task = code.setupTask("cvs") ;
        //for testing.  Normally the caller (any class) would perform these steps.
        Cvs cvs = (Cvs)task;
        cvs.setCommand("update");
        cvs.setNoexec(true);
        code.runTask() ;
    }
*/
}
