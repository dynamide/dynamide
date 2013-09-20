package com.dynamide.util;

/*

*/
//see: C:\bin\jakarta-ant-1.4.1\docs\manual\api\index.html
public class CVS {

/*
    private boolean m_noExecMode = false;
    public boolean getNoExecMode(){return m_noExecMode;}
    public void setNoExecMode(boolean new_value){m_noExecMode = new_value;}

    private String  m_cvsRoot = "";
    public String  getCvsRoot(){return m_cvsRoot;}
    public void setCvsRoot(String  new_value){m_cvsRoot = new_value;}

    private String m_baseDirname = "";
    public String getBaseDirname(){return m_baseDirname;}
    public void setBaseDirname(String new_value){m_baseDirname = new_value;}



    public String doCommand(String command){
        AntRunner run = new AntRunner();
        run.setProjectBaseDir(new File(m_baseDirname));
        Task task = run.setupTask("cvs");
        Cvs cvs = (Cvs)task;
        cvs.setCvsRoot(m_cvsRoot);
        cvs.setCommand(command);
        cvs.setNoexec(m_noExecMode);
        cvs.setPort(2401);
        run.runTask();
        return "success";
    }

    public static void main( String[] args ){
        try {
            CVS cvs = new CVS() ;
            cvs.setNoExecMode(true);
            //Works using local file repository
            //cvs.setCvsRoot(Tools.fixFilename("C:/temp/cvsroot.lar"));//win32, but didn't work.
            //cvs.setCvsRoot(":pserver:laramie@localhost:/c//temp/cvsroot.lar");
            cvs.setCvsRoot(":pserver:laramie@localhost:\\C\\\\temp\\cvsroot.lar");
            //cvs -d :pserver:laramie@localhost:\C\\temp\cvsroot.lar login
            cvs.setBaseDirname("C:/java/com/dynamide"); //works from bash
            cvs.doCommand("update");
            cvs.setNoExecMode(false);
            cvs.doCommand("add conf");
            cvs.doCommand("add conf/*.xml");
            cvs.setNoExecMode(true);
            cvs.doCommand("commit conf");
        } catch (Exception e){
            System.out.println("Test failed: "+e);
            e.printStackTrace();
        }
    }


 */
}
