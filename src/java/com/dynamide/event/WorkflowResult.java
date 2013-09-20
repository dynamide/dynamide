package com.dynamide.event;

public class WorkflowResult {
    /*
     *  private int action = WFA_CONTINUE;
        public int getAction(){return action;}
        public void setAction(int new_value){action = new_value;}

            Use this if you DON'T wish to override processing for this object, and continue with more events and workflow.
        public static final int WFA_CONTINUE = 1;
        /** Use this if you wish to override processing for this object, but continue with more events and workflow.
        public static final int WFA_CONTINUE_SKIP_THIS = 2;
        public static final int WFA_CONTINUE_SKIP_CHILDREN = 3;
        public static final int WFA_CONTINUE_SKIP_SIBLINGS = 4;
        /** Use this if you DON'T wish to override processing for this object, but don't wish to stop events and workflow.
        public static final int WFA_STOP_KEEP_THIS= 5;
        /** Use this if you wish to override processing for this object, and STOP events and workflow.
        public static final int WFA_STOP_SKIP_THIS = 6;
        public static final int WFA_ABORT = 7;
     *
     */

    public boolean doDefaultForThis = true;
    public boolean getDoDefaultForThis(){ return doDefaultForThis;}
    public void setDoDefaultForThis(boolean new_doDefaultForThis){ doDefaultForThis = new_doDefaultForThis;}

    public boolean skipChildren = false;
    public boolean getSkipChildren(){ return skipChildren;}
    public void setSkipChildren(boolean new_skipChildren){ skipChildren = new_skipChildren;}

    public boolean skipSiblings = false;
    public boolean getSkipSiblings(){ return skipSiblings;}
    public void setSkipSiblings(boolean new_skipSiblings){ skipSiblings = new_skipSiblings;}

    public boolean abort = false;
    public boolean getAbort(){ return abort;}
    public void setAbort(boolean new_abort){ abort = new_abort;}

    public String message = "";
    public String getMessage(){return message;}
    public void setMessage(String new_value){message = new_value;}

    private StringBuffer statusMessage = new StringBuffer();
    public String getStatusMessage(){return statusMessage.toString();}
    public void setStatusMessage(String new_value){statusMessage = new StringBuffer(new_value);}
    public void addStatusMessage(String s){statusMessage.append(s);}
}

