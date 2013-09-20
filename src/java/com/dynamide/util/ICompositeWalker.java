package com.dynamide.util;

/** Implement this interface to receive a callback on each visit of a Composite node in the tree of nodes
 *  owned by Composite, during one of the walks
 * ({@link Composite#walkPreOrder walkPreOrder} and {@link Composite#walkPostOrder walkPostOrder})
 * provided by Composite.
 */
public interface ICompositeWalker {
    public static final int PREORDER = 1;
    public static final int POSTORDER = 2;

    /** @return true if you wish the Composite walk to continue for child nodes in a PREORDER walk;
     *   POSTORDER walks are unaffected, of course, since by the time the parent node is visited, it
     * is too late, as the children are visited first; there is no way to skip visiting subsequent children.
     */
    public boolean onNode(IComposite node);
}

