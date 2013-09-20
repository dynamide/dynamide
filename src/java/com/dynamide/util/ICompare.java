// Interface for sorting callback in com.dynamide.util.SortVector.

package com.dynamide.util;

public interface ICompare {
  boolean lessThan(Object lhs, Object rhs);
  boolean lessThanOrEqual(Object lhs, Object rhs);
}

