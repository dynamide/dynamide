package com.dynamide.event;

public class ErrorHandlerResult {

    public String displayMessage = "";

    public String source = "";

    /** A valid code for "action" member -- if action == RETURN_SOURCE,
     *  ErrorHandlerResult.source will be substituted for the invalid page or widget object's output.
     *  If you wish to jump to another page, use the jumpToPage method of the ScriptEvent.
     */
    public static final int RETURN_SOURCE = 1;

    /** A valid code for "action" member -- if action == RETURN_EMPTY,
     *  the invalid page or widget object's output will be replaced by the empty string.
     */
    public static final int RETURN_EMPTY = 2;

    /** A valid code for "action" member -- if action == UNHANDLED,
     *  Dynamide will show the normal chain of error pages.
     */
    public static final int UNHANDLED = 3;

    public int action = UNHANDLED;

}

