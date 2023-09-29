package Frontend.Parser;

import Frontend.Grammar.BNFSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Tree<Integer, T> {

    private final Integer head;
    private final Integer maxLeavesNb;
    private final ArrayList<Tree<Integer, T>> leaves = new ArrayList<Tree<Integer, T>>();
    private Tree<Integer, T> parent = null;
    private T data;
    private HashMap<Integer, Tree<Integer, T>> locate = new HashMap<Integer, Tree<Integer, T>>();

    public Tree(Integer head, T data, Integer maxLeavesNb) {
        this.head = head;
        this.data = data;
        this.maxLeavesNb = maxLeavesNb;
        locate.put(head, this);
    }

    public Tree(Integer head, T data) {
        this(head, data, null);
    }

    public void addNode(Integer root, Integer leaf, T data, Integer maxLeavesNb) {
        if (locate.containsKey(root)) {
            locate.get(root).addNode(leaf, data, maxLeavesNb);
        } else {
            addNode(root, data, maxLeavesNb).addNode(leaf, data, maxLeavesNb);
        }
    }

    public Tree<Integer, T> addNode(Integer leaf, T data, Integer maxLeavesNb) {
        Tree<Integer, T> t = new Tree<Integer, T>(leaf, data, maxLeavesNb);
        leaves.add(t);
        t.parent = this;
        t.locate = this.locate;
        locate.put(leaf, t);
        return t;
    }

    public Integer getHead() {
        return head;
    }

    public Tree<Integer, T> getTree(Integer element) {
        return locate.get(element);
    }

    public Tree<Integer, T> getParent() {
        return parent;
    }

    public void setParent(Tree<Integer, T> parent) {
        this.parent = parent;
    }

    public int getNumberOfLeaves() {
        return leaves.size();
    }
    public Integer getMaxLeavesNb() {
        return maxLeavesNb;
    }

    @Override
    public String toString() {
        return printTree(0);
    }

    private static final int indent = 2;

    private String printTree(int increment) {
        StringBuilder s = new StringBuilder();
        if (data instanceof BNFSymbol) {
            s = new StringBuilder(" ".repeat(Math.max(0, increment)) + ((BNFSymbol) data).getName());
        } else {
            s = new StringBuilder(" ".repeat(Math.max(0, increment)) + head);
        }
        for (Tree<Integer, T> child : leaves) {
            s.append("\n").append(child.printTree(increment + indent));
        }
        return s.toString();
    }

    public T getData() {
        return data;
    }

    public ArrayList<Tree<Integer, T>> getLeaves() {
        return leaves;
    }

    public void setLeaves(List<Tree<Integer, T>> leaves) {
        this.leaves.clear();
        for (Tree<Integer, T> leaf : leaves) {
            leaf.setParent(this);
        }
        this.leaves.addAll(leaves);
    }
}