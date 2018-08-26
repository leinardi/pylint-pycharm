/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leinardi.pycharm.pylint.toolwindow;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.List;

/**
 * Tree node with togglable visibility.
 */
public class TogglableTreeNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = -4490734768175672868L;

    private boolean visible = true;

    public TogglableTreeNode() {
    }

    public TogglableTreeNode(final Object userObject) {
        super(userObject);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    @SuppressWarnings("unchecked")
    List<TogglableTreeNode> getAllChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public TreeNode getChildAt(final int index) {
        int realIndex = -1;
        int visibleIndex = -1;

        for (final Object child : children) {
            final TogglableTreeNode node = (TogglableTreeNode) child;
            if (node.isVisible()) {
                ++visibleIndex;
            }
            ++realIndex;
            if (visibleIndex == index) {
                return (TreeNode) children.get(realIndex);
            }
        }

        throw new ArrayIndexOutOfBoundsException("Invalid index: " + index);
    }

    @Override
    public int getChildCount() {
        if (children == null) {
            return 0;
        }

        int count = 0;
        for (final Object child : children) {
            final TogglableTreeNode node = (TogglableTreeNode) child;
            if (node.isVisible()) {
                ++count;
            }
        }

        return count;
    }
}
