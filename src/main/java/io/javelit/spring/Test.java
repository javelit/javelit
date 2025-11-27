/*
 * Copyright © 2025 Cyril de Catheu (cdecatheu@hey.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javelit.spring;

public class Test {

  public static class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode() {
    }

    TreeNode(int val) {
      this.val = val;
    }

    TreeNode(int val, TreeNode left, TreeNode right) {
      this.val = val;
      this.left = left;
      this.right = right;
    }
  }

  public static void main(String[] args) {
    var root = new TreeNode(2147483647, null, null);
    var res = isValidBST(root, Integer.MIN_VALUE, Integer.MAX_VALUE);
    System.out.println(res);
  }

  private static boolean isValidBST(TreeNode root, int min, int max) {
    if (root == null) {
      return true;
    }
    if (root.val > min && root.val < max) {
      return isValidBST(root.left, min, root.val) && isValidBST(root.right, root.val, max);
    }
    return false;
  }
}
